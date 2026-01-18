package si.um.feri.bozinov;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherDataManager {
    private static final String OPENWEATHER_API_KEY = "c55932282557548fa0e13cf7975bfc0d";
    private static final boolean USE_STATIC_DATA = false;

    public void loadWeatherDataForAllCities(java.util.List<City> cities) {
        if (USE_STATIC_DATA) {
            System.out.println("Using static weather data from JSON file");
            return;
        }

        new Thread(() -> {
            for (City city : cities) {
                if (city.isStatic) {
                    System.out.println("Skipping API call for static city: " + city.name);
                    continue;
                }

                try {
                    loadWeatherData(city);
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.err.println("Failed to load weather for " + city.name + ": " + e.getMessage());
                }
            }
        }).start();
    }

    public void loadWeatherData(City city) throws Exception {
        String urlString = String.format(
            "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=metric&appid=%s",
            city.lat, city.lon, OPENWEATHER_API_KEY
        );

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());

            city.temperature = json.getJSONObject("main").getDouble("temp");
            city.humidity = json.getJSONObject("main").getInt("humidity");
            city.pressure = json.getJSONObject("main").getInt("pressure");
            city.windSpeed = json.getJSONObject("wind").getDouble("speed");
            city.description = json.getJSONArray("weather").getJSONObject(0).getString("description");
            city.icon = json.getJSONArray("weather").getJSONObject(0).getString("icon");
            city.weatherLoaded = true;

            System.out.println("Weather loaded for " + city.name + ": " + city.temperature + "°C");
        }
    }

    public boolean isUsingStaticData() {
        return USE_STATIC_DATA;
    }
}
