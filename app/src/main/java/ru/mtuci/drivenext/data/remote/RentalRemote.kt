package ru.mtuci.drivenext.data.remote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.mtuci.drivenext.BuildConfig
import ru.mtuci.drivenext.domain.model.*
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

class RentalRemote {
    private fun car(row: JSONObject) = Car(row.getLong("id"), row.getString("brand"), row.getString("model"), row.getInt("daily_price"), row.getString("transmission"), row.getString("fuel"), row.getString("image_key"))
    suspend fun details(id: Long, token: String): CarDetails {
        val row = JSONArray(request("GET", "/rest/v1/cars?id=eq.$id&select=*", token)).getJSONObject(0)
        return CarDetails(car(row), row.getString("address"), row.getString("description"), row.getInt("insurance_daily"), row.getInt("deposit"))
    }
    suspend fun driver(id: String, token: String): Pair<String, String> {
        val r = JSONArray(request("GET", "/rest/v1/profiles?id=eq.$id&select=last_name,first_name,middle_name,license_number", token)).getJSONObject(0)
        return listOf("last_name", "first_name", "middle_name").map { if (r.isNull(it)) "" else r.optString(it) }.filter { it.isNotBlank() }.joinToString(" ") to (if (r.isNull("license_number")) "" else r.optString("license_number"))
    }
    suspend fun bookings(token: String): List<Booking> {
        val rows = JSONArray(request("GET", "/rest/v1/bookings?select=*&order=start_at.desc", token))
        return List(rows.length()) { i ->
            val r = rows.getJSONObject(i)
            val c = Car(r.getLong("car_id"), r.getString("brand"), r.getString("model"), r.getInt("daily_price"), r.getString("transmission"), r.getString("fuel"), r.getString("image_key"))
            Booking(r.getLong("id"), c, r.getString("address"), Instant.parse(r.getString("start_at")), Instant.parse(r.getString("end_at")), r.getString("driver_name"), r.getString("license_number"), r.getString("status"), r.getLong("rent_total"), r.getLong("insurance_total"), r.getLong("deposit"))
        }
    }
    suspend fun book(id: Long, draft: RentalDraft, token: String) {
        request("POST", "/rest/v1/rpc/drivenext_book_car", token, JSONObject().put("p_car", id).put("p_start", draft.start.toString()).put("p_end", draft.end.toString()).put("p_driver", draft.driver.trim()).put("p_license", draft.license).put("p_request", draft.requestId))
    }
    suspend fun cancel(id: Long, token: String) { request("POST", "/rest/v1/rpc/drivenext_cancel_booking", token, JSONObject().put("p_id", id)) }
    private suspend fun request(method: String, path: String, token: String, body: JSONObject? = null): String = withContext(Dispatchers.IO) {
        val c = URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method; c.connectTimeout = 15000; c.readTimeout = 25000; c.instanceFollowRedirects = false
            c.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY); c.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) { val bytes = body.toString().toByteArray(Charsets.UTF_8); c.doOutput = true; c.setRequestProperty("Content-Type", "application/json; charset=utf-8"); c.setFixedLengthStreamingMode(bytes.size); c.outputStream.use { it.write(bytes) } }
            val code = c.responseCode
            if (code !in 200..299) {
                val error = c.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val reason = runCatching { JSONObject(error).optString("message") }.getOrDefault("")
                throw IllegalStateException(when (reason) { "CAR_BUSY" -> "Автомобиль уже забронирован на этот период."; "INVALID_RENTAL" -> "Проверьте даты и данные водителя."; "CAR_UNAVAILABLE" -> "Автомобиль недоступен."; else -> "Ошибка сервера ($code). Попробуйте снова." })
            }
            if (code == 204) "" else c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { c.disconnect() }
    }
}
