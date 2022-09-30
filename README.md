# flex-bucket-uploader

Applikasjon som tar i mot og behandler bilder av kvitteringer til sykepengesøknader med reisetilskudd. 
Bildene konverteres til `jpeg` og justeres til riktig størrelse før til lastes opp til en GCP bucket.

Applikasjonen lagrer kvitteringer i GCP bucket med fødselsnummer som metadata. Dataene er personidentifiserbare. 
Kvitteringer slettes hvis brukeren sletter kvitteringen i søknadsdialogen før innsending. 
Etter innsending er det ingen sletting av kvitteringene. 

## Teknologi

* Kotlin
* Spring Boot
* Gradle

---

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles til flex@nav.no

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #flex.
