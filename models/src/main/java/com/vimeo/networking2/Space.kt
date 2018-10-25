package com.vimeo.networking2

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vimeo.networking2.enums.UploadSpaceType

/**
 * Upload quota space data.
 */
@JsonClass(generateAdapter = true)
data class Space(

    /**
     * The number of bytes remaining in your upload quota.
     */
    @Json(name = "free")
    val free: Long? = null,

    /**
     * The maximum number of bytes allotted to your upload quota.
     */
    @Json(name = "max")
    val max: Long? = null,

    /**
     * Whether the values of the upload_quota.space fields are for the lifetime quota or
     * the periodic quota.
     */
    @Json(name = "showing")
    val showing: UploadSpaceType? = null,

    /**
     * The number of bytes that you've already uploaded against your quota.
     */
    @Json(name = "used")
    val used: Long? = null

)
