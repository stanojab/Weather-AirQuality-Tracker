package si.um.feri.bozinov;

public class City {
    String name;
    double lat,lon;

    //Weather Data
    double temperature;
    int humidity;
    int pressure;
    double windSpeed;
    String description;
    String icon;
    boolean weatherLoaded =false;

    City(String name, double lat,double lon){
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }
}
