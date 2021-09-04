package com.reflectednetwork.rfnetapi

import jakarta.xml.bind.DatatypeConverter
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

fun sha256(input: File) = sha256(input.readBytes())
fun sha256(input: ByteArray) = hashString("SHA256", input)

private fun hashString(type: String, input: ByteArray): String {
    val bytes = MessageDigest
        .getInstance(type)
        .digest(input)
    return DatatypeConverter.printHexBinary(bytes).lowercase()
}

fun download(urlString: String, file: File, ignoreHash: Boolean) {
    if (ignoreHash && file.exists() && file.readBytes().isNotEmpty()) return

    var downloadMsg = "Downloading"
    val url = URL(urlString)
    val urlConnection = url.openConnection()

    val bytes = urlConnection.getInputStream().readAllBytes()

    val existingFileHash = sha256(file)
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