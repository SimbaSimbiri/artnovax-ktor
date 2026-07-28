package com.simbiri.data.security

import com.simbiri.domain.model.auth.PasswordHashAlgorithm
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Argon2PasswordHasherTest {

    private val passwordHasher = Argon2PasswordHasher()

    @Test
    fun `hash verifies correct password and rejects incorrect password`() = runBlocking {
        val correctPassword = "correct-horse-battery-staple".toCharArray()

        val incorrectPassword = "incorrect-password".toCharArray()

        try {
            val encodedHash = passwordHasher.hash(
                correctPassword
            )

            assertTrue(
                encodedHash.startsWith(
                    prefix = "\$argon2id\$"
                )
            )

            assertTrue(
                passwordHasher.verify(
                    password = correctPassword,
                    encodedHash = encodedHash,
                )
            )

            assertFalse(
                passwordHasher.verify(
                    password = incorrectPassword,
                    encodedHash = encodedHash,
                )
            )

            assertEquals(passwordHasher.algorithm, PasswordHashAlgorithm.ARGON2ID)
        } finally {
            correctPassword.fill('\u0000')
            incorrectPassword.fill('\u0000')
        }
    }

    @Test
    fun `hashing same password produces independently salted hashes`() = runBlocking {
        val password = "same-password".toCharArray()

        try {
            val firstHash = passwordHasher.hash(password)

            val secondHash = passwordHasher.hash(password)

            assertNotEquals(
                illegal = firstHash,
                actual = secondHash,
            )

            assertTrue(
                passwordHasher.verify(
                    password = password,
                    encodedHash = firstHash,
                )
            )

            assertTrue(
                passwordHasher.verify(
                    password = password,
                    encodedHash = secondHash,
                )
            )
        } finally {
            password.fill('\u0000')
        }
    }
}
