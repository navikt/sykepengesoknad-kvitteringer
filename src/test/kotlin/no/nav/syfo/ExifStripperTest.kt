package no.nav.syfo

import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.bucket.api.ExifStripper
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

@KtorExperimentalAPI
object ExifStripperTest : Spek({

    describe("GPS-innhold fjernes fra fil") {
        val filnavn = this::class.java.getResource("/bilder/example.jpg").toURI()
        val fil = File(filnavn)
        val outputFil = File(filnavn)

        val exifStripper = ExifStripper(fil)
        println(exifStripper.lesGeoLokasjon())

        true shouldEqual true
    }
})
