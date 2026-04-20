#  Interaktivni Vremenski Zemljevid Slovenije

![Java](https://img.shields.io/badge/Java-11%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![LibGDX](https://img.shields.io/badge/LibGDX-1.12-E74C3C?style=for-the-badge&logo=libgdx&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-Build-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![OpenWeatherMap](https://img.shields.io/badge/OpenWeatherMap-API-EB6E4B?style=for-the-badge&logo=openweathermap&logoColor=white)
![Geoapify](https://img.shields.io/badge/Geoapify-Maps-0078D4?style=for-the-badge&logo=googlemaps&logoColor=white)

Namizna aplikacija za vizualizacijo vremenskih podatkov in kakovosti zraka za mesta v Sloveniji, razvita z uporabo ogrodja **LibGDX** in **Java**.

---

## Opis projekta

Aplikacija prikazuje interaktivni zemljevid Slovenije z realnimi vremenskimi podatki in podatki o kakovosti zraka za slovenska mesta. Podatki se pridobivajo iz OpenWeatherMap API-ja in se prikazujejo z animiranimi ikonami, delci in informacijskimi paneli.

---

## Posnetki zaslona
**Pregled zemljevida z vremenskimi ikonami**
![Pregled zemljevida](screenshots/screenshot_overview.png)

**Vremenski panel za izbrano mesto**
![Vremenski panel – Ljubljana](screenshots/screenshot_weather_panel.png)

---

##  Funkcionalnosti

- **Interaktivni zemljevid zgeneriran s Geoapify** –  premikanje, povečevanje in oddaljitev z miško ali tipkovnico
- **Vremenski podatki v realnem času** – temperatura, vlažnost, tlak, hitrost vetra in opis vremena
- **Podatki o kakovosti zraka** – AQI indeks, PM2.5, ozon (O₃) in dušikov dioksid (NO₂)
- **Animirani delci** – vizualni prikaz dežja, snega, oblakov, vetra in onesnaženosti zraka
- **Informacijski panel** – animiran stranski panel z izbranimi podatki mesta
- **Način urejanja** – dodajanje, urejanje in brisanje mest
- **Predpomnilnik** – shranjevanje zemljevida in vremenskih ikon lokalno za hitrejše nalaganje
- **Statični podatki** – možnost ročnega vnosa podatkov za posamezna mesta

---

##  Tehnologije

| Komponenta | Tehnologija |
|---|---|
| Ogrodje | LibGDX |
| Jezik | Java |
| Vreme API | OpenWeatherMap |
| Zemljevid | Geoapify Static Maps API |
| Serializacija | LibGDX Json |
| UI | LibGDX Scene2D |

---



## ️ Namestitev in zagon

### Konfiguracija API ključev

Ustvari datoteko `local.properties` v korenu projekta:

```properties
OPENWEATHER_API_KEY=tvoj_api_kljuc
GEOAPIFY_API_KEY=tvoj_api_kljuc
```

### Zagon

```bash
./gradlew lwjgl3:run
```

---

