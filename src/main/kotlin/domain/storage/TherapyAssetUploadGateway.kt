package com.simbiri.domain.storage

import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

interface TherapyAssetUploadGateway {

    fun createUploadGrant(
        specification: TherapyAssetUploadSpecification,
    ): ResultType<TherapyAssetUploadGrant, DataError>
}
