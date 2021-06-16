package com.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.models.DailyForecastModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DFA: RecyclerView.Adapter<DFA.ViewHolder> {

    inner class ViewHolder : RecyclerView.ViewHolder {
        var tvDate: TextView
        var tvTemp: TextView
        var iconWeather: ImageView

        constructor(itemView: View) : super(itemView) {
            tvDate = itemView.findViewById(R.id.tvDate)
            tvTemp = itemView.findViewById(R.id.tvTemp)
            iconWeather = itemView.findViewById(R.id.ivWeatherDaily)
        }
    }

    private var weatherList: List<DailyForecastModel>
    private var context: Context

    constructor(weatherList: List<DailyForecastModel>, context: Context) : super() {
        this.weatherList = weatherList
        this.context = context
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(context).inflate(R.layout.layout_daily_forecast, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var weather = weatherList[position]
        holder.tvTemp.text = weather.temp.toString() + "º"
        holder.iconWeather.setImageResource(getIconImage(weather.icon))

        var date = Date(weather.date * 1000)
        var dateFormat: DateFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone(weather.timeZone)
        holder.tvDate.text = dateFormat.format(date)
    }

    override fun getItemCount(): Int {
        return weatherList.size
    }

    fun getIconImage(icon: String) : Int {
        return when(icon) {
            "01d" -> R.drawable.d01
            "01n" -> R.drawable.n01
            "02d" -> R.drawable.d02
            "02n" -> R.drawable.n02
            "03d" -> R.drawable.n03
            "03n" -> R.drawable.n03
            "04d" -> R.drawable.n04
            "04n" -> R.drawable.n04
            "09d" -> R.drawable.n09
            "09n" -> R.drawable.n09
            "10d" -> R.drawable.d10
            "10n" -> R.drawable.n10
            "11d" -> R.drawable.d11
            "11n" -> R.drawable.n11
            "13d" -> R.drawable.d13
            "13n" -> R.drawable.n13
            "50d" -> R.drawable.d50
            "50n" -> R.drawable.n50
            else -> R.drawable.n50
        }
    }
}