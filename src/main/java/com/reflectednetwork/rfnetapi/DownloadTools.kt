package com.reflectednetwork.rfnetapi

//import jakarta.xml.bind.DatatypeConverter
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URL

fun sha256(input: File?) = sha256(input?.readBytes())
fun sha256(input: ByteArray?) = hashString("SHA256", input)

private fun hashString(type: String, input: ByteArray?): String {
//    val bytes = MessageDigest
//        .getInstance(type)
//        .digest(input ?: ByteArray(0))
//    return DatatypeConverter.printHexBinary(bytes).lowercase()
    return Math.random()
        .toString() // Return a random hash, redownload everything. Since this is just the packaged version,
    // You'll only ever need to update once, and it's not worth the extra library for packaging.
    // However, this needs to be accurate for the server version for efficiency.
}

fun download(urlString: String, file: File, ignoreHash: Boolean) {
    if (ignoreHash && file.exists()) return

    var downloadMsg = "Downloading"
    val url = URL(urlString)
    val urlConnection = url.openConnection()

    val bytes = urlConnection.getInputStream().readAllBytes()

    val existingFileHash = sha256(if (file.exists()) file else null)
    val downloadedFileHash = sha256(bytes.clone())
    if (file.exists() && existingFileHash != downloadedFileHash) {
        downloadMsg = "Re-downloading"
        println("Hash check failed: $existingFileHash != $downloadedFileHash")
    }

    if (!file.exists() || existingFileHash != downloadedFileHash) {
        println("--> $downloadMsg ${file.name}")
        val downloadStream = FileOutputStream(file)
        IOUtils.copy(bytes.clone().inputStream(), downloadStream)
    }
}

fun download(urlString: String, pluginName: String) {
    download(urlString, File("./plugins/$pluginName.jar"), true)
}