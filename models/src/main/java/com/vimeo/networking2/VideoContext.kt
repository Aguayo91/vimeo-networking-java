package com.vimeo.networking2

import com.vimeo.networking2.enums.VideoActionType

/**
 * Video context data.
 */
data class VideoContext(

    /**
     * The contextual action.
     */
    val action: VideoActionType? = null,

    /**
     * The contextual resource type.
     */
    val resourceType: String? = null

)
