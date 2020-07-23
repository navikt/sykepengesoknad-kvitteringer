package no.nav.syfo.bucket.api
import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.Metadata
import com.drew.metadata.exif.GpsDirectory
import no.nav.syfo.log
import java.io.File
import java.lang.NullPointerException

class ExifStripper(fil: File) {
    private val fil: File = fil
    fun lesGeoLokasjon(): List<String> {
        try {
            val metadata: Metadata = ImageMetadataReader.readMetadata(fil)
            val gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
            try {
                val latitude: String = gpsDir.getString(GpsDirectory.TAG_LONGITUDE)
                val longitude: String = gpsDir.getString(GpsDirectory.TAG_LATITUDE)
                log.error("Bruker har forsøkt å laste opp metadata")
                return listOf(latitude, longitude)
            } catch (exception: NullPointerException) {
                return emptyList()
            }
        } catch (exception: ImageProcessingException) {
            log.info("Klarer ikke å prosessere ugyldig filformat")
        }
        return emptyList()
    }
}
