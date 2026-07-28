package com.simbiri.data.security

import com.simbiri.domain.model.auth.PasswordHashAlgorithm
import com.simbiri.domain.security.PasswordHasher
import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Argon2id implementation of the password-hashing contract.
 *
 * Hashing runs on Dispatchers.Default because it is intentionally
 * CPU- and memory-intensive work and must not block a Ktor event loop.
 */
class Argon2PasswordHasher(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PasswordHasher {

    override val algorithm: PasswordHashAlgorithm = PasswordHashAlgorithm.ARGON2ID

    override suspend fun hash(
        password: CharArray,
    ): String = usePasswordCopy(password) {argon2, passwordCopy, ->

        argon2.hash(
            ITERATIONS,
            MEMORY_KIB,
            PARALLELISM,
            passwordCopy,
        )
    }

    override suspend fun verify(
        password: CharArray,
        encodedHash: String,
    ): Boolean = usePasswordCopy(password) { argon2, passwordCopy, ->

        argon2.verify(
            encodedHash,
            passwordCopy,
        )
    }

    /**
     * Prevents the hasher from mutating the caller's CharArray while still
     * ensuring internal plaintext copy is cleared after.
     */
    private suspend fun <T> usePasswordCopy(
        password: CharArray,
        operation: ( argon2: Argon2, passwordCopy: CharArray,) -> T,
    ): T = withContext(dispatcher) {
        val passwordCopy = password.copyOf()

        val argon2 = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id
        )

        try {
            operation(
                argon2,
                passwordCopy,
            )
        } finally {
            argon2.wipeArray(
                passwordCopy
            )
        }
    }

    companion object {

        /*
         * OWASP minimum Argon2id baseline:
         * memory=19 MiB, iterations=2, parallelism=1.
         */
        private const val ITERATIONS = 2
        private const val MEMORY_KIB = 19 * 1024
        private const val PARALLELISM = 1
    }
}
