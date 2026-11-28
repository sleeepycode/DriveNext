package ru.mtuci.drivenext.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.mtuci.drivenext.BuildConfig
import ru.mtuci.drivenext.domain.model.Car
import ru.mtuci.drivenext.domain.model.UserProfile
import java.net.HttpURLConnection
import java.net.URL

class CatalogRemote {
    suspend fun cars(query: String, token: String): List<Car> {
        val body = JSONObject().put("search_text", query)
        val rows = JSONArray(request("POST", "/rest/v1/rpc/drivenext_search_cars", token, body).toString(Charsets.UTF_8))
        return List(rows.length()) { i ->
            val row = rows.getJSONObject(i)
            Car(row.getLong("id"), row.getString("brand"), row.getString("model"), row.getInt("daily_price"),
                row.getString("transmission"), row.getString("fuel"), row.getString("image_key"))
        }
    }
    suspend fun profile(id: String, token: String): UserProfile {
        val fields = "id,email,first_name,last_name,gender,provider,avatar_path,created_at"
        val row = JSONArray(request("GET", "/rest/v1/profiles?id=eq.$id&select=$fields", token).toString(Charsets.UTF_8)).getJSONObject(0)
        return UserProfile(row.getString("id"), row.getString("email"), row.optString("first_name"),
            row.optString("last_name"), if (row.isNull("gender")) "" else row.optString("gender"),
            row.optString("provider"), if (row.isNull("avatar_path")) null else row.optString("avatar_path"), row.optString("created_at"))
    }
    suspend fun avatar(id: String, path: String, token: String): ByteArray {
        require(path == "$id/avatar.jpg")
        return request("GET", "/storage/v1/object/authenticated/drivenext-documents/$path", token)
    }
    suspend fun updateAvatar(id: String, path: String, token: String) {
        require(path == "$id/avatar.jpg")
        request("PATCH", "/rest/v1/profiles?id=eq.$id", token, JSONObject().put("avatar_path", path))
    }
    private suspend fun request(method: String, path: String, token: String, body: JSONObject? = null): ByteArray = withContext(Dispatchers.IO) {
        val conn = URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setFixedLengthStreamingMode(bytes.size)
                conn.outputStream.use { it.write(bytes) }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.errorStream?.close()
                throw IllegalStateException("HTTP $code")
            }
            if (code == 204) ByteArray(0) else conn.inputStream.use { it.readBytes() }
        } finally { conn.disconnect() }
    }
}
