package com.example.data.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class SystemTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val chargingSource: String,
    val batteryTempCelsius: Float?,
    val batteryVoltageMv: Int?,
    val networkType: String,
    val isConnected: Boolean,
    val wifiDetail: String,
    val rxKbps: Double,
    val txKbps: Double,
    val linkDownMbps: Int?,
    val uptimeFormatted: String,
    val systemBrightnessPercent: Int?,
    val usedRamMb: Long,
    val totalRamMb: Long,
    val deviceModel: String,
    val androidVersion: String
)

class SystemTelemetryProvider(private val context: Context) {

    fun observeTelemetry(intervalMs: Long = 2000L): Flow<SystemTelemetry> = flow {
        var lastRxBytes = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
        var lastTxBytes = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
        var lastSampleTime = SystemClock.elapsedRealtime()

        while (true) {
            val nowTime = SystemClock.elapsedRealtime()
            val dtSec = ((nowTime - lastSampleTime).coerceAtLeast(500L)) / 1000.0
            val curRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
            val curTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
            val rxKbps = if (curRx >= lastRxBytes) ((curRx - lastRxBytes) / 1024.0) / dtSec else 0.0
            val txKbps = if (curTx >= lastTxBytes) ((curTx - lastTxBytes) / 1024.0) / dtSec else 0.0
            lastRxBytes = curRx
            lastTxBytes = curTx
            lastSampleTime = nowTime

            emit(snapshot(rxKbps, txKbps))
            delay(intervalMs)
        }
    }

    fun snapshot(rxKbps: Double = 0.0, txKbps: Double = 0.0): SystemTelemetry {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        }
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Charging" else "Battery"
        }
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val tempC = if (tempTenths != Int.MIN_VALUE) tempTenths / 10f else null
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val networkType = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            else -> "Connected"
        }
        val linkDownMbps = caps?.linkDownstreamBandwidthKbps?.let { if (it > 0) it / 1000 else null }

        val wifiDetail = if (networkType == "Wi-Fi") {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val rssi = wm?.connectionInfo?.rssi
            if (rssi != null && rssi > -127) {
                "Signal ${rssi} dBm (SSID hidden by Android privacy without fine location)"
            } else {
                "Wi-Fi active"
            }
        } else {
            networkType
        }

        val uptimeSec = SystemClock.elapsedRealtime() / 1000L
        val days = uptimeSec / 86400
        val hours = (uptimeSec % 86400) / 3600
        val mins = (uptimeSec % 3600) / 60
        val uptimeStr = buildString {
            if (days > 0) append("${days}d ")
            append("${hours}h ${mins}m")
        }

        val sysBrightness = runCatching {
            val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            ((raw / 255f) * 100).toInt().coerceIn(0, 100)
        }.getOrNull()

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024L * 1024L)
        val availMb = memInfo.availMem / (1024L * 1024L)

        return SystemTelemetry(
            batteryPercent = pct.coerceIn(0, 100),
            isCharging = isCharging,
            chargingSource = source,
            batteryTempCelsius = tempC,
            batteryVoltageMv = voltage,
            networkType = networkType,
            isConnected = isConnected,
            wifiDetail = wifiDetail,
            rxKbps = rxKbps,
            txKbps = txKbps,
            linkDownMbps = linkDownMbps,
            uptimeFormatted = uptimeStr,
            systemBrightnessPercent = sysBrightness,
            usedRamMb = (totalMb - availMb).coerceAtLeast(0L),
            totalRamMb = totalMb,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
