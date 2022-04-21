package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "flex-bucket-uploader"),
    val bucketName: String = getEnvVar("BUCKET_NAME", "flex-reisetilskudd-kvitteringer"),
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME"),
    val imageProcessingUrl: String = getEnvVar("IMAGE_PROCESSING_URL", "http://flex-bildeprosessering"),
    val sidecarInitialDelay: Long = getEnvVar("SIDECAR_INITIAL_DELAY", "15000").toLong(),
    val loginserviceIdportenDiscoveryUrl: String = getEnvVar("LOGINSERVICE_IDPORTEN_DISCOVERY_URL"),
    val loginserviceIdportenAudience: String = getEnvVar("LOGINSERVICE_IDPORTEN_AUDIENCE"),
    val azureAppClientId: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppWellKnownUrl: String = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
    val azureAppPreAuthorizedApps: List<PreAuthorizedClient> = getAuthorizedApps(getEnvVar("AZURE_APP_PRE_AUTHORIZED_APPS"))
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun getAuthorizedApps(aadAuthorizedApps: String): List<PreAuthorizedClient> {
    val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    return objectMapper.readValue(aadAuthorizedApps)
}

data class PreAuthorizedClient(val name: String, val clientId: String)
