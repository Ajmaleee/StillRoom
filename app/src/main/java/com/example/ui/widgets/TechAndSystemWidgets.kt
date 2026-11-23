package com.example.ui.widgets

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.device.SystemTelemetry
import com.example.data.local.RssFeedSourceEntity
import com.example.data.network.GitHubSnapshot
import com.example.data.network.IssPositionDto
import com.example.data.network.NasaApodDto
import com.example.data.network.RssArticle
import com.example.data.network.WikiSummaryDto
import com.example.domain.model.ResourceState
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.theme.CormorantFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.StillroomAmber
import com.example.ui.theme.StillroomAsh
import com.example.ui.theme.StillroomBone
import com.example.ui.theme.StillroomCharcoal
import com.example.ui.theme.StillroomLinen
import com.example.ui.theme.StillroomSage
import kotlin.math.roundToInt

@Composable
fun SystemTelemetryTile(
    telemetry: SystemTelemetry,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("system_telemetry_card")
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Memory, contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("SYSTEM TELEMETRY", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.4.sp, color = StillroomBone)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (telemetry.isCharging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryStd,
                        contentDescription = "Battery",
                        tint = if (telemetry.isCharging) StillroomSage else StillroomAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("${telemetry.batteryPercent}% · ${telemetry.chargingSource}", color = StillroomLinen, fontSize = 14.sp)
                }
                val tempText = telemetry.batteryTempCelsius?.let { "%.1f°C".format(it) } ?: "--"
                Text(tempText, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = StillroomBone)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Wifi, contentDescription = "Network", tint = StillroomSage, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(telemetry.networkType, color = StillroomLinen, fontSize = 13.sp)
                }
                Text(
                    "↓ %.1f KB/s · ↑ %.1f KB/s".format(telemetry.rxKbps, telemetry.txKbps),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = StillroomAmber
                )
            }

            Text(telemetry.wifiDetail, fontSize = 11.sp, color = StillroomAsh)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Uptime ${telemetry.uptimeFormatted}", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomBone)
                Text(
                    "Bright ${telemetry.systemBrightnessPercent ?: "--"}%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = StillroomBone
                )
                Text(
                    "RAM ${telemetry.usedRamMb}/${telemetry.totalRamMb} MB",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = StillroomBone
                )
            }

            Text("${telemetry.deviceModel} · ${telemetry.androidVersion}", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = StillroomAsh)
        }
    }
}

