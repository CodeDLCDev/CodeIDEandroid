package com.example.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenCodeService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(endpoint: String, apiKey: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("API Key is empty"))
                }
                val requestBody = JSONObject().apply {
                    put("model", "gpt-3.5-turbo")
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", "ping")
                        })
                    })
                    put("max_tokens", 5)
                }.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success("Connection test successful! (HTTP ${response.code})")
                    } else {
                        val errBody = response.body?.string() ?: ""
                        Result.failure(Exception("HTTP ${response.code}: $errBody"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun sendMessage(
        endpoint: String,
        apiKey: String,
        userPrompt: String,
        codeSnippet: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("API key missing. Please enter your API key in Settings."))
                }

                val fullPrompt = if (!codeSnippet.isNullOrBlank()) {
                    "Code context:\n```\n$codeSnippet\n```\n\nUser request: $userPrompt"
                } else {
                    userPrompt
                }

                val requestBody = JSONObject().apply {
                    put("model", "gpt-3.5-turbo")
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "You are OpenCode AI, an expert software developer assistant embedded inside CodeIDE mobile environment.")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", fullPrompt)
                        })
                    })
                }.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(bodyString)
                        val choices = jsonResponse.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val content = choices.getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                            Result.success(content)
                        } else {
                            Result.success(bodyString)
                        }
                    } else {
                        Result.failure(Exception("API Error (${response.code}): $bodyString"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
