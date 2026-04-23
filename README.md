# Interactive Weather Map of Slovenia
![Java](https://img.shields.io/badge/Java-11%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![LibGDX](https://img.shields.io/badge/LibGDX-1.12-E74C3C?style=for-the-badge&logo=libgdx&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-Build-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![OpenWeatherMap](https://img.shields.io/badge/OpenWeatherMap-API-EB6E4B?style=for-the-badge&logo=openweathermap&logoColor=white)
![Geoapify](https://img.shields.io/badge/Geoapify-Maps-0078D4?style=for-the-badge&logo=googlemaps&logoColor=white)

A desktop application for visualizing weather data and air quality for cities in Slovenia, built with the **LibGDX** framework and **Java**.

---

## Project Description

The application displays an interactive map of Slovenia with real-time weather data and air quality information for Slovenian cities. Data is fetched from the OpenWeatherMap API and presented with animated icons, particles, and information panels.

---

## Screenshots

**Map overview with weather icons**
![Map overview](screenshots/screenshot_overview.png)

**Weather panel for a selected city**
![Weather panel – Ljubljana](screenshots/screenshot_weather_panel.png)

---

## Features

- **Interactive map generated with Geoapify** – pan, zoom in and out with mouse or keyboard
- **Real-time weather data** – temperature, humidity, pressure, wind speed, and weather description
- **Air quality data** – AQI index, PM2.5, ozone (O₃), and nitrogen dioxide (NO₂)
- **Animated particles** – visual representation of rain, snow, clouds, wind, and air pollution
- **Information panel** – animated side panel with data for the selected city
- **Edit mode** – add, edit, and delete cities
- **Cache** – stores the map and weather icons locally for faster loading
- **Static data** – option to manually enter data for individual cities

---

## Technologies

| Component | Technology |
|---|---|
| Framework | LibGDX |
| Language | Java |
| Weather API | OpenWeatherMap |
| Map | Geoapify Static Maps API |
| Serialization | LibGDX Json |
| UI | LibGDX Scene2D |

---

## Installation & Setup

### API Key Configuration

Create a `local.properties` file in the project root:

```properties
OPENWEATHER_API_KEY=your_api_key
GEOAPIFY_API_KEY=your_api_key
```

### Run

```bash
./gradlew lwjgl3:run
```

---
