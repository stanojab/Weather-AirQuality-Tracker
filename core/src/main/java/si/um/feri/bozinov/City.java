package si.um.feri.bozinov;

public class City {
    public String name;
    public double lat;
    public double lon;

    // Weather data
    public double temperature;
    public int humidity;
    public int pressure;
    public double windSpeed;
    public String description;
    public String icon;
    public boolean weatherLoaded;

    //AirQuality data
    public int aqi;
    public double co;
    public double no2;
    public double o3;
    public double pm2_5;
    public double pm10;
    public boolean airQualityLoaded = false;

    public boolean isStatic;


    // Empty constructor for JSON serialization
    public City() {
    }

    public City(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.weatherLoaded = false;
    }

    @Override
    public String toString() {
        return name + " (" + String.format("%.4f", lat) + ", " + String.format("%.4f", lon) + ")";
    }
}
