package com.example.data.iot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Minimal, dependency-free MQTT 3.1.1 TCP client supporting CONNECT, SUBSCRIBE,
 * PUBLISH (QoS 0), PINGREQ, and incoming PUBLISH packet parsing.
 */
class MqttLightClient(
    private val host: String,
    private val port: Int = 1883,
    private val clientId: String = "stillroom_${System.currentTimeMillis() % 100000}",
    private val username: String = "",
    private val password: String = ""
) {
    private var socket: Socket? = null
    private var output: OutputStream? = null
    private var input: DataInputStream? = null
    private var packetId = 1

    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val s = Socket()
            s.connect(InetSocketAddress(host.trim(), port), 6000)
            s.soTimeout = 15000
            socket = s
            output = s.getOutputStream()
            input = DataInputStream(s.getInputStream())

            sendConnectPacket()
            val header = input!!.readUnsignedByte()
            val remLen = readRemainingLength(input!!)
            val payload = ByteArray(remLen)
            input!!.readFully(payload)
            if ((header and 0xF0) != 0x20 || payload.size < 2 || payload[1].toInt() != 0) {
                val code = payload.getOrNull(1)?.toInt() ?: -1
                error("MQTT CONNACK rejected (code=$code)")
            }
        }
    }

    suspend fun subscribe(topic: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val out = output ?: error("Not connected")
            val pid = nextPacketId()
            val varAndPayload = ByteArrayOutputStream().apply {
                write((pid shr 8) and 0xFF)
                write(pid and 0xFF)
                writeUtf8(topic)
                write(0) // QoS 0
            }.toByteArray()
            synchronized(this@MqttLightClient) {
                out.write(0x82) // SUBSCRIBE
                writeRemainingLength(out, varAndPayload.size)
                out.write(varAndPayload)
                out.flush()
            }
        }
    }

    suspend fun publish(topic: String, message: String, retain: Boolean = false): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val out = output ?: error("Not connected")
                val body = ByteArrayOutputStream().apply {
                    writeUtf8(topic)
                    write(message.toByteArray(Charsets.UTF_8))
                }.toByteArray()
                val header = 0x30 or (if (retain) 0x01 else 0x00)
                synchronized(this@MqttLightClient) {
                    out.write(header)
                    writeRemainingLength(out, body.size)
                    out.write(body)
                    out.flush()
                }
            }
        }

    suspend fun ping(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val out = output ?: error("Not connected")
            synchronized(this@MqttLightClient) {
                out.write(0xC0)
                out.write(0x00)
                out.flush()
            }
        }
    }

    /** Blocking read for the next incoming PUBLISH (topic to payload); returns null on keepalive/SUBACK. */
    fun readNextMessage(): Pair<String, String>? {
        val inp = input ?: return null
        val fixedHeader = inp.readUnsignedByte()
        val packetType = fixedHeader and 0xF0
        val qos = (fixedHeader shr 1) and 0x03
        val remLen = readRemainingLength(inp)
        val data = ByteArray(remLen)
        if (remLen > 0) inp.readFully(data)

        if (packetType == 0x30 && data.size >= 2) {
            val topicLen = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            var offset = 2
            if (offset + topicLen <= data.size) {
                val topic = String(data, offset, topicLen, Charsets.UTF_8)
                offset += topicLen
                if (qos > 0) offset += 2 // skip packet identifier
                val msg = if (offset <= data.size) {
                    String(data, offset, data.size - offset, Charsets.UTF_8)
                } else ""
                return topic to msg
            }
        }
        return null
    }

    fun disconnect() {
        runCatching {
            output?.write(0xE0)
            output?.write(0x00)
            output?.flush()
        }
        runCatching { socket?.close() }
        socket = null
        output = null
        input = null
    }

    private fun sendConnectPacket() {
        val out = output ?: error("No output stream")
        val hasUser = username.isNotBlank()
        val hasPass = password.isNotBlank() && hasUser
        var flags = 0x02 // Clean session
        if (hasUser) flags = flags or 0x80
        if (hasPass) flags = flags or 0x40

        val body = ByteArrayOutputStream().apply {
            writeUtf8("MQTT")
            write(0x04) // Protocol level 4 (3.1.1)
            write(flags)
            write(0x00)
            write(0x3C) // KeepAlive = 60s
            writeUtf8(clientId)
            if (hasUser) writeUtf8(username)
            if (hasPass) writeUtf8(password)
        }.toByteArray()

        out.write(0x10) // CONNECT
        writeRemainingLength(out, body.size)
        out.write(body)
        out.flush()
    }

    private fun nextPacketId(): Int {
        packetId = (packetId % 65535) + 1
        return packetId
    }

    private fun ByteArrayOutputStream.writeUtf8(str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        write((bytes.size shr 8) and 0xFF)
        write(bytes.size and 0xFF)
        write(bytes)
    }

    private fun writeRemainingLength(out: OutputStream, length: Int) {
        var x = length
        do {
            var encodedByte = x % 128
            x /= 128
            if (x > 0) encodedByte = encodedByte or 128
            out.write(encodedByte)
        } while (x > 0)
    }

    private fun readRemainingLength(inp: DataInputStream): Int {
        var multiplier = 1
        var value = 0
        do {
            val encodedByte = inp.readUnsignedByte()
            value += (encodedByte and 127) * multiplier
            multiplier *= 128
            if (multiplier > 128 * 128 * 128) error("Malformed MQTT remaining length")
        } while ((encodedByte and 128) != 0)
        return value
    }
}
