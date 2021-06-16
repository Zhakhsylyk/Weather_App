package com.weatherapp.models

class DailyForecastModel (val date: Long,
                          val timeZone: String,
                          val temp: Double,
                          val icon: String) {
    override fun toString(): String {
        return "DailyForecastModel(date=$date, timeZone='$timeZone', temp=$temp, icon='$icon')"
    }
}