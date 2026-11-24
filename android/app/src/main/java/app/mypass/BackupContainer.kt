package app.mypass

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupContainer {
    private val MAGIC = byteArrayOf(0x4d, 0x59, 0x50, 0x42) // MYPB
    private const val VERSION: Byte = 1
    private const val KDF_ARGON2ID: Byte = 1
    private const val MAX_ENTRY_BYTES = 200L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024

    fun encrypt(passphrase: String, plaintext: ByteArray): ByteArray {
        val salt = Crypto.randomBytes(16)
        val ops = Crypto.PIN_OPS
        val mem = Crypto.PIN_MEM
        val key = Crypto.argon2id(passphrase, salt, ops, mem)
        return try {
            val header = ByteBuffer.allocate(30)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(MAGIC)
                .put(VERSION)
                .put(KDF_ARGON2ID)
                .putInt(ops.toInt())
                .putInt(mem.toInt())
                .put(salt)
                .array()

            val encrypted = Crypto.encryptFile(key, plaintext)
            header + encrypted
        } finally {
            Crypto.memzero(key)
        }
    }

    fun decrypt(passphrase: String, container: ByteArray): ByteArray {
        check(container.size > 30) { "La copia de seguridad está incompleta." }
        val buffer = ByteBuffer.wrap(container, 0, 30).order(ByteOrder.LITTLE_ENDIAN)
        val magic = ByteArray(4).also(buffer::get)
        check(magic.contentEquals(MAGIC)) { "El archivo no es una copia de My Pass." }
        val version = buffer.get()
        check(version == VERSION) { "La versión de la copia no es compatible." }
        val kdf = buffer.get()
        check(kdf == KDF_ARGON2ID) { "La copia usa un cifrado no compatible." }
        val ops = buffer.int.toLong() and 0xffffffffL
        val mem = buffer.int.toLong() and 0xffffffffL
        val salt = ByteArray(16).also(buffer::get)
        check(ops in 1..20L && mem in 8L..1024L * 1024 * 1024) { "Los parámetros de la copia no son válidos." }

        val key = Crypto.argon2id(passphrase, salt, ops, mem)
        return try {
            Crypto.decryptFile(key, container.copyOfRange(30, container.size))
        } finally {
            Crypto.memzero(key)
        }
    }

    fun zip(entries: Map<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            for ((name, bytes) in entries) {
                val entry = ZipEntry(name)
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                var entryTotal = 0L
                while (true) {
                    val read = zip.read(buffer)
                    if (read <= 0) {
                        break
                    }
                    entryTotal += read
                    total += read
                    check(entryTotal <= MAX_ENTRY_BYTES) { "Un archivo de la copia es demasiado grande." }
                    check(total <= MAX_TOTAL_BYTES) { "La copia de seguridad es demasiado grande." }
                    output.write(buffer, 0, read)
                }
                entries[entry.name] = output.toByteArray()
                zip.closeEntry()
            }
        }
        return entries
    }
}
