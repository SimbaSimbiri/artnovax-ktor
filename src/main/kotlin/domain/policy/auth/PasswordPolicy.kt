package com.simbiri.domain.policy.auth

import com.simbiri.domain.util.DataError

/**
 * Validates plaintext passwords before hashing.
 *
 * Since we use passwords as a single authentication factor,
 * minimum required length is fifteen Unicode code points.
 *
 * No uppercase, lowercase, number, or symbol composition requirements are
 * imposed.
 */
object PasswordPolicy {

    fun validateForCredentialCreation(
        password: CharArray,
    ): DataError.ValidationError? {
        val codePointCount = countUnicodeCodePointsOrNull(password) ?: return validationError(
            reason = "Password contains malformed Unicode."
        )

        if (codePointCount < MINIMUM_CODE_POINTS) {
            return validationError(
                reason = "Password must contain at least $MINIMUM_CODE_POINTS characters."
            )
        }

        if (codePointCount > MAXIMUM_CODE_POINTS) {
            return validationError(
                reason = "Password must not exceed $MAXIMUM_CODE_POINTS characters."
            )
        }

        if (password.all(Char::isWhitespace)) {
            return validationError(
                reason = "Password must not consist entirely of whitespace."
            )
        }

        return null
    }

    /**
     * Counts Unicode code points without converting password material into
     * an immutable String.
     *
     * Null indicates an unmatched UTF-16 surrogate.
     */
    private fun countUnicodeCodePointsOrNull(
        password: CharArray,
    ): Int? {
        var index = 0
        var codePointCount = 0

        while (index < password.size) {
            val currentCharacter = password[index]

            when {
                Character.isHighSurrogate(
                    currentCharacter
                ) -> {
                    val nextIndex = index + 1

                    if (nextIndex >= password.size || !Character.isLowSurrogate(
                            password[nextIndex]
                        )
                    ) {
                        return null
                    }

                    index += 2
                }

                Character.isLowSurrogate(
                    currentCharacter
                ) -> {
                    return null
                }

                else -> {
                    index += 1
                }
            }

            codePointCount += 1
        }

        return codePointCount
    }


    private fun validationError(
        reason: String,
    ): DataError.ValidationError = DataError.ValidationError(
        message = "Password validation failed. $reason"
    )

    const val MINIMUM_CODE_POINTS = 15
    const val MAXIMUM_CODE_POINTS = 128
}
