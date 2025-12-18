package si.um.feri.bozinov;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class SloveniaMap extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture mapTexture;
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;


    // Slovenia bounding box
    private static final double MIN_LON = 13.3;
    private static final double MAX_LON = 16.6;
    private static final double MIN_LAT = 45.4;
    private static final double MAX_LAT = 46.9;

    // Screen dimensions, u can change dimentiosn for a more detaield mapp
    private static final int MAP_WIDTH = 1280;
    private static final int MAP_HEIGHT = 720;

    // Camera control settings
    private static final float PAN_SPEED = 500f; // pixels per second
    private static final float ZOOM_SPEED = 2f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 1f;

    private List<City> cities;
    private City selectedCity;

    private static final String GEOAPIFY_API_KEY = "930e7c22c63b486eac329474adc56afd";
    private static final String OPENWEATHER_API_KEY = "c55932282557548fa0e13cf7975bfc0d";

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);

        // Setup camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, MAP_WIDTH, MAP_HEIGHT);
        camera.position.set(MAP_WIDTH / 2f, MAP_HEIGHT / 2f, 0);
        camera.zoom = 0.9f;
        camera.update();

        initializeCities();


        // Build Geoapify Static Maps API URL
        String mapUrl = buildGeoapifyUrl(MAP_WIDTH, MAP_HEIGHT);

        // Load the map texture from URL
        try {
            mapTexture = downloadMapTexture(mapUrl);
            System.out.println("Map loaded successfully!");
        } catch (Exception e) {
            System.err.println("Failed to load map: " + e.getMessage());
            e.printStackTrace();
        }
        loadWeatherDataForAllCities();
    }

    private void initializeCities() {
        cities = new ArrayList<>();

        // Major cities in Slovenia with their coordinates
        cities.add(new City("Ljubljana", 46.0569, 14.5058));
        cities.add(new City("Maribor", 46.5547, 15.6459));
        cities.add(new City("Celje", 46.2395, 15.2675));
        cities.add(new City("Kranj", 46.2384, 14.3555));
        cities.add(new City("Koper", 45.5469, 13.7301));
        cities.add(new City("Novo Mesto", 45.8039, 15.1696));
        cities.add(new City("Velenje", 46.3594, 15.1109));
        cities.add(new City("Nova Gorica", 45.9556, 13.6475));
        cities.add(new City("Ptuj", 46.4205, 15.8697));
        cities.add(new City("Murska Sobota", 46.6611, 16.1664));
        cities.add(new City("Slovenj Gradec", 46.5101, 15.0798));
        cities.add(new City("Kamnik", 46.2258, 14.6083));
        cities.add(new City("Jesenice", 46.4297, 14.0531));
        cities.add(new City("Izola", 45.5368, 13.6614));
        cities.add(new City("Krško", 45.9589, 15.4919));
    }

    private void loadWeatherDataForAllCities() {
        new Thread(() -> {
            for (City city : cities) {
                try {
                    loadWeatherData(city);
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.err.println("Failed to load weather for " + city.name + ": " + e.getMessage());
                }
            }
        }).start();
    }

    private void loadWeatherData(City city) throws Exception {
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

            // Parse JSON response
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

    private String buildGeoapifyUrl(int width, int height) {
        String area = String.format("rect:%f,%f,%f,%f", MIN_LON, MIN_LAT, MAX_LON, MAX_LAT);
        String style = "osm-bright";
        return String.format(
            "https://maps.geoapify.com/v1/staticmap?style=%s&width=%d&height=%d&area=%s&apiKey=%s",
            style, width, height, area, GEOAPIFY_API_KEY
        );
    }

    private Texture downloadMapTexture(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "LibGDX Slovenia Map App");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();

            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            int bytesRead;
            byte[] data = new byte[4096];
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            buffer.flush();
            byte[] imageData = buffer.toByteArray();
            inputStream.close();

            // Create Pixmap from image data
            Pixmap pixmap = new Pixmap(imageData, 0, imageData.length);
            Texture texture = new Texture(pixmap);
            pixmap.dispose();

            return texture;
        } else {
            throw new Exception("HTTP error code: " + responseCode);
        }
    }

    private Vector2 geoToScreen(double lat, double lon) {
        // Convert geographic coordinates to screen coordinates
        float x = (float) ((lon - MIN_LON) / (MAX_LON - MIN_LON) * MAP_WIDTH);
        float y = (float) ((lat - MIN_LAT) / (MAX_LAT - MIN_LAT) * MAP_HEIGHT);
        return new Vector2(x, y);
    }

    private void handleInput() {
        float delta = Gdx.graphics.getDeltaTime();
        float moveAmount = PAN_SPEED * delta * camera.zoom;

        // Panning with arrow keys
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            camera.position.x -= moveAmount;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            camera.position.x += moveAmount;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            camera.position.y += moveAmount;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            camera.position.y -= moveAmount;
        }

        // Zooming with +/- keys
        if (Gdx.input.isKeyPressed(Input.Keys.PLUS) || Gdx.input.isKeyPressed(Input.Keys.EQUALS) ||
            Gdx.input.isKeyPressed(Input.Keys.PAGE_UP)) {
            camera.zoom -= ZOOM_SPEED * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS) || Gdx.input.isKeyPressed(Input.Keys.PAGE_DOWN)) {
            camera.zoom += ZOOM_SPEED * delta;
        }

        // Clamp zoom
        camera.zoom = MathUtils.clamp(camera.zoom, MIN_ZOOM, MAX_ZOOM);

        // Keep camera within map bounds
        float effectiveWidth = MAP_WIDTH * camera.zoom;
        float effectiveHeight = MAP_HEIGHT * camera.zoom;

        camera.position.x = MathUtils.clamp(camera.position.x,
            effectiveWidth / 2f,
            MAP_WIDTH - effectiveWidth / 2f);
        camera.position.y = MathUtils.clamp(camera.position.y,
            effectiveHeight / 2f,
            MAP_HEIGHT - effectiveHeight / 2f);

        camera.update();
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);

            for (City city : cities) {
                Vector2 cityPos = geoToScreen(city.lat, city.lon);
                float distance = Vector2.dst(touchPos.x, touchPos.y, cityPos.x, cityPos.y);

                if (distance < 15) { // Click radius
                    selectedCity = (selectedCity == city) ? null : city;
                    if (selectedCity != null && selectedCity.weatherLoaded) {
                        printWeatherDetails(selectedCity);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void render() {
        handleInput();

        ScreenUtils.clear(0.5f, 0.7f, 0.9f, 1f);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (mapTexture != null) {
            batch.draw(mapTexture, 0, 0, MAP_WIDTH, MAP_HEIGHT);
        }
        batch.end();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (City city : cities) {
            Vector2 pos = geoToScreen(city.lat, city.lon);

            // Color based on weather data availability
            if (city.weatherLoaded) {
                // Color based on temperature
                if (city.temperature < 0) {
                    shapeRenderer.setColor(0.3f, 0.5f, 1f, 1f); // Blue
                } else if (city.temperature < 15) {
                    shapeRenderer.setColor(0.3f, 0.8f, 0.3f, 1f); // Green
                } else if (city.temperature < 25) {
                    shapeRenderer.setColor(1f, 0.8f, 0.2f, 1f); // Yellow
                } else {
                    shapeRenderer.setColor(1f, 0.3f, 0.2f, 1f); // Red for
                }
            } else {
                shapeRenderer.setColor(0.7f, 0.7f, 0.7f, 1f); // Gray for
            }

            // Draw outer circle
            shapeRenderer.circle(pos.x, pos.y, 8);

            // Draw inner circle (white)
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.circle(pos.x, pos.y, 5);
        }
        shapeRenderer.end();

// Draw city labels and weather info
        batch.begin();
        for (City city : cities) {
            Vector2 pos = geoToScreen(city.lat, city.lon);

            // Draw city name
//            font.setColor(Color.BLACK);
//           font.draw(batch, city.name, pos.x + 10, pos.y + 5);

            // Draw temperature if loaded
            if (city.weatherLoaded) {
                String tempText = String.format("%.1f°C", city.temperature);
                font.setColor(Color.BLUE);
                font.draw(batch, tempText, pos.x + 10, pos.y - 10);
            }
        }
        batch.end();

    }

    private void printWeatherDetails(City city) {
        System.out.println("--- Weather Details for: " + city.name.toUpperCase() + " ---");
        System.out.println(String.format("Temperature: %.1f°C", city.temperature));
        System.out.println(String.format("Humidity:    %d%%", city.humidity));
        System.out.println(String.format("Wind Speed:  %.1f m/s", city.windSpeed));
        System.out.println("Description: " + city.description);
        System.out.println("------------------------------------------");
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (mapTexture != null) {
            mapTexture.dispose();
        }
    }


    public boolean scrolled(float amountX, float amountY) {
        // Mouse wheel zoom
        camera.zoom += amountY * 0.1f;
        camera.zoom = MathUtils.clamp(camera.zoom, MIN_ZOOM, MAX_ZOOM);
        return true;
    }
}
