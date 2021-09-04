package com.reflectednetwork.rfnetapi

import jakarta.xml.bind.DatatypeConverter
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.charset.Charset
import java.security.MessageDigest

fun md5(input: String) = hashString("MD5", input)

private fun hashString(type: String, input: String): String {
    val bytes = MessageDigest
        .getInstance(type)
        .digest(input.toByteArray())
    return DatatypeConverter.printHexBinary(bytes).uppercase()
}

fun download(urlString: String, file: File) {
    var downloadMsg = "Downloading file:"
    val downloadStream = FileOutputStream(file)
    val url = URL(urlString)
    val urlConnection = url.openConnection()

    val existingFileHash = md5(file.readText(Charset.defaultCharset()))
    val downloadedFileHash = md5(urlConnection.getInputStream().readAllBytes().contentToString())
    if (file.exists() && existingFileHash != downloadedFileHash) {
        file.delete()
        downloadMsg = "Refreshing corrupt file:"
    }

    if (!file.exists()) {
        println("--> $downloadMsg ${file.name}")
        IOUtils.copy(urlConnection.getInputStream(), downloadStream)
    }
}

fun download(urlString: String, pluginName: String) {
    download(urlString, File("./plugins/$pluginName.jar"))
}