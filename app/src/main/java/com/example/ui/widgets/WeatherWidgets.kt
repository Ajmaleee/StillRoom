package com.example.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.GeoCityDto
import com.example.data.network.WeatherSnapshot
import com.example.domain.model.ResourceState
import com.example.domain.model.WidgetSize
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.theme.CormorantFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.StillroomAmber
import com.example.ui.theme.StillroomAsh
import com.example.ui.theme.StillroomBone
import com.example.ui.theme.StillroomLinen
import com.example.ui.theme.StillroomSage
import kotlin.math.roundToInt

@Composable
fun WeatherAmbientTile(
    state: ResourceState<WeatherSnapshot>,
    cityName: String,
    useFahrenheit: Boolean,
    size: WidgetSize,
    onExpand: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unitSymbol = if (useFahrenheit) "°F" else "°C"

    LiquidGlassSurface(
        onClick = onExpand,
        onLongClick = onLongPress,
        modifier = modifier.testTag("weather_widget_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Weather location",
                        tint = StillroomAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = cityName.uppercase(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = StillroomBone
                    )
                }
                if (state is ResourceState.Success && state.isCachedOffline) {
                    Text(
                        text = "CACHED",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = StillroomAsh
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when (state) {
                is ResourceState.Idle, is ResourceState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = StillroomAmber
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Reading atmosphere…",
                            fontFamily = PlusJakartaFontFamily,
                            fontSize = 13.sp,
                            color = StillroomBone
                        )
                    }
                }
                is ResourceState.Error -> {
                    Text(
                        text = state.message,
                        fontFamily = PlusJakartaFontFamily,
                        fontSize = 13.sp,
                        color = StillroomBone
                    )
                }
                is ResourceState.Success -> {
                    val w = state.data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = weatherIconForCode(w.weatherCode, w.isDay),
                                contentDescription = w.conditionLabel,
                                tint = StillroomAmber,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "${w.temperature.roundToInt()}$unitSymbol",
                                fontFamily = CormorantFontFamily,
                                fontSize = if (size == WidgetSize.COMPACT) 36.sp else 46.sp,
                                fontWeight = FontWeight.Medium,
                                color = StillroomLinen
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = w.conditionLabel,
                                fontFamily = PlusJakartaFontFamily,
                                fontSize = 14.sp,
                                color = StillroomLinen
                            )
                            Text(
                                text = "Feels ${w.apparentTemperature.roundToInt()}$unitSymbol · Rain ${w.rainProbabilityPercent}%",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = StillroomBone
                            )
                        }
                    }

                    if (size != WidgetSize.COMPACT) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WeatherStatMicro("HUM", "${w.humidityPercent}%")
                            WeatherStatMicro("WIND", "${w.windSpeedKmh.roundToInt()} km/h")
                            WeatherStatMicro("UV", "${w.uvIndex}")
                            WeatherStatMicro("MOON", w.moonPhaseName.substringBefore(" "))
                        }
                    }

                    if (size == WidgetSize.EXPANDED && w.hourly.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            w.hourly.take(6).forEach { hr ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(hr.hourLabel, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = StillroomAsh)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${hr.temp.roundToInt()}°", fontFamily = PlusJakartaFontFamily, fontSize = 13.sp, color = StillroomLinen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherStatMicro(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = StillroomAsh
        )
        Text(
            text = value,
            fontFamily = PlusJakartaFontFamily,
            fontSize = 12.sp,
            color = StillroomLinen
        )
    }
}

@Composable
fun WeatherExpandedSheet(
    state: ResourceState<WeatherSnapshot>,
    cityName: String,
    useFahrenheit: Boolean,
    cityResults: List<GeoCityDto>,
    onSearchCity: (String) -> Unit,
    onSelectCity: (GeoCityDto) -> Unit,
    onUseCoarseLocation: () -> Unit,
    onToggleUnit: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val unitSymbol = if (useFahrenheit) "°F" else "°C"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Atmosphere · $cityName",
                fontFamily = CormorantFontFamily,
                fontSize = 28.sp,
                color = StillroomLinen
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onToggleUnit,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(if (useFahrenheit) "°F" else "°C", color = StillroomAmber, fontFamily = JetBrainsMonoFontFamily)
                }
                IconButton(
                    onClick = onUseCoarseLocation,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = "Use device location", tint = StillroomSage)
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh weather", tint = StillroomLinen)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 2) onSearchCity(it)
                },
                placeholder = { Text("Search city…", color = StillroomAsh) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = StillroomAmber) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StillroomAmber,
                    unfocusedBorderColor = StillroomAsh.copy(alpha = 0.4f),
                    focusedTextColor = StillroomLinen,
                    unfocusedTextColor = StillroomLinen
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("weather_city_search")
            )
        }

        if (cityResults.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cityResults.forEach { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCity(city)
                                query = ""
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = listOfNotNull(city.name, city.admin1, city.country).joinToString(", "),
                            color = StillroomLinen,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${city.latitude.toInt()}°, ${city.longitude.toInt()}°",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = StillroomAsh
                        )
                    }
                }
            }
        }

        if (state is ResourceState.Success) {
            val w = state.data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherMetricPill(Icons.Outlined.WaterDrop, "Humidity", "${w.humidityPercent}%")
                WeatherMetricPill(Icons.Outlined.Air, "Wind", "${w.windSpeedKmh.roundToInt()} km/h")
                WeatherMetricPill(Icons.Outlined.Umbrella, "Rain", "${w.rainProbabilityPercent}%")
                WeatherMetricPill(Icons.Outlined.WbSunny, "UV", "${w.uvIndex}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sunrise ${w.sunriseTime}", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = StillroomBone)
                Text("Sunset ${w.sunsetTime}", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = StillroomBone)
                Text("${w.moonPhaseName} (${w.moonIlluminationPercent}%)", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = StillroomAmber)
            }

            Text("HOURLY", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                w.hourly.forEach { hr ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(hr.hourLabel, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                        Spacer(Modifier.height(4.dp))
                        Icon(weatherIconForCode(hr.weatherCode, true), contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("${hr.temp.roundToInt()}$unitSymbol", fontSize = 13.sp, color = StillroomLinen)
                        Text("${hr.rainProbability}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = StillroomSage)
                    }
                }
            }

            Text("5-DAY OUTLOOK", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
            w.daily.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(day.dayLabel, color = StillroomLinen, fontSize = 14.sp, modifier = Modifier.width(68.dp))
                    Icon(weatherIconForCode(day.weatherCode, true), contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
                    Text("${day.rainProbabilityMax}% rain", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomSage)
                    Text(
                        "${day.minTemp.roundToInt()}$unitSymbol / ${day.maxTemp.roundToInt()}$unitSymbol",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        color = StillroomBone
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricPill(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = StillroomAmber, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = PlusJakartaFontFamily, fontSize = 14.sp, color = StillroomLinen)
        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = StillroomAsh)
    }
}

fun weatherIconForCode(code: Int, isDay: Boolean): ImageVector = when {
    code == 0 || code == 1 -> if (isDay) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
    code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99) -> Icons.Outlined.Umbrella
    else -> Icons.Outlined.Cloud
}
