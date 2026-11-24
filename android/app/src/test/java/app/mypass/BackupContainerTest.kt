package app.mypass

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupContainerTest {
    @Test
    fun zipRoundTripKeepsEntries() {
        val entries = mapOf(
            "manifest.json" to "{\"format\":1}".toByteArray(),
            "files/one.bin" to ByteArray(2048) { (it % 251).toByte() },
        )

        val restored = BackupContainer.unzip(BackupContainer.zip(entries))

        assertEquals(entries.keys, restored.keys)
        assertArrayEquals(entries["manifest.json"], restored["manifest.json"])
        assertArrayEquals(entries["files/one.bin"], restored["files/one.bin"])
    }
}
