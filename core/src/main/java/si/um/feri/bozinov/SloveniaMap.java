package si.um.feri.bozinov;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Version using Geoapify Static Maps API with camera controls
 * Get your free API key at: https://www.geoapify.com/
 * Free tier: 3,000 requests/day
 *
 * Controls:
 * - Arrow keys: Pan the map
 * - Plus/Minus or Page Up/Down: Zoom in/out
 * - Mouse wheel: Zoom in/out
 */
public class SloveniaMap extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture mapTexture;
    private OrthographicCamera camera;

    // Slovenia center coordinates
    private static final double SLOVENIA_LAT = 46.1512;
    private static final double SLOVENIA_LON = 14.9955;

    // Screen dimensions
    private static final int MAP_WIDTH = 1280;
    private static final int MAP_HEIGHT = 720;

    // Camera control settings
    private static final float PAN_SPEED = 500f; // pixels per second
    private static final float ZOOM_SPEED = 2f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 1f;

    private static final String GEOAPIFY_API_KEY = "930e7c22c63b486eac329474adc56afd";

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Setup camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, MAP_WIDTH, MAP_HEIGHT);
        camera.position.set(MAP_WIDTH / 2f, MAP_HEIGHT / 2f, 0);
        camera.zoom = 0.9f;
        camera.update();

        // Build Geoapify Static Maps API URL
        String mapUrl = buildGeoapifyUrl(SLOVENIA_LON, SLOVENIA_LAT, MAP_WIDTH, MAP_HEIGHT);


        // Load the map texture from URL
        try {
            mapTexture = downloadMapTexture(mapUrl);
            System.out.println("Map loaded successfully!");
        } catch (Exception e) {
            System.err.println("Failed to load map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildGeoapifyUrl(double lon, double lat, int width, int height) {
        // Slovenia bounding box for better fit
        double minLon = 13.3;
        double maxLon = 16.6;
        double minLat = 45.4;
        double maxLat = 46.9;

        // Using area parameter for exact Slovenia bounding box
        // Format: rect:minLon,minLat,maxLon,maxLat
        String area = String.format("rect:%f,%f,%f,%f", minLon, minLat, maxLon, maxLat);

        // Available styles: osm-carto, osm-bright, osm-liberty, maptiler-3d, positron, dark-matter, klokantech-basic
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
