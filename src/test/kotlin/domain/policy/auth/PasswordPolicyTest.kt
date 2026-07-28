package com.simbiri.domain.policy.auth

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PasswordPolicyTest {

    @Test
    fun `accepts password containing fifteen code points`() {
        val password = "123456789012345".toCharArray()

        try {
            assertNull(
                PasswordPolicy.validateForCredentialCreation(
                        password
                    )
            )
        } finally {
            password.fill('\u0000')
        }
    }

    @Test
    fun `rejects password shorter than fifteen code points`() {
        val password = "too-short-pass".toCharArray()

        try {
            assertNotNull(
                PasswordPolicy.validateForCredentialCreation(
                        password
                    )
            )
        } finally {
            password.fill('\u0000')
        }
    }

    @Test
    fun `counts surrogate pairs as one Unicode code point`() {
        /*
         * Each emoji occupies two UTF-16 Char values but represents one
         * Unicode code point.
         */
        val password = "😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀".toCharArray()

        try {
            assertNull(
                PasswordPolicy.validateForCredentialCreation(
                        password
                    )
            )
        } finally {
            password.fill('\u0000')
        }
    }

    @Test
    fun `rejects password longer than maximum`() {
        val password = CharArray(
            PasswordPolicy.MAXIMUM_CODE_POINTS + 1
        ) {
            'a'
        }

        try {
            assertNotNull(
                PasswordPolicy.validateForCredentialCreation(
                        password
                    )
            )
        } finally {
            password.fill('\u0000')
        }
    }

    @Test
    fun `rejects password containing only whitespace`() {
        val password = CharArray(15) {
            ' '
        }

        try {
            assertNotNull(
                PasswordPolicy.validateForCredentialCreation(
                        password
                    )
            )
        } finally {
            password.fill('\u0000')
        }
    }
}
