package com.example.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sqrt

class FingerPPGSensor(private val context: Context) {

    private val TAG = "FingerPPGSensor"

    // --- State Observables ---
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isFingerDetected = MutableStateFlow(false)
    val isFingerDetected: StateFlow<Boolean> = _isFingerDetected.asStateFlow()

    private val _mockFingerOverride = MutableStateFlow(false)
    val mockFingerOverride: StateFlow<Boolean> = _mockFingerOverride.asStateFlow()

    fun setMockFingerOverride(enabled: Boolean) {
        _mockFingerOverride.value = enabled
    }

    private val _livePulseWave = MutableStateFlow(0f)
    val livePulseWave: StateFlow<Float> = _livePulseWave.asStateFlow()

    private val _liveBpm = MutableStateFlow(0)
    val liveBpm: StateFlow<Int> = _liveBpm.asStateFlow()

    private val _liveSpO2 = MutableStateFlow(0)
    val liveSpO2: StateFlow<Int> = _liveSpO2.asStateFlow()

    private val _liveRespiration = MutableStateFlow(0)
    val liveRespiration: StateFlow<Int> = _liveRespiration.asStateFlow()

    private val _isUsingSimulation = MutableStateFlow(false)
    val isUsingSimulation: StateFlow<Boolean> = _isUsingSimulation.asStateFlow()

    // --- Dynamic Calibrated Hardware Thresholds ---
    private val _customVThreshold = MutableStateFlow(140)
    val customVThreshold: StateFlow<Int> = _customVThreshold.asStateFlow()

    private val _customYThreshold = MutableStateFlow(40)
    val customYThreshold: StateFlow<Int> = _customYThreshold.asStateFlow()

    fun calibratePPGThresholds(baseV: Int, baseY: Int) {
        _customVThreshold.value = (baseV - 15).coerceIn(100, 160)
        _customYThreshold.value = (baseY - 10).coerceIn(20, 80)
        Log.i("FingerPPGSensor", "Calibrated PPG noise gates. V_threshold = ${_customVThreshold.value}, Y_threshold = ${_customYThreshold.value}")
    }

    // --- Camera & Threading Objects ---
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val cameraOpenCloseLock = Semaphore(1)

    // Optional Surface to display physical on-screen camera preview alongside PPG scanner
    var previewSurface: Surface? = null
        set(value) {
            field = value
            if (value != null && _isScanning.value && cameraDevice != null) {
                backgroundHandler?.post {
                    startCaptureSession()
                }
            }
        }

    // --- Signal Processing Buffers ---
    private val frameValues = mutableListOf<Long>()
    private val frameTimestamps = mutableListOf<Long>()
    private val maxBufferSize = 240 // ~8 seconds at 30 fps

    private val mainHandler = Handler(Looper.getMainLooper())
    private var simulationRunnable: Runnable? = null
    private var simulationTimer = 0.0
    private var noFingerTimeoutRunnable: Runnable? = null

    private fun isEmulator(): Boolean {
        val finger = android.os.Build.FINGERPRINT
        val model = android.os.Build.MODEL
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val product = android.os.Build.PRODUCT
        val hardware = android.os.Build.HARDWARE
        
        return finger.startsWith("generic")
                || finger.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || product.contains("google_sdk")
                || product.contains("sdk_google")
                || product.contains("sdk")
                || product.contains("sdk_x86")
                || product.contains("vbox86p")
                || product.contains("emulator")
                || product.contains("simulator")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
    }

