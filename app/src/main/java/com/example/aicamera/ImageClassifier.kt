package com.aicamera

import android.annotation.SuppressLint
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

suspend fun classifyImage(image: InputImage, onResult: (String, Int) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://us-central1-aiplatform.googleapis.com/v1/projects/abstract-mode-442610-t6/locations/us-central1/publishers/google/models/chat-bison:predict")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Convert image to Base64
            val base64Image = convertImageToBase64(image)

            // Prepare JSON request
            val jsonInputString = """
                {
                    "image": "$base64Image"
                }
            """.trimIndent()

            // Send request
            connection.outputStream.use { os ->
                val input = jsonInputString.toByteArray()
                os.write(input, 0, input.size)
            }

            // Handle response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { reader ->
                    val response = reader.readText()
                    val jsonResponse = JSONObject(response)
                    val type = jsonResponse.getString("type")
                    val points = jsonResponse.getInt("points")
                    onResult(type, points)
                }
            } else {
                onResult("Error: $responseCode", 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onResult("Error", 0)
        }
    }
}

@SuppressLint("NewApi")
fun convertImageToBase64(image: InputImage): String {
    val bitmap = image.bitmapInternal ?: throw IllegalArgumentException("Bitmap is null")
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.getEncoder().encodeToString(byteArray)
}

// Tambahkan fungsi sendPrompt
fun sendPrompt(prompt: String, onResult: (String) -> Unit) {
    val client = OkHttpClient()
    val url = "https://us-central1-aiplatform.googleapis.com/v1/projects/your-project-id/locations/us-central1/publishers/google/models/chat-bison:predict"

    val jsonRequest = JSONObject()
    jsonRequest.put("instances", listOf(mapOf("content" to prompt)))

    val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonRequest.toString())
    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer your-api-key-or-token")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onResult("Error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body?.string()
            val jsonResponse = JSONObject(responseBody ?: "")
            val result = jsonResponse.getJSONArray("predictions")
                .getJSONObject(0)
                .getString("content")
            onResult(result)
        }
    })
}