package com.elite.jarvishud

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object NewsRepository {

    private val client = OkHttpClient()

    /**
     * Fetches top headlines. Calls onResult with a list of headline strings.
     * Calls onError with a message if something goes wrong.
     */
    fun fetchTopHeadlines(
        apiKey: String,
        country: String,
        onResult: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (apiKey.isBlank()) {
            onError("News API key not set. Add it in Settings.")
            return
        }
        val url = "https://newsapi.org/v2/top-headlines?country=$country&pageSize=10&apiKey=$apiKey"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("News fetch failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bodyStr = response.body()?.string().orEmpty()
                    val obj = JSONObject(bodyStr)
                    if (obj.optString("status") != "ok") {
                        onError("News API error: ${obj.optString("message", "unknown")}")
                        return
                    }
                    val articles = obj.getJSONArray("articles")
                    val headlines = mutableListOf<String>()
                    for (i in 0 until articles.length()) {
                        val title = articles.getJSONObject(i).optString("title")
                        if (title.isNotBlank()) headlines.add(title)
                    }
                    onResult(headlines)
                } catch (e: Exception) {
                    onError("News parse error: ${e.message}")
                }
            }
        })
    }
}