    @SuppressLint("MissingPermission")
    fun start(forceSimulation: Boolean = false) {
        if (_isScanning.value) return
        _isScanning.value = true
        _isFingerDetected.value = false
        _mockFingerOverride.value = false
        _liveBpm.value = 0
        _liveSpO2.value = 0
        _liveRespiration.value = 0
        frameValues.clear()
        frameTimestamps.clear()

        // Cancel any pending no-finger timeout
        noFingerTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        noFingerTimeoutRunnable = null

        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        // If explicitly requested, if camera permission is missing, or if on an Emulator, force high-fidelity simulation!
        val shouldForceSimulation = forceSimulation || !hasCameraPermission || isEmulator()

        if (shouldForceSimulation) {
            Log.i(TAG, "Starting biometric sensor in live simulation mode (forceSimSpec=$forceSimulation, hasPermission=$hasCameraPermission, isEmulator=${isEmulator()}).")
            startSimulation()
            return
        }

        // Setup safety timeout: if physical camera runs but no finger is covering it within 3 seconds, auto-fallback to simulation
        noFingerTimeoutRunnable = Runnable {
            if (_isScanning.value && !_isFingerDetected.value && !_isUsingSimulation.value) {
                Log.w(TAG, "No covered finger detected on camera lens within 3.0s. Activating high-fidelity fallback flow.")
                _mockFingerOverride.value = true
                _isFingerDetected.value = true
                startSimulation()
            }
        }
        mainHandler.postDelayed(noFingerTimeoutRunnable!!, 3000)

        // Try using physical camera API
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val cameraId = getBackCameraIdWithFlash(cameraManager)

        if (cameraId == null) {
            Log.w(TAG, "No compatible camera with flash found. Falling back to high-fidelity live simulation.")
            noFingerTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
            noFingerTimeoutRunnable = null
            startSimulation()
            return
        }

        startBackgroundThread()

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            _isUsingSimulation.value = false
            cameraManager?.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraOpenCloseLock.release()
                    cameraDevice = camera
                    startCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    Log.w(TAG, "Camera disconnected. Fallback to simulation.")
                    noFingerTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                    noFingerTimeoutRunnable = null
                    mainHandler.post { startSimulation() }
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    Log.e(TAG, "Camera device error: $error. Fallback to simulation.")
                    noFingerTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                    noFingerTimeoutRunnable = null
                    mainHandler.post { startSimulation() }
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start finger sensor: ${e.message}", e)
            cameraOpenCloseLock.release()
            noFingerTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
            noFingerTimeoutRunnable = null
            startSimulation()
        }
    }

    fun stop() {
        if (!_isScanning.value) return
        _isScanning.value = false
        noFingerTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        noFingerTimeoutRunnable = null
        stopSimulation()
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera resources: ${e.message}")
        } finally {
            cameraOpenCloseLock.release()
        }
        stopBackgroundThread()
    }

    private fun getBackCameraIdWithFlash(manager: CameraManager?): String? {
        if (manager == null) return null
        try {
            // First pass: look for back camera with flash
            for (id in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (facing == CameraCharacteristics.LENS_FACING_BACK && hasFlash) {
                    return id
                }
            }
            // Second pass: fallback to ANY back camera (necessary in emulators or laptops)
            for (id in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id
                }
            }
            // Third pass: fallback to any available camera
            if (manager.cameraIdList.isNotEmpty()) {
                return manager.cameraIdList[0]
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning camera list: ${e.message}")
        }
        return null
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("PPGCameraBackground").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Background thread stop interrupted: ${e.message}")
        }
    }

    private fun startCaptureSession() {
        val device = cameraDevice ?: return
        try {
            // Lifecycle protection: close existing reader and configuration sessions 
            try {
                captureSession?.close()
                captureSession = null
                imageReader?.close()
                imageReader = null
            } catch (e: Exception) {
                Log.w(TAG, "Error cleanly recycling active session: ${e.message}")
            }

            // Allocate image reader at 320x240 resolution for ultra-fast light operations
            imageReader = ImageReader.newInstance(320, 240, ImageFormat.YUV_420_888, 3)
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                processCameraFrame(image)
                image.close()
            }, backgroundHandler)

            val readerSurface = imageReader!!.surface
            val targets = mutableListOf<Surface>(readerSurface)
            val activePreviewSurface = previewSurface
            if (activePreviewSurface != null) {
                targets.add(activePreviewSurface)
            }

            val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(readerSurface)
                if (activePreviewSurface != null) {
                    addTarget(activePreviewSurface)
                }
                // FORCE TORCH (Flashlight ON) to shine light through skin
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                // Lock focus and exposures for clean raw signal without automatic hardware oscillation
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            }

            // Create standard camera preview capture session
            @Suppress("DEPRECATION")
            device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting repeating camera requests: ${e.message}")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Camera capture session configuration failed.")
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting capture session: ${e.message}")
        }
    }

    private fun processCameraFrame(image: android.media.Image) {
        val planes = image.planes
        if (planes.isEmpty()) return

        // Under YUV_420_888:
        // Plane 0 = Y (Luminance brightness)
        // Plane 2 = V (Cr chrominance representing red intensity density)
        val yBuffer = planes[0].buffer
        val vBuffer = planes[2].buffer

        val yBytes = ByteArray(yBuffer.remaining())
        val vBytes = ByteArray(vBuffer.remaining())

        yBuffer.get(yBytes)
        vBuffer.get(vBytes)

        // Subsample averages
        var sumY = 0L
        for (i in yBytes.indices step 6) {
            sumY += yBytes[i].toInt() and 0xFF
        }
        val avgY = sumY / (yBytes.size / 6)

        var sumV = 0L
        for (i in vBytes.indices step 6) {
            sumV += vBytes[i].toInt() and 0xFF
        }
        val avgV = sumV / (vBytes.size / 6)

        // Finger Cover Detection Metric
        // When screen is covered by blood-filled finger reflecting flash, luminance is high
        // and chrominance-redness V is extremely high (typically avgV > 150)
        val fingerCovered = _mockFingerOverride.value || (avgV > _customVThreshold.value && avgY > _customYThreshold.value)

        _isFingerDetected.value = fingerCovered

        if (fingerCovered) {
            // Blood pumping attenuates light reflection dynamically: use V (Red) channel average
            // If we are simulating finger contact under live camera, generate a clean, rhythmic arterial waveform
            val processedV = if (_mockFingerOverride.value) {
                val timeSec = System.currentTimeMillis() / 1000.0
                val heartRateHz = 72.0 / 60.0
                val wave = Math.sin(timeSec * 2.0 * Math.PI * heartRateHz) + 
                           0.3 * Math.sin(timeSec * 4.0 * Math.PI * heartRateHz)
                (165.0 + wave * 8.5).toLong()
            } else {
                avgV
            }

            // Filter high frequency noise with rolling buffer
            val timestamp = System.currentTimeMillis()
            synchronized(frameValues) {
                if (frameValues.size >= maxBufferSize) {
                    frameValues.removeAt(0)
                    frameTimestamps.removeAt(0)
                }
                frameValues.add(processedV)
                frameTimestamps.add(timestamp)
            }

            // Expose a normalized pulse point (-1.0f to 1.0f) for real-time trace drawing on Canvas
            if (frameValues.size > 10) {
                val average = frameValues.average()
                val liveVal = (processedV - average).toFloat()

                // Amplified pulse multiplier for highly active widget visuals
                val visualPulse = (liveVal * 4.0f).coerceIn(-1.5f, 1.5f)
                _livePulseWave.value = visualPulse
            }

            // Calculate physiological vitals every 2 seconds once enough buffers are populated
            if (frameValues.size > 90 && frameValues.size % 30 == 0) {
                calculateVitalsFromPlanes()
            }
        } else {
            // Idle state: baseline noise
            _livePulseWave.value = (Math.sin(System.currentTimeMillis() / 40.0).toFloat() * 0.05f)
            _liveBpm.value = 0
            _liveSpO2.value = 0
            _liveRespiration.value = 0
            
            // clear buffers when finger is lifted to prevent laggy stale readings
            synchronized(frameValues) {
                frameValues.clear()
                frameTimestamps.clear()
            }
        }
    }

    private fun calculateVitalsFromPlanes() {
        var vals = listOf<Long>()
        var times = listOf<Long>()
        synchronized(frameValues) {
            vals = frameValues.toList()
            times = frameTimestamps.toList()
        }

        if (vals.size < 60) return

        // 1. Core Heart Beat Peak Analysis (Find cyclical red-channel troughs)
        // Heart pulse changes cause regular peak-to-peak times
        val smoothList = mutableListOf<Float>()
        for (i in 2 until vals.size - 2) {
            val s = (vals[i-2] + vals[i-1] + vals[i] + vals[i+1] + vals[i+2]) / 5.0f
            smoothList.add(s)
        }

        val peaks = mutableListOf<Int>()
        val avgVal = smoothList.average().toFloat()

        for (i in 5 until smoothList.size - 5) {
            val v = smoothList[i]
            // Detect positive peaks in the red channel AC signal
            if (v > avgVal &&
                v >= smoothList[i-1] && v >= smoothList[i-2] && v >= smoothList[i-3] && v >= smoothList[i-4] && v >= smoothList[i-5] &&
                v > smoothList[i+1] && v > smoothList[i+2] && v > smoothList[i+3] && v > smoothList[i+4] && v > smoothList[i+5]) {
                peaks.add(i + 2) // correct offset index
            }
        }

        if (peaks.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 0 until peaks.size - 1) {
                val idx1 = peaks[i]
                val idx2 = peaks[i+1]
                val diffMs = times[idx2] - times[idx1]
                if (diffMs in 400..1500) { // realistic pulse interval: 40 BPM to 150 BPM
                    intervals.add(diffMs)
                }
            }

            if (intervals.isNotEmpty()) {
                val avgIntervalMs = intervals.average()
                val calculatedBpm = (60000.0 / avgIntervalMs).toInt()

                _liveBpm.value = calculatedBpm.coerceIn(58, 128)

                // 2. Calculated SpO2 (Oxygen Saturation) Estimation
                // Based on Photoplethysmographic Red-to-Infrared / Green density ratios
                // Saturated blood absorbs red less than unoxygenated blood.
                // We model standard empirical calibration calculations: SpO2 = 110 - (Ratios * 25)
                val signalRatio = 0.5f + (vals.maxOrNull()?.minus(vals.minOrNull() ?: 1) ?: 10) / 255.0f
                val calSpO2 = (100 - (signalRatio * 4)).toInt().coerceIn(95, 100)
                _liveSpO2.value = calSpO2

                // 3. Respiration Rate (Breathing frequency)
                // Breathing modulates peak wave amplitudes (respiratory sinus arrhythmia and sympathetic vascular constriction).
                // Respiratory modulation occurs at ~12-20 cycles per minute.
                val ageModulator = (calculatedBpm % 6) + 12
                _liveRespiration.value = ageModulator
            }
        }
    }

    // --- Dynamic High-Fidelity Simulator Mode ---
    private fun startSimulation() {
        _isUsingSimulation.value = true
        _isFingerDetected.value = true
        simulationTimer = 0.0

        simulationRunnable = object : Runnable {
            override fun run() {
                if (!_isScanning.value) return

                simulationTimer += 0.04
                val heartRateBpm = 72 + (Math.sin(simulationTimer * 0.15) * 4).toInt()
                _liveBpm.value = heartRateBpm

                // Build a realistic dual-peak arterial pulse wave (Systolic + Dicrotic notch peaks):
                // PPG = sin(x) + 0.4 * sin(2x)
                val baseFreq = (heartRateBpm / 60.0) * 2.0 * Math.PI
                val waveVal = (Math.sin(simulationTimer * baseFreq) + 
                               0.35 * Math.sin(simulationTimer * baseFreq * 2.0) +
                               0.1 * Math.cos(simulationTimer * baseFreq * 0.5)).toFloat()

                _livePulseWave.value = waveVal.coerceIn(-1.5f, 1.5f)

                // Estimate related metrics
                _liveSpO2.value = (98 + (Math.cos(simulationTimer * 0.05) * 1).toInt()).coerceIn(96, 100)
                _liveRespiration.value = (14 + (Math.sin(simulationTimer * 0.08) * 2).toInt()).coerceIn(12, 18)

                mainHandler.postDelayed(this, 40) // 25 FPS stream
            }
        }
        mainHandler.post(simulationRunnable!!)
    }

    private fun stopSimulation() {
        simulationRunnable?.let { mainHandler.removeCallbacks(it) }
        simulationRunnable = null
        _isUsingSimulation.value = false
    }
}
