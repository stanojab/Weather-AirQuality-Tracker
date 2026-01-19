package si.um.feri.bozinov;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapRenderer {
    private static final double MIN_LON = 13.3;
    private static final double MAX_LON = 16.6;
    private static final double MIN_LAT = 45.4;
    private static final double MAX_LAT = 46.9;
    private static final int MAP_WIDTH = 1920;
    private static final int MAP_HEIGHT = 1080;

    private ShapeRenderer shapeRenderer;
    private BitmapFont smallFont;

    // Icon caching
    private Map<String, Texture> iconCache;
    private Map<String, byte[]> pendingIcons;
    private Texture defaultWeatherIcon;
    private Map<Integer, Texture> aqiIcons;
    private static final String ICON_BASE_URL = "https://openweathermap.org/img/wn/";

    // File cache directory
    private static final String CACHE_DIR = "cache/";
    private static final String ICON_CACHE_DIR = CACHE_DIR + "icons/";

    public MapRenderer(ShapeRenderer shapeRenderer, BitmapFont smallFont) {
        this.shapeRenderer = shapeRenderer;
        this.smallFont = smallFont;
        this.iconCache = new ConcurrentHashMap<>();
        this.pendingIcons = new ConcurrentHashMap<>();
        this.aqiIcons = new HashMap<>();

        // Ensure cache directories exist
        ensureCacheDirectories();

        createDefaultIcons();

        // Load cached icons on startup
        loadCachedIcons();
    }

    private void ensureCacheDirectories() {
        try {
            FileHandle cacheDir = Gdx.files.local(CACHE_DIR);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
                System.out.println("Created cache directory: " + CACHE_DIR);
            }

            FileHandle iconCacheDir = Gdx.files.local(ICON_CACHE_DIR);
            if (!iconCacheDir.exists()) {
                iconCacheDir.mkdirs();
                System.out.println("Created icon cache directory: " + ICON_CACHE_DIR);
            }
        } catch (Exception e) {
            System.err.println("Failed to create cache directories: " + e.getMessage());
        }
    }

    private void loadCachedIcons() {
        try {
            FileHandle iconDir = Gdx.files.local(ICON_CACHE_DIR);
            if (iconDir.exists() && iconDir.isDirectory()) {
                FileHandle[] cachedFiles = iconDir.list(".png");
                for (FileHandle file : cachedFiles) {
                    String iconCode = file.nameWithoutExtension();
                    try {
                        byte[] imageData = file.readBytes();
                        Pixmap pixmap = new Pixmap(imageData, 0, imageData.length);
                        Texture texture = new Texture(pixmap);
                        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                        pixmap.dispose();

                        iconCache.put(iconCode, texture);
                        System.out.println("Loaded cached icon: " + iconCode);
                    } catch (Exception e) {
                        System.err.println("Failed to load cached icon " + iconCode + ": " + e.getMessage());
                        // Delete corrupted cache file
                        file.delete();
                    }
                }
                System.out.println("Loaded " + iconCache.size() + " cached weather icons");
            }
        } catch (Exception e) {
            System.err.println("Error loading cached icons: " + e.getMessage());
        }
    }

    private void createDefaultIcons() {
        defaultWeatherIcon = createCircleIcon(new Color(0.7f, 0.7f, 0.8f, 1f), 64);

        // Create detailed AQI icons
        aqiIcons.put(1, createAQIIcon(1)); // Good
        aqiIcons.put(2, createAQIIcon(2)); // Fair
        aqiIcons.put(3, createAQIIcon(3)); // Moderate
        aqiIcons.put(4, createAQIIcon(4)); // Poor
        aqiIcons.put(5, createAQIIcon(5)); // Very Poor
    }

    private Texture createCircleIcon(Color color, int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0.3f));
        pixmap.fillCircle(size/2 + 1, size/2 - 1, size/2 - 4);

        pixmap.setColor(color);
        pixmap.fillCircle(size/2, size/2, size/2 - 4);

        pixmap.setColor(color.r * 0.7f, color.g * 0.7f, color.b * 0.7f, 1f);
        pixmap.drawCircle(size/2, size/2, size/2 - 5);
        pixmap.drawCircle(size/2, size/2, size/2 - 6);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private Texture createAQIIcon(int aqi) {
        int size = 60;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        Color baseColor = getAQIColor(aqi);
        Color darkColor = new Color(baseColor.r * 0.6f, baseColor.g * 0.6f, baseColor.b * 0.6f, 1f);
        Color lightColor = new Color(
            Math.min(1f, baseColor.r * 1.3f),
            Math.min(1f, baseColor.g * 1.3f),
            Math.min(1f, baseColor.b * 1.3f),
            1f
        );

        // Shadow
        pixmap.setColor(0, 0, 0, 0.3f);
        pixmap.fillCircle(size/2 + 2, size/2 - 2, size/2 - 4);

        // Main circle with gradient effect
        pixmap.setColor(baseColor);
        pixmap.fillCircle(size/2, size/2, size/2 - 4);

        // Highlight (top-left)
        pixmap.setColor(lightColor.r, lightColor.g, lightColor.b, 0.6f);
        pixmap.fillCircle(size/2 - 8, size/2 + 8, size/4);

        // Border rings
        pixmap.setColor(darkColor);
        pixmap.drawCircle(size/2, size/2, size/2 - 5);
        pixmap.drawCircle(size/2, size/2, size/2 - 6);

        pixmap.setColor(1f, 1f, 1f, 0.8f);
        pixmap.drawCircle(size/2, size/2, size/2 - 7);

        // Inner circle for depth
        pixmap.setColor(darkColor.r, darkColor.g, darkColor.b, 0.5f);
        pixmap.fillCircle(size/2, size/2, size/2 - 16);

        // Draw AQI level indicator (simple bars/dots based on level)
        drawAQILevelIndicator(pixmap, size, aqi, baseColor);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private void drawAQILevelIndicator(Pixmap pixmap, int size, int aqi, Color color) {
        int cx = size / 2;
        int cy = size / 2;

        // Draw bars or indicator based on AQI level
        pixmap.setColor(1f, 1f, 1f, 0.9f);

        int barCount = aqi;
        int barHeight = 3;
        int barWidth = 2;
        int spacing = 3;
        int totalWidth = (barCount * barWidth) + ((barCount - 1) * spacing);
        int startX = cx - totalWidth / 2;

        for (int i = 0; i < barCount; i++) {
            int x = startX + (i * (barWidth + spacing));
            int y = cy - barHeight - 2;
            pixmap.fillRectangle(x, y, barWidth, barHeight + (i * 2));
        }

        // Add center dot
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fillCircle(cx, cy + 5, 2);
    }

    private byte[] downloadWeatherIconData(String iconCode) {
        try {
            // Check if icon is already cached on disk
            FileHandle cachedFile = Gdx.files.local(ICON_CACHE_DIR + iconCode + ".png");
            if (cachedFile.exists()) {
                System.out.println("Using cached icon file: " + iconCode);
                return cachedFile.readBytes();
            }

            String urlString = ICON_BASE_URL + iconCode + "@2x.png";
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "LibGDX Weather App");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

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

                // Save to cache
                try {
                    cachedFile.writeBytes(imageData, false);
                    System.out.println("Downloaded and cached weather icon: " + iconCode);
                } catch (Exception e) {
                    System.err.println("Failed to cache icon " + iconCode + ": " + e.getMessage());
                }

                return imageData;
            } else {
                System.err.println("Failed to download icon " + iconCode + ": HTTP " + responseCode);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error downloading icon " + iconCode + ": " + e.getMessage());
            return null;
        }
    }

    public void preloadWeatherIcons(List<City> cities) {
        new Thread(() -> {
            for (City city : cities) {
                if (city.icon != null && !city.icon.isEmpty() &&
                    !iconCache.containsKey(city.icon) &&
                    !pendingIcons.containsKey(city.icon)) {

                    byte[] iconData = downloadWeatherIconData(city.icon);
                    if (iconData != null) {
                        pendingIcons.put(city.icon, iconData);
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Finished preloading weather icons");
        }).start();
    }

    public void processPendingIcons() {
        if (!pendingIcons.isEmpty()) {
            String iconCode = pendingIcons.keySet().iterator().next();
            byte[] imageData = pendingIcons.remove(iconCode);

            if (imageData != null) {
                try {
                    Pixmap pixmap = new Pixmap(imageData, 0, imageData.length);
                    Texture texture = new Texture(pixmap);
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    pixmap.dispose();

                    iconCache.put(iconCode, texture);
                    System.out.println("Created texture for icon: " + iconCode);
                } catch (Exception e) {
                    System.err.println("Failed to create texture for icon " + iconCode + ": " + e.getMessage());
                }
            }
        }
    }

    private Texture getWeatherIcon(String iconCode) {
        if (iconCode == null || iconCode.isEmpty()) {
            return defaultWeatherIcon;
        }

        if (iconCache.containsKey(iconCode)) {
            return iconCache.get(iconCode);
        }

        if (pendingIcons.containsKey(iconCode)) {
            return defaultWeatherIcon;
        }

        final String code = iconCode;
        new Thread(() -> {
            byte[] iconData = downloadWeatherIconData(code);
            if (iconData != null) {
                pendingIcons.put(code, iconData);
            }
        }).start();

        return defaultWeatherIcon;
    }

    public Vector2 geoToScreen(double lat, double lon) {
        float x = (float) ((lon - MIN_LON) / (MAX_LON - MIN_LON) * MAP_WIDTH);
        float y = (float) ((lat - MIN_LAT) / (MAX_LAT - MIN_LAT) * MAP_HEIGHT);
        return new Vector2(x, y);
    }

    public Vector2 screenToGeo(float x, float y) {
        double lon = MIN_LON + (x / MAP_WIDTH) * (MAX_LON - MIN_LON);
        double lat = MIN_LAT + (y / MAP_HEIGHT) * (MAX_LAT - MIN_LAT);
        return new Vector2((float)lat, (float)lon);
    }

    public void drawCityMarkers(List<City> cities, City selectedCity, City hoveredCity,
                                boolean editMode, float markerPulse, boolean airQualityMode) {
        // Deprecated - use drawCityIcons instead
    }

    public void drawCityIcons(SpriteBatch batch, List<City> cities, City selectedCity,
                              City hoveredCity, boolean editMode, float markerPulse,
                              boolean airQualityMode) {
        processPendingIcons();

        batch.begin();

        for (City city : cities) {
            Vector2 pos = geoToScreen(city.lat, city.lon);
            boolean isSelected = (city == selectedCity);
            boolean isHovered = (city == hoveredCity);

            // Get appropriate icon
            Texture icon;
            if (airQualityMode) {
                if (city.airQualityLoaded) {
                    icon = aqiIcons.getOrDefault(city.aqi, defaultWeatherIcon);
                } else {
                    icon = defaultWeatherIcon;
                }
            } else {
                if (city.weatherLoaded && city.icon != null && !city.icon.isEmpty()) {
                    icon = getWeatherIcon(city.icon);
                } else {
                    icon = defaultWeatherIcon;
                }
            }

            // Calculate icon size
            float baseSize = 64f;
            float iconSize = baseSize;

            if (isSelected) {
                float pulse = 1f + (float)Math.sin(markerPulse) * 0.15f;
                iconSize = baseSize * pulse;
            } else if (isHovered) {
                iconSize = baseSize * 1.2f;
            }

            float x = pos.x - iconSize / 2f;
            float y = pos.y - iconSize / 2f;

            // Draw glow for selected or hovered
            if (isSelected || isHovered) {
                batch.setColor(1f, 1f, 1f, 0.5f);
                float glowSize = iconSize * 1.5f;
                batch.draw(icon,
                    pos.x - glowSize / 2f,
                    pos.y - glowSize / 2f,
                    glowSize, glowSize);

                // Additional color glow for AQI mode
                if (airQualityMode && city.airQualityLoaded) {
                    Color aqiColor = getAQIColor(city.aqi);
                    batch.setColor(aqiColor.r, aqiColor.g, aqiColor.b, 0.3f);
                    batch.draw(icon,
                        pos.x - iconSize / 2f,
                        pos.y - iconSize / 2f,
                        iconSize, iconSize);
                }

                batch.setColor(Color.WHITE);
            }

            // Draw shadow
            batch.setColor(0f, 0f, 0f, 0.4f);
            batch.draw(icon, x + 2, y - 2, iconSize, iconSize);

            // Draw main icon
            batch.setColor(Color.WHITE);
            batch.draw(icon, x, y, iconSize, iconSize);

            // Draw border for edit mode
            if (editMode) {
                batch.setColor(1f, 0.8f, 0.2f, 0.7f);
                float borderSize = iconSize * 1.2f;
                batch.draw(icon,
                    pos.x - borderSize / 2f,
                    pos.y - borderSize / 2f,
                    borderSize, borderSize);
                batch.setColor(Color.WHITE);
            }
        }

        batch.end();

        // Draw selection/hover circles
        if (selectedCity != null || hoveredCity != null || editMode) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            for (City city : cities) {
                Vector2 pos = geoToScreen(city.lat, city.lon);
                boolean isSelected = (city == selectedCity);
                boolean isHovered = (city == hoveredCity);

                if (isSelected) {
                    if (airQualityMode && city.airQualityLoaded) {
                        Color aqiColor = getAQIColor(city.aqi);
                        shapeRenderer.setColor(aqiColor.r, aqiColor.g, aqiColor.b, 1f);
                    } else {
                        shapeRenderer.setColor(0.3f, 0.8f, 1f, 1f);
                    }
                    Gdx.gl.glLineWidth(3f);
                    float radius = 32 + (float)Math.sin(markerPulse) * 5;
                    shapeRenderer.circle(pos.x, pos.y, radius, 40);
                } else if (isHovered) {
                    shapeRenderer.setColor(1f, 1f, 0.3f, 1f);
                    Gdx.gl.glLineWidth(2f);
                    shapeRenderer.circle(pos.x, pos.y, 36, 40);
                } else if (editMode) {
                    shapeRenderer.setColor(1f, 0.5f, 0f, 0.5f);
                    Gdx.gl.glLineWidth(1.5f);
                    shapeRenderer.circle(pos.x, pos.y, 34, 30);
                }
            }

            Gdx.gl.glLineWidth(1f);
            shapeRenderer.end();
        }
    }

    public void drawCityLabels(SpriteBatch batch, List<City> cities, City selectedCity,
                               City hoveredCity, float cameraZoom) {
        batch.begin();
        for (City city : cities) {
            if (city == selectedCity || city == hoveredCity || cameraZoom < 0.7f) {
                Vector2 pos = geoToScreen(city.lat, city.lon);

                // Text shadow
                smallFont.setColor(0, 0, 0, 0.8f);
                smallFont.draw(batch, city.name, pos.x - 20 + 1, pos.y - 36 - 1);

                // Text
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, city.name, pos.x - 20, pos.y - 36);
            }
        }
        batch.end();
    }

    public void drawPendingLocationMarker(Vector2 pendingLocation, float markerPulse) {
        if (pendingLocation == null) return;

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

    public Color getTemperatureColor(double temp) {
        if (temp < 0) {
            return new Color(0.3f, 0.6f, 1f, 1f);
        } else if (temp < 10) {
            return new Color(0.4f, 0.75f, 0.95f, 1f);
        } else if (temp < 20) {
            return new Color(0.4f, 0.85f, 0.4f, 1f);
        } else if (temp < 28) {
            return new Color(1f, 0.85f, 0.2f, 1f);
        } else {
            return new Color(1f, 0.4f, 0.3f, 1f);
        }
    }

    public Color getAQIColor(int aqi) {
        switch (aqi) {
            case 1: return new Color(0.3f, 0.85f, 0.3f, 1f);   // Good - Green
            case 2: return new Color(0.8f, 0.85f, 0.3f, 1f);   // Fair - Yellow
            case 3: return new Color(1f, 0.7f, 0.2f, 1f);      // Moderate - Orange
            case 4: return new Color(1f, 0.4f, 0.3f, 1f);      // Poor - Red
            case 5: return new Color(0.8f, 0.2f, 0.5f, 1f);    // Very Poor - Purple
            default: return new Color(0.6f, 0.6f, 0.6f, 1f);   // Unknown - Gray
        }
    }

    /**
     * Clear the icon cache (useful for debugging or forcing refresh)
     */
    public void clearIconCache() {
        try {
            FileHandle iconDir = Gdx.files.local(ICON_CACHE_DIR);
            if (iconDir.exists() && iconDir.isDirectory()) {
                iconDir.deleteDirectory();
                iconDir.mkdirs();
                System.out.println("Cleared icon cache");
            }
        } catch (Exception e) {
            System.err.println("Failed to clear icon cache: " + e.getMessage());
        }
    }

    public void dispose() {
        if (defaultWeatherIcon != null) {
            defaultWeatherIcon.dispose();
        }

        for (Texture icon : iconCache.values()) {
            if (icon != null) {
                icon.dispose();
            }
        }
        iconCache.clear();

        for (Texture icon : aqiIcons.values()) {
            if (icon != null) {
                icon.dispose();
            }
        }
        aqiIcons.clear();

        pendingIcons.clear();
    }
}
