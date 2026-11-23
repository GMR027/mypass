package app.mypass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {
    @Test
    fun generatedPasswordsHaveTheRequestedLengthAndCharacterGroups() {
        val password = PasswordGenerator.generate(24)

        assertEquals(24, password.length)
        assertTrue(password.any(Char::isLowerCase))
        assertTrue(password.any(Char::isUpperCase))
        assertTrue(password.any(Char::isDigit))
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun strengthGrowsWithLengthAndVariety() {
        assertTrue(PasswordGenerator.strength("abc") < PasswordGenerator.strength("Abc123!@#LargaClave2026"))
    }
}
