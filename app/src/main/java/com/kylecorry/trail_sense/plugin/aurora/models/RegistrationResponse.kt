package com.kylecorry.trail_sense.plugin.aurora.models

data class RegistrationResponse(
    val features: RegistrationFeaturesResponse
)

data class RegistrationFeaturesResponse(
    val mapLayers: List<RegistrationMapLayerResponse> = emptyList()
)

data class RegistrationMapLayerResponse(
    val endpoint: String,
    val name: String,
    val layerType: String,
    val attribution: RegistrationMapLayerAttributionResponse? = null,
    val description: String? = null,
    val minZoomLevel: Int? = null,
    val isTimeDependent: Boolean = false,
    val refreshInterval: Long? = null,
    val shouldMultiply: Boolean = false
)

data class RegistrationMapLayerAttributionResponse(
    val attribution: String,
    val longAttribution: String? = null,
    val alwaysShow: Boolean = false
)
