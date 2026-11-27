package ru.mtuci.drivenext.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.mtuci.drivenext.BuildConfig
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** PostgREST and private Storage requests use the current user's JWT, not an admin key. */
class ProfileRemote(private val context: Context? = null) {
    suspend fun complete(id: String, token: String): Boolean {
        val json = request("GET", "/rest/v1/profiles?id=eq.$id&select=registration_complete", token)
        return JSONArray(json).optJSONObject(0)?.optBoolean("registration_complete") == true
    }

    suspend fun save(profile: JSONObject, token: String) {
        request("POST", "/rest/v1/profiles?on_conflict=id", token,
            profile.toString().toByteArray(Charsets.UTF_8), "application/json",
            mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"))
    }

    suspend fun upload(id: String, name: String, uri: String, token: String): String = withContext(Dispatchers.IO) {
        val resolver = requireNotNull(context).contentResolver
        val source = ImageDecoder.createSource(resolver, Uri.parse(uri))
        // Re-encode to JPEG to limit memory and remove original EXIF metadata.
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val size = info.size
            val sample = maxOf(1, (maxOf(size.width, size.height) + 1599) / 1600)
            decoder.setTargetSampleSize(sample)
        }
        val bytes = try {
            ByteArrayOutputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)) { "Не удалось обработать фото." }
                out.toByteArray()
            }
        } finally { bitmap.recycle() }
        require(bytes.size <= 6 * 1024 * 1024) { "Фото превышает 6 МБ." }
        val path = "$id/$name.jpg"
        request("POST", "/storage/v1/object/drivenext-documents/$path", token, bytes,
            "image/jpeg", mapOf("x-upsert" to "true"))
        path
    }

    private suspend fun request(method: String, path: String, token: String,
        body: ByteArray? = null, contentType: String? = null,
        headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val connection = URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 25000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $token")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType ?: "application/octet-stream")
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                connection.errorStream?.close()
                throw IllegalStateException("Ошибка сервера ($status). Проверьте настройки Supabase и повторите попытку.")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
    }
}
