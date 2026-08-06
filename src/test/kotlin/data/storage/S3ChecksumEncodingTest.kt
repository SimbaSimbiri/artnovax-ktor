package com.simbiri.data.storage

import kotlin.test.Test
import kotlin.test.assertEquals

class S3ChecksumEncodingTest {

    @Test
    fun `sha256 hex digest converts to base64 required by S3`() {
        val emptyFileSha256 = "e3b0c44298fc1c149afbf4c8996fb924" +
                "27ae41e4649b934ca495991b7852b855"

        assertEquals(
            expected = "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=",
            actual = sha256HexToBase64(emptyFileSha256),
        )
    }

    @Test
    fun `S3 base64 checksum converts back to lowercase hex`() {
        assertEquals(
            expected = "e3b0c44298fc1c149afbf4c8996fb924" +
                    "27ae41e4649b934ca495991b7852b855",
            actual = sha256Base64ToHex(
                "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU="
            ),
        )
    }
}
