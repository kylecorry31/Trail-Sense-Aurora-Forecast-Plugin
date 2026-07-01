package com.kylecorry.trail_sense.plugin.aurora.service

import com.kylecorry.andromeda.ipc.server.InterprocessCommunicationRouter
import com.kylecorry.andromeda.ipc.server.InterprocessCommunicationService
import com.kylecorry.andromeda.json.fromJson
import com.kylecorry.trail_sense.plugin.aurora.models.MapTileLayerRequest
import com.kylecorry.trail_sense.plugin.aurora.models.RegistrationFeaturesResponse
import com.kylecorry.trail_sense.plugin.aurora.models.RegistrationMapLayerAttributionResponse
import com.kylecorry.trail_sense.plugin.aurora.models.RegistrationMapLayerResponse
import com.kylecorry.trail_sense.plugin.aurora.models.RegistrationResponse

class AuroraForecastPluginService : InterprocessCommunicationService() {

    private suspend fun getTile(payload: MapTileLayerRequest): ByteArray? {
        return AuroraTileRenderer.render(payload)
    }

    override val router: InterprocessCommunicationRouter
        get() = InterprocessCommunicationRouter(
            mapOf(
                "/registration" to { context, payload ->
                    success(
                        RegistrationResponse(
                            RegistrationFeaturesResponse(
                                mapLayers = listOf(
                                    RegistrationMapLayerResponse(
                                        endpoint = "/tiles",
                                        name = "Aurora Forecast",
                                        layerType = "tile",
                                        attribution = RegistrationMapLayerAttributionResponse(
                                            attribution = "NOAA SWPC",
                                            longAttribution = "Aurora forecast data from NOAA Space Weather Prediction Center OVATION model."
                                        ),
                                        description = "Latest NOAA OVATION aurora probability forecast.",
                                        minZoomLevel = 0,
                                        refreshInterval = 900000
                                    )
                                )
                            )
                        )
                    )
                },
                "/tiles" to { context, request ->
                    val parsedPayload = request.payload?.fromJson<MapTileLayerRequest>()
                    parsedPayload?.let { success(getTile(it)) } ?: badRequest()
                }
            )
        )
}
