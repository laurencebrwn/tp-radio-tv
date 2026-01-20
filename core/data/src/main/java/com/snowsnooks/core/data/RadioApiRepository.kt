package com.snowsnooks.core.data

import com.snowsnooks.core.domain.NowPlayingResponse
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class RadioApiRepository(private val client: OkHttpClient = OkHttpClient()) {

    fun fetchNowPlaying(
        url: String,
        onResult: (NowPlayingResponse) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ) {
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError?.invoke(e) ?: e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    try {
                        val obj = JSONObject(json)

                        val data = NowPlayingResponse(
                            status = obj.optBoolean("status"),
                            nowplaying = obj.optString("nowplaying"),
                            coverart = obj.optString("coverart"),
                            bitrate = obj.optString("bitrate"),
                            format = obj.optString("format")
                        )

                        onResult(data)
                    } catch (e: Exception) {
                        onError?.invoke(e) ?: e.printStackTrace()
                    }
                } ?: onError?.invoke(IllegalStateException("Empty response body"))
            }
        })
    }
}