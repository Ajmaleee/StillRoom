package com.example.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.iot.BrokerConnectionState
import com.example.data.local.IotDeviceEntity
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.theme.CormorantFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.StillroomAmber
import com.example.ui.theme.StillroomAsh
import com.example.ui.theme.StillroomBone
import com.example.ui.theme.StillroomCharcoal
import com.example.ui.theme.StillroomCrimson
import com.example.ui.theme.StillroomLinen
import com.example.ui.theme.StillroomSage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IotHabitatPanel(
    devices: List<IotDeviceEntity>,
    deviceStatus: Map<Long, String>,
    brokerStates: Map<String, BrokerConnectionState>,
    onSaveDevice: (IotDeviceEntity) -> Unit,
    onDeleteDevice: (Long) -> Unit,
    onToggleSwitch: (IotDeviceEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LiquidGlassSurface(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Hub, contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("IoT Habitat · MQTT & ESP32", fontFamily = CormorantFontFamily, fontSize = 26.sp, color = StillroomLinen)
                        Text(
                            if (devices.isEmpty()) "Configure real MQTT brokers, topics, or ESP32 HTTP endpoints"
                            else "${devices.size} configured endpoint(s)",
                            fontSize = 12.sp,
                            color = StillroomBone
                        )
                    }
                }
                TextButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_iot_device_btn")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = StillroomAmber)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Device", color = StillroomAmber, fontFamily = JetBrainsMonoFontFamily)
                }
            }
        }

        if (brokerStates.isNotEmpty()) {
            LiquidGlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("BROKER CONNECTIONS", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = StillroomAsh)
                    brokerStates.forEach { (key, st) ->
                        val (label, color) = when (st) {
                            BrokerConnectionState.Idle -> "$key · Idle" to StillroomAsh
                            BrokerConnectionState.Connecting -> "$key · Connecting…" to StillroomAmber
                            is BrokerConnectionState.Connected -> "${st.brokerLabel} · Connected" to StillroomSage
                            is BrokerConnectionState.Error -> "$key · ${st.message}" to StillroomCrimson
                        }
                        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = color)
                    }
                }
            }
        }

        if (devices.isEmpty()) {
            LiquidGlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No hardware devices added yet", fontFamily = PlusJakartaFontFamily, fontSize = 16.sp, color = StillroomLinen)
                    Text(
                        "Stillroom never simulates fake sensors. Tap 'Add Device' above to connect your Mosquitto MQTT broker (TCP port 1883) or an ESP32/Tasmota JSON HTTP endpoint on your local network.",
                        fontSize = 13.sp,
                        color = StillroomBone
                    )
                }
            }
        } else {
            devices.forEach { dev ->
                val statusNote = deviceStatus[dev.id] ?: "Awaiting reading…"
                val updatedLabel = if (dev.lastUpdatedMillis > 0L) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(dev.lastUpdatedMillis))
                } else "--:--"
                val icon = when (dev.deviceKind) {
                    "TEMPERATURE" -> Icons.Outlined.DeviceThermostat
                    "HUMIDITY" -> Icons.Outlined.WaterDrop
                    "SOIL_MOISTURE" -> Icons.Outlined.Grass
                    "SWITCH" -> Icons.Outlined.PowerSettingsNew
                    else -> Icons.Outlined.Sensors
                }

                LiquidGlassSurface(modifier = Modifier.fillMaxWidth().testTag("iot_device_card_${dev.id}")) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(icon, contentDescription = dev.deviceKind, tint = StillroomAmber, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(dev.name, fontSize = 16.sp, color = StillroomLinen)
                                Text(
                                    "${dev.protocol} · ${dev.hostOrUrl} · ${dev.topicOrJsonPath}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = StillroomAsh
                                )
                                Text("$statusNote · $updatedLabel", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = StillroomSage)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (dev.deviceKind == "SWITCH") {
                                val isOn = dev.lastReading.equals(dev.publishPayloadOn, ignoreCase = true) ||
                                    dev.lastReading == "1" || dev.lastReading.equals("true", ignoreCase = true)
                                Switch(
                                    checked = isOn,
                                    onCheckedChange = { onToggleSwitch(dev) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = StillroomAmber)
                                )
                            } else {
                                val valText = if (dev.lastReading.isBlank()) "—" else "${dev.lastReading}${dev.unit}"
                                Text(valText, fontFamily = CormorantFontFamily, fontSize = 30.sp, color = StillroomLinen)
                            }
                            IconButton(onClick = { onDeleteDevice(dev.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove IoT device", tint = StillroomAsh)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var protocol by remember { mutableStateOf("MQTT") }
        var hostOrUrl by remember { mutableStateOf("192.168.1.50") }
        var port by remember { mutableStateOf("1883") }
        var topicOrPath by remember { mutableStateOf("home/livingroom/temp") }
        var kind by remember { mutableStateOf("TEMPERATURE") }
        var unit by remember { mutableStateOf("°C") }
        var user by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = StillroomCharcoal,
            title = { Text("Add IoT Device", fontFamily = CormorantFontFamily, fontSize = 26.sp, color = StillroomLinen) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Device name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MQTT", "HTTP").forEach { proto ->
                            FilterChip(
                                selected = protocol == proto,
                                onClick = {
                                    protocol = proto
                                    if (proto == "HTTP" && !hostOrUrl.startsWith("http")) {
                                        hostOrUrl = "http://192.168.1.60/status"
                                        topicOrPath = "temperature"
                                    }
                                },
                                label = { Text(proto) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("TEMPERATURE", "HUMIDITY", "SOIL_MOISTURE", "SWITCH", "GENERIC").forEach { k ->
                            FilterChip(
                                selected = kind == k,
                                onClick = {
                                    kind = k
                                    unit = when (k) {
                                        "TEMPERATURE" -> "°C"
                                        "HUMIDITY", "SOIL_MOISTURE" -> "%"
                                        else -> ""
                                    }
                                },
                                label = { Text(k.take(4), fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = hostOrUrl,
                        onValueChange = { hostOrUrl = it },
                        label = { Text(if (protocol == "MQTT") "MQTT Broker Host/IP" else "HTTP Endpoint URL") },
                        singleLine = true
                    )
                    if (protocol == "MQTT") {
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it.filter { c -> c.isDigit() } },
                            label = { Text("Port (default 1883)") },
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = topicOrPath,
                        onValueChange = { topicOrPath = it },
                        label = { Text(if (protocol == "MQTT") "MQTT Topic" else "JSON Field Path (e.g. sensor.temp)") },
                        singleLine = true
                    )
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (°C, %, ppm…)") }, singleLine = true)
                    if (protocol == "MQTT") {
                        OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username (optional)") }, singleLine = true)
                        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password (optional)") }, singleLine = true)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank() && hostOrUrl.isNotBlank()) {
                        onSaveDevice(
                            IotDeviceEntity(
                                name = name.trim(),
                                protocol = protocol,
                                hostOrUrl = hostOrUrl.trim(),
                                port = port.toIntOrNull() ?: 1883,
                                topicOrJsonPath = topicOrPath.trim(),
                                deviceKind = kind,
                                unit = unit.trim(),
                                username = user.trim(),
                                password = pass.trim()
                            )
                        )
                    }
                    showAddDialog = false
                }) { Text("Save", color = StillroomAmber) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = StillroomBone) }
            }
        )
    }
}
