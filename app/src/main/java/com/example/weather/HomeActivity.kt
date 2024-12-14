package com.example.weather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.roundToInt

class HomeActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // UI Components for Weather Data
    private var cityName: TextView? = null
    private var dateAndTime: TextView? = null
    private var temperature: TextView? = null
    private var weatherCondition: TextView? = null
    private var minTemperature: TextView? = null
    private var maxTemperature: TextView? = null
    private var windSpeed: TextView? = null

    private val API_KEY = "9542e9ba34d26a62cc2292e44e85ad3b"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Show loading layout initially
        showLoadingLayout()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Fetch location and weather data
        fetchLocationAndWeather()
    }

    private fun fetchLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showErrorLayout("Location permission is required.")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                fetchWeather(latitude, longitude)
            } else {
                showErrorLayout("Unable to get location.")
            }
        }.addOnFailureListener {
            showErrorLayout("Failed to get location.")
        }
    }

    private fun fetchWeather(latitude: Double, longitude: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherService::class.java)
        val call = service.getWeather(latitude, longitude, API_KEY, "metric")

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    if (weather != null) {
                        // Show the weather UI and update data
                        showWeatherLayout()
                        updateWeatherUI(weather)
                    }
                } else {
                    showErrorLayout("Error fetching weather data.")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                showErrorLayout("Network Error: ${t.message}")
            }
        })
    }

    private fun updateWeatherUI(weather: WeatherResponse) {
        cityName?.text = weather.name
        dateAndTime?.text = "Today" // Replace with formatted time
        temperature?.text = "${weather.main.temp.roundToInt()}°"
        weatherCondition?.text = weather.weather[0].description.capitalize()
        minTemperature?.text = "${weather.main.temp_min.roundToInt()}°"
        maxTemperature?.text = "${weather.main.temp_max.roundToInt()}°"
        windSpeed?.text = "${weather.wind.speed} m/s"
    }

    private fun showLoadingLayout() {
        setContentView(R.layout.layout_loading)
    }

    private fun showErrorLayout(message: String) {
        setContentView(R.layout.layout_error)
        val errorTextView: TextView = findViewById(R.id.errorTextView)
        errorTextView.text = message
    }

    private fun showWeatherLayout() {
        setContentView(R.layout.activity_home)

        // Initialize weather UI views
        cityName = findViewById(R.id.cityName)
        dateAndTime = findViewById(R.id.dateAndTime)
        temperature = findViewById(R.id.temperature)
        weatherCondition = findViewById(R.id.weatherCondition)
        minTemperature = findViewById(R.id.minTemperature)
        maxTemperature = findViewById(R.id.maxTemperature)
        windSpeed = findViewById(R.id.windSpeed)
    }
}

interface WeatherService {
    @GET("weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String
    ): Call<WeatherResponse>
}

data class WeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind
)

data class Main(val temp: Double, val temp_min: Double, val temp_max: Double)
data class Weather(val description: String)
data class Wind(val speed: Double)
