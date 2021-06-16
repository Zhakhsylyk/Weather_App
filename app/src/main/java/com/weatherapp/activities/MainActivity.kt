package com.weatherapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Ion
import com.weatherapp.DFA
import com.weatherapp.R
import com.weatherapp.models.DailyForecastModel
import com.weatherapp.models.WeatherResponse
import com.weatherapp.network.WeatherService
import com.weatherapp.utils.Constants
import retrofit.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// OpenWeather Link : https://openweathermap.org/api
/**
 * The useful link or some more explanation for this app you can checkout this link :
 * https://medium.com/@sasude9/basic-android-weather-app-6a7c0855caf4
 */
class MainActivity : AppCompatActivity() {

    // A fused location client variable which is further user to get the user's current location
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            // This will redirect you to settings from where you need to turn on the location provider.
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }
    }

    /**
     * A function which is used to verify that the location or GPS is enable or not of the user's device.
     */
    private fun isLocationEnabled(): Boolean {

        // This provides access to the system location services.
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
     */
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }

    /**
     * A function to request the current location. Using the fused location provider client.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * A location callback object of fused location provider client where we will get the current location details.
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")

            // TODO (STEP 6: Pass the latitude and longitude as parameters in function)
            getLocationWeatherDetails(latitude, longitude)
        }
    }

    /**
     * Function is used to get the weather details of the current location based on the latitude longitude
     */
    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {

        if (Constants.isNetworkAvailable(this@MainActivity)) {

            // TODO (STEP 1: Make an api call using retrofit.)
            // START
            /**
             * Add the built-in converter factory first. This prevents overriding its
             * behavior but also ensures correct behavior when using converters that consume all types.
             */
            val retrofit: Retrofit = Retrofit.Builder()
                // API base URL.
                .baseUrl(Constants.BASE_URL)
                /** Add converter factory for serialization and deserialization of objects. */
                /**
                 * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and
                 * decoding from JSON (when no charset is specified by a header) will use UTF-8.
                 */
                .addConverterFactory(GsonConverterFactory.create())
                /** Create the Retrofit instances. */
                .build()
            // END

            // TODO (STEP 5: Further step for API call)
            // START
            /**
             * Here we map the service interface in which we declares the end point and the API type
             *i.e GET, POST and so on along with the request parameter which are required.
             */
            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            /** An invocation of a Retrofit method that sends a request to a web-server and returns a response.
             * Here we pass the required param in the service
             */
            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            // Callback methods are executed using the Retrofit callback executor.
            listCall.enqueue(object : Callback<WeatherResponse> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    response: Response<WeatherResponse>,
                    retrofit: Retrofit
                ) {

                    // Check weather the response is success or not.
                    if (response.isSuccess) {

                        /** The de-serialized response body of a successful response. */
                        val weatherList: WeatherResponse = response.body()
                        Log.i("Response Result", "$weatherList")
                        tvCity.text = weatherList.name
                        ivIcon.setImageResource(getIconImage(weatherList.weather[0].icon.toString()))
                        tvTemperature.text = weatherList.main.temp.toString() + "º"
                        tvTempMin.text = weatherList.main.temp_min.toString() + "º"
                        tvTempMax.text = weatherList.main.temp_max.toString() + "º"
                        tvTempFeelsLike.text = weatherList.main.feels_like.toString() + "º"

                        var dateFormat = SimpleDateFormat("EEE, d MMM HH:mm")
                        var localDate = Date(Date().time + weatherList.timezone.toLong()*1000)

                        tvDateTime.text=dateFormat.format(localDate)
                        tvId.text = getIdText(weatherList.weather[0].id)

                        loadDailyForecast(weatherList.coord.lon!!,weatherList.coord.lat!!)
                    } else {
                        // If the response is not success then we check the response code.
                        val sc = response.code()
                        when (sc) {
                            400 -> {
                                Log.e("Error 400", "Bad Request")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        } 
                    }
                }

                override fun onFailure(t: Throwable) {
                    Log.e("Errorrrrr", t.message.toString())
                }
            })
            // END

        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun loadDailyForecast(lon: Double, lat: Double) {
        var apiUrl = "https://api.openweathermap.org/data/2.5/onecall?lat=" + lat + "&lon=" + lon + "&exclude=hourly,%20minutely,%20current&units=metric&appid=" + Constants.APP_ID
        Ion.with(this)
            .load(apiUrl)
            .asJsonObject()
            .setCallback(object : FutureCallback<JsonObject> {
                override fun onCompleted(e: Exception?, result: JsonObject?) {
                    if (e != null) {
                        e.printStackTrace()
                        Toast.makeText(this@MainActivity, "Server Error: $e", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("result_response", result.toString())
                        var weatherList: ArrayList<DailyForecastModel> = ArrayList()
                        var timeZone: String = result!!.get("timezone").asString
                        var daily: JsonArray = result!!.get("daily").asJsonArray

                        for (i in 1..daily.size()-1) {
                            var date = daily.get(i).asJsonObject.get("dt").asLong
                            var temp = daily.get(i).asJsonObject.get("temp").asJsonObject.get("day").asDouble
                            var icon = daily.get(i).asJsonObject.get("weather").asJsonArray.get(0).asJsonObject.get("icon").asString

                            weatherList.add(DailyForecastModel(date, timeZone, temp, icon))
                        }

                        var dailyWeatherAdapter = DFA(weatherList, this@MainActivity)
                        rvDailyForecast.layoutManager = LinearLayoutManager(this@MainActivity)
                        rvDailyForecast.adapter = dailyWeatherAdapter
                    }
                }
            })
    }



    fun getIdText(id: Int?) : String {
        return when(id) {
            200 -> "Thunderstorm with light rain"
            201 -> "Thunderstorm with rain"
            202 -> "Thunderstorm with heavy rain"
            210 -> "Light thunderstorm"
            211 -> "Thunderstorm"
            212 -> "Heavy thunderstorm"
            221 -> "Ragged thunderstorm"
            230 -> "Thunderstorm with light drizzle"
            231 -> "Thunderstorm with drizzle"
            232 -> "Thunderstorm with heavy drizzle"

            300 -> "Light intensity drizzle"
            301 -> "Drizzle"
            302 -> "Heavy intensity drizzle"
            310 -> "Light intensity drizzle rain"
            311 -> "Drizzle rain"
            312 -> "Heavy intensity drizzle rain"
            313 -> "Shower rain and drizzle"
            314 -> "Heavy shower rain and drizzle"
            321 -> "Shower drizzle"

            500 -> "Light rain"
            501 -> "Moderate rain"
            502 -> "Heavy intensity rain"
            503 -> "Very heavy rain"
            504 -> "Extreme rain"
            511 -> "Freezing rain"
            520 -> "Light intensity shower rain"
            521 -> "Shower rain"
            522 -> "Heavy intensity shower rain"
            531 -> "Ragged shower rain"

            600 -> "Light snow"
            601 -> "Snow"
            602 -> "Heavy snow"
            611 -> "Sleet"
            612 -> "Light shower sleet"
            613 -> "Shower sleet"
            615 -> "Light rain and snow"
            616 -> "Rain and snow"
            620 -> "Light shower snow"
            621 -> "Shower snow"
            622 -> "Heavy shower snow"

            701 -> "Mist"
            711 -> "Smoke"
            721 -> "Haze"
            731 -> "Sand / Dust whirls"
            741 -> "Fog"
            751 -> "Sand"
            761 -> "Dust"
            762 -> "Volcanic ash"
            771 -> "Squalls"
            781 -> "Tornado"

            800 -> "Few clouds: 11-25%"
            801 -> "Scattered clouds: 25-50%"
            802 -> "Broken clouds: 51-84%"
            803 -> "Overcast clouds: 85-100%"
            else -> ""
        }
    }

    fun getIconImage(icon: String) : Int {
        return when(icon) {
            "01d" -> com.google.android.gms.location.R.drawable.d01
            "01n" -> com.google.android.gms.location.R.drawable.n01
            "02d" -> com.google.android.gms.location.R.drawable.d02
            "02n" -> com.google.android.gms.location.R.drawable.n02
            "03d" -> com.google.android.gms.location.R.drawable.n03
            "03n" -> com.google.android.gms.location.R.drawable.n03
            "04d" -> com.google.android.gms.location.R.drawable.n04
            "04n" -> com.google.android.gms.location.R.drawable.n04
            "09d" -> com.google.android.gms.location.R.drawable.n09
            "09n" -> com.google.android.gms.location.R.drawable.n09
            "10d" -> com.google.android.gms.location.R.drawable.d10
            "10n" -> com.google.android.gms.location.R.drawable.n10
            "11d" -> com.google.android.gms.location.R.drawable.d11
            "11n" -> com.google.android.gms.location.R.drawable.n11
            "13d" -> com.google.android.gms.location.R.drawable.d13
            "13n" -> com.google.android.gms.location.R.drawable.n13
            "50d" -> com.google.android.gms.location.R.drawable.d50
            "50n" -> com.google.android.gms.location.R.drawable.n50
            else -> com.google.android.gms.location.R.drawable.n50
        }
    }


}