package si.um.feri.bozinov;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
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
    private BitmapFont extraSmallFont;

    // Scene2D for UI
    private Stage stage;
    private Skin skin;
    private java.util.function.Consumer<Vector2> locationPickCallback = null;
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
    private static final int PANEL_HEIGHT = 450;
    private static final int PANEL_PADDING = 20;
    private static final int PANEL_MARGIN = 30;

    private List<City> cities;
    private City selectedCity;
    private boolean showWeatherPanel = false;
    private float panelAnimationProgress = 0f;
    private float markerPulse = 0f;

    // Edit mode
    private boolean editMode = false;
    private City hoveredCity = null;
    private boolean awaitingLocationClick = false;
    private Vector2 pendingLocation = null;

    //AirQualityMocde
    private boolean airQualityMode = false;
    private TextButton modeToggleButton;

    private static final String GEOAPIFY_API_KEY = "930e7c22c63b486eac329474adc56afd";
    private static final String OPENWEATHER_API_KEY = "c55932282557548fa0e13cf7975bfc0d";
    private static final String CITIES_FILE = "cities.json";

    // Debug mode: set to true to use static data from JSON file instead of API calls
    private static final boolean USE_STATIC_WEATHER_DATA = false;

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

        extraSmallFont = new BitmapFont();
        extraSmallFont.setColor(Color.WHITE);
        extraSmallFont.getData().setScale(0.95f);

        // Setup map camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, MAP_WIDTH, MAP_HEIGHT);
        camera.position.set(MAP_WIDTH / 2f, MAP_HEIGHT / 2f, 0);
        camera.zoom = 0.9f;
        camera.update();

        // Setup UI camera
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Setup Scene2D
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Create input multiplexer to handle both stage and map input
        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                return SloveniaMap.this.scrolled(amountX, amountY);
            }
        });
        Gdx.input.setInputProcessor(multiplexer);

        // Load or initialize cities
        loadCitiesFromFile();
        if (cities == null || cities.isEmpty()) {
            initializeDefaultCities();
            saveCitiesToFile();
        }

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
        createModeToggleButton();


        loadWeatherDataForAllCities();
        loadAirQualityDataForAllCities();
    }

    private void initializeDefaultCities() {
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

    private void loadCitiesFromFile() {
        try {
            FileHandle file = Gdx.files.local(CITIES_FILE);
            if (file.exists()) {
                String jsonString = file.readString();
                Json json = new Json();
                cities = json.fromJson(ArrayList.class, City.class, jsonString);
                System.out.println("Loaded " + cities.size() + " cities from file");
            }
        } catch (Exception e) {
            System.err.println("Failed to load cities from file: " + e.getMessage());
            cities = new ArrayList<>();
        }
    }

    private void saveCitiesToFile() {
        try {
            Json json = new Json();
            json.setUsePrototypes(false);
            String jsonString = json.prettyPrint(cities);
            FileHandle file = Gdx.files.local(CITIES_FILE);
            file.writeString(jsonString, false);
            System.out.println("Saved " + cities.size() + " cities to file");
        } catch (Exception e) {
            System.err.println("Failed to save cities to file: " + e.getMessage());
        }
    }

    private void loadWeatherDataForAllCities() {
        if (USE_STATIC_WEATHER_DATA) {
            System.out.println("Using static weather data from JSON file");
            return; // Weather data already loaded from file
        }

        new Thread(() -> {
            for (City city : cities) {
                // Skip API call if city is marked as static
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

    private void loadAirQualityDataForAllCities() {
        if (USE_STATIC_WEATHER_DATA) {
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
    private void loadAirQualityData(City city) throws Exception {
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

            // Parse JSON response
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

    private Vector2 screenToGeo(float x, float y) {
        // Convert screen coordinates to geographic coordinates
        double lon = MIN_LON + (x / MAP_WIDTH) * (MAX_LON - MIN_LON);
        double lat = MIN_LAT + (y / MAP_HEIGHT) * (MAX_LAT - MIN_LAT);
        return new Vector2((float)lat, (float)lon);
    }
    private void createModeToggleButton() {
        modeToggleButton = new TextButton("Air Quality Mode", skin);
        modeToggleButton.setSize(180, 45);
        modeToggleButton.setPosition(20, Gdx.graphics.getHeight() - 70);

        modeToggleButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                airQualityMode = !airQualityMode;
                modeToggleButton.setText(airQualityMode ? "Weather Mode" : "Air Quality Mode");

                // Close panel if open and refresh if needed
                if (showWeatherPanel && selectedCity != null) {
                    showWeatherPanel = false;
                    selectedCity = null;
                }

                System.out.println("Mode switched to: " + (airQualityMode ? "Air Quality" : "Weather"));
            }
        });

        stage.addActor(modeToggleButton);
    }

    private void handleInput() {
        float delta = Gdx.graphics.getDeltaTime();
        float moveAmount = PAN_SPEED * delta * camera.zoom;

        // Toggle edit mode with 'E' key
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            editMode = !editMode;
            showWeatherPanel = false;
            selectedCity = null;
            awaitingLocationClick = false;
            pendingLocation = null;
            System.out.println("Edit mode: " + (editMode ? "ON" : "OFF"));
        }

        // Add new city with 'A' key in edit mode
        if (editMode && Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            awaitingLocationClick = true;
            pendingLocation = null;
            System.out.println("Click on the map to select location for new city");
        }

        // Cancel location selection with ESC
        if (awaitingLocationClick && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            awaitingLocationClick = false;
            pendingLocation = null;
            System.out.println("Location selection cancelled");
        }

        // Delete selected city with DELETE key in edit mode
        if (editMode && selectedCity != null && Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            showDeleteConfirmDialog(selectedCity);
        }

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

        // Update hovered city
        if (stage.getKeyboardFocus() == null && !awaitingLocationClick) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            hoveredCity = null;

            for (City city : cities) {
                Vector2 cityPos = geoToScreen(city.lat, city.lon);
                float distance = Vector2.dst(touchPos.x, touchPos.y, cityPos.x, cityPos.y);

                if (distance < 15) {
                    hoveredCity = city;
                    break;
                }
            }
        }

        // Handle city clicks or location selection
        if (Gdx.input.justTouched() && stage.getKeyboardFocus() == null) {
            // If awaiting location click, capture the coordinates
            if (awaitingLocationClick) {
                Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(touchPos);

                Vector2 geoCoords = screenToGeo(touchPos.x, touchPos.y);
                pendingLocation = new Vector2(touchPos.x, touchPos.y);

                // Show dialog with pre-filled coordinates
                if (locationPickCallback != null) {
                    locationPickCallback.accept(geoCoords);
                } else {
                    // No callback means we're adding a new city
                    showAddCityDialog(geoCoords.x, geoCoords.y);
                    awaitingLocationClick = false;
                }
                return;
            }

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

                    if (editMode) {
                        showEditCityDialog(city);
                    } else {
                        boolean dataLoaded = airQualityMode ? city.airQualityLoaded : city.weatherLoaded;
                        showWeatherPanel = dataLoaded;
                        panelAnimationProgress = 0f;
                    }
                    break;
                }
            }
        }
    }

    private void showAddCityDialog(final float lat, final float lon) {
        Dialog dialog = new Dialog("Add New City", skin);

        Table content = dialog.getContentTable();
        content.pad(20);

        final TextField nameField = new TextField("", skin);
        nameField.setMessageText("City name");

        final TextField latField = new TextField(String.format("%.4f", lat), skin);
        latField.setMessageText("Latitude");

        final TextField lonField = new TextField(String.format("%.4f", lon), skin);
        lonField.setMessageText("Longitude");

        content.add(new Label("City Name:", skin)).left().padBottom(5);
        content.row();
        content.add(nameField).width(300).padBottom(15);
        content.row();
        content.add(new Label("Latitude:", skin)).left().padBottom(5);
        content.row();
        content.add(latField).width(300).padBottom(15);
        content.row();
        content.add(new Label("Longitude:", skin)).left().padBottom(5);
        content.row();
        content.add(lonField).width(300).padBottom(15);
        content.row();

        Label hintLabel = new Label("(Coordinates from map click)", skin);
        hintLabel.setColor(0.7f, 0.7f, 0.7f, 1f);
        content.add(hintLabel).padBottom(10);

        TextButton okButton = new TextButton("Add City", skin);
        okButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    String name = nameField.getText().trim();
                    double latVal = Double.parseDouble(latField.getText().trim());
                    double lonVal = Double.parseDouble(lonField.getText().trim());

                    if (name.isEmpty()) {
                        showErrorDialog("Please enter a city name");
                        return;
                    }

                    if (latVal < MIN_LAT || latVal > MAX_LAT || lonVal < MIN_LON || lonVal > MAX_LON) {
                        showErrorDialog("Coordinates must be within Slovenia bounds:\n" +
                            "Lat: " + MIN_LAT + " - " + MAX_LAT + "\n" +
                            "Lon: " + MIN_LON + " - " + MAX_LON);
                        return;
                    }

                    City newCity = new City(name, latVal, lonVal);
                    cities.add(newCity);
                    saveCitiesToFile();

                    // Load weather data for new city (only if not in static mode)
                    if (!USE_STATIC_WEATHER_DATA) {
                        new Thread(() -> {
                            try {
                                loadWeatherData(newCity);
                                loadAirQualityData(newCity);
                            } catch (Exception e) {
                                System.err.println("Failed to load data for new city: " + e.getMessage());
                            }
                        }).start();
                    } else {
                        System.out.println("Static mode: Skipping  API call for new city");
                    }

                    dialog.hide();
                    pendingLocation = null;
                    showSuccessDialog("City '" + name + "' added successfully!");
                } catch (NumberFormatException e) {
                    showErrorDialog("Please enter valid numbers for coordinates");
                }
            }
        });

        TextButton cancelButton = new TextButton("Cancel", skin);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
                pendingLocation = null;
            }
        });

        Table buttonTable = new Table();
        buttonTable.add(okButton).width(120).padRight(10);
        buttonTable.add(cancelButton).width(120);

        content.row();
        content.add(buttonTable).padTop(20);

        dialog.key(Input.Keys.ESCAPE, false);
        dialog.show(stage);
    }

    private void showEditCityDialog(final City city) {
        Dialog dialog = new Dialog("Edit City: " + city.name, skin);
        dialog.getTitleLabel().setAlignment(Align.center);

        Table content = dialog.getContentTable();
        content.pad(20);

        // City name field
        final TextField nameField = new TextField(city.name, skin);
        content.add(new Label("City Name:", skin)).left().padBottom(5);
        content.row();
        content.add(nameField).width(300).padBottom(15);
        content.row();

        // Coordinates with "Pick on Map" button (same as before)
        final TextField latField = new TextField(String.valueOf(city.lat), skin);
        final TextField lonField = new TextField(String.valueOf(city.lon), skin);

        content.add(new Label("Latitude:", skin)).left().padBottom(5);
        content.row();

        Table latTable = new Table();
        latTable.add(latField).width(200).padRight(10);

        TextButton pickLatLonButton = new TextButton("Pick on Map", skin);
        pickLatLonButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
                awaitingLocationClick = true;
                pendingLocation = null;

                locationPickCallback = new java.util.function.Consumer<Vector2>() {
                    @Override
                    public void accept(Vector2 geoCoords) {
                        latField.setText(String.format("%.4f", geoCoords.x));
                        lonField.setText(String.format("%.4f", geoCoords.y));
                        locationPickCallback = null;
                        pendingLocation = null;
                        awaitingLocationClick = false;
                        dialog.show(stage);
                    }
                };

                System.out.println("Click on map to update city location");
            }
        });

        latTable.add(pickLatLonButton).width(90);
        content.add(latTable).padBottom(10);
        content.row();

        content.add(new Label("Longitude:", skin)).left().padBottom(5);
        content.row();
        content.add(lonField).width(300).padBottom(20);
        content.row();

        // Static mode checkbox
        final CheckBox staticCheckBox = new CheckBox(" Use Static Data", skin);
        staticCheckBox.setChecked(city.isStatic);
        content.add(staticCheckBox).left().padBottom(20);
        content.row();

        // === WEATHER DATA SECTION ===
        Label weatherLabel = new Label("Weather Data:", skin);
        weatherLabel.setColor(0.7f, 0.9f, 0.7f, 1f);
        content.add(weatherLabel).left().padBottom(10);
        content.row();

        final TextField tempField = new TextField(
            city.weatherLoaded ? String.format("%.1f", city.temperature) : "0.0", skin);
        tempField.setDisabled(!city.isStatic);
        content.add(new Label("Temperature (°C):", skin)).left().padBottom(5);
        content.row();
        content.add(tempField).width(300).padBottom(10);
        content.row();

        final TextField humidityField = new TextField(
            city.weatherLoaded ? String.valueOf(city.humidity) : "0", skin);
        humidityField.setDisabled(!city.isStatic);
        content.add(new Label("Humidity (%):", skin)).left().padBottom(5);
        content.row();
        content.add(humidityField).width(300).padBottom(10);
        content.row();

        final TextField pressureField = new TextField(
            city.weatherLoaded ? String.valueOf(city.pressure) : "0", skin);
        pressureField.setDisabled(!city.isStatic);
        content.add(new Label("Pressure (hPa):", skin)).left().padBottom(5);
        content.row();
        content.add(pressureField).width(300).padBottom(10);
        content.row();

        final TextField windSpeedField = new TextField(
            city.weatherLoaded ? String.format("%.1f", city.windSpeed) : "0.0", skin);
        windSpeedField.setDisabled(!city.isStatic);
        content.add(new Label("Wind Speed (m/s):", skin)).left().padBottom(5);
        content.row();
        content.add(windSpeedField).width(300).padBottom(10);
        content.row();

        final TextField descriptionField = new TextField(
            city.weatherLoaded ? city.description : "", skin);
        descriptionField.setDisabled(!city.isStatic);
        content.add(new Label("Description:", skin)).left().padBottom(5);
        content.row();
        content.add(descriptionField).width(300).padBottom(10);
        content.row();

        final TextField iconField = new TextField(
            city.weatherLoaded ? city.icon : "01d", skin);
        iconField.setDisabled(!city.isStatic);
        content.add(new Label("Icon Code:", skin)).left().padBottom(5);
        content.row();
        content.add(iconField).width(300).padBottom(20);
        content.row();

        // === AIR QUALITY DATA SECTION ===
        Label airQualityLabel = new Label("Air Quality Data:", skin);
        airQualityLabel.setColor(0.7f, 0.9f, 1f, 1f);
        content.add(airQualityLabel).left().padBottom(10);
        content.row();

        final TextField aqiField = new TextField(
            city.airQualityLoaded ? String.valueOf(city.aqi) : "1", skin);
        aqiField.setDisabled(!city.isStatic);
        content.add(new Label("AQI (1-5):", skin)).left().padBottom(5);
        content.row();
        content.add(aqiField).width(300).padBottom(10);
        content.row();

        final TextField coField = new TextField(
            city.airQualityLoaded ? String.format("%.2f", city.co) : "0.0", skin);
        coField.setDisabled(!city.isStatic);
        content.add(new Label("CO (μg/m³):", skin)).left().padBottom(5);
        content.row();
        content.add(coField).width(300).padBottom(10);
        content.row();

        final TextField no2Field = new TextField(
            city.airQualityLoaded ? String.format("%.2f", city.no2) : "0.0", skin);
        no2Field.setDisabled(!city.isStatic);
        content.add(new Label("NO₂ (μg/m³):", skin)).left().padBottom(5);
        content.row();
        content.add(no2Field).width(300).padBottom(10);
        content.row();

        final TextField o3Field = new TextField(
            city.airQualityLoaded ? String.format("%.2f", city.o3) : "0.0", skin);
        o3Field.setDisabled(!city.isStatic);
        content.add(new Label("O₃ (μg/m³):", skin)).left().padBottom(5);
        content.row();
        content.add(o3Field).width(300).padBottom(10);
        content.row();

        final TextField pm25Field = new TextField(
            city.airQualityLoaded ? String.format("%.2f", city.pm2_5) : "0.0", skin);
        pm25Field.setDisabled(!city.isStatic);
        content.add(new Label("PM2.5 (μg/m³):", skin)).left().padBottom(5);
        content.row();
        content.add(pm25Field).width(300).padBottom(10);
        content.row();

        final TextField pm10Field = new TextField(
            city.airQualityLoaded ? String.format("%.2f", city.pm10) : "0.0", skin);
        pm10Field.setDisabled(!city.isStatic);
        content.add(new Label("PM10 (μg/m³):", skin)).left().padBottom(5);
        content.row();
        content.add(pm10Field).width(300).padBottom(15);
        content.row();

        // Enable/disable all fields based on static checkbox
        staticCheckBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean isStatic = staticCheckBox.isChecked();
                // Weather fields
                tempField.setDisabled(!isStatic);
                humidityField.setDisabled(!isStatic);
                pressureField.setDisabled(!isStatic);
                windSpeedField.setDisabled(!isStatic);
                descriptionField.setDisabled(!isStatic);
                iconField.setDisabled(!isStatic);
                // Air quality fields
                aqiField.setDisabled(!isStatic);
                coField.setDisabled(!isStatic);
                no2Field.setDisabled(!isStatic);
                o3Field.setDisabled(!isStatic);
                pm25Field.setDisabled(!isStatic);
                pm10Field.setDisabled(!isStatic);
            }
        });

        Table buttonTable = new Table();

        TextButton saveButton = new TextButton("Save", skin);
        saveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    String newName = nameField.getText().trim();
                    double newLat = Double.parseDouble(latField.getText().trim());
                    double newLon = Double.parseDouble(lonField.getText().trim());

                    if (newName.isEmpty()) {
                        showErrorDialog("Please enter a city name");
                        return;
                    }

                    if (newLat < MIN_LAT || newLat > MAX_LAT || newLon < MIN_LON || newLon > MAX_LON) {
                        showErrorDialog("Coordinates must be within Slovenia bounds:\n" +
                            "Lat: " + MIN_LAT + " - " + MAX_LAT + "\n" +
                            "Lon: " + MIN_LON + " - " + MAX_LON);
                        return;
                    }

                    // Update basic city info
                    city.name = newName;
                    city.lat = newLat;
                    city.lon = newLon;
                    city.isStatic = staticCheckBox.isChecked();

                    // If static mode is enabled, update both weather and air quality data
                    if (city.isStatic) {
                        try {
                            // Weather data
                            city.temperature = Double.parseDouble(tempField.getText().trim());
                            city.humidity = Integer.parseInt(humidityField.getText().trim());
                            city.pressure = Integer.parseInt(pressureField.getText().trim());
                            city.windSpeed = Double.parseDouble(windSpeedField.getText().trim());
                            city.description = descriptionField.getText().trim();
                            city.icon = iconField.getText().trim();
                            city.weatherLoaded = true;

                            // Air quality data
                            city.aqi = Integer.parseInt(aqiField.getText().trim());
                            city.co = Double.parseDouble(coField.getText().trim());
                            city.no2 = Double.parseDouble(no2Field.getText().trim());
                            city.o3 = Double.parseDouble(o3Field.getText().trim());
                            city.pm2_5 = Double.parseDouble(pm25Field.getText().trim());
                            city.pm10 = Double.parseDouble(pm10Field.getText().trim());
                            city.airQualityLoaded = true;

                            System.out.println("Updated static data for " + city.name);
                        } catch (NumberFormatException e) {
                            showErrorDialog("Please enter valid numbers for all data fields");
                            return;
                        }
                    } else if (!USE_STATIC_WEATHER_DATA) {
                        // If not static, reload from API
                        new Thread(() -> {
                            try {
                                loadWeatherData(city);
                                loadAirQualityData(city);
                            } catch (Exception e) {
                                System.err.println("Failed to reload data: " + e.getMessage());
                            }
                        }).start();
                    }

                    saveCitiesToFile();
                    dialog.hide();
                    showSuccessDialog("City updated successfully!");
                } catch (NumberFormatException e) {
                    showErrorDialog("Please enter valid numbers for coordinates");
                }
            }
        });

        TextButton deleteButton = new TextButton("Delete", skin);
        deleteButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
                showDeleteConfirmDialog(city);
            }
        });

        TextButton cancelButton = new TextButton("Cancel", skin);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
                awaitingLocationClick = false;
                pendingLocation = null;
                locationPickCallback = null;
            }
        });

        buttonTable.add(saveButton).width(100).padRight(10);
        buttonTable.add(deleteButton).width(100).padRight(10);
        buttonTable.add(cancelButton).width(100);

        content.row();
        content.add(buttonTable).padTop(20);

        dialog.key(Input.Keys.ESCAPE, false);
        dialog.show(stage);
    }
    private void showDeleteConfirmDialog(final City city) {
        Dialog dialog = new Dialog("Confirm Delete", skin) {
            @Override
            protected void result(Object object) {
                if (object != null && (Boolean) object) {
                    cities.remove(city);
                    saveCitiesToFile();
                    if (selectedCity == city) {
                        selectedCity = null;
                        showWeatherPanel = false;
                    }
                    showSuccessDialog("City '" + city.name + "' deleted successfully!");
                }
            }
        };

        dialog.text("Are you sure you want to delete\n'" + city.name + "'?");
        dialog.button("Delete", true);
        dialog.button("Cancel", false);
        dialog.key(Input.Keys.ENTER, true);
        dialog.key(Input.Keys.ESCAPE, false);
        dialog.show(stage);
    }

    private void showErrorDialog(String message) {
        Dialog dialog = new Dialog("Error", skin) {
            @Override
            protected void result(Object object) {
                // Just close the dialog
            }
        };
        dialog.text(message);
        dialog.button("OK", true);
        dialog.key(Input.Keys.ENTER, true);
        dialog.key(Input.Keys.ESCAPE, true);
        dialog.show(stage);
    }

    private void showSuccessDialog(String message) {
        Dialog dialog = new Dialog("Success", skin) {
            @Override
            protected void result(Object object) {
                // Just close the dialog
            }
        };
        dialog.text(message);
        dialog.button("OK", true);
        dialog.key(Input.Keys.ENTER, true);
        dialog.key(Input.Keys.ESCAPE, true);
        dialog.show(stage);
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

        // Draw pending location marker
        if (awaitingLocationClick && pendingLocation != null) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            float pulseSize = 20 + (float)Math.sin(markerPulse * 3) * 5;
            shapeRenderer.setColor(0.2f, 1f, 0.4f, 0.3f);
            shapeRenderer.circle(pendingLocation.x, pendingLocation.y, pulseSize);

            shapeRenderer.setColor(0.2f, 1f, 0.4f, 0.8f);
            shapeRenderer.circle(pendingLocation.x, pendingLocation.y, 12);

            shapeRenderer.setColor(1f, 1f, 1f, 1f);
            shapeRenderer.circle(pendingLocation.x, pendingLocation.y, 8);

            shapeRenderer.end();
        }

        // Draw city markers with improved styling
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (City city : cities) {
            Vector2 pos = geoToScreen(city.lat, city.lon);
            boolean isSelected = (city == selectedCity);
            boolean isHovered = (city == hoveredCity);

            // Outer glow for selected or hovered city
            if (isSelected || isHovered) {
                float pulseSize = 20 + (float)Math.sin(markerPulse) * 3;
                Color markerColor = airQualityMode ? getAQIColor(city.aqi) : getTemperatureColor(city.temperature);
                shapeRenderer.setColor(markerColor.r, markerColor.g, markerColor.b, 0.3f);
                shapeRenderer.circle(pos.x, pos.y, pulseSize);
            }

            // Edit mode indicator
            if (editMode) {
                shapeRenderer.setColor(1f, 0.8f, 0.2f, 0.4f);
                shapeRenderer.circle(pos.x, pos.y, 16);
            }

            // Shadow
            shapeRenderer.setColor(0, 0, 0, 0.4f);
            shapeRenderer.circle(pos.x + 1, pos.y - 1, isSelected ? 14 : 10);

            // Main marker circle - colored by mode
            if (airQualityMode) {
                if (city.airQualityLoaded) {
                    Color aqiColor = getAQIColor(city.aqi);
                    shapeRenderer.setColor(aqiColor);
                } else {
                    shapeRenderer.setColor(0.6f, 0.6f, 0.65f, 1f);
                }
            } else {
                if (city.weatherLoaded) {
                    Color tempColor = getTemperatureColor(city.temperature);
                    shapeRenderer.setColor(tempColor);
                } else {
                    shapeRenderer.setColor(0.6f, 0.6f, 0.65f, 1f);
                }
            }
            shapeRenderer.circle(pos.x, pos.y, isSelected ? 13 : 9);

            // Border ring
            shapeRenderer.setColor(1f, 1f, 1f, 0.9f);
            shapeRenderer.circle(pos.x, pos.y, isSelected ? 11 : 7.5f);

            // Inner core
            if (airQualityMode && city.airQualityLoaded) {
                Color aqiColor = getAQIColor(city.aqi);
                shapeRenderer.setColor(aqiColor.r * 0.8f, aqiColor.g * 0.8f, aqiColor.b * 0.8f, 1f);
            } else if (!airQualityMode && city.weatherLoaded) {
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
            if (city == selectedCity || city == hoveredCity || camera.zoom < 0.7f) {
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

        // Draw weather/air quality panel if a city is selected
        if (showWeatherPanel && selectedCity != null && !editMode) {
            if (airQualityMode && selectedCity.airQualityLoaded) {
                drawAirQualityPanel();
            } else if (!airQualityMode && selectedCity.weatherLoaded) {
                drawWeatherPanel();
            }
        }

        // Draw control hints
        drawControlHints();

        // Draw edit mode indicator
        if (editMode) {
            drawEditModeIndicator();
        }

        // Draw location selection indicator
        if (awaitingLocationClick) {
            drawLocationSelectionIndicator();
        }

        // Draw and update Stage
        stage.act(delta);
        stage.draw();
    }

    private void drawLocationSelectionIndicator() {
        batch.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        float barWidth = 350;
        float barHeight = 50;
        float barX = (Gdx.graphics.getWidth() - barWidth) / 2;
        float barY = Gdx.graphics.getHeight() - barHeight - 70;

        // Background with pulsing effect
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float pulse = 0.8f + (float)Math.sin(markerPulse * 2) * 0.2f;
        shapeRenderer.setColor(0.2f * pulse, 1f * pulse, 0.4f * pulse, 0.9f);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        shapeRenderer.setColor(0.4f, 1f, 0.6f, 1f);
        shapeRenderer.rect(barX, barY + barHeight - 3, barWidth, 3);
        shapeRenderer.end();

        // Text
        batch.begin();
        font.setColor(0.05f, 0.05f, 0.05f, 1f);
        font.draw(batch, "Click on map to select location", barX + 45, barY + 32);
        extraSmallFont.setColor(0.15f, 0.15f, 0.15f, 1f);
        extraSmallFont.draw(batch, "ESC to cancel", barX + 125, barY + 14);
        batch.end();
    }

    private void drawEditModeIndicator() {
        batch.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        float barWidth = 200;
        float barHeight = 50;
        float barX = (Gdx.graphics.getWidth() - barWidth) / 2;
        float barY = Gdx.graphics.getHeight() - barHeight - 10;

        // Background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 0.8f, 0.2f, 0.9f);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        shapeRenderer.setColor(1f, 0.9f, 0.4f, 1f);
        shapeRenderer.rect(barX, barY + barHeight - 3, barWidth, 3);
        shapeRenderer.end();

        // Text
        batch.begin();
        font.setColor(0.1f, 0.1f, 0.1f, 1f);
        font.draw(batch, "EDIT MODE", barX + 50, barY + 32);
        extraSmallFont.setColor(0.2f, 0.2f, 0.2f, 1f);
        extraSmallFont.draw(batch, "A: Add  Del: Delete  Click: Edit", barX + 12, barY + 14);
        batch.end();
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

        // Draw "°C" for Celsius next to temperature
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

    private void drawAirQualityPanel() {
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        float panelX = Gdx.graphics.getWidth() - PANEL_WIDTH - PANEL_MARGIN;
        float panelY = Gdx.graphics.getHeight() - PANEL_HEIGHT - PANEL_MARGIN;

        float animatedX = panelX + (1 - easeOutCubic(panelAnimationProgress)) * (PANEL_WIDTH + PANEL_MARGIN);

        // Draw panel background with shadow
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(0, 0, 0, 0.15f);
        shapeRenderer.rect(animatedX + 8, panelY - 8, PANEL_WIDTH, PANEL_HEIGHT);
        shapeRenderer.setColor(0, 0, 0, 0.1f);
        shapeRenderer.rect(animatedX + 4, panelY - 4, PANEL_WIDTH, PANEL_HEIGHT);

        shapeRenderer.setColor(0.11f, 0.13f, 0.16f, 0.98f);
        shapeRenderer.rect(animatedX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        // Header gradient based on AQI
        Color aqiColor = getAQIColor(selectedCity.aqi);

        for (int i = 0; i < 100; i++) {
            float alpha = 0.85f - (i / 100f) * 0.45f;
            shapeRenderer.setColor(aqiColor.r, aqiColor.g, aqiColor.b, alpha);
            shapeRenderer.rect(animatedX, panelY + PANEL_HEIGHT - 100 + i, PANEL_WIDTH, 1);
        }

        shapeRenderer.setColor(aqiColor.r * 1.2f, aqiColor.g * 1.2f, aqiColor.b * 1.2f, 1f);
        shapeRenderer.rect(animatedX, panelY + PANEL_HEIGHT - 3, PANEL_WIDTH, 3);

        shapeRenderer.setColor(0.25f, 0.27f, 0.3f, 0.8f);
        shapeRenderer.rect(animatedX + PANEL_PADDING, panelY + PANEL_HEIGHT - 105,
            PANEL_WIDTH - PANEL_PADDING * 2, 2);

        shapeRenderer.setColor(0.2f, 0.22f, 0.25f, 0.9f);
        shapeRenderer.circle(animatedX + PANEL_WIDTH - 40, panelY + PANEL_HEIGHT - 40, 18);

        shapeRenderer.end();

        // Draw text content
        batch.begin();

        float textX = animatedX + PANEL_PADDING;
        float textY = panelY + PANEL_HEIGHT - PANEL_PADDING - 5;

        // City name
        titleFont.setColor(1f, 1f, 1f, 0.15f);
        titleFont.draw(batch, selectedCity.name, textX + 2, textY - 12);
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, selectedCity.name, textX, textY - 10);

        // Close button
        font.setColor(0.9f, 0.9f, 0.9f, 1f);
        font.draw(batch, "X", animatedX + PANEL_WIDTH - 47, textY - 10);

        textY -= 90;

        // AQI - large display
        largeFont.setColor(0, 0, 0, 0.3f);
        String aqiText = String.valueOf(selectedCity.aqi);
        largeFont.draw(batch, aqiText, textX + 3, textY - 3);
        largeFont.setColor(Color.WHITE);
        largeFont.draw(batch, aqiText, textX, textY);

        // AQI label
        titleFont.setColor(new Color(0.8f, 0.8f, 0.8f, 1f));
        titleFont.draw(batch, "AQI", textX + 90, textY - 10);

        // AQI description
        smallFont.setColor(new Color(0.85f, 0.87f, 0.9f, 1f));
        smallFont.draw(batch, getAQIDescription(selectedCity.aqi), textX, textY - 50);

        textY -= 105;

        batch.end();

        // Draw icons
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float iconX = textX + 10;
        float iconY = textY - 5;

        drawPM25Icon(iconX, iconY);
        iconY -= 50;

//        drawPM10Icon(iconX, iconY);
//        iconY -= 50;

        drawO3Icon(iconX, iconY);
        iconY -= 50;

        drawNO2Icon(iconX, iconY);
        iconY -= 50;
//
//        drawCOIcon(iconX, iconY);
//        iconY -= 50;

        drawLocationIcon(iconX, iconY);

        shapeRenderer.end();

        batch.begin();

        // Draw info cards
        drawInfoCard(batch, textX, textY, "PM2.5", String.format("%.1f μg/m³", selectedCity.pm2_5));
        textY -= 50;

//        drawInfoCard(batch, textX, textY, "PM10", String.format("%.1f μg/m³", selectedCity.pm10));
//        textY -= 50;

        drawInfoCard(batch, textX, textY, "Ozone (O₃)", String.format("%.1f μg/m³", selectedCity.o3));
        textY -= 50;

       drawInfoCard(batch, textX, textY, "NO₂", String.format("%.1f μg/m³", selectedCity.no2));
        textY -= 50;
//
//        drawInfoCard(batch, textX, textY, "CO", String.format("%.1f μg/m³", selectedCity.co));
//        textY -= 50;

        drawInfoCard(batch, textX, textY, "Location",
            String.format("%.2f N, %.2f E", selectedCity.lat, selectedCity.lon));

        // Footer hint
        textY = panelY + PANEL_PADDING + 10;
        smallFont.setColor(new Color(0.5f, 0.52f, 0.55f, 1f));
        smallFont.draw(batch, "Press ESC or click X to close", textX, textY);

        batch.end();
    }
    private Color getAQIColor(int aqi) {
        switch (aqi) {
            case 1: return new Color(0.3f, 0.85f, 0.3f, 1f); // Good - Green
            case 2: return new Color(0.8f, 0.85f, 0.3f, 1f); // Fair - Yellow
            case 3: return new Color(1f, 0.7f, 0.2f, 1f); // Moderate - Orange
            case 4: return new Color(1f, 0.4f, 0.3f, 1f); // Poor - Red
            case 5: return new Color(0.8f, 0.2f, 0.5f, 1f); // Very Poor - Purple
            default: return new Color(0.6f, 0.6f, 0.6f, 1f); // Unknown - Gray
        }
    }

    private String getAQIDescription(int aqi) {
        switch (aqi) {
            case 1: return "Good";
            case 2: return "Fair";
            case 3: return "Moderate";
            case 4: return "Poor";
            case 5: return "Very Poor";
            default: return "Unknown";
        }
    }

    // Icon drawing methods for air quality pollutants:
    private void drawPM25Icon(float x, float y) {
        shapeRenderer.setColor(0.95f, 0.5f, 0.3f, 1f);
        // Small particles
        shapeRenderer.circle(x - 3, y + 3, 2.5f);
        shapeRenderer.circle(x + 3, y + 1, 2f);
        shapeRenderer.circle(x, y - 3, 2.5f);
        shapeRenderer.circle(x + 4, y - 2, 1.5f);
    }
    private void drawPM10Icon(float x, float y) {
        shapeRenderer.setColor(0.85f, 0.6f, 0.4f, 1f);
        shapeRenderer.circle(x - 4, y + 3, 3.5f);
        shapeRenderer.circle(x + 4, y, 3f);
        shapeRenderer.circle(x, y - 4, 3.5f);
    }

    private void drawO3Icon(float x, float y) {
        shapeRenderer.setColor(0.5f, 0.7f, 0.95f, 1f);
        // Ozone molecule representation
        shapeRenderer.circle(x, y + 4, 3f);
        shapeRenderer.circle(x - 4, y - 2, 3f);
        shapeRenderer.circle(x + 4, y - 2, 3f);
    }

    private void drawNO2Icon(float x, float y) {
        shapeRenderer.setColor(0.9f, 0.6f, 0.3f, 1f);
        // Chemical symbol representation
        shapeRenderer.circle(x - 3, y, 3.5f);
        shapeRenderer.circle(x + 3, y + 2, 2.5f);
        shapeRenderer.circle(x + 3, y - 2, 2.5f);
    }

    private void drawCOIcon(float x, float y) {
        shapeRenderer.setColor(0.7f, 0.5f, 0.8f, 1f);
        // Carbon monoxide representation
        shapeRenderer.circle(x - 3, y, 4f);
        shapeRenderer.circle(x + 3, y, 3f);
    }
    private void drawControlHints() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        smallFont.setColor(new Color(1f, 1f, 1f, 0.7f));
        String hints = editMode ?
            "Arrow Keys: Pan  |  +/-: Zoom  |  A: Click Map to Add  |  Click City: Edit  |  Del: Delete  |  E: Exit Edit" :
            "Arrow Keys: Pan  |  +/-: Zoom  |  Click City: Weather Info  |  E: Edit Mode";
        float textWidth = 700; // Approximate
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
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        uiCamera.setToOrtho(false, width, height);
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
        if (stage != null) {
            stage.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
    }

    public boolean scrolled(float amountX, float amountY) {
        camera.zoom += amountY * 0.1f;
        camera.zoom = MathUtils.clamp(camera.zoom, MIN_ZOOM, MAX_ZOOM);
        return true;
    }
}
