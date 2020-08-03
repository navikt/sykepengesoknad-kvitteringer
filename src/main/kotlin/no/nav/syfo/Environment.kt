package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "flex-bucket-uploader"),
    val bucketName: String = getEnvVar("BUCKET_NAME", "flex-reisetilskudd-kvitteringer"),
    val jwtIssuer: String = getEnvVar("JWT_ISSUER"),
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME"),
    val clientId: String = getEnvVar("CLIENT_ID"),
    val appIds: List<String> = getEnvVar("ALLOWED_APP_IDS")
        .split(",")
        .map { it.trim() }
)


data class VaultSecrets(
    val serviceuserUsername: String,
    val serviceuserPassword: String,
    val syfomockUsername: String,
    val syfomockPassword: String,
    val oidcWellKnownUri: String,
    val loginserviceClientId: String,
    val internalJwtIssuer: String,
    val internalJwtWellKnownUri: String,
    val internalLoginServiceClientId: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
