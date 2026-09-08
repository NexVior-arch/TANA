package com.tana.security

import android.content.Context
import android.hardware.biometrics.BiometricManager as AndroidBiometricManager
import android.hardware.biometrics.BiometricPrompt as AndroidBiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

object BiometricAuthHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val biometricManager = context.getSystemService(AndroidBiometricManager::class.java)
            if (biometricManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val canAuth = biometricManager.canAuthenticate(
                        AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK
                    )
                    canAuth == AndroidBiometricManager.BIOMETRIC_SUCCESS
                } else {
                    val canAuth = biometricManager.canAuthenticate()
                    canAuth == AndroidBiometricManager.BIOMETRIC_SUCCESS
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun promptBiometric(
        activity: ComponentActivity,
        title: String = "Autentikasi Sidik Jari",
        subtitle: String = "Pindai sidik jari Anda untuk membuka TANA",
        negativeButtonText: String = "Gunakan PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError("Biometrik tidak didukung pada versi Android ini.")
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val cancellationSignal = CancellationSignal()

            val callback = object : AndroidBiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: AndroidBiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString?.toString() ?: "Autentikasi dibatalkan")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }

            val prompt = AndroidBiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton(negativeButtonText, executor) { _, _ ->
                    onError("Dibatalkan pengguna")
                }
                .build()

            prompt.authenticate(cancellationSignal, executor, callback)
        } catch (e: Exception) {
            onError("Gagal memanggil sensor: ${e.message}")
        }
    }
}
