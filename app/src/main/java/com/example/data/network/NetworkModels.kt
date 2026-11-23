package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Weather (Open-Meteo) ---

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "",
    val current: CurrentWeatherDto? = null,
    val hourly: HourlyWeatherDto? = null,
    val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int = 0,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
    @Json(name = "is_day") val isDay: Int = 1
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeoCityDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeoCityDto(
    val id: Long = 0L,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String = "",
    val admin1: String? = null
)

data class HourlyForecastItem(
    val hourLabel: String,
    val temp: Double,
    val rainProbability: Int,
    val weatherCode: Int
)

data class DailyForecastItem(
    val dayLabel: String,
    val minTemp: Double,
    val maxTemp: Double,
    val weatherCode: Int,
    val rainProbabilityMax: Int
)

data class WeatherSnapshot(
    val cityName: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val conditionLabel: String,
    val rainProbabilityPercent: Int,
    val uvIndex: Double,
    val sunriseTime: String,
    val sunsetTime: String,
    val moonPhaseName: String,
    val moonIlluminationPercent: Int,
    val isDay: Boolean,
    val hourly: List<HourlyForecastItem>,
    val daily: List<DailyForecastItem>
)

// --- GitHub ---

@JsonClass(generateAdapter = true)
data class GitHubUserDto(
    val login: String = "",
    val name: String? = null,
    val bio: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubRepoDto(
    val name: String = "",
    val description: String? = null,
    val language: String? = null,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "updated_at") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubEventDto(
    val type: String = "",
    val repo: GitHubEventRepoDto? = null,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubEventRepoDto(
    val name: String = ""
)

data class GitHubSnapshot(
    val user: GitHubUserDto,
    val topRepos: List<GitHubRepoDto>,
    val recentEvents: List<GitHubEventDto>
)

// --- NASA APOD ---

@JsonClass(generateAdapter = true)
data class NasaApodDto(
    val title: String = "",
    val explanation: String = "",
    val url: String = "",
    @Json(name = "media_type") val mediaType: String = "image",
    val date: String = "",
    val copyright: String? = null
)

// --- ISS Live Location ---

@JsonClass(generateAdapter = true)
data class IssPositionDto(
    val name: String = "iss",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val velocity: Double = 0.0,
    val visibility: String = "",
    val timestamp: Long = 0L
)

// --- Wikipedia Random Summary ---

@JsonClass(generateAdapter = true)
data class WikiSummaryDto(
    val title: String = "",
    val description: String? = null,
    val extract: String = "",
    val thumbnail: WikiThumbnailDto? = null,
    @Json(name = "content_urls") val contentUrls: WikiContentUrlsDto? = null
)

@JsonClass(generateAdapter = true)
data class WikiThumbnailDto(
    val source: String = ""
)

@JsonClass(generateAdapter = true)
data class WikiContentUrlsDto(
    val desktop: WikiDesktopUrlDto? = null
)

@JsonClass(generateAdapter = true)
data class WikiDesktopUrlDto(
    val page: String = ""
)

// --- RSS ---

data class RssArticle(
    val title: String,
    val link: String,
    val summary: String,
    val publishedAt: String,
    val feedTitle: String
)
