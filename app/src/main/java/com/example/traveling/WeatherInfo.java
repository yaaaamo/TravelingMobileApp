package com.example.traveling;

public class WeatherInfo {
    private final double tempMax;
    private final double tempMin;
    private final double precipitation;  // mm
    private final int weatherCode;

    public WeatherInfo(double tempMax, double tempMin,
                       double precipitation, int weatherCode) {
        this.tempMax = tempMax;
        this.tempMin = tempMin;
        this.precipitation = precipitation;
        this.weatherCode = weatherCode;
    }

    public double getTempMax() { return tempMax; }
    public double getTempMin() { return tempMin; }
    public double getPrecipitation() { return precipitation; }
    public int getWeatherCode() { return weatherCode; }


    public int getAverageTemp() {
        return (int) Math.round((tempMax + tempMin) / 2);
    }


    public String getDescription() {
        if (weatherCode == 0) return "Ensoleillé";
        if (weatherCode <= 3) return "Partiellement nuageux";
        if (weatherCode <= 48) return "Brumeux";
        if (weatherCode <= 67) return "Pluvieux";
        if (weatherCode <= 77) return "Neigeux";
        if (weatherCode <= 82) return "Averses";
        if (weatherCode <= 99) return "Orageux";
        return "Inconnu";
    }


    public String getEmoji() {
        if (weatherCode == 0) return "☀️";
        if (weatherCode <= 3) return "⛅";
        if (weatherCode <= 48) return "🌫️";
        if (weatherCode <= 67) return "🌧️";
        if (weatherCode <= 77) return "❄️";
        if (weatherCode <= 82) return "🌦️";
        if (weatherCode <= 99) return "⛈️";
        return "🌡️";
    }

    public String getShortDisplay() {
        return getEmoji() + " " + getAverageTemp() + "° " + getDescription();
    }
}