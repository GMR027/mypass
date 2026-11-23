package app.mypass

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.AEAD
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.PwHash
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom

object Crypto {
    private val nativeSodium = SodiumAndroid()
    private val sodium = LazySodiumAndroid(nativeSodium)
    private val random = SecureRandom()

    const val KEY_BYTES = 32
    const val NPUB_BYTES = AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES
    const val ABYTES = AEAD.XCHACHA20POLY1305_IETF_ABYTES
    const val CHUNK_SIZE = 1024 * 1024
    const val HEADER_SIZE = 4 + 1 + 4 + 8 + 16
    val PIN_OPS: Long = PwHash.OPSLIMIT_INTERACTIVE
    val PIN_MEM: Long = PwHash.MEMLIMIT_INTERACTIVE.toLong()

    private val MAGIC = byteArrayOf(0x4d, 0x59, 0x50, 0x44) // MYPD
    private const val VERSION: Byte = 1

    fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        return bytes
    }

    fun argon2id(password: String, salt: ByteArray): ByteArray {
        val output = ByteArray(32)
        val ok = sodium.cryptoPwHash(
            output,
            output.size,
            password.toByteArray(Charsets.UTF_8),
            password.toByteArray(Charsets.UTF_8).size,
            salt,
            PwHash.OPSLIMIT_INTERACTIVE,
            PwHash.MEMLIMIT_INTERACTIVE,
            PwHash.Alg.PWHASH_ALG_ARGON2ID13,
        )

        check(ok) { "No se pudo derivar la llave del PIN." }

        return output
    }

    fun seal(key: ByteArray, plaintext: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        val nonce = randomBytes(NPUB_BYTES)
        val ciphertext = ByteArray(plaintext.size + ABYTES)
        val ciphertextLength = longArrayOf(0)

        val ok = sodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(
            ciphertext,
            ciphertextLength,
            plaintext,
            plaintext.size.toLong(),
            associatedData,
            associatedData.size.toLong(),
            null,
            nonce,
            key,
        )

        check(ok) { "No se pudo cifrar el contenido." }

        return nonce + ciphertext.copyOf(ciphertextLength[0].toInt())
    }

    fun open(key: ByteArray, sealed: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        check(sealed.size > NPUB_BYTES + ABYTES) { "El contenido cifrado está incompleto." }

        val nonce = sealed.copyOfRange(0, NPUB_BYTES)
        val ciphertext = sealed.copyOfRange(NPUB_BYTES, sealed.size)
        val plaintext = ByteArray(ciphertext.size - ABYTES)
        val plaintextLength = longArrayOf(0)

        val ok = sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
            plaintext,
            plaintextLength,
            null,
            ciphertext,
            ciphertext.size.toLong(),
            associatedData,
            associatedData.size.toLong(),
            nonce,
            key,
        )

        check(ok) { "No se pudo descifrar el contenido." }

        return plaintext.copyOf(plaintextLength[0].toInt())
    }

    fun encryptFile(key: ByteArray, input: ByteArray): ByteArray {
        val prefix = randomBytes(16)
        val header = ByteArray(HEADER_SIZE)
        header[0] = MAGIC[0]
        header[1] = MAGIC[1]
        header[2] = MAGIC[2]
        header[3] = MAGIC[3]
        header[4] = VERSION
        putInt(header, 5, CHUNK_SIZE)
        putLong(header, 9, input.size.toLong())
        System.arraycopy(prefix, 0, header, 17, 16)

        val output = ByteArrayOutputStream()
        output.write(header)

        var offset = 0
        var counter = 0

        while (offset < input.size) {
            val end = minOf(offset + CHUNK_SIZE, input.size)
            val chunk = input.copyOfRange(offset, end)
            val nonce = makeNonce(prefix, counter)
            val ciphertext = ByteArray(chunk.size + ABYTES)
            val ciphertextLength = longArrayOf(0)

            val ok = sodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(
                ciphertext,
                ciphertextLength,
                chunk,
                chunk.size.toLong(),
                header,
                header.size.toLong(),
                null,
                nonce,
                key,
            )
            check(ok) { "No se pudo cifrar el archivo." }

            val lengthBytes = ByteArray(4)
            putInt(lengthBytes, 0, ciphertextLength[0].toInt())
            output.write(lengthBytes)
            output.write(ciphertext, 0, ciphertextLength[0].toInt())

            offset = end
            counter += 1
        }

        return output.toByteArray()
    }

    fun decryptFile(key: ByteArray, input: ByteArray): ByteArray {
        check(input.size >= HEADER_SIZE) { "El archivo está incompleto." }
        check(input[0] == MAGIC[0] && input[1] == MAGIC[1] && input[2] == MAGIC[2] && input[3] == MAGIC[3]) {
            "El archivo no tiene el formato de My Pass."
        }
        check(input[4] == VERSION) { "La versión del archivo no es compatible." }

        val header = input.copyOfRange(0, HEADER_SIZE)
        val chunkSize = getInt(header, 5)
        val totalSize = getLong(header, 9)
        check(chunkSize in 1..(16 * 1024 * 1024)) { "El tamaño de bloque no es válido." }
        val prefix = header.copyOfRange(17, 33)

        val output = ByteArrayOutputStream()
        var offset = HEADER_SIZE
        var counter = 0
        var written = 0L

        while (offset < input.size) {
            check(offset + 4 <= input.size) { "El archivo está truncado." }
            val length = getInt(input, offset)
            offset += 4
            check(length >= ABYTES && offset + length <= input.size) { "El archivo está dañado." }

            val ciphertext = input.copyOfRange(offset, offset + length)
            offset += length
            val plaintext = ByteArray(length - ABYTES)
            val plaintextLength = longArrayOf(0)
            val nonce = makeNonce(prefix, counter)

            val ok = sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
                plaintext,
                plaintextLength,
                null,
                ciphertext,
                ciphertext.size.toLong(),
                header,
                header.size.toLong(),
                nonce,
                key,
            )
            check(ok) { "El archivo está dañado o la llave no es correcta." }

            output.write(plaintext, 0, plaintextLength[0].toInt())
            written += plaintextLength[0]
            counter += 1
        }

        check(written == totalSize) { "El archivo está incompleto." }

        return output.toByteArray()
    }

    fun boxSeal(message: ByteArray, publicKey: ByteArray): ByteArray {
        val output = ByteArray(message.size + Box.SEALBYTES)
        val result = sodium.cryptoBoxSeal(
            output,
            message,
            message.size.toLong(),
            publicKey,
        )
        check(result) { "No se pudo cifrar para el contacto." }

        return output
    }

    fun boxSealOpen(sealed: ByteArray, publicKey: ByteArray, privateKey: ByteArray): ByteArray {
        val output = ByteArray(sealed.size - Box.SEALBYTES)
        val result = sodium.cryptoBoxSealOpen(
            output,
            sealed,
            sealed.size.toLong(),
            publicKey,
            privateKey,
        )
        check(result) { "No se pudo descifrar el envío." }

        return output
    }

    fun selfTest(): Boolean {
        val publicKey = ByteArray(32)
        val secretKey = ByteArray(32)
        nativeSodium.crypto_box_keypair(publicKey, secretKey)
        val message = "mypass self test".toByteArray(Charsets.UTF_8)
        val sealed = boxSeal(message, publicKey)
        val opened = boxSealOpen(sealed, publicKey, secretKey)

        return opened.contentEquals(message)
    }

    fun memzero(bytes: ByteArray?) {
        bytes?.fill(0)
    }

    private fun makeNonce(prefix: ByteArray, counter: Int): ByteArray {
        val nonce = ByteArray(NPUB_BYTES)
        System.arraycopy(prefix, 0, nonce, 0, 16)
        nonce[16] = (counter and 0xff).toByte()
        nonce[17] = ((counter ushr 8) and 0xff).toByte()
        nonce[18] = ((counter ushr 16) and 0xff).toByte()
        nonce[19] = ((counter ushr 24) and 0xff).toByte()
        return nonce
    }

    private fun putInt(target: ByteArray, offset: Int, value: Int) {
        ByteBuffer.wrap(target, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value)
    }

    private fun putLong(target: ByteArray, offset: Int, value: Long) {
        ByteBuffer.wrap(target, offset, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(value)
    }

    private fun getInt(source: ByteArray, offset: Int): Int {
        return ByteBuffer.wrap(source, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun getLong(source: ByteArray, offset: Int): Long {
        return ByteBuffer.wrap(source, offset, 8).order(ByteOrder.LITTLE_ENDIAN).long
    }
}
