package si.um.feri.bozinov;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import java.util.List;

public class MapRenderer {
    private static final double MIN_LON = 13.3;
    private static final double MAX_LON = 16.6;
    private static final double MIN_LAT = 45.4;
    private static final double MAX_LAT = 46.9;
    private static final int MAP_WIDTH = 1920;
    private static final int MAP_HEIGHT = 1080;

    private ShapeRenderer shapeRenderer;
    private BitmapFont smallFont;

    public MapRenderer(ShapeRenderer shapeRenderer, BitmapFont smallFont) {
        this.shapeRenderer = shapeRenderer;
        this.smallFont = smallFont;
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
    }

    public void drawCityLabels(SpriteBatch batch, List<City> cities, City selectedCity,
                               City hoveredCity, float cameraZoom) {
        batch.begin();
        for (City city : cities) {
            if (city == selectedCity || city == hoveredCity || cameraZoom < 0.7f) {
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

    public Color getAQIColor(int aqi) {
        switch (aqi) {
            case 1: return new Color(0.3f, 0.85f, 0.3f, 1f); // Good - Green
            case 2: return new Color(0.8f, 0.85f, 0.3f, 1f); // Fair - Yellow
            case 3: return new Color(1f, 0.7f, 0.2f, 1f); // Moderate - Orange
            case 4: return new Color(1f, 0.4f, 0.3f, 1f); // Poor - Red
            case 5: return new Color(0.8f, 0.2f, 0.5f, 1f); // Very Poor - Purple
            default: return new Color(0.6f, 0.6f, 0.6f, 1f); // Unknown - Gray
        }
    }
}
