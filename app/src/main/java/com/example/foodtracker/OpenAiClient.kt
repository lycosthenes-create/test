package com.example.foodtracker

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenAiClient(
    private val baseUrl: String = BuildConfig.OPENAI_BASE_URL,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    fun analyzeFood(base64Image: String, apiKey: String): Result<String> {
        val imageUrl = "data:image/jpeg;base64,$base64Image"

        val payload = JSONObject()
            .put("model", "gpt-4o-mini")
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "content",
                            JSONArray()
                                .put(
                                    JSONObject()
                                        .put("type", "text")
                                        .put(
                                            "text",
                                            "Identify foods in this photo and estimate calories per item. Return concise bullet points."
                                        )
                                )
                                .put(
                                    JSONObject()
                                        .put("type", "image_url")
                                        .put("image_url", JSONObject().put("url", imageUrl))
                                )
                        )
                )
            )
            .put("max_tokens", 300)

        val request = Request.Builder()
            .url("${baseUrl}v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("OpenAI request failed (${response.code}): $responseBody")
                }

                val json = JSONObject(responseBody)
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        }
    }
}
