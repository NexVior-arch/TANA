package com.tana.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TelegramAlertService {
    private const val TAG = "TelegramAlertService"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun sanitizeToken(token: String): String {
        var clean = token.trim()
        if (clean.startsWith("bot", ignoreCase = true)) {
            clean = clean.substring(3).trim()
        }
        return clean
    }

    private fun parseTelegramErrorMessage(code: Int, responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val desc = json.optString("description", "")
            when {
                code == 401 -> "Bot Token tidak valid (401 Unauthorized). Pastikan Anda menyalin token dari @BotFather dengan benar."
                code == 400 && desc.contains("chat not found", ignoreCase = true) -> "Chat ID tidak ditemukan. Buka bot Anda di Telegram, lalu klik START (/start) terlebih dahulu."
                code == 400 && desc.contains("chat_id is empty", ignoreCase = true) -> "Chat ID tidak boleh kosong."
                code == 403 && desc.contains("can't send messages to the bot", ignoreCase = true) -> "Chat ID yang Anda masukkan adalah ID Bot itu sendiri! Masukkan Chat ID akun Telegram pribadi Anda (dapatkan via bot @userinfobot di Telegram)."
                code == 403 -> "Akses ditolak (403 Forbidden). Buka bot Anda di Telegram dan tekan tombol START (/start) agar bot dapat mengirim pesan ke Anda."
                desc.isNotBlank() -> "Gagal API Telegram ($code): $desc"
                else -> "Gagal terhubung ke Telegram (HTTP $code)"
            }
        } catch (_: Exception) {
            "Gagal terhubung ke Telegram (HTTP $code): ${responseBody.take(100)}"
        }
    }

    suspend fun testConnection(botToken: String, chatId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = sanitizeToken(botToken)
            val cleanChatId = chatId.trim()
            if (cleanToken.isBlank() || cleanChatId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Bot Token dan Chat ID wajib diisi!"))
            }

            // Check if user accidentally entered the Bot's own ID as Chat ID
            val botIdFromToken = cleanToken.split(":").firstOrNull()?.trim()
            if (botIdFromToken != null && botIdFromToken == cleanChatId) {
                return@withContext Result.failure(IllegalArgumentException("Chat ID yang Anda masukkan ($cleanChatId) adalah ID dari Bot itu sendiri! Masukkan Chat ID akun Telegram pribadi Anda (dapatkan via bot @userinfobot)."))
            }

            val timestamp = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date())
            val messageHtml = """
                🛡️ <b>[TANA Security Alert Test]</b>

                ✅ <b>Koneksi Telegram Bot Berhasil!</b>
                Sistem keamanan <i>Intruder Camera Trap</i> siap mengawasi aplikasi TANA Anda.

                📅 <b>Waktu:</b> <code>$timestamp</code>
                🔒 <b>Status:</b> Aktif &amp; Terhubung
            """.trimIndent()

            val url = "https://api.telegram.org/bot$cleanToken/sendMessage"
            val formBody = FormBody.Builder()
                .add("chat_id", cleanChatId)
                .add("text", messageHtml)
                .add("parse_mode", "HTML")
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success("Pesan uji coba berhasil terkirim ke Telegram!")
            } else {
                val errorMsg = parseTelegramErrorMessage(response.code, responseBody)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Telegram test connection failed", e)
            val friendlyMsg = when {
                e is java.net.UnknownHostException -> "Tidak ada koneksi internet / gagal menjangkau api.telegram.org"
                e is java.net.SocketTimeoutException -> "Koneksi ke Telegram timeout. Periksa jaringan internet Anda."
                else -> e.localizedMessage ?: "Terjadi kesalahan jaringan"
            }
            Result.failure(Exception(friendlyMsg))
        }
    }

    suspend fun sendIntruderAlertWithPhoto(
        botToken: String,
        chatId: String,
        photoBytes: ByteArray?,
        failedAttempts: Int,
        deviceModel: String = android.os.Build.MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = sanitizeToken(botToken)
            val cleanChatId = chatId.trim()
            if (cleanToken.isBlank() || cleanChatId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Bot Token atau Chat ID belum disetel."))
            }

            val timestamp = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date())
            val captionHtml = """
                🚨 <b>PERINGATAN PENYUSUP TERDETEKSI!</b> 🚨

                Seseorang mencoba memaksa membuka aplikasi <b>TANA</b> dengan salah memasukkan PIN sebanyak <b>$failedAttempts kali berturut-turut</b>!

                📅 <b>Waktu Kejadian:</b> <code>$timestamp</code>
                📱 <b>Perangkat:</b> $deviceModel (Android ${android.os.Build.VERSION.RELEASE})
                🔒 <b>Tindakan:</b> Aplikasi langsung dihentikan (Force Close) demi keamanan.

                📸 <i>Foto wajah penyusup terlampir di atas jika kamera depan diizinkan.</i>
            """.trimIndent()

            if (photoBytes != null && photoBytes.isNotEmpty()) {
                val url = "https://api.telegram.org/bot$cleanToken/sendPhoto"
                val requestFile = photoBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, photoBytes.size)
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", cleanChatId)
                    .addFormDataPart("caption", captionHtml)
                    .addFormDataPart("parse_mode", "HTML")
                    .addFormDataPart("photo", "intruder_${System.currentTimeMillis()}.jpg", requestFile)
                    .build()

                val request = Request.Builder().url(url).post(body).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success("Foto penyusup berhasil dikirim ke Telegram!")
                } else {
                    val err = response.body?.string() ?: ""
                    val errorMsg = parseTelegramErrorMessage(response.code, err)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                // Fallback text message if camera captured nothing or permission denied
                val url = "https://api.telegram.org/bot$cleanToken/sendMessage"
                val textWithNote = captionHtml + "\n\n⚠️ <i>(Foto tidak tersedia karena izin kamera belum aktif di perangkat).</i>"
                val formBody = FormBody.Builder()
                    .add("chat_id", cleanChatId)
                    .add("text", textWithNote)
                    .add("parse_mode", "HTML")
                    .build()

                val request = Request.Builder().url(url).post(formBody).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success("Peringatan penyusup teks terkirim ke Telegram!")
                } else {
                    val err = response.body?.string() ?: ""
                    val errorMsg = parseTelegramErrorMessage(response.code, err)
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending intruder alert to Telegram", e)
            Result.failure(e)
        }
    }
}

