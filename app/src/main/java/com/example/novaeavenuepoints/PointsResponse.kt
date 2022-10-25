package com.example.novaeavenuepoints


import com.fasterxml.jackson.annotation.JsonProperty
import androidx.annotation.Keep

@Keep
data class PointsResponse(
    @JsonProperty("results")
    val results: Results,
    @JsonProperty("status")
    val status: String
) {
    @Keep
    data class Results(
        @JsonProperty("api_output_type")
        val apiOutputType: String,
        @JsonProperty("api_type")
        val apiType: String,
        @JsonProperty("area")
        val area: List<String>?,
        @JsonProperty("distance")
        val distance: String,
        @JsonProperty("latLng")
        val latLng: List<String>,
        @JsonProperty("pin_code")
        val pinCode: List<String>?,
        @JsonProperty("pts_address")
        val ptsAddress: List<PtsAddres>?,
        @JsonProperty("pts_tot")
        val ptsTot: Int,
        @JsonProperty("tot_types")
        val totTypes: List<String>?
    ) {
        @Keep
        data class PtsAddres(
            @JsonProperty("lat")
            val lat: Double,
            @JsonProperty("lng")
            val lng: Double,
            @JsonProperty("pt_id")
            val ptId: String,
            @JsonProperty("pt_type")
            val ptType: String
        )
    }
}