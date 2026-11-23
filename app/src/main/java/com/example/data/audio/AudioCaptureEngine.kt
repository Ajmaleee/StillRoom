package com.example.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import com.example.domain.model.VisualizerConfig
import com.example.domain.model.VisualizerRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

sealed class AudioCaptureStatus {
    data object PermissionRequired : AudioCaptureStatus()
    data object IdleDisabled : AudioCaptureStatus()
    data object ActiveOutputMix : AudioCaptureStatus()
    data object ActiveMicrophone : AudioCaptureStatus()
    data class Unavailable(val reason: String) : AudioCaptureStatus()
}

data class AudioFrameData(
    val status: AudioCaptureStatus = AudioCaptureStatus.PermissionRequired,
    val bands: FloatArray = FloatArray(32),
    val waveform: FloatArray = FloatArray(64),
    val bassEnergy: Float = 0f,
    val midEnergy: Float = 0f,
    val trebleEnergy: Float = 0f,
    val overallRms: Float = 0f,
    val isSignalPresent: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFrameData) return false
        return status == other.status &&
            bands.contentEquals(other.bands) &&
            waveform.contentEquals(other.waveform) &&
            overallRms == other.overallRms
    }

    override fun hashCode(): Int = status.hashCode() * 31 + overallRms.hashCode()
}

class AudioCaptureEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _frame = MutableStateFlow(AudioFrameData())
    val frame: StateFlow<AudioFrameData> = _frame.asStateFlow()

    @Volatile
    private var config: VisualizerConfig = VisualizerConfig()

    private var visualizer: Visualizer? = null
    private var micJob: Job? = null
    private var smoothedBands = FloatArray(32)
    private var smoothedWaveform = FloatArray(64)

    fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun updateConfig(newConfig: VisualizerConfig) {
        val wasEnabled = config.enabled
        config = newConfig
        if (!newConfig.enabled) {
            stop()
            _frame.value = AudioFrameData(status = AudioCaptureStatus.IdleDisabled)
        } else if (!wasEnabled && hasAudioPermission()) {
            start()
        }
    }

    fun start() {
        if (!config.enabled) {
            _frame.value = AudioFrameData(status = AudioCaptureStatus.IdleDisabled)
            return
        }
        if (!hasAudioPermission()) {
            _frame.value = AudioFrameData(status = AudioCaptureStatus.PermissionRequired)
            return
        }
        if (visualizer != null || micJob?.isActive == true) return

        // Attempt system output mix Visualizer first; many devices restrict session 0 so fall back cleanly to AudioRecord mic
        val visualizerStarted = tryStartVisualizer()
        if (!visualizerStarted) {
            startMicrophoneCapture()
        }
    }

    fun stop() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        micJob?.cancel()
        micJob = null
    }

    private fun tryStartVisualizer(): Boolean {
        return runCatching {
            val v = Visualizer(0)
            val range = Visualizer.getCaptureSizeRange()
            val captureSize = min(1024, max(range[0], 512)).coerceAtMost(range[1])
            v.captureSize = captureSize
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        vis: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (waveform != null && config.enabled) {
                            processVisualizerWaveform(waveform)
                        }
                    }

                    override fun onFftDataCapture(
                        vis: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft != null && config.enabled) {
                            processVisualizerFft(fft)
                        }
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                true
            )
            v.enabled = true
            visualizer = v
            _frame.value = _frame.value.copy(status = AudioCaptureStatus.ActiveOutputMix)
            true
        }.getOrElse {
            runCatching { visualizer?.release() }
            visualizer = null
            false
        }
    }

    private fun processVisualizerWaveform(raw: ByteArray) {
        val out = FloatArray(64)
        val step = max(1, raw.size / 64)
        for (i in 0 until 64) {
            val idx = (i * step).coerceAtMost(raw.lastIndex)
            val sample = ((raw[idx].toInt() and 0xFF) - 128) / 128f
            val s = config.smoothing.coerceIn(0.1f, 0.94f)
            smoothedWaveform[i] = smoothedWaveform[i] * s + (sample * config.sensitivity).coerceIn(-1f, 1f) * (1f - s)
            out[i] = smoothedWaveform[i]
        }
        _frame.value = _frame.value.copy(waveform = out)
    }

    private fun processVisualizerFft(fft: ByteArray) {
        val binCount = fft.size / 2
        if (binCount < 4) return
        val magnitudes = FloatArray(binCount)
        magnitudes[0] = kotlin.math.abs(fft[0].toInt()).toFloat()
        for (k in 1 until binCount) {
            val re = fft[2 * k].toFloat()
            val im = fft[2 * k + 1].toFloat()
            magnitudes[k] = hypot(re, im)
        }
        publishMagnitudes(magnitudes, AudioCaptureStatus.ActiveOutputMix, scaleDivisor = 48f)
    }

    @SuppressLint("MissingPermission")
    private fun startMicrophoneCapture() {
        micJob = scope.launch {
            val sampleRate = 22050
            val fftSize = 512
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) {
                _frame.value = AudioFrameData(
                    status = AudioCaptureStatus.Unavailable("AudioRecord buffer unsupported on this device")
                )
                return@launch
            }

            val recorder = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    max(minBuf, fftSize * 4)
                )
            }.getOrNull()

            if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder?.release()
                _frame.value = AudioFrameData(
                    status = AudioCaptureStatus.Unavailable("Audio input hardware busy or unavailable")
                )
                return@launch
            }

            val pcm = ShortArray(fftSize)
            val re = DoubleArray(fftSize)
            val im = DoubleArray(fftSize)
            val magnitudes = FloatArray(fftSize / 2)

            try {
                recorder.startRecording()
                _frame.value = _frame.value.copy(status = AudioCaptureStatus.ActiveMicrophone)

                while (isActive && config.enabled) {
                    val read = recorder.read(pcm, 0, fftSize)
                    if (read > 0) {
                        // Waveform
                        val waveOut = FloatArray(64)
                        val waveStep = max(1, read / 64)
                        val smooth = config.smoothing.coerceIn(0.1f, 0.94f)
                        for (i in 0 until 64) {
                            val idx = (i * waveStep).coerceAtMost(read - 1)
                            val norm = (pcm[idx] / 32768f) * config.sensitivity * 2.2f
                            smoothedWaveform[i] = smoothedWaveform[i] * smooth + norm.coerceIn(-1f, 1f) * (1f - smooth)
                            waveOut[i] = smoothedWaveform[i]
                        }

                        // Hann window + Radix-2 FFT
                        for (i in 0 until fftSize) {
                            val sample = if (i < read) pcm[i] / 32768.0 else 0.0
                            val hann = 0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))
                            re[i] = sample * hann
                            im[i] = 0.0
                        }
                        fftInPlace(re, im)
                        for (k in 0 until fftSize / 2) {
                            magnitudes[k] = hypot(re[k], im[k]).toFloat()
                        }
                        _frame.value = _frame.value.copy(waveform = waveOut)
                        publishMagnitudes(magnitudes, AudioCaptureStatus.ActiveMicrophone, scaleDivisor = 12f)
                    }
                    delay(24L)
                }
            } catch (t: Throwable) {
                _frame.value = AudioFrameData(
                    status = AudioCaptureStatus.Unavailable(t.message ?: "Microphone capture stopped")
                )
            } finally {
                runCatching { recorder.stop() }
                runCatching { recorder.release() }
            }
        }
    }

    private fun publishMagnitudes(
        magnitudes: FloatArray,
        status: AudioCaptureStatus,
        scaleDivisor: Float
    ) {
        val rangeBounds = when (config.frequencyRange) {
            VisualizerRange.FULL -> 1 to (magnitudes.size - 1)
            VisualizerRange.BASS -> 1 to max(4, magnitudes.size / 6)
            VisualizerRange.MIDS -> max(4, magnitudes.size / 8) to max(12, magnitudes.size / 2)
            VisualizerRange.HIGHS -> max(12, magnitudes.size / 3) to (magnitudes.size - 1)
        }

        val startBin = rangeBounds.first.coerceIn(1, magnitudes.lastIndex)
        val endBin = rangeBounds.second.coerceIn(startBin, magnitudes.lastIndex)
        val span = max(1, endBin - startBin)
        val smooth = config.smoothing.coerceIn(0.1f, 0.94f)
        val newBands = FloatArray(32)

        for (b in 0 until 32) {
            val t0 = b / 32f
            val t1 = (b + 1) / 32f
            val i0 = startBin + (t0 * t0 * span).toInt()
            val i1 = max(i0 + 1, startBin + (t1 * t1 * span).toInt()).coerceAtMost(endBin + 1)
            var sum = 0f
            for (k in i0 until i1) {
                sum += magnitudes[k]
            }
            val avg = sum / max(1, i1 - i0)
            val db = ln(1.0 + avg * config.sensitivity * 2.5).toFloat() / ln(1.0 + scaleDivisor).toFloat()
            val clamped = db.coerceIn(0f, 1f)
            smoothedBands[b] = smoothedBands[b] * smooth + clamped * (1f - smooth)
            newBands[b] = smoothedBands[b]
        }

        val bass = (0 until 8).map { newBands[it] }.average().toFloat()
        val mid = (8 until 20).map { newBands[it] }.average().toFloat()
        val treble = (20 until 32).map { newBands[it] }.average().toFloat()
        val rms = newBands.average().toFloat()

        _frame.value = _frame.value.copy(
            status = status,
            bands = newBands,
            bassEnergy = bass,
            midEnergy = mid,
            trebleEnergy = treble,
            overallRms = rms,
            isSignalPresent = rms > 0.03f
        )
    }

    private fun fftInPlace(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wLenRe = cos(ang)
            val wLenIm = sin(ang)
            var i = 0
            while (i < n) {
                var wRe = 1.0
                var wIm = 0.0
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + len / 2] * wRe - im[i + k + len / 2] * wIm
                    val vIm = re[i + k + len / 2] * wIm + im[i + k + len / 2] * wRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + len / 2] = uRe - vRe
                    im[i + k + len / 2] = uIm - vIm
                    val nextWRe = wRe * wLenRe - wIm * wLenIm
                    wIm = wRe * wLenIm + wIm * wLenRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}
