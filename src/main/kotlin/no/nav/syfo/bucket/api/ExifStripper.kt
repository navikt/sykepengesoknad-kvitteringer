package no.nav.syfo.bucket.api
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.Metadata
import com.drew.metadata.Tag
import com.drew.metadata.exif.ExifImageDirectory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDescriptor
import com.drew.metadata.exif.GpsDirectory
import java.io.File

class ExifStripper(fil: File) {
    val metadata: Metadata = ImageMetadataReader.readMetadata(fil)
    fun lesGeoLokasjon() : List<String>{
        val gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
        val latitude : String = gpsDir.getString(GpsDirectory.TAG_LONGITUDE)
        val longitude : String = gpsDir.getString(GpsDirectory.TAG_LATITUDE)
        return listOf(latitude, longitude)
    }
    fun printMetadata() {
        for (dir: Directory in metadata.directories) {
            for (tag: Tag in dir.tags) {
                println("DIR $dir    | TAG $tag")
            }
        }
    }
}