# flex-bucket-uploader
Brukes til å laste opp kvitteringer fra reisetilskuddsøknader.

## Teknologi brukt
* Kotlin
* Ktor
* Gradle

## Data
Applikasjonen har en bucket i GCP.
Her lagres kvitteringer som er lastet opp med fødselsnummer som metadata. 
Dataene er personidentifiserbare.
Kvitteringer slettes hvis brukeren sletter kvitteringen i søknadsdialogen før innsending.
Etter innsending er det ingen sletting av kvitteringene. 


# Komme i gang

Bygges med gradle. Standard spring boot oppsett.

---

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles til flex@nav.no

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #flex.
