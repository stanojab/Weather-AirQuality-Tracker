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
    private OrthographicCamera uiCamera;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont titleFont;
    private BitmapFont smallFont;
    private BitmapFont largeFont;

    // Slovenia bounding box
    private static final double MIN_LON = 13.3;
    private static final double MAX_LON = 16.6;
    private static final double MIN_LAT = 45.4;
    private static final double MAX_LAT = 46.9;

    // Screen dimensions
    private static final int MAP_WIDTH = 1280;
    private static final int MAP_HEIGHT = 720;

    // Camera control settings
    private static final float PAN_SPEED = 500f;
    private static final float ZOOM_SPEED = 2f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 1f;

    // Weather panel settings
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 480;
    private static final int PANEL_PADDING = 25;
    private static final int PANEL_MARGIN = 30;

    private List<City> cities;
    private City selectedCity;
    private boolean showWeatherPanel = false;
    private float panelAnimationProgress = 0f;
    private float markerPulse = 0f;

    private static final String GEOAPIFY_API_KEY = "930e7c22c63b486eac329474adc56afd";
    private static final String OPENWEATHER_API_KEY = "c55932282557548fa0e13cf7975bfc0d";

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Create fonts with different sizes
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.3f);

        titleFont = new BitmapFont();
        titleFont.setColor(Color.WHITE);
        titleFont.getData().setScale(2.2f);

        smallFont = new BitmapFont();
        smallFont.setColor(Color.WHITE);
        smallFont.getData().setScale(1.1f);

        largeFont = new BitmapFont();
        largeFont.setColor(Color.WHITE);
        largeFont.getData().setScale(3.5f);

        // Setup map camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, MAP_WIDTH, MAP_HEIGHT);
        camera.position.set(MAP_WIDTH / 2f, MAP_HEIGHT / 2f, 0);
        camera.zoom = 0.9f;
        camera.update();

        // Setup UI camera
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

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

        // Close panel with ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            showWeatherPanel = false;
            selectedCity = null;
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

        // Handle city clicks
        if (Gdx.input.justTouched()) {
            // Check if clicking on close button of panel
            if (showWeatherPanel) {
                float panelX = Gdx.graphics.getWidth() - PANEL_WIDTH - PANEL_MARGIN;
                float panelY = Gdx.graphics.getHeight() - PANEL_HEIGHT - PANEL_MARGIN;
                float closeX = panelX + PANEL_WIDTH - 45;
                float closeY = panelY + PANEL_HEIGHT - 45;

                if (Gdx.input.getX() >= closeX && Gdx.input.getX() <= closeX + 35 &&
                    Gdx.graphics.getHeight() - Gdx.input.getY() >= closeY &&
                    Gdx.graphics.getHeight() - Gdx.input.getY() <= closeY + 35) {
                    showWeatherPanel = false;
                    selectedCity = null;
                    return;
                }
            }

            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);

            for (City city : cities) {
                Vector2 cityPos = geoToScreen(city.lat, city.lon);
                float distance = Vector2.dst(touchPos.x, touchPos.y, cityPos.x, cityPos.y);

                if (distance < 15) {
                    selectedCity = city;
                    showWeatherPanel = city.weatherLoaded;
                    panelAnimationProgress = 0f;
                    break;
                }
            }
        }
    }

    @Override
    public void render() {
        handleInput();

        // Update animations
        float delta = Gdx.graphics.getDeltaTime();
        markerPulse += delta * 2f;
        if (showWeatherPanel && panelAnimationProgress < 1f) {
            panelAnimationProgress = Math.min(1f, panelAnimationProgress + delta * 4f);
        }

        ScreenUtils.clear(0.12f, 0.15f, 0.2f, 1f);

        // Draw map with slight vignette effect
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (mapTexture != null) {
            batch.setColor(0.95f, 0.95f, 0.95f, 1f);
            batch.draw(mapTexture, 0, 0, MAP_WIDTH, MAP_HEIGHT);
            batch.setColor(Color.WHITE);
        }
        batch.end();

        // Draw city markers with improved styling
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (City city : cities) {
            Vector2 pos = geoToScreen(city.lat, city.lon);
            boolean isSelected = (city == selectedCity);

            // Outer glow for selected city
            if (isSelected) {
                float pulseSize = 20 + (float)Math.sin(markerPulse) * 3;
                Color tempColor = getTemperatureColor(city.temperature);
                shapeRenderer.setColor(tempColor.r, tempColor.g, tempColor.b, 0.3f);
                shapeRenderer.circle(pos.x, pos.y, pulseSize);
            }

            // Shadow
            shapeRenderer.setColor(0, 0, 0, 0.4f);
            shapeRenderer.circle(pos.x + 1, pos.y - 1, isSelected ? 14 : 10);

            // Main marker circle - temperature colored
            if (city.weatherLoaded) {
                Color tempColor = getTemperatureColor(city.temperature);
                shapeRenderer.setColor(tempColor);
            } else {
                shapeRenderer.setColor(0.6f, 0.6f, 0.65f, 1f);
            }
            shapeRenderer.circle(pos.x, pos.y, isSelected ? 13 : 9);

            // Border ring
            shapeRenderer.setColor(1f, 1f, 1f, 0.9f);
            shapeRenderer.circle(pos.x, pos.y, isSelected ? 11 : 7.5f);

            // Inner core
            if (city.weatherLoaded) {
                Color tempColor = getTemperatureColor(city.temperature);
                shapeRenderer.setColor(tempColor.r * 0.8f, tempColor.g * 0.8f, tempColor.b * 0.8f, 1f);
            } else {
                shapeRenderer.setColor(0.5f, 0.5f, 0.55f, 1f);
            }
            shapeRenderer.circle(pos.x, pos.y, isSelected ? 6 : 4);
        }
        shapeRenderer.end();

        // Draw city labels
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (City city : cities) {
            if (city == selectedCity || camera.zoom < 0.7f) {
                Vector2 pos = geoToScreen(city.lat, city.lon);

                // Text shadow
                smallFont.setColor(0, 0, 0, 0.7f);
                smallFont.draw(batch, city.name, pos.x - 20 + 1, pos.y - 18 - 1);

                // Text
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, city.name, pos.x - 20, pos.y - 18);
            }
        }
        batch.end();

        // Draw weather panel if a city is selected
        if (showWeatherPanel && selectedCity != null && selectedCity.weatherLoaded) {
            drawWeatherPanel();
        }

        // Draw control hints
        drawControlHints();
    }

    private void drawWeatherPanel() {
        // Switch to UI camera
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        float panelX = Gdx.graphics.getWidth() - PANEL_WIDTH - PANEL_MARGIN;
        float panelY = Gdx.graphics.getHeight() - PANEL_HEIGHT - PANEL_MARGIN;

        // Apply slide-in animation
        float animatedX = panelX + (1 - easeOutCubic(panelAnimationProgress)) * (PANEL_WIDTH + PANEL_MARGIN);

        // Draw panel background with shadow
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Multiple shadow layers for depth
        shapeRenderer.setColor(0, 0, 0, 0.15f);
        shapeRenderer.rect(animatedX + 8, panelY - 8, PANEL_WIDTH, PANEL_HEIGHT);
        shapeRenderer.setColor(0, 0, 0, 0.1f);
        shapeRenderer.rect(animatedX + 4, panelY - 4, PANEL_WIDTH, PANEL_HEIGHT);

        // Main panel background
        shapeRenderer.setColor(0.11f, 0.13f, 0.16f, 0.98f);
        shapeRenderer.rect(animatedX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        // Header gradient background
        Color headerColor = getTemperatureColor(selectedCity.temperature);

        // Top gradient
        for (int i = 0; i < 100; i++) {
            float alpha = 0.85f - (i / 100f) * 0.45f;
            shapeRenderer.setColor(headerColor.r, headerColor.g, headerColor.b, alpha);
            shapeRenderer.rect(animatedX, panelY + PANEL_HEIGHT - 100 + i, PANEL_WIDTH, 1);
        }

        // Accent line at top
        shapeRenderer.setColor(headerColor.r * 1.2f, headerColor.g * 1.2f, headerColor.b * 1.2f, 1f);
        shapeRenderer.rect(animatedX, panelY + PANEL_HEIGHT - 3, PANEL_WIDTH, 3);

        // Divider line
        shapeRenderer.setColor(0.25f, 0.27f, 0.3f, 0.8f);
        shapeRenderer.rect(animatedX + PANEL_PADDING, panelY + PANEL_HEIGHT - 105,
            PANEL_WIDTH - PANEL_PADDING * 2, 2);

        // Close button background
        shapeRenderer.setColor(0.2f, 0.22f, 0.25f, 0.9f);
        shapeRenderer.circle(animatedX + PANEL_WIDTH - 40, panelY + PANEL_HEIGHT - 40, 18);

        shapeRenderer.end();

        // Draw text content
        batch.begin();

        float textX = animatedX + PANEL_PADDING;
        float textY = panelY + PANEL_HEIGHT - PANEL_PADDING - 5;

        // City name with glow effect
        titleFont.setColor(1f, 1f, 1f, 0.15f);
        titleFont.draw(batch, selectedCity.name, textX + 2, textY - 12);
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, selectedCity.name, textX, textY - 10);

        // Close button X
        font.setColor(0.9f, 0.9f, 0.9f, 1f);
        font.draw(batch, "X", animatedX + PANEL_WIDTH - 47, textY - 10);

        textY -= 90;

        // Temperature - extra large display with shadow
        largeFont.setColor(0, 0, 0, 0.3f);
        String tempText = String.format("%.0f", selectedCity.temperature);
        largeFont.draw(batch, tempText, textX + 3, textY - 3);
        largeFont.setColor(Color.WHITE);
        largeFont.draw(batch, tempText, textX, textY);

        // Draw "C" for Celsius next to temperature
        titleFont.setColor(new Color(0.8f, 0.8f, 0.8f, 1f));
        titleFont.draw(batch, "C", textX + 90, textY - 10);

        // Description
        smallFont.setColor(new Color(0.85f, 0.87f, 0.9f, 1f));
        String capitalizedDesc = capitalizeFirst(selectedCity.description);
        smallFont.draw(batch, capitalizedDesc, textX, textY - 50);

        textY -= 105;

        // Weather details in styled boxes
        font.setColor(new Color(0.75f, 0.77f, 0.8f, 1f));

        // End batch to draw icons with ShapeRenderer
        batch.end();

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw icons for each weather detail
        float iconX = textX + 10;
        float iconY = textY - 5;

        drawHumidityIcon(iconX, iconY);
        iconY -= 50;

        drawWindIcon(iconX, iconY);
        iconY -= 50;

        drawPressureIcon(iconX, iconY);
        iconY -= 50;

        drawLocationIcon(iconX, iconY);

        shapeRenderer.end();

        // Resume batch for text
        batch.begin();

        // Draw info cards
        drawInfoCard(batch, textX, textY, "Humidity", selectedCity.humidity + "%");
        textY -= 50;

        drawInfoCard(batch, textX, textY, "Wind Speed", String.format("%.1f m/s", selectedCity.windSpeed));
        textY -= 50;

        drawInfoCard(batch, textX, textY, "Pressure", selectedCity.pressure + " hPa");
        textY -= 50;

        drawInfoCard(batch, textX, textY, "Location",
            String.format("%.2f N, %.2f E", selectedCity.lat, selectedCity.lon));

        // Footer hint
        textY = panelY + PANEL_PADDING + 10;
        smallFont.setColor(new Color(0.5f, 0.52f, 0.55f, 1f));
        smallFont.draw(batch, "Press ESC or click X to close", textX, textY);

        batch.end();
    }

    private void drawInfoCard(SpriteBatch batch, float x, float y, String label, String value) {
        // Card background (using shape renderer would be better, but keeping it simple)
        font.setColor(new Color(0.6f, 0.62f, 0.65f, 1f));
        font.draw(batch, label, x + 30, y);

        titleFont.getData().setScale(1.4f);
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, value, x + 160, y + 2);
        titleFont.getData().setScale(2.2f);
    }

    private void drawHumidityIcon(float x, float y) {
        shapeRenderer.setColor(0.4f, 0.65f, 0.95f, 1f);
        // Droplet shape
        shapeRenderer.circle(x, y - 2, 6);
        shapeRenderer.triangle(x - 6, y - 2, x + 6, y - 2, x, y + 8);
    }

    private void drawWindIcon(float x, float y) {
        shapeRenderer.setColor(0.6f, 0.75f, 0.9f, 1f);
        // Wind lines
        shapeRenderer.rectLine(x - 8, y + 4, x + 8, y + 4, 2);
        shapeRenderer.rectLine(x - 6, y, x + 6, y, 2);
        shapeRenderer.rectLine(x - 4, y - 4, x + 4, y - 4, 2);
    }

    private void drawPressureIcon(float x, float y) {
        shapeRenderer.setColor(0.75f, 0.6f, 0.85f, 1f);
        // Gauge/meter shape
        shapeRenderer.circle(x, y, 7);
        shapeRenderer.setColor(0.11f, 0.13f, 0.16f, 1f);
        shapeRenderer.circle(x, y, 5);
        shapeRenderer.setColor(0.75f, 0.6f, 0.85f, 1f);
        shapeRenderer.rectLine(x, y, x + 4, y + 4, 2);
    }

    private void drawLocationIcon(float x, float y) {
        shapeRenderer.setColor(0.95f, 0.4f, 0.4f, 1f);
        // Map pin shape
        shapeRenderer.circle(x, y + 4, 6);
        shapeRenderer.setColor(0.11f, 0.13f, 0.16f, 1f);
        shapeRenderer.circle(x, y + 4, 3);
        shapeRenderer.setColor(0.95f, 0.4f, 0.4f, 1f);
        shapeRenderer.triangle(x - 3, y + 1, x + 3, y + 1, x, y - 6);
    }

    private void drawControlHints() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        smallFont.setColor(new Color(1f, 1f, 1f, 0.7f));
        String hints = "Arrow Keys: Pan  |  +/- : Zoom  |  Click City: Weather Info";
        float textWidth = 400; // Approximate
        smallFont.draw(batch, hints,
            (Gdx.graphics.getWidth() - textWidth) / 2,
            30);

        batch.end();
    }

    private Color getTemperatureColor(double temp) {
        if (temp < 0) {
            return new Color(0.3f, 0.6f, 1f, 1f); // Cool blue
        } else if (temp < 10) {
            return new Color(0.4f, 0.75f, 0.95f, 1f); // Light blue
        } else if (temp < 20) {
            return new Color(0.4f, 0.85f, 0.4f, 1f); // Green
        } else if (temp < 28) {
            return new Color(1f, 0.85f, 0.2f, 1f); // Yellow-orange
        } else {
            return new Color(1f, 0.4f, 0.3f, 1f); // Hot red
        }
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private float easeOutCubic(float t) {
        return 1 - (float)Math.pow(1 - t, 3);
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (mapTexture != null) {
            mapTexture.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (titleFont != null) {
            titleFont.dispose();
        }
        if (smallFont != null) {
            smallFont.dispose();
        }
        if (largeFont != null) {
            largeFont.dispose();
        }
    }

    public boolean scrolled(float amountX, float amountY) {
        camera.zoom += amountY * 0.1f;
        camera.zoom = MathUtils.clamp(camera.zoom, MIN_ZOOM, MAX_ZOOM);
        return true;
    }
}
