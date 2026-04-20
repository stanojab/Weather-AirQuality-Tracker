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

import java.io.InputStream;
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

    // Fonts
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
    private static final int MAP_WIDTH = 1920;
    private static final int MAP_HEIGHT = 1080;

    // Camera control settings
    private static final float PAN_SPEED = 500f;
    private static final float ZOOM_SPEED = 2f;
    private static final float MIN_ZOOM = 0.3f;
    private static final float MAX_ZOOM = 1f;

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

    // Camera animation
    private boolean isAnimatingCamera = false;
    private float cameraAnimationProgress = 0f;
    private Vector2 cameraStartPos = new Vector2();
    private Vector2 cameraTargetPos = new Vector2();
    private float cameraStartZoom = 1f;
    private float cameraTargetZoom = 1f;
    private static final float CAMERA_ANIMATION_DURATION = 0.8f;
    private static final float ZOOM_IN_LEVEL = 0.4f;

    // Air Quality Mode
    private boolean airQualityMode = false;
    private TextButton modeToggleButton;

    private static final String GEOAPIFY_API_KEY = "930e7c22c63b486eac329474adc56afd";
    private static final String CITIES_FILE = "cities.json";

    // Cache settings
    private static final String CACHE_DIR = "cache/";
    private static final String MAP_CACHE_FILE = CACHE_DIR + "slovenia_map.png";

    // Manager and Renderer instances
    private WeatherDataManager weatherDataManager;
    private AirQualityDataManager airQualityDataManager;
    private MapRenderer mapRenderer;
    private UIRenderer uiRenderer;
    private ParticleEffectsManager particleEffectsManager;

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

        // Setup cameras
        camera = new OrthographicCamera();
        camera.setToOrtho(false, MAP_WIDTH, MAP_HEIGHT);
        camera.position.set(MAP_WIDTH / 2f, MAP_HEIGHT / 2f, 0);
        camera.zoom = 0.9f;
        camera.update();

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Setup Scene2D
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Create input multiplexer
        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                return SloveniaMap.this.scrolled(amountX, amountY);
            }
        });
        Gdx.input.setInputProcessor(multiplexer);

        // Initialize managers and renderers
        weatherDataManager = new WeatherDataManager();
        airQualityDataManager = new AirQualityDataManager();
        mapRenderer = new MapRenderer(shapeRenderer, smallFont);
        uiRenderer = new UIRenderer(shapeRenderer, batch, font, titleFont, smallFont, largeFont, extraSmallFont);
        particleEffectsManager = new ParticleEffectsManager(mapRenderer);

        // Load or initialize cities
        loadCitiesFromFile();
        if (cities == null || cities.isEmpty()) {
            initializeDefaultCities();
            saveCitiesToFile();
        }

        // Load map texture with caching
        mapTexture = loadMapWithCache();

        createModeToggleButton();

        // Load weather and air quality data
        weatherDataManager.loadWeatherDataForAllCities(cities);
        airQualityDataManager.loadAirQualityDataForAllCities(cities);
        mapRenderer.preloadWeatherIcons(cities);
    }

    private void initializeDefaultCities() {
        cities = new ArrayList<>();
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
        cities.add(new City("Krsko", 45.9589, 15.4919));
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

    private Texture loadMapWithCache() {
        try {
            // Try to load from cache first
            FileHandle cachedMap = Gdx.files.local(MAP_CACHE_FILE);
            if (cachedMap.exists()) {
                System.out.println("Loading map from cache...");
                Texture texture = new Texture(cachedMap);
                System.out.println("Map loaded from cache successfully!");
                return texture;
            }

            // If not cached, download it
            System.out.println("Map not cached, downloading...");
            String mapUrl = buildGeoapifyUrl(MAP_WIDTH, MAP_HEIGHT);
            Texture texture = downloadAndCacheMapTexture(mapUrl);
            System.out.println("Map downloaded and cached successfully!");
            return texture;

        } catch (Exception e) {
            System.err.println("Failed to load map: " + e.getMessage());
            e.printStackTrace();
            return null;
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

    private Texture downloadAndCacheMapTexture(String urlString) throws Exception {
        // Ensure cache directory exists
        FileHandle cacheDir = Gdx.files.local(CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
            System.out.println("Created cache directory");
        }

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

            // Create texture from data
            Pixmap pixmap = new Pixmap(imageData, 0, imageData.length);
            Texture texture = new Texture(pixmap);

            // Save to cache
            try {
                FileHandle cachedMap = Gdx.files.local(MAP_CACHE_FILE);
                cachedMap.writeBytes(imageData, false);
                System.out.println("Map cached to: " + MAP_CACHE_FILE);
            } catch (Exception e) {
                System.err.println("Failed to cache map: " + e.getMessage());
            }

            pixmap.dispose();
            return texture;
        } else {
            throw new Exception("HTTP error code: " + responseCode);
        }
    }


    private void clearMapCache() {
        try {
            FileHandle cachedMap = Gdx.files.local(MAP_CACHE_FILE);
            if (cachedMap.exists()) {
                cachedMap.delete();
                System.out.println("Map cache cleared");
            }
        } catch (Exception e) {
            System.err.println("Failed to clear map cache: " + e.getMessage());
        }
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

                if (showWeatherPanel && selectedCity != null) {
                    showWeatherPanel = false;
                    selectedCity = null;
                }

                System.out.println("Mode switched to: " + (airQualityMode ? "Air Quality" : "Weather"));
            }
        });

        stage.addActor(modeToggleButton);
    }

    private void animateCameraToCity(City city) {
        Vector2 cityScreenPos = mapRenderer.geoToScreen(city.lat, city.lon);

        // Store current camera state
        cameraStartPos.set(camera.position.x, camera.position.y);
        cameraStartZoom = camera.zoom;

        // Set target camera state
        cameraTargetPos.set(cityScreenPos.x, cityScreenPos.y);
        cameraTargetZoom = ZOOM_IN_LEVEL;

        // Start animation
        isAnimatingCamera = true;
        cameraAnimationProgress = 0f;
        particleEffectsManager.setFocusedCity(city);

    }

    private void updateCameraAnimation(float delta) {
        if (!isAnimatingCamera) return;

        cameraAnimationProgress += delta / CAMERA_ANIMATION_DURATION;

        if (cameraAnimationProgress >= 1f) {
            // Animation complete
            cameraAnimationProgress = 1f;
            isAnimatingCamera = false;
        }

        // Ease-out cubic interpolation for smooth animation
        float t = easeOutCubic(cameraAnimationProgress);

        // Interpolate position
        camera.position.x = cameraStartPos.x + (cameraTargetPos.x - cameraStartPos.x) * t;
        camera.position.y = cameraStartPos.y + (cameraTargetPos.y - cameraStartPos.y) * t;

        // Interpolate zoom
        camera.zoom = cameraStartZoom + (cameraTargetZoom - cameraStartZoom) * t;

        // Clamp to bounds
        camera.zoom = MathUtils.clamp(camera.zoom, MIN_ZOOM, MAX_ZOOM);

        float effectiveWidth = MAP_WIDTH * camera.zoom;
        float effectiveHeight = MAP_HEIGHT * camera.zoom;

        camera.position.x = MathUtils.clamp(camera.position.x,
            effectiveWidth / 2f,
            MAP_WIDTH - effectiveWidth / 2f);
        camera.position.y = MathUtils.clamp(camera.position.y,
            effectiveHeight / 2f,
            MAP_HEIGHT - effectiveHeight / 2f);

        camera.update();
    }

    private float easeOutCubic(float t) {
        return 1 - (float)Math.pow(1 - t, 3);
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

        // Toggle particle effects with 'P' key
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            particleEffectsManager.setEnabled(!particleEffectsManager.isEnabled());
            System.out.println("Particle effects: " + (particleEffectsManager.isEnabled() ? "ON" : "OFF"));
        }

        // Clear cache with 'C' key (for debugging)
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            clearMapCache();
            mapRenderer.clearIconCache();
            showSuccessDialog("Cache cleared! Restart the app to re-download.");
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

        // Only allow manual camera control if not animating
        if (!isAnimatingCamera) {
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
        }

        // Close panel with ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            showWeatherPanel = false;
            selectedCity = null;
            particleEffectsManager.setFocusedCity(null);
        }

        // Clamp zoom (only if not animating, animation handles its own clamping)
        if (!isAnimatingCamera) {
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
        }

        // Update hovered city
        if (stage.getKeyboardFocus() == null && !awaitingLocationClick) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            hoveredCity = null;

            for (City city : cities) {
                Vector2 cityPos = mapRenderer.geoToScreen(city.lat, city.lon);
                float distance = Vector2.dst(touchPos.x, touchPos.y, cityPos.x, cityPos.y);

                if (distance < 15) {
                    hoveredCity = city;
                    break;
                }
            }
        }

        // Handle city clicks or location selection
        if (Gdx.input.justTouched() && stage.getKeyboardFocus() == null) {
            if (awaitingLocationClick) {
                Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(touchPos);

                Vector2 geoCoords = mapRenderer.screenToGeo(touchPos.x, touchPos.y);
                pendingLocation = new Vector2(touchPos.x, touchPos.y);

                if (locationPickCallback != null) {
                    locationPickCallback.accept(geoCoords);
                } else {
                    showAddCityDialog(geoCoords.x, geoCoords.y);
                    awaitingLocationClick = false;
                }
                return;
            }

            // Check if clicking on close button of panel
            if (showWeatherPanel) {
                int PANEL_WIDTH = 380;
                int PANEL_HEIGHT = 450;
                int PANEL_MARGIN = 30;
                float panelX = Gdx.graphics.getWidth() - PANEL_WIDTH - PANEL_MARGIN;
                float panelY = Gdx.graphics.getHeight() - PANEL_HEIGHT - PANEL_MARGIN;
                float closeX = panelX + PANEL_WIDTH - 45;
                float closeY = panelY + PANEL_HEIGHT - 45;

                if (Gdx.input.getX() >= closeX && Gdx.input.getX() <= closeX + 35 &&
                    Gdx.graphics.getHeight() - Gdx.input.getY() >= closeY &&
                    Gdx.graphics.getHeight() - Gdx.input.getY() <= closeY + 35) {
                    showWeatherPanel = false;
                    selectedCity = null;
                    particleEffectsManager.setFocusedCity(null);
                    return;
                }
            }

            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);

            for (City city : cities) {
                Vector2 cityPos = mapRenderer.geoToScreen(city.lat, city.lon);
                float distance = Vector2.dst(touchPos.x, touchPos.y, cityPos.x, cityPos.y);

                if (distance < 15) {
                    selectedCity = city;

                    if (editMode) {
                        showEditCityDialog(city);
                    } else {
                        boolean dataLoaded = airQualityMode ? city.airQualityLoaded : city.weatherLoaded;
                        showWeatherPanel = dataLoaded;
                        panelAnimationProgress = 0f;

                        // Animate camera to the city
                        animateCameraToCity(city);

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

                    if (!weatherDataManager.isUsingStaticData()) {
                        new Thread(() -> {
                            try {
                                weatherDataManager.loadWeatherData(newCity);
                                airQualityDataManager.loadAirQualityData(newCity);
                            } catch (Exception e) {
                                System.err.println("Failed to load data for new city: " + e.getMessage());
                            }
                        }).start();
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

        final TextField nameField = new TextField(city.name, skin);
        content.add(new Label("City Name:", skin)).left().padBottom(5);
        content.row();
        content.add(nameField).width(300).padBottom(15);
        content.row();

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

        final CheckBox staticCheckBox = new CheckBox(" Use Static Data", skin);
        staticCheckBox.setChecked(city.isStatic);
        content.add(staticCheckBox).left().padBottom(20);
        content.row();

        // Icon selection dropdown
        final SelectBox<String> iconSelectBox = new SelectBox<String>(skin);
        String[] iconOptions = {
            "Clear Sky",
            "Few Clouds",
            "Scattered Clouds",
            "Broken Clouds",
            "Rain",
            "Thunderstorm",
            "Snow",
            "Mist"
        };
        iconSelectBox.setItems(iconOptions);

        // Set current icon selection based on city's icon data
        if (city.icon != null) {
            switch (city.icon) {
                case "01d": iconSelectBox.setSelected("Clear Sky"); break;
                case "02d": iconSelectBox.setSelected("Few Clouds"); break;
                case "03d": iconSelectBox.setSelected("Scattered Clouds"); break;
                case "04d": iconSelectBox.setSelected("Broken Clouds"); break;
                case "10d": iconSelectBox.setSelected("Rain"); break;
                case "11d": iconSelectBox.setSelected("Thunderstorm"); break;
                case "13d": iconSelectBox.setSelected("Snow"); break;
                case "50d": iconSelectBox.setSelected("Mist"); break;
                default: iconSelectBox.setSelected("Clear Sky"); break;
            }
        } else {
            iconSelectBox.setSelected("Clear Sky");
        }

        content.add(new Label("Weather Icon:", skin)).left().padBottom(5);
        content.row();
        content.add(iconSelectBox).width(300).padBottom(20);
        content.row();

        // Data fields based on mode
        final TextField tempField, humidityField, pressureField, windSpeedField, descriptionField;
        final TextField aqiField, no2Field, o3Field, pm25Field;

        if (airQualityMode) {
            Label airQualityLabel = new Label("Air Quality Data:", skin);
            airQualityLabel.setColor(0.7f, 0.9f, 1f, 1f);
            content.add(airQualityLabel).left().padBottom(10);
            content.row();

            aqiField = new TextField(
                city.airQualityLoaded ? String.valueOf(city.aqi) : "1", skin);
            aqiField.setDisabled(!city.isStatic);
            content.add(new Label("AQI (1-5):", skin)).left().padBottom(5);
            content.row();
            content.add(aqiField).width(300).padBottom(10);
            content.row();

            no2Field = new TextField(
                city.airQualityLoaded ? String.format("%.2f", city.no2) : "0.0", skin);
            no2Field.setDisabled(!city.isStatic);
            content.add(new Label("NO₂ ", skin)).left().padBottom(5);
            content.row();
            content.add(no2Field).width(300).padBottom(10);
            content.row();

            o3Field = new TextField(
                city.airQualityLoaded ? String.format("%.2f", city.o3) : "0.0", skin);
            o3Field.setDisabled(!city.isStatic);
            content.add(new Label("O₃ :", skin)).left().padBottom(5);
            content.row();
            content.add(o3Field).width(300).padBottom(10);
            content.row();

            pm25Field = new TextField(
                city.airQualityLoaded ? String.format("%.2f", city.pm2_5) : "0.0", skin);
            pm25Field.setDisabled(!city.isStatic);
            content.add(new Label("PM2.5 :", skin)).left().padBottom(5);
            content.row();
            content.add(pm25Field).width(300).padBottom(10);
            content.row();

            tempField = null;
            humidityField = null;
            pressureField = null;
            windSpeedField = null;
            descriptionField = null;

        } else {
            Label weatherLabel = new Label("Weather Data:", skin);
            weatherLabel.setColor(0.7f, 0.9f, 0.7f, 1f);
            content.add(weatherLabel).left().padBottom(10);
            content.row();

            tempField = new TextField(
                city.weatherLoaded ? String.format("%.1f", city.temperature) : "0.0", skin);
            tempField.setDisabled(!city.isStatic);
            content.add(new Label("Temperature (°C):", skin)).left().padBottom(5);
            content.row();
            content.add(tempField).width(300).padBottom(10);
            content.row();

            humidityField = new TextField(
                city.weatherLoaded ? String.valueOf(city.humidity) : "0", skin);
            humidityField.setDisabled(!city.isStatic);
            content.add(new Label("Humidity (%):", skin)).left().padBottom(5);
            content.row();
            content.add(humidityField).width(300).padBottom(10);
            content.row();

            pressureField = new TextField(
                city.weatherLoaded ? String.valueOf(city.pressure) : "0", skin);
            pressureField.setDisabled(!city.isStatic);
            content.add(new Label("Pressure (hPa):", skin)).left().padBottom(5);
            content.row();
            content.add(pressureField).width(300).padBottom(10);
            content.row();

            windSpeedField = new TextField(
                city.weatherLoaded ? String.format("%.1f", city.windSpeed) : "0.0", skin);
            windSpeedField.setDisabled(!city.isStatic);
            content.add(new Label("Wind Speed (m/s):", skin)).left().padBottom(5);
            content.row();
            content.add(windSpeedField).width(300).padBottom(10);
            content.row();


            descriptionField = new TextField(
                city.weatherLoaded && city.description != null ? city.description : "", skin);
            descriptionField.setDisabled(!city.isStatic);
            content.add(new Label("Description:", skin)).left().padBottom(5);
            content.row();
            content.add(descriptionField).width(300).padBottom(10);
            content.row();

            aqiField = null;
            no2Field = null;
            o3Field = null;
            pm25Field = null;
        }

        staticCheckBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean isStatic = staticCheckBox.isChecked();
                if (airQualityMode) {
                    if (aqiField != null) aqiField.setDisabled(!isStatic);
                    if (no2Field != null) no2Field.setDisabled(!isStatic);
                    if (o3Field != null) o3Field.setDisabled(!isStatic);
                    if (pm25Field != null) pm25Field.setDisabled(!isStatic);
                } else {
                    if (tempField != null) tempField.setDisabled(!isStatic);
                    if (humidityField != null) humidityField.setDisabled(!isStatic);
                    if (pressureField != null) pressureField.setDisabled(!isStatic);
                    if (windSpeedField != null) windSpeedField.setDisabled(!isStatic);
                    if (descriptionField != null) descriptionField.setDisabled(!isStatic);
                }
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

                    city.name = newName;
                    city.lat = newLat;
                    city.lon = newLon;
                    city.isStatic = staticCheckBox.isChecked();

                    // Save selected icon
                    String selectedIcon = iconSelectBox.getSelected();
                    switch (selectedIcon) {
                        case "Clear Sky": city.icon = "01d"; break;
                        case "Few Clouds": city.icon = "02d"; break;
                        case "Scattered Clouds": city.icon = "03d"; break;
                        case "Broken Clouds": city.icon = "04d"; break;
                        case "Rain": city.icon = "10d"; break;
                        case "Thunderstorm": city.icon = "11d"; break;
                        case "Snow": city.icon = "13d"; break;
                        case "Mist": city.icon = "50d"; break;
                        default: city.icon = "01d"; break;
                    }

                    if (city.isStatic) {
                        try {
                            if (airQualityMode) {
                                city.aqi = Integer.parseInt(aqiField.getText().trim());
                                city.no2 = Double.parseDouble(no2Field.getText().trim());
                                city.o3 = Double.parseDouble(o3Field.getText().trim());
                                city.pm2_5 = Double.parseDouble(pm25Field.getText().trim());
                                city.airQualityLoaded = true;
                            } else {
                                city.temperature = Double.parseDouble(tempField.getText().trim());
                                city.humidity = Integer.parseInt(humidityField.getText().trim());
                                city.pressure = Integer.parseInt(pressureField.getText().trim());
                                city.windSpeed = Double.parseDouble(windSpeedField.getText().trim());
                                city.description = descriptionField.getText().trim();
                                city.weatherLoaded = true;
                            }
                        } catch (NumberFormatException e) {
                            showErrorDialog("Please enter valid numbers for all data fields");
                            return;
                        }
                    } else if (!weatherDataManager.isUsingStaticData()) {
                        new Thread(() -> {
                            try {
                                if (airQualityMode) {
                                    airQualityDataManager.loadAirQualityData(city);
                                } else {
                                    weatherDataManager.loadWeatherData(city);
                                }
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

        float delta = Gdx.graphics.getDeltaTime();
        markerPulse += delta * 2f;
        if (showWeatherPanel && panelAnimationProgress < 1f) {
            panelAnimationProgress = Math.min(1f, panelAnimationProgress + delta * 4f);
        }

        // Update camera animation
        updateCameraAnimation(delta);

        // Update particle effects
        particleEffectsManager.update(delta, cities, airQualityMode);

        ScreenUtils.clear(0.12f, 0.15f, 0.2f, 1f);

        // Draw map
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
            mapRenderer.drawPendingLocationMarker(pendingLocation, markerPulse);
        }


        shapeRenderer.setProjectionMatrix(camera.combined);
        particleEffectsManager.render(shapeRenderer);


        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);
        mapRenderer.drawCityIcons(batch, cities, selectedCity, hoveredCity, editMode, markerPulse, airQualityMode);

        // Draw city labels
        batch.setProjectionMatrix(camera.combined);
        mapRenderer.drawCityLabels(batch, cities, selectedCity, hoveredCity, camera.zoom);

        // Draw weather/air quality panel
        if (showWeatherPanel && selectedCity != null && !editMode) {
            shapeRenderer.setProjectionMatrix(uiCamera.combined);
            batch.setProjectionMatrix(uiCamera.combined);

            if (airQualityMode && selectedCity.airQualityLoaded) {
                uiRenderer.drawAirQualityPanel(selectedCity, panelAnimationProgress, markerPulse, mapRenderer);
            } else if (!airQualityMode && selectedCity.weatherLoaded) {
                uiRenderer.drawWeatherPanel(selectedCity, panelAnimationProgress, markerPulse, mapRenderer);
            }
        }

        // Draw UI elements
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        uiRenderer.drawControlHints(editMode, particleEffectsManager.isEnabled());

        if (editMode) {
            uiRenderer.drawEditModeIndicator();
        }

        if (awaitingLocationClick) {
            uiRenderer.drawLocationSelectionIndicator(markerPulse);
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        uiCamera.setToOrtho(false, width, height);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (mapTexture != null) mapTexture.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        if (smallFont != null) smallFont.dispose();
        if (largeFont != null) largeFont.dispose();
        if (extraSmallFont != null) extraSmallFont.dispose();
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (mapRenderer != null) mapRenderer.dispose();

    }

    public boolean scrolled(float amountX, float amountY) {

        isAnimatingCamera = false;

        camera.zoom += amountY * 0.1f;
        camera.zoom = MathUtils.clamp(camera.zoom, MIN_ZOOM, MAX_ZOOM);
        return true;
    }
}
