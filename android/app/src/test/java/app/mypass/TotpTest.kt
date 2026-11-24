package app.mypass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TotpTest {
    @Test
    fun generatesTheRfc6238Sha1Vector() {
        val config = Totp.parse("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")?.copy(digits = 8)

        assertEquals("94287082", config?.let { Totp.code(it, 59_000L) })
    }

    @Test
    fun parsesOtpauthUriAndRawSecrets() {
        val uri = Totp.parse(
            "otpauth://totp/My%20Pass:edgar?secret=JBSWY3DPEHPK3PXP&digits=6&period=30&algorithm=SHA1",
        )
        assertEquals("JBSWY3DPEHPK3PXP", uri?.secret)
        assertEquals(6, uri?.digits)
        assertEquals(30, uri?.period)

        assertNull(Totp.parse("no es base32!"))
        assertEquals("JBSWY3DPEHPK3PXP", Totp.parse("jbswy3dp ehpk3pxp")?.secret)
    }

    @Test
    fun parsesIssuerAndAccountFromOtpauthUri() {
        val parsed = Totp.parseUri(
            "otpauth://totp/Google:persona@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=Google",
        )

        assertEquals("Google", parsed?.issuer)
        assertEquals("persona@gmail.com", parsed?.account)
        assertEquals("JBSWY3DPEHPK3PXP", parsed?.config?.secret)
    }

    @Test
    fun countsDownWithinThePeriod() {
        assertEquals(30, Totp.secondsLeft(30, 0L))
        assertEquals(1, Totp.secondsLeft(30, 29_000L))
    }
}
