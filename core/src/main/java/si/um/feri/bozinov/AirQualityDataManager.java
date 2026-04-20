package si.um.feri.bozinov;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AirQualityDataManager {
    private static final String OPENWEATHER_API_KEY = loadApiKey();

    private static String loadApiKey() {
        // Try system property first (Gradle run)
        String key = System.getProperty("OPENWEATHER_API_KEY");
        if (key != null && !key.isEmpty()) return key;


        try {
            java.io.File file = new java.io.File("../local.properties");
            if (!file.exists()) file = new java.io.File("local.properties");

            java.util.Properties props = new java.util.Properties();
            props.load(new java.io.FileInputStream(file));
            key = props.getProperty("OPENWEATHER_API_KEY", "");
            return key;
        } catch (Exception e) {
            System.err.println("Could not load API key: " + e.getMessage());
            return "";
        }
    }
    private static final boolean USE_STATIC_DATA = false;

    public void loadAirQualityDataForAllCities(java.util.List<City> cities) {
        if (USE_STATIC_DATA) {
            System.out.println("Using static air quality data from JSON file");
            return;
        }

        new Thread(() -> {
            for (City city : cities) {
                if (city.isStatic) {
                    System.out.println("Skipping API call for static city: " + city.name);
                    continue;
                }

                try {
                    loadAirQualityData(city);
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.err.println("Failed to load air quality for " + city.name + ": " + e.getMessage());
                }
            }
        }).start();
    }

    public void loadAirQualityData(City city) throws Exception {
        String urlString = String.format(
            "https://api.openweathermap.org/data/2.5/air_pollution?lat=%f&lon=%f&appid=%s",
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
            JSONObject components = json.getJSONArray("list").getJSONObject(0).getJSONObject("components");

            city.aqi = json.getJSONArray("list").getJSONObject(0).getJSONObject("main").getInt("aqi");
            city.co = components.getDouble("co");
            city.no2 = components.getDouble("no2");
            city.o3 = components.getDouble("o3");
            city.pm2_5 = components.getDouble("pm2_5");
            city.pm10 = components.getDouble("pm10");
            city.airQualityLoaded = true;

            System.out.println("Air quality loaded for " + city.name + ": AQI " + city.aqi);
        }
    }

    public boolean isUsingStaticData() {
        return USE_STATIC_DATA;
    }
}
