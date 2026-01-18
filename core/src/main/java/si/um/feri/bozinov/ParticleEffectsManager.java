package si.um.feri.bozinov;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParticleEffectsManager {
    private List<Particle> particles;
    private MapRenderer mapRenderer;
    private boolean enabled = true;

    // Particle generation rates
    private static final int MAX_PARTICLES_PER_CITY = 50;
    private static final float PARTICLE_SPAWN_RATE = 0.1f; // seconds between spawns

    private float spawnTimer = 0f;

    public ParticleEffectsManager(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
        this.particles = new ArrayList<>();
    }

    public void update(float delta, List<City> cities, boolean airQualityMode) {
        if (!enabled) return;

        spawnTimer += delta;

        // Spawn new particles
        if (spawnTimer >= PARTICLE_SPAWN_RATE) {
            spawnTimer = 0f;
            for (City city : cities) {
                int cityParticleCount = countParticlesForCity(city);
                if (cityParticleCount < MAX_PARTICLES_PER_CITY) {
                    spawnParticlesForCity(city, airQualityMode);
                }
            }
        }

        // Update existing particles
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update(delta);
            if (particle.isDead()) {
                iterator.remove();
            }
        }
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (!enabled) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Particle particle : particles) {
            particle.render(shapeRenderer);
        }
        shapeRenderer.end();
    }

    private void spawnParticlesForCity(City city, boolean airQualityMode) {
        Vector2 cityPos = mapRenderer.geoToScreen(city.lat, city.lon);

        if (airQualityMode && city.airQualityLoaded) {
            spawnAirQualityParticles(city, cityPos);
        } else if (!airQualityMode && city.weatherLoaded) {
            spawnWeatherParticles(city, cityPos);
        }
    }

    private void spawnWeatherParticles(City city, Vector2 cityPos) {
        String description = city.description.toLowerCase();
        double temp = city.temperature;

        // Rain particles
        if (description.contains("rain") || description.contains("drizzle")) {
            int count = description.contains("heavy") ? 3 : 1;
            for (int i = 0; i < count; i++) {
                particles.add(createRainParticle(cityPos, city.windSpeed));
            }
        }
        // Snow particles
        else if (description.contains("snow") || (temp < 0 && description.contains("cloud"))) {
            int count = temp < -5 ? 2 : 1;
            for (int i = 0; i < count; i++) {
                particles.add(createSnowParticle(cityPos, city.windSpeed));
            }
        }
        // Fog/mist particles
        else if (description.contains("fog") || description.contains("mist") || city.humidity > 90) {
            particles.add(createFogParticle(cityPos));
        }
        // Cloud particles for cloudy weather
        else if (description.contains("cloud")) {
            if (MathUtils.random() < 0.3f) { // 30% chance
                particles.add(createCloudParticle(cityPos, city.windSpeed));
            }
        }
        // Heat shimmer for hot weather
        else if (temp > 28) {
            if (MathUtils.random() < 0.4f) {
                particles.add(createHeatShimmerParticle(cityPos));
            }
        }
        // Wind particles for windy conditions
        if (city.windSpeed > 8) {
            if (MathUtils.random() < 0.5f) {
                particles.add(createWindParticle(cityPos, city.windSpeed));
            }
        }
    }

    private void spawnAirQualityParticles(City city, Vector2 cityPos) {
        int aqi = city.aqi;

        // Spawn pollution particles based on AQI level
        if (aqi >= 3) { // Moderate or worse
            int count = (aqi - 2); // 1 for moderate, 2 for poor, 3 for very poor
            for (int i = 0; i < count; i++) {
                particles.add(createPollutionParticle(cityPos, aqi, city.pm2_5));
            }
        }

        // Spawn smog for high PM2.5
        if (city.pm2_5 > 15) {
            if (MathUtils.random() < 0.3f) {
                particles.add(createSmogParticle(cityPos, city.pm2_5));
            }
        }

        // Good air quality - clean sparkles
        if (aqi == 1) {
            if (MathUtils.random() < 0.2f) {
                particles.add(createCleanAirParticle(cityPos));
            }
        }
    }

    // ===== WEATHER PARTICLE CREATORS =====

    private Particle createRainParticle(Vector2 cityPos, double windSpeed) {
        float offsetX = MathUtils.random(-40f, 40f);
        float offsetY = MathUtils.random(30f, 60f);
        float x = cityPos.x + offsetX;
        float y = cityPos.y + offsetY;

        float windEffect = (float) windSpeed * 1.5f;
        float velocityX = MathUtils.random(-1f, 1f) + windEffect;
        float velocityY = -MathUtils.random(150f, 200f);

        return new RainParticle(x, y, velocityX, velocityY);
    }

    private Particle createSnowParticle(Vector2 cityPos, double windSpeed) {
        float offsetX = MathUtils.random(-50f, 50f);
        float offsetY = MathUtils.random(30f, 70f);
        float x = cityPos.x + offsetX;
        float y = cityPos.y + offsetY;

        float windEffect = (float) windSpeed * 0.8f;
        float velocityX = MathUtils.random(-10f, 10f) + windEffect;
        float velocityY = -MathUtils.random(20f, 40f);

        return new SnowParticle(x, y, velocityX, velocityY);
    }

    private Particle createFogParticle(Vector2 cityPos) {
        float offsetX = MathUtils.random(-60f, 60f);
        float offsetY = MathUtils.random(-30f, 30f);
        float x = cityPos.x + offsetX;
        float y = cityPos.y + offsetY;

        float velocityX = MathUtils.random(-5f, 5f);

        return new FogParticle(x, y, velocityX);
    }

    private Particle createCloudParticle(Vector2 cityPos, double windSpeed) {
        float offsetX = MathUtils.random(-70f, 70f);
        float offsetY = MathUtils.random(20f, 50f);
        float x = cityPos.x + offsetX;
        float y = cityPos.y + offsetY;

        float velocityX = (float) windSpeed * 2f + MathUtils.random(-2f, 2f);

        return new CloudParticle(x, y, velocityX);
    }

    private Particle createHeatShimmerParticle(Vector2 cityPos) {
        float offsetX = MathUtils.random(-40f, 40f);
        float offsetY = MathUtils.random(-20f, 20f);
        float x = cityPos.x + offsetX;
        float y = cityPos.y + offsetY;

        return new HeatShimmerParticle(x, y);
    }

    private Particle createWindParticle(Vector2 cityPos, double windSpeed) {
        float offsetX = MathUtils.random(-80f, -40f);
        float offsetY = MathUtils.random(-20f, 20f);
        float x = cityPos.x + offsetX;
        float y = cityPos.y + offsetY;

        float velocity = (float) windSpeed * 3f;

        return new WindParticle(x, y, velocity);
    }

    // ===== AIR QUALITY PARTICLE CREATORS =====

    private Particle createPollutionParticle(Vector2 cityPos, int aqi, double pm25) {
        float offsetX = MathUtils.random(-50f, 50f);
        float offsetY = MathUtils.random(-30f, 30f);
        float x = cityPos.x + offsetX;
        float y = cityPos.y + offsetY;

        float velocityY = MathUtils.random(5f, 15f);

        return new PollutionParticle(x, y, velocityY, aqi);
    }

    private Particle createSmogParticle(Vector2 cityPos, double pm25) {
        float offsetX = MathUtils.random(-70f, 70f);
        float offsetY = MathUtils.random(-40f, 40f);
        float x = cityPos.x + offsetX;
        float y = cityPos.y + offsetY;

        return new SmogParticle(x, y, (float) pm25);
    }

    private Particle createCleanAirParticle(Vector2 cityPos) {
        float offsetX = MathUtils.random(-45f, 45f);
        float offsetY = MathUtils.random(-45f, 45f);
        float x = cityPos.x + offsetX;
        float y = cityPos.y + offsetY;

        return new CleanAirParticle(x, y);
    }

    private int countParticlesForCity(City city) {
        // Count particles near this city (within 100 pixels)
        Vector2 cityPos = mapRenderer.geoToScreen(city.lat, city.lon);
        int count = 0;
        for (Particle particle : particles) {
            if (Vector2.dst(particle.x, particle.y, cityPos.x, cityPos.y) < 100) {
                count++;
            }
        }
        return count;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void clear() {
        particles.clear();
    }

    // ===== BASE PARTICLE CLASS =====

    private abstract static class Particle {
        float x, y;
        float velocityX, velocityY;
        float lifetime;
        float maxLifetime;
        Color color;
        float size;
        float alpha;

        public Particle(float x, float y, float maxLifetime) {
            this.x = x;
            this.y = y;
            this.lifetime = 0f;
            this.maxLifetime = maxLifetime;
            this.alpha = 1f;
        }

        public void update(float delta) {
            lifetime += delta;
            x += velocityX * delta;
            y += velocityY * delta;
            updateCustom(delta);
        }

        protected abstract void updateCustom(float delta);
        public abstract void render(ShapeRenderer shapeRenderer);

        public boolean isDead() {
            return lifetime >= maxLifetime;
        }

        protected float getLifetimeProgress() {
            return lifetime / maxLifetime;
        }
    }

    // ===== WEATHER PARTICLE IMPLEMENTATIONS =====

    private static class RainParticle extends Particle {
        public RainParticle(float x, float y, float velocityX, float velocityY) {
            super(x, y, 0.8f);
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.color = new Color(0.6f, 0.7f, 0.9f, 0.7f);
            this.size = MathUtils.random(1f, 2f);
        }

        @Override
        protected void updateCustom(float delta) {
            alpha = 1f - getLifetimeProgress();
        }

        @Override
        public void render(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(color.r, color.g, color.b, alpha * 0.7f);
            // Draw rain streak
            shapeRenderer.rectLine(x, y, x - velocityX * 0.02f, y - 8f, size);
        }
    }

    private static class SnowParticle extends Particle {
        private float wobble = 0f;

        public SnowParticle(float x, float y, float velocityX, float velocityY) {
            super(x, y, 3f);
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.color = new Color(1f, 1f, 1f, 0.9f);
            this.size = MathUtils.random(2f, 4f);
        }

        @Override
        protected void updateCustom(float delta) {
            wobble += delta * 3f;
            velocityX += MathUtils.sin(wobble) * 5f * delta;
            alpha = 1f - getLifetimeProgress();
        }

        @Override
        public void render(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(color.r, color.g, color.b, alpha);
            shapeRenderer.circle(x, y, size);
        }
    }

    private static class FogParticle extends Particle {
        private float pulsePhase;

        public FogParticle(float x, float y, float velocityX) {
            super(x, y, 4f);
            this.velocityX = velocityX;
            this.velocityY = MathUtils.random(-2f, 2f);
            this.color = new Color(0.8f, 0.85f, 0.9f, 0.15f);
            this.size = MathUtils.random(15f, 30f);
            this.pulsePhase = MathUtils.random(0f, MathUtils.PI2);
        }

        @Override
        protected void updateCustom(float delta) {
            pulsePhase += delta;
            float pulse = (MathUtils.sin(pulsePhase) + 1f) * 0.5f;
            alpha = 0.15f * (1f - getLifetimeProgress()) * (0.7f + pulse * 0.3f);
            size += delta * 2f;
        }

        @Override
        public void render(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(color.r, color.g, color.b, alpha);
            shapeRenderer.circle(x, y, size);
        }
    }

    private static class CloudParticle extends Particle {
        public CloudParticle(float x, float y, float velocityX) {
            super(x, y, 5f);
            this.velocityX = velocityX;
            this.velocityY = MathUtils.random(-1f, 1f);
            this.color = new Color(0.85f, 0.88f, 0.92f, 0.3f);
            this.size = MathUtils.random(10f, 20f);
        }

        @Override
        protected void updateCustom(float delta) {
            alpha = 0.3f * (1f - getLifetimeProgress());
        }

        @Override
        public void render(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(color.r, color.g, color.b, alpha);
            shapeRenderer.circle(x, y, size);
            shapeRenderer.circle(x + size * 0.6f, y + size * 0.2f, size * 0.7f);
            shapeRenderer.circle(x - size * 0.6f, y - size * 0.2f, size * 0.7f);
        }
    }

    private static class HeatShimmerParticle extends Particle {
        private float wavePhase;

        public HeatShimmerParticle(float x, float y) {
            super(x, y, 2f);
            this.velocityY = MathUtils.random(30f, 50f);
            this.color = new Color(1f, 0.9f, 0.7f, 0.2f);
            this.size = MathUtils.random(3f, 6f);
            this.wavePhase = MathUtils.random(0f, MathUtils.PI2);
        }

        @Override
        protected void updateCustom(float delta) {
            wavePhase += delta * 4f;
            velocityX = MathUtils.sin(wavePhase) * 15f;
            alpha = 0.2f * (1f - getLifetimeProgress());
        }

        @Override
        public void render(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(color.r, color.g, color.b, alpha);
            shapeRenderer.circle(x, y, size);
        }
    }

    private static class WindParticle extends Particle {
        public WindParticle(float x, float y, float velocity) {
            super(x, y, 1.5f);
            this.velocityX = velocity;
            this.velocityY = MathUtils.random(-5f, 5f);
            this.color = new Color(0.9f, 0.95f, 1f, 0.4f);
            this.size = MathUtils.random(2f, 4f);
        }

        @Override
        protected void updateCustom(float delta) {
            alpha = 0.4f * (1f - getLifetimeProgress());
        }

        @Override
        public void render(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(color.r, color.g, color.b, alpha);
            shapeRenderer.rectLine(x, y, x - velocityX * 0.1f, y, size);
        }
    }

    // ===== AIR QUALITY PARTICLE IMPLEMENTATIONS =====

    private static class PollutionParticle extends Particle {
        private float driftPhase;

        public PollutionParticle(float x, float y, float velocityY, int aqi) {
            super(x, y, 6f);
            this.velocityY = velocityY;
            this.size = MathUtils.random(3f, 7f);
            this.driftPhase = MathUtils.random(0f, MathUtils.PI2);

            // Color based on AQI
            switch (aqi) {
                case 3: this.color = new Color(1f, 0.7f, 0.2f, 0.3f); break; // Orange
                case 4: this.color = new Color(1f, 0.4f, 0.3f, 0.4f); break; // Red
                case 5: this.color = new Color(0.8f, 0.2f, 0.5f, 0.5f); break; // Purple
                default: this.color = new Color(0.7f, 0.7f, 0.6f, 0.3f); // Gray
            }
        }

        @Override
        protected void updateCustom(float delta) {
            driftPhase += delta * 2f;
            velocityX = MathUtils.sin(driftPhase) * 10f;
            alpha = color.a * (1f - getLifetimeProgress());
        }

        @Override
        public void render(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(color.r, color.g, color.b, alpha);
            shapeRenderer.circle(x, y, size);
        }
    }

    private static class SmogParticle extends Particle {
        public SmogParticle(float x, float y, float pm25) {
            super(x, y, 8f);
            this.velocityX = MathUtils.random(-3f, 3f);
            this.velocityY = MathUtils.random(-2f, 2f);
            this.size = MathUtils.random(20f, 40f);

            float intensity = MathUtils.clamp(pm25 / 50f, 0.3f, 0.8f);
            this.color = new Color(0.6f, 0.5f, 0.4f, 0.15f * intensity);
        }

        @Override
        protected void updateCustom(float delta) {
            alpha = color.a * (1f - getLifetimeProgress());
            size += delta * 3f;
        }

        @Override
        public void render(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(color.r, color.g, color.b, alpha);
            shapeRenderer.circle(x, y, size);
        }
    }

    private static class CleanAirParticle extends Particle {
        private float twinklePhase;

        public CleanAirParticle(float x, float y) {
            super(x, y, 2f);
            this.velocityY = MathUtils.random(10f, 20f);
            this.color = new Color(0.7f, 1f, 0.9f, 0.6f);
            this.size = MathUtils.random(2f, 4f);
            this.twinklePhase = MathUtils.random(0f, MathUtils.PI2);
        }

        @Override
        protected void updateCustom(float delta) {
            twinklePhase += delta * 8f;
            float twinkle = (MathUtils.sin(twinklePhase) + 1f) * 0.5f;
            alpha = 0.6f * twinkle * (1f - getLifetimeProgress());
        }

        @Override
        public void render(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(color.r, color.g, color.b, alpha);
            shapeRenderer.circle(x, y, size);
            // Add cross sparkle
            shapeRenderer.rectLine(x - size, y, x + size, y, 1f);
            shapeRenderer.rectLine(x, y - size, x, y + size, 1f);
        }
    }
}
