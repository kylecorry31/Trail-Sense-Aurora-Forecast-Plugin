package com.kylecorry.trail_sense.plugin.aurora.service

import android.os.Bundle
import com.kylecorry.andromeda.ipc.CODE_BAD_REQUEST
import com.kylecorry.andromeda.ipc.CODE_OK
import com.kylecorry.andromeda.ipc.InterprocessCommunicationResponse
import com.kylecorry.andromeda.json.toJsonBytes

fun success(payload: Any?): InterprocessCommunicationResponse {
    val bytes = when (payload) {
        null -> {
            null
        }

        is ByteArray -> {
            payload
        }

        is String -> {
            payload.toByteArray()
        }

        else -> {
            payload.toJsonBytes()
        }
    }
    return InterprocessCommunicationResponse(CODE_OK, Bundle(), bytes)
}

fun badRequest(): InterprocessCommunicationResponse {
    return InterprocessCommunicationResponse(CODE_BAD_REQUEST, Bundle(), null)
}
