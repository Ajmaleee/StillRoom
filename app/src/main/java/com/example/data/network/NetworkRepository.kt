package com.example.data.network

import com.example.data.local.RssFeedSourceEntity
import com.example.data.local.SettingsDataStore
import com.example.domain.model.ResourceState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.roundToInt

class NetworkRepository(
    private val settingsDataStore: SettingsDataStore
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "StillroomAmbientDashboard/1.0 (Android)")
                .build()
            chain.proceed(req)
        }
        .build()

    private val weatherAdapter = moshi.adapter(OpenMeteoResponse::class.java)
    private val geoAdapter = moshi.adapter(GeocodingResponse::class.java)
    private val ghUserAdapter = moshi.adapter(GitHubUserDto::class.java)
    private val ghRepoListAdapter = moshi.adapter<List<GitHubRepoDto>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, GitHubRepoDto::class.java)
    )
    private val ghEventListAdapter = moshi.adapter<List<GitHubEventDto>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, GitHubEventDto::class.java)
    )
    private val nasaAdapter = moshi.adapter(NasaApodDto::class.java)
    private val issAdapter = moshi.adapter(IssPositionDto::class.java)
    private val wikiAdapter = moshi.adapter(WikiSummaryDto::class.java)

    suspend fun fetchWeather(
        cityName: String,
        lat: Double,
        lon: Double,
        useFahrenheit: Boolean
    ): ResourceState<WeatherSnapshot> = withContext(Dispatchers.IO) {
        val unitParam = if (useFahrenheit) "fahrenheit" else "celsius"
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,precipitation_probability,weather_code" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max" +
            "&temperature_unit=$unitParam&wind_speed_unit=kmh&timezone=auto&forecast_days=5"

        val networkResult = runCatching {
            val body = executeGet(url)
            val dto = weatherAdapter.fromJson(body) ?: error("Empty weather payload")
            settingsDataStore.cacheWeatherJson(body)
            mapWeatherDto(dto, cityName)
        }

        networkResult.fold(
            onSuccess = { ResourceState.Success(it, System.currentTimeMillis(), isCachedOffline = false) },
            onFailure = { err ->
                val (cachedJson, cachedAt) = settingsDataStore.cachedWeatherFlow.first()
                val cached = cachedJson?.let { runCatching { weatherAdapter.fromJson(it) }.getOrNull() }
                if (cached != null) {
                    ResourceState.Success(
                        data = mapWeatherDto(cached, cityName),
                        updatedAtMillis = cachedAt,
                        isCachedOffline = true
                    )
                } else {
                    ResourceState.Error(err.message ?: "Unable to fetch weather")
                }
            }
        )
    }

    suspend fun searchCities(query: String): List<GeoCityDto> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=6&language=en&format=json"
        runCatching {
            val json = executeGet(url)
            geoAdapter.fromJson(json)?.results ?: emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun fetchGitHub(username: String): ResourceState<GitHubSnapshot> = withContext(Dispatchers.IO) {
        if (username.isBlank()) return@withContext ResourceState.Error("Configure a GitHub username in Settings")
        val clean = username.trim()
        runCatching {
            val userJson = executeGet("https://api.github.com/users/$clean")
            val user = ghUserAdapter.fromJson(userJson) ?: error("User not found")
            val reposJson = executeGet("https://api.github.com/users/$clean/repos?sort=updated&per_page=4")
            val repos = ghRepoListAdapter.fromJson(reposJson) ?: emptyList()
            val eventsJson = executeGet("https://api.github.com/users/$clean/events/public?per_page=5")
            val events = ghEventListAdapter.fromJson(eventsJson) ?: emptyList()
            GitHubSnapshot(user = user, topRepos = repos, recentEvents = events)
        }.fold(
            onSuccess = { ResourceState.Success(it) },
            onFailure = { ResourceState.Error(it.message ?: "GitHub request failed") }
        )
    }

    suspend fun fetchNasaApod(apiKey: String): ResourceState<NasaApodDto> = withContext(Dispatchers.IO) {
        val key = apiKey.ifBlank { "DEMO_KEY" }
        runCatching {
            val json = executeGet("https://api.nasa.gov/planetary/apod?api_key=$key")
            nasaAdapter.fromJson(json) ?: error("Invalid NASA response")
        }.fold(
            onSuccess = { ResourceState.Success(it) },
            onFailure = { ResourceState.Error(it.message ?: "NASA APOD unavailable") }
        )
    }

    suspend fun fetchIssPosition(): ResourceState<IssPositionDto> = withContext(Dispatchers.IO) {
        runCatching {
            val json = executeGet("https://api.wheretheiss.at/v1/satellites/25544")
            issAdapter.fromJson(json) ?: error("Invalid ISS telemetry")
        }.fold(
            onSuccess = { ResourceState.Success(it) },
            onFailure = { ResourceState.Error(it.message ?: "ISS telemetry unavailable") }
        )
    }

    suspend fun fetchRandomWikipedia(): ResourceState<WikiSummaryDto> = withContext(Dispatchers.IO) {
        runCatching {
            val json = executeGet("https://en.wikipedia.org/api/rest_v1/page/random/summary")
            wikiAdapter.fromJson(json) ?: error("Invalid Wikipedia summary")
        }.fold(
            onSuccess = { ResourceState.Success(it) },
            onFailure = { ResourceState.Error(it.message ?: "Wikipedia unavailable") }
        )
    }

    suspend fun fetchRssFeeds(feeds: List<RssFeedSourceEntity>): ResourceState<List<RssArticle>> = withContext(Dispatchers.IO) {
        val active = feeds.filter { it.enabled }
        if (active.isEmpty()) return@withContext ResourceState.Error("No RSS feeds enabled")
        val combined = mutableListOf<RssArticle>()
        var lastError: String? = null
        for (feed in active) {
            runCatching {
                val xml = executeGet(feed.url)
                RssParser.parse(xml, feed.title, limit = 6)
            }.onSuccess { combined.addAll(it) }
                .onFailure { lastError = it.message }
        }
        if (combined.isNotEmpty()) {
            ResourceState.Success(combined.take(14))
        } else {
            ResourceState.Error(lastError ?: "Unable to load RSS feeds")
        }
    }

    private fun executeGet(url: String): String {
        val req = Request.Builder().url(url).get().build()
        okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                error("HTTP ${resp.code}: ${resp.message}")
            }
            return resp.body?.string() ?: error("Empty body")
        }
    }

    private fun mapWeatherDto(dto: OpenMeteoResponse, cityName: String): WeatherSnapshot {
        val current = dto.current ?: CurrentWeatherDto()
        val daily = dto.daily ?: DailyWeatherDto()
        val hourly = dto.hourly ?: HourlyWeatherDto()

        val nowPrefix = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US).format(Date())
        val startIndex = hourly.time.indexOfFirst { it >= nowPrefix }.coerceAtLeast(0)
        val hourlyItems = (startIndex until (startIndex + 10).coerceAtMost(hourly.time.size)).map { idx ->
            val label = hourly.time[idx].substringAfter("T", "00:00").take(5)
            HourlyForecastItem(
                hourLabel = label,
                temp = hourly.temperature2m.getOrElse(idx) { current.temperature2m },
                rainProbability = hourly.precipitationProbability.getOrElse(idx) { 0 },
                weatherCode = hourly.weatherCode.getOrElse(idx) { current.weatherCode }
            )
        }

        val dailyItems = daily.time.indices.map { idx ->
            DailyForecastItem(
                dayLabel = formatDayLabel(daily.time[idx], idx),
                minTemp = daily.temperature2mMin.getOrElse(idx) { 0.0 },
                maxTemp = daily.temperature2mMax.getOrElse(idx) { 0.0 },
                weatherCode = daily.weatherCode.getOrElse(idx) { 0 },
                rainProbabilityMax = daily.precipitationProbabilityMax.getOrElse(idx) { 0 }
            )
        }

        val (moonName, moonIllum) = calculateMoonPhase()

        return WeatherSnapshot(
            cityName = cityName,
            temperature = current.temperature2m,
            apparentTemperature = current.apparentTemperature,
            humidityPercent = current.relativeHumidity2m,
            windSpeedKmh = current.windSpeed10m,
            weatherCode = current.weatherCode,
            conditionLabel = wmoDescription(current.weatherCode),
            rainProbabilityPercent = hourlyItems.firstOrNull()?.rainProbability
                ?: daily.precipitationProbabilityMax.firstOrNull() ?: 0,
            uvIndex = daily.uvIndexMax.firstOrNull() ?: 0.0,
            sunriseTime = daily.sunrise.firstOrNull()?.substringAfter("T")?.take(5) ?: "--:--",
            sunsetTime = daily.sunset.firstOrNull()?.substringAfter("T")?.take(5) ?: "--:--",
            moonPhaseName = moonName,
            moonIlluminationPercent = moonIllum,
            isDay = current.isDay == 1,
            hourly = hourlyItems,
            daily = dailyItems
        )
    }

    private fun formatDayLabel(isoDate: String, index: Int): String {
        if (index == 0) return "Today"
        return runCatching {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)
            if (parsed != null) SimpleDateFormat("EEE", Locale.US).format(parsed) else isoDate
        }.getOrDefault(isoDate)
    }

    companion object {
        fun wmoDescription(code: Int): String = when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Misty Fog"
            51, 53, 55 -> "Light Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75, 77 -> "Snowfall"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Calm"
        }

        fun calculateMoonPhase(nowMillis: Long = System.currentTimeMillis()): Pair<String, Int> {
            // Known New Moon epoch: 2000-01-06 18:14 UTC
            val knownNewMoon = 947182440000L
            val synodicPeriodMs = 29.53058867 * 86_400_000.0
            val phaseFraction = (((nowMillis - knownNewMoon) % synodicPeriodMs) + synodicPeriodMs) % synodicPeriodMs / synodicPeriodMs
            val illumination = ((1.0 - cos(phaseFraction * 2.0 * Math.PI)) * 50.0).roundToInt().coerceIn(0, 100)
            val name = when {
                phaseFraction < 0.0625 || phaseFraction >= 0.9375 -> "New Moon"
                phaseFraction < 0.1875 -> "Waxing Crescent"
                phaseFraction < 0.3125 -> "First Quarter"
                phaseFraction < 0.4375 -> "Waxing Gibbous"
                phaseFraction < 0.5625 -> "Full Moon"
                phaseFraction < 0.6875 -> "Waning Gibbous"
                phaseFraction < 0.8125 -> "Last Quarter"
                else -> "Waning Crescent"
            }
            return name to illumination
        }
    }
}
