package com.example.docconveter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private const val COMPRESSION_API_URL =
    "http://192.168.1.204:8000/compress"

private val compressionHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(120, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .callTimeout(180, TimeUnit.SECONDS)
    .build()

suspend fun compressPdfWithApi(
    inputFile: File,
    outputFile: File
) = withContext(Dispatchers.IO) {

    require(inputFile.exists() && inputFile.length() > 0L) {
        "The selected PDF is empty or unavailable."
    }

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file",
            inputFile.name,
            inputFile.asRequestBody("application/pdf".toMediaType())
        )
        .build()

    val request = Request.Builder()
        .url(COMPRESSION_API_URL)
        .post(requestBody)
        .build()

    compressionHttpClient.newCall(request).execute().use { response ->

        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()

            throw Exception(
                "Compression server error (${response.code}): " +
                        errorBody.take(300)
            )
        }

        val body = response.body
            ?: throw Exception("The server returned an empty response.")

        outputFile.parentFile?.mkdirs()

        body.byteStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }

        if (!outputFile.exists() || outputFile.length() < 5L) {
            outputFile.delete()
            throw Exception("The server returned an invalid PDF.")
        }

        val header = outputFile.inputStream().use {
            ByteArray(5).also { bytes ->
                it.read(bytes)
            }
        }

        if (!header.contentEquals("%PDF-".toByteArray())) {
            outputFile.delete()
            throw Exception("The server response is not a valid PDF.")
        }
    }
}
