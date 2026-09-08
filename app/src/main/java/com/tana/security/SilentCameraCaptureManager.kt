package com.tana.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume

object SilentCameraCaptureManager {
    private const val TAG = "SilentCameraCapture"

    /**
     * Silently captures a frame from the Front-Facing camera without any visible preview or sound.
     * Returns JPEG byte array or null if unavailable / permission denied.
     */
    suspend fun captureFrontPhoto(context: Context): ByteArray? = suspendCancellableCoroutine { cont ->
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission not granted for intruder capture")
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var frontCameraId: String? = null

            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = id
                    break
                }
            }

            if (frontCameraId == null) {
                frontCameraId = cameraManager.cameraIdList.firstOrNull()
            }

            if (frontCameraId == null) {
                Log.e(TAG, "No camera found on device")
                cont.resume(null)
                return@suspendCancellableCoroutine
            }

            val thread = HandlerThread("IntruderCameraBackground").apply { start() }
            val backgroundHandler = Handler(thread.looper)

            val width = 640
            val height = 480
            val imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2)

            var cameraDeviceRef: CameraDevice? = null

            val cleanup = {
                try {
                    imageReader.close()
                    cameraDeviceRef?.close()
                    thread.quitSafely()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in camera cleanup", e)
                }
            }

            cont.invokeOnCancellation {
                cleanup()
            }

            imageReader.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        image.close()

                        cleanup()
                        if (cont.isActive) {
                            cont.resume(bytes)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error acquiring camera image", e)
                    cleanup()
                    if (cont.isActive) {
                        cont.resume(null)
                    }
                }
            }, backgroundHandler)

            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDeviceRef = camera
                    try {
                        val dummySurfaceTexture = SurfaceTexture(10).apply {
                            setDefaultBufferSize(width, height)
                        }
                        val dummySurface = Surface(dummySurfaceTexture)

                        val surfaces = listOf(dummySurface, imageReader.surface)
                        camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                try {
                                    val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                        addTarget(imageReader.surface)
                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                    }
                                    session.capture(captureBuilder.build(), null, backgroundHandler)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to send capture request", e)
                                    cleanup()
                                    if (cont.isActive) cont.resume(null)
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                Log.e(TAG, "Camera session configuration failed")
                                cleanup()
                                if (cont.isActive) cont.resume(null)
                            }
                        }, backgroundHandler)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error configuring camera session", e)
                        cleanup()
                        if (cont.isActive) cont.resume(null)
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cleanup()
                    if (cont.isActive) cont.resume(null)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera open error: $error")
                    cleanup()
                    if (cont.isActive) cont.resume(null)
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error initializing front camera", e)
            if (cont.isActive) cont.resume(null)
        }
    }
}
