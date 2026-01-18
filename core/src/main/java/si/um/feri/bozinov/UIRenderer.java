package si.um.feri.bozinov;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class UIRenderer {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 450;
    private static final int PANEL_PADDING = 20;
    private static final int PANEL_MARGIN = 30;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont titleFont;
    private BitmapFont smallFont;
    private BitmapFont largeFont;
    private BitmapFont extraSmallFont;

    public UIRenderer(ShapeRenderer shapeRenderer, SpriteBatch batch,
                      BitmapFont font, BitmapFont titleFont, BitmapFont smallFont,
                      BitmapFont largeFont, BitmapFont extraSmallFont) {
        this.shapeRenderer = shapeRenderer;
        this.batch = batch;
        this.font = font;
        this.titleFont = titleFont;
        this.smallFont = smallFont;
        this.largeFont = largeFont;
        this.extraSmallFont = extraSmallFont;
    }

    public void drawWeatherPanel(City city, float panelAnimationProgress, float markerPulse, MapRenderer mapRenderer) {
        float panelX = Gdx.graphics.getWidth() - PANEL_WIDTH - PANEL_MARGIN;
        float panelY = Gdx.graphics.getHeight() - PANEL_HEIGHT - PANEL_MARGIN;
        float animatedX = panelX + (1 - easeOutCubic(panelAnimationProgress)) * (PANEL_WIDTH + PANEL_MARGIN);

        drawPanelBackground(animatedX, panelY, mapRenderer.getTemperatureColor(city.temperature));

        batch.begin();
        float textX = animatedX + PANEL_PADDING;
        float textY = panelY + PANEL_HEIGHT - PANEL_PADDING - 5;

        drawCityHeader(city, animatedX, textX, textY);
        textY -= 90;

        drawTemperatureDisplay(city, textX, textY);
        textY -= 105;

        batch.end();

        drawWeatherIcons(textX, textY);

        batch.begin();
        drawWeatherInfo(city, textX, textY);
        drawFooterHint(textX, panelY);
        batch.end();
    }

    public void drawAirQualityPanel(City city, float panelAnimationProgress, float markerPulse, MapRenderer mapRenderer) {
        float panelX = Gdx.graphics.getWidth() - PANEL_WIDTH - PANEL_MARGIN;
        float panelY = Gdx.graphics.getHeight() - PANEL_HEIGHT - PANEL_MARGIN;
        float animatedX = panelX + (1 - easeOutCubic(panelAnimationProgress)) * (PANEL_WIDTH + PANEL_MARGIN);

        drawPanelBackground(animatedX, panelY, mapRenderer.getAQIColor(city.aqi));

        batch.begin();
        float textX = animatedX + PANEL_PADDING;
        float textY = panelY + PANEL_HEIGHT - PANEL_PADDING - 5;

        drawCityHeader(city, animatedX, textX, textY);
        textY -= 90;

        drawAQIDisplay(city, textX, textY);
        textY -= 105;

        batch.end();

        drawAirQualityIcons(textX, textY);

        batch.begin();
        drawAirQualityInfo(city, textX, textY);
        drawFooterHint(textX, panelY);
        batch.end();
    }

    private void drawPanelBackground(float animatedX, float panelY, Color headerColor) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Shadow layers
        shapeRenderer.setColor(0, 0, 0, 0.15f);
        shapeRenderer.rect(animatedX + 8, panelY - 8, PANEL_WIDTH, PANEL_HEIGHT);
        shapeRenderer.setColor(0, 0, 0, 0.1f);
        shapeRenderer.rect(animatedX + 4, panelY - 4, PANEL_WIDTH, PANEL_HEIGHT);

        // Main background
        shapeRenderer.setColor(0.11f, 0.13f, 0.16f, 0.98f);
        shapeRenderer.rect(animatedX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        // Header gradient
        for (int i = 0; i < 100; i++) {
            float alpha = 0.85f - (i / 100f) * 0.45f;
            shapeRenderer.setColor(headerColor.r, headerColor.g, headerColor.b, alpha);
            shapeRenderer.rect(animatedX, panelY + PANEL_HEIGHT - 100 + i, PANEL_WIDTH, 1);
        }

        // Accent line
        shapeRenderer.setColor(headerColor.r * 1.2f, headerColor.g * 1.2f, headerColor.b * 1.2f, 1f);
        shapeRenderer.rect(animatedX, panelY + PANEL_HEIGHT - 3, PANEL_WIDTH, 3);

        // Divider
        shapeRenderer.setColor(0.25f, 0.27f, 0.3f, 0.8f);
        shapeRenderer.rect(animatedX + PANEL_PADDING, panelY + PANEL_HEIGHT - 105,
            PANEL_WIDTH - PANEL_PADDING * 2, 2);

        // Close button
        shapeRenderer.setColor(0.2f, 0.22f, 0.25f, 0.9f);
        shapeRenderer.circle(animatedX + PANEL_WIDTH - 40, panelY + PANEL_HEIGHT - 40, 18);

        shapeRenderer.end();
    }

    private void drawCityHeader(City city, float animatedX, float textX, float textY) {
        titleFont.setColor(1f, 1f, 1f, 0.15f);
        titleFont.draw(batch, city.name, textX + 2, textY - 12);
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, city.name, textX, textY - 10);

        font.setColor(0.9f, 0.9f, 0.9f, 1f);
        font.draw(batch, "X", animatedX + PANEL_WIDTH - 47, textY - 10);
    }

    private void drawTemperatureDisplay(City city, float textX, float textY) {
        largeFont.setColor(0, 0, 0, 0.3f);
        String tempText = String.format("%.0f", city.temperature);
        largeFont.draw(batch, tempText, textX + 3, textY - 3);
        largeFont.setColor(Color.WHITE);
        largeFont.draw(batch, tempText, textX, textY);

        titleFont.setColor(new Color(0.8f, 0.8f, 0.8f, 1f));
        titleFont.draw(batch, "C", textX + 90, textY - 10);

        smallFont.setColor(new Color(0.85f, 0.87f, 0.9f, 1f));
        String capitalizedDesc = capitalizeFirst(city.description);
        smallFont.draw(batch, capitalizedDesc, textX, textY - 50);
    }

    private void drawAQIDisplay(City city, float textX, float textY) {
        largeFont.setColor(0, 0, 0, 0.3f);
        String aqiText = String.valueOf(city.aqi);
        largeFont.draw(batch, aqiText, textX + 3, textY - 3);
        largeFont.setColor(Color.WHITE);
        largeFont.draw(batch, aqiText, textX, textY);

        titleFont.setColor(new Color(0.8f, 0.8f, 0.8f, 1f));
        titleFont.draw(batch, "AQI", textX + 90, textY - 10);

        smallFont.setColor(new Color(0.85f, 0.87f, 0.9f, 1f));
        smallFont.draw(batch, getAQIDescription(city.aqi), textX, textY - 50);
    }

    private void drawWeatherIcons(float textX, float textY) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

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
    }

    private void drawAirQualityIcons(float textX, float textY) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float iconX = textX + 10;
        float iconY = textY - 5;

        drawPM25Icon(iconX, iconY);
        iconY -= 50;
        drawO3Icon(iconX, iconY);
        iconY -= 50;
        drawNO2Icon(iconX, iconY);
        iconY -= 50;
        drawLocationIcon(iconX, iconY);

        shapeRenderer.end();
    }

    private void drawWeatherInfo(City city, float textX, float textY) {
        drawInfoCard("Humidity", city.humidity + "%", textX, textY);
        textY -= 50;
        drawInfoCard("Wind Speed", String.format("%.1f m/s", city.windSpeed), textX, textY);
        textY -= 50;
        drawInfoCard("Pressure", city.pressure + " hPa", textX, textY);
        textY -= 50;
        drawInfoCard("Location", String.format("%.2f N, %.2f E", city.lat, city.lon), textX, textY);
    }

    private void drawAirQualityInfo(City city, float textX, float textY) {
        drawInfoCard("PM2.5", String.format("%.1f μg/m³", city.pm2_5), textX, textY);
        textY -= 50;
        drawInfoCard("Ozone (O₃)", String.format("%.1f μg/m³", city.o3), textX, textY);
        textY -= 50;
        drawInfoCard("NO₂", String.format("%.1f μg/m³", city.no2), textX, textY);
        textY -= 50;
        drawInfoCard("Location", String.format("%.2f N, %.2f E", city.lat, city.lon), textX, textY);
    }

    private void drawInfoCard(String label, String value, float x, float y) {
        font.setColor(new Color(0.6f, 0.62f, 0.65f, 1f));
        font.draw(batch, label, x + 30, y);

        titleFont.getData().setScale(1.4f);
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, value, x + 160, y + 2);
        titleFont.getData().setScale(2.2f);
    }

    private void drawFooterHint(float textX, float panelY) {
        float textY = panelY + PANEL_PADDING + 10;
        smallFont.setColor(new Color(0.5f, 0.52f, 0.55f, 1f));
        smallFont.draw(batch, "Press ESC or click X to close", textX, textY);
    }

    public void drawEditModeIndicator() {
        float barWidth = 200;
        float barHeight = 50;
        float barX = (Gdx.graphics.getWidth() - barWidth) / 2;
        float barY = Gdx.graphics.getHeight() - barHeight - 10;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 0.8f, 0.2f, 0.9f);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        shapeRenderer.setColor(1f, 0.9f, 0.4f, 1f);
        shapeRenderer.rect(barX, barY + barHeight - 3, barWidth, 3);
        shapeRenderer.end();

        batch.begin();
        font.setColor(0.1f, 0.1f, 0.1f, 1f);
        font.draw(batch, "EDIT MODE", barX + 50, barY + 32);
        extraSmallFont.setColor(0.2f, 0.2f, 0.2f, 1f);
        extraSmallFont.draw(batch, "A: Add  Del: Delete  Click: Edit", barX + 12, barY + 14);
        batch.end();
    }

    public void drawLocationSelectionIndicator(float markerPulse) {
        float barWidth = 350;
        float barHeight = 50;
        float barX = (Gdx.graphics.getWidth() - barWidth) / 2;
        float barY = Gdx.graphics.getHeight() - barHeight - 70;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float pulse = 0.8f + (float)Math.sin(markerPulse * 2) * 0.2f;
        shapeRenderer.setColor(0.2f * pulse, 1f * pulse, 0.4f * pulse, 0.9f);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        shapeRenderer.setColor(0.4f, 1f, 0.6f, 1f);
        shapeRenderer.rect(barX, barY + barHeight - 3, barWidth, 3);
        shapeRenderer.end();

        batch.begin();
        font.setColor(0.05f, 0.05f, 0.05f, 1f);
        font.draw(batch, "Click on map to select location", barX + 45, barY + 32);
        extraSmallFont.setColor(0.15f, 0.15f, 0.15f, 1f);
        extraSmallFont.draw(batch, "ESC to cancel", barX + 125, barY + 14);
        batch.end();
    }

    public void drawControlHints(boolean editMode) {
        batch.begin();
        smallFont.setColor(new Color(1f, 1f, 1f, 0.7f));
        String hints = editMode ?
            "Arrow Keys: Pan  |  +/-: Zoom  |  A: Click Map to Add  |  Click City: Edit  |  Del: Delete  |  E: Exit Edit" :
            "Arrow Keys: Pan  |  +/-: Zoom  |  Click City: Weather Info  |  E: Edit Mode";
        float textWidth = 700;
        smallFont.draw(batch, hints, (Gdx.graphics.getWidth() - textWidth) / 2, 30);
        batch.end();
    }

    // Icon drawing methods
    private void drawHumidityIcon(float x, float y) {
        shapeRenderer.setColor(0.4f, 0.65f, 0.95f, 1f);
        shapeRenderer.circle(x, y - 2, 6);
        shapeRenderer.triangle(x - 6, y - 2, x + 6, y - 2, x, y + 8);
    }

    private void drawWindIcon(float x, float y) {
        shapeRenderer.setColor(0.6f, 0.75f, 0.9f, 1f);
        shapeRenderer.rectLine(x - 8, y + 4, x + 8, y + 4, 2);
        shapeRenderer.rectLine(x - 6, y, x + 6, y, 2);
        shapeRenderer.rectLine(x - 4, y - 4, x + 4, y - 4, 2);
    }

    private void drawPressureIcon(float x, float y) {
        shapeRenderer.setColor(0.75f, 0.6f, 0.85f, 1f);
        shapeRenderer.circle(x, y, 7);
        shapeRenderer.setColor(0.11f, 0.13f, 0.16f, 1f);
        shapeRenderer.circle(x, y, 5);
        shapeRenderer.setColor(0.75f, 0.6f, 0.85f, 1f);
        shapeRenderer.rectLine(x, y, x + 4, y + 4, 2);
    }

    private void drawLocationIcon(float x, float y) {
        shapeRenderer.setColor(0.95f, 0.4f, 0.4f, 1f);
        shapeRenderer.circle(x, y + 4, 6);
        shapeRenderer.setColor(0.11f, 0.13f, 0.16f, 1f);
        shapeRenderer.circle(x, y + 4, 3);
        shapeRenderer.setColor(0.95f, 0.4f, 0.4f, 1f);
        shapeRenderer.triangle(x - 3, y + 1, x + 3, y + 1, x, y - 6);
    }

    private void drawPM25Icon(float x, float y) {
        shapeRenderer.setColor(0.95f, 0.5f, 0.3f, 1f);
        shapeRenderer.circle(x - 3, y + 3, 2.5f);
        shapeRenderer.circle(x + 3, y + 1, 2f);
        shapeRenderer.circle(x, y - 3, 2.5f);
        shapeRenderer.circle(x + 4, y - 2, 1.5f);
    }

    private void drawO3Icon(float x, float y) {
        shapeRenderer.setColor(0.5f, 0.7f, 0.95f, 1f);
        shapeRenderer.circle(x, y + 4, 3f);
        shapeRenderer.circle(x - 4, y - 2, 3f);
        shapeRenderer.circle(x + 4, y - 2, 3f);
    }

    private void drawNO2Icon(float x, float y) {
        shapeRenderer.setColor(0.9f, 0.6f, 0.3f, 1f);
        shapeRenderer.circle(x - 3, y, 3.5f);
        shapeRenderer.circle(x + 3, y + 2, 2.5f);
        shapeRenderer.circle(x + 3, y - 2, 2.5f);
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

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private float easeOutCubic(float t) {
        return 1 - (float)Math.pow(1 - t, 3);
    }
}
