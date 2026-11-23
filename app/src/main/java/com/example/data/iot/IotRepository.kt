package com.example.data.iot

import com.example.data.local.IotDeviceEntity
import com.example.data.local.StillroomDao
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.SocketTimeoutException

sealed class BrokerConnectionState {
    data object Idle : BrokerConnectionState()
    data object Connecting : BrokerConnectionState()
    data class Connected(val brokerLabel: String) : BrokerConnectionState()
    data class Error(val message: String) : BrokerConnectionState()
}

class IotRepository(
    private val dao: StillroomDao,
    private val httpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _deviceStatus = MutableStateFlow<Map<Long, String>>(emptyMap())
    val deviceStatus: StateFlow<Map<Long, String>> = _deviceStatus.asStateFlow()

    private val _brokerStates = MutableStateFlow<Map<String, BrokerConnectionState>>(emptyMap())
    val brokerStates: StateFlow<Map<String, BrokerConnectionState>> = _brokerStates.asStateFlow()

    private val mqttJobs = mutableMapOf<String, Job>()
    private val mqttClients = mutableMapOf<String, MqttLightClient>()
    private var httpPollingJob: Job? = null

    fun syncDevices(devices: List<IotDeviceEntity>) {
        // Group MQTT devices by host:port:username
        val mqttGroups = devices.filter { it.protocol == "MQTT" }
            .groupBy { "${it.hostOrUrl.trim()}:${it.port}:${it.username}" }

        val obsoleteKeys = mqttJobs.keys - mqttGroups.keys
        obsoleteKeys.forEach { key ->
            mqttJobs.remove(key)?.cancel()
            mqttClients.remove(key)?.disconnect()
        }

        mqttGroups.forEach { (brokerKey, groupDevices) ->
            if (mqttJobs[brokerKey]?.isActive != true) {
                val sample = groupDevices.first()
                startMqttLoop(brokerKey, sample, groupDevices)
            }
        }

        val httpDevices = devices.filter { it.protocol == "HTTP" }
        httpPollingJob?.cancel()
        if (httpDevices.isNotEmpty()) {
            httpPollingJob = scope.launch {
                while (isActive) {
                    pollHttpDevices(httpDevices)
                    delay(15_000L)
                }
            }
        }
    }

    private fun startMqttLoop(
        brokerKey: String,
        sample: IotDeviceEntity,
        devices: List<IotDeviceEntity>
    ) {
        mqttJobs[brokerKey] = scope.launch {
            while (isActive) {
                updateBrokerState(brokerKey, BrokerConnectionState.Connecting)
                val client = MqttLightClient(
                    host = sample.hostOrUrl,
                    port = sample.port,
                    username = sample.username,
                    password = sample.password
                )
                val conn = client.connect()
                if (conn.isSuccess) {
                    mqttClients[brokerKey] = client
                    updateBrokerState(brokerKey, BrokerConnectionState.Connected("${sample.hostOrUrl}:${sample.port}"))
                    devices.forEach { dev ->
                        if (dev.topicOrJsonPath.isNotBlank()) {
                            client.subscribe(dev.topicOrJsonPath)
                        }
                    }

                    val pingJob = launch {
                        while (isActive && client.isConnected) {
                            delay(25_000L)
                            client.ping()
                        }
                    }

                    try {
                        while (isActive && client.isConnected) {
                            try {
                                val msg = client.readNextMessage()
                                if (msg != null) {
                                    val (topic, payload) = msg
                                    devices.filter { it.topicOrJsonPath == topic }.forEach { match ->
                                        dao.updateIotReading(match.id, payload.trim(), System.currentTimeMillis())
                                        setDeviceNote(match.id, "Live via MQTT")
                                    }
                                }
                            } catch (_: SocketTimeoutException) {
                                // Normal read timeout; keepAlive ping handles health
                            }
                        }
                    } catch (t: Throwable) {
                        updateBrokerState(brokerKey, BrokerConnectionState.Error(t.message ?: "MQTT disconnected"))
                    } finally {
                        pingJob.cancel()
                        client.disconnect()
                        mqttClients.remove(brokerKey)
                    }
                } else {
                    val errMsg = conn.exceptionOrNull()?.message ?: "Broker unreachable"
                    updateBrokerState(brokerKey, BrokerConnectionState.Error(errMsg))
                    devices.forEach { setDeviceNote(it.id, errMsg) }
                }
                delay(8_000L)
            }
        }
    }

    suspend fun pollHttpDevices(httpDevices: List<IotDeviceEntity>) {
        httpDevices.forEach { dev ->
            runCatching {
                val req = Request.Builder().url(dev.hostOrUrl.trim()).get().build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val raw = resp.body?.string() ?: error("Empty response")
                    val extracted = extractJsonOrText(raw, dev.topicOrJsonPath.trim())
                    dao.updateIotReading(dev.id, extracted, System.currentTimeMillis())
                    setDeviceNote(dev.id, "HTTP ${resp.code} OK")
                }
            }.onFailure { err ->
                setDeviceNote(dev.id, err.message ?: "Endpoint unreachable")
            }
        }
    }

    suspend fun toggleSwitchDevice(device: IotDeviceEntity, turnOn: Boolean) {
        val payload = if (turnOn) device.publishPayloadOn else device.publishPayloadOff
        if (device.protocol == "MQTT") {
            val brokerKey = "${device.hostOrUrl.trim()}:${device.port}:${device.username}"
            val client = mqttClients[brokerKey]
            if (client != null && client.isConnected) {
                val res = client.publish(device.topicOrJsonPath, payload, retain = true)
                if (res.isSuccess) {
                    dao.updateIotReading(device.id, payload, System.currentTimeMillis())
                    setDeviceNote(device.id, "Published '$payload'")
                } else {
                    setDeviceNote(device.id, res.exceptionOrNull()?.message ?: "Publish failed")
                }
            } else {
                setDeviceNote(device.id, "MQTT broker not connected")
            }
        } else {
            scope.launch {
                runCatching {
                    val body = payload.toRequestBody("text/plain; charset=utf-8".toMediaType())
                    val req = Request.Builder().url(device.hostOrUrl.trim()).post(body).build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) error("HTTP ${resp.code}")
                        dao.updateIotReading(device.id, payload, System.currentTimeMillis())
                        setDeviceNote(device.id, "POST '$payload' (${resp.code})")
                    }
                }.onFailure { err ->
                    setDeviceNote(device.id, err.message ?: "Command failed")
                }
            }
        }
    }

    private fun extractJsonOrText(raw: String, jsonPath: String): String {
        if (jsonPath.isBlank()) return raw.take(40)
        return runCatching {
            var current: Any? = JSONObject(raw)
            for (segment in jsonPath.split(".")) {
                current = (current as? JSONObject)?.opt(segment)
            }
            current?.toString() ?: raw.take(40)
        }.getOrElse { raw.take(40) }
    }

    private fun updateBrokerState(key: String, state: BrokerConnectionState) {
        _brokerStates.value = _brokerStates.value + (key to state)
    }

    private fun setDeviceNote(id: Long, note: String) {
        _deviceStatus.value = _deviceStatus.value + (id to note)
    }

    fun shutdown() {
        httpPollingJob?.cancel()
        mqttJobs.values.forEach { it.cancel() }
        mqttClients.values.forEach { it.disconnect() }
        mqttJobs.clear()
        mqttClients.clear()
    }
}