@Composable
fun RssFeedTile(
    state: ResourceState<List<RssArticle>>,
    feeds: List<RssFeedSourceEntity>,
    onRefresh: () -> Unit,
    onAddFeed: (title: String, url: String) -> Unit,
    onDeleteFeed: (Long) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showManage by remember { mutableStateOf(false) }

    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("rss_feed_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.RssFeed, contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("RSS & WEB FEEDS", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.4.sp, color = StillroomBone)
                }
                Row {
                    IconButton(onClick = { showManage = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Outlined.Add, contentDescription = "Manage RSS feeds", tint = StillroomAmber)
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh RSS", tint = StillroomBone)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when (state) {
                is ResourceState.Idle, is ResourceState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = StillroomAmber)
                }
                is ResourceState.Error -> {
                    Text(state.message, fontSize = 13.sp, color = StillroomBone)
                }
                is ResourceState.Success -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.data.take(6).forEach { article ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (article.link.startsWith("http")) {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.link)))
                                            }
                                        }
                                    }
                            ) {
                                Text(article.feedTitle.uppercase(), fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = StillroomSage)
                                Text(article.title, color = StillroomLinen, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManage) {
        var feedTitle by remember { mutableStateOf("") }
        var feedUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManage = false },
            containerColor = StillroomCharcoal,
            title = { Text("RSS Sources", fontFamily = CormorantFontFamily, fontSize = 26.sp, color = StillroomLinen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    feeds.forEach { src ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(src.title, color = StillroomLinen, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            IconButton(onClick = { onDeleteFeed(src.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove feed", tint = StillroomAsh)
                            }
                        }
                    }
                    OutlinedTextField(value = feedTitle, onValueChange = { feedTitle = it }, label = { Text("Feed label") }, singleLine = true)
                    OutlinedTextField(value = feedUrl, onValueChange = { feedUrl = it }, label = { Text("https://…/rss") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (feedUrl.isNotBlank()) onAddFeed(feedTitle, feedUrl)
                    showManage = false
                }) { Text("Add Feed", color = StillroomAmber) }
            },
            dismissButton = {
                TextButton(onClick = { showManage = false }) { Text("Close", color = StillroomBone) }
            }
        )
    }
}

@Composable
fun GitHubTile(
    state: ResourceState<GitHubSnapshot>,
    username: String,
    onRefresh: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("github_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Code, contentDescription = null, tint = StillroomSage, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("GITHUB · @$username", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.2.sp, color = StillroomBone)
                }
                IconButton(onClick = onRefresh, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh GitHub", tint = StillroomBone)
                }
            }
            Spacer(Modifier.height(8.dp))
            when (state) {
                is ResourceState.Idle, is ResourceState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = StillroomAmber)
                is ResourceState.Error ->
                    Text(state.message, fontSize = 13.sp, color = StillroomBone)
                is ResourceState.Success -> {
                    val gh = state.data
                    Text(
                        "${gh.user.name ?: gh.user.login} · ${gh.user.publicRepos} repos · ${gh.user.followers} followers",
                        color = StillroomLinen,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    gh.topRepos.take(3).forEach { repo ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(repo.name, color = StillroomAmber, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${repo.language ?: "Code"} · ★ ${repo.stargazersCount}", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomBone)
                        }
                    }
                    if (gh.recentEvents.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        val latest = gh.recentEvents.first()
                        Text(
                            "Latest: ${latest.type.removeSuffix("Event")} on ${latest.repo?.name ?: ""}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = StillroomAsh,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NasaAndIssTile(
    nasaState: ResourceState<NasaApodDto>,
    issState: ResourceState<IssPositionDto>,
    onRefreshIss: () -> Unit,
    onRefreshNasa: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("nasa_iss_card")
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.SatelliteAlt, contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ISS ORBIT & NASA APOD", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.2.sp, color = StillroomBone)
                }
                IconButton(onClick = { onRefreshIss(); onRefreshNasa() }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh space telemetry", tint = StillroomBone)
                }
            }

            // ISS Live Telemetry & Mini Coordinate Plot
            when (issState) {
                is ResourceState.Success -> {
                    val iss = issState.data
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "ISS %.2f°, %.2f°".format(iss.latitude, iss.longitude),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = StillroomAmber
                        )
                        Text(
                            "${iss.altitude.roundToInt()} km · ${iss.velocity.roundToInt()} km/h",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = StillroomSage
                        )
                    }
                    Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        // Equator and prime meridian hairlines
                        drawLine(StillroomAsh.copy(alpha = 0.25f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1f)
                        drawLine(StillroomAsh.copy(alpha = 0.25f), Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1f)
                        val x = (((iss.longitude + 180.0) / 360.0) * size.width).toFloat().coerceIn(4f, size.width - 4f)
                        val y = (((90.0 - iss.latitude) / 180.0) * size.height).toFloat().coerceIn(4f, size.height - 4f)
                        drawCircle(StillroomAmber.copy(alpha = 0.28f), radius = 12f, center = Offset(x, y))
                        drawCircle(StillroomAmber, radius = 4.5f, center = Offset(x, y))
                    }
                }
                is ResourceState.Error -> Text(issState.message, fontSize = 12.sp, color = StillroomAsh)
                else -> Text("Acquiring ISS orbital coordinates…", fontSize = 12.sp, color = StillroomBone)
            }

            // NASA Astronomy Picture of the Day
            when (nasaState) {
                is ResourceState.Success -> {
                    val apod = nasaState.data
                    if (apod.mediaType == "image" && apod.url.isNotBlank()) {
                        AsyncImage(
                            model = apod.url,
                            contentDescription = apod.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                    Text(apod.title, fontFamily = CormorantFontFamily, fontSize = 20.sp, color = StillroomLinen)
                    Text(apod.explanation, fontFamily = PlusJakartaFontFamily, fontSize = 12.sp, color = StillroomBone, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                is ResourceState.Error -> Text(nasaState.message, fontSize = 12.sp, color = StillroomAsh)
                else -> Unit
            }
        }
    }
}

@Composable
fun WikipediaTile(
    state: ResourceState<WikiSummaryDto>,
    onNextRandom: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        onClick = onNextRandom,
        onLongClick = onLongPress,
        modifier = modifier.testTag("wikipedia_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TravelExplore, contentDescription = null, tint = StillroomSage, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("WIKIPEDIA DISCOVERY", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.4.sp, color = StillroomBone)
                }
                Icon(Icons.Outlined.Public, contentDescription = "Next random article", tint = StillroomAmber, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            when (state) {
                is ResourceState.Idle, is ResourceState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = StillroomAmber)
                is ResourceState.Error ->
                    Text(state.message, fontSize = 13.sp, color = StillroomBone)
                is ResourceState.Success -> {
                    val wiki = state.data
                    Text(wiki.title, fontFamily = CormorantFontFamily, fontSize = 24.sp, color = StillroomLinen)
                    if (!wiki.description.isNullOrBlank()) {
                        Text(wiki.description, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAmber)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(wiki.extract, fontSize = 13.sp, color = StillroomBone, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text("Tap for another article", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = StillroomAsh)
                }
            }
        }
    }
}
