package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "flex-bucket-uploader"),
    val bucketName: String = getEnvVar("BUCKET_NAME", "flex-reisetilskudd-kvitteringer"),
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME"),
    val sidecarInitialDelay: Long = getEnvVar("SIDECAR_INITIAL_DELAY", "15000").toLong(),
    val loginserviceIdportenDiscoveryUrl: String = getEnvVar("LOGINSERVICE_IDPORTEN_DISCOVERY_URL"),
    val loginserviceIdportenAudience: String = getEnvVar("LOGINSERVICE_IDPORTEN_AUDIENCE")
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
