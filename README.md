#  Interaktivni Vremenski Zemljevid Slovenije

Namizna aplikacija za vizualizacijo vremenskih podatkov in kakovosti zraka za mesta v Sloveniji, razvita z uporabo ogrodja **LibGDX** in **Java**.

---

## Opis projekta

Aplikacija prikazuje interaktivni zemljevid Slovenije z realnimi vremenskimi podatki in podatki o kakovosti zraka za slovenska mesta. Podatki se pridobivajo iz OpenWeatherMap API-ja in se prikazujejo z animiranimi ikonami, delci in informacijskimi paneli.

---

## ✨ Funkcionalnosti

- **Interaktivni zemljevid** – premikanje, povečevanje in oddaljitev z miško ali tipkovnico
- **Vremenski podatki v realnem času** – temperatura, vlažnost, tlak, hitrost vetra in opis vremena
- **Podatki o kakovosti zraka** – AQI indeks, PM2.5, ozon (O₃) in dušikov dioksid (NO₂)
- **Animirani delci** – vizualni prikaz dežja, snega, oblakov, vetra in onesnaženosti zraka
- **Informacijski panel** – animiran stranski panel z izbranimi podatki mesta
- **Način urejanja** – dodajanje, urejanje in brisanje mest
- **Predpomnilnik** – shranjevanje zemljevida in vremenskih ikon lokalno za hitrejše nalaganje
- **Statični podatki** – možnost ročnega vnosa podatkov za posamezna mesta

---

## 🛠️ Tehnologije

| Komponenta | Tehnologija |
|---|---|
| Ogrodje | LibGDX |
| Jezik | Java |
| Vreme API | OpenWeatherMap |
| Zemljevid | Geoapify Static Maps API |
| Serializacija | LibGDX Json |
| UI | LibGDX Scene2D |

---

## 📁 Struktura projekta

```
si.um.feri.bozinov/
├── SloveniaMap.java          # Glavna aplikacija (vstopna točka)
├── City.java                 # Model podatkov za mesto
├── MapRenderer.java          # Risanje ikon, markerjev in oznak mest
├── UIRenderer.java           # Risanje UI panelov in elementov
├── ParticleEffectsManager.java  # Animirani delci (dež, sneg, onesnaženost...)
├── WeatherDataManager.java   # Pridobivanje vremenskih podatkov iz API-ja
└── AirQualityDataManager.java   # Pridobivanje podatkov o kakovosti zraka
```

---

## ⚙️ Namestitev in zagon

### Predpogoji

- Java 11 ali novejši
- Gradle
- Veljavni API ključi

### Konfiguracija API ključev

Ustvari datoteko `local.properties` v korenu projekta:

```properties
OPENWEATHER_API_KEY=tvoj_api_kljuc_tukaj
```

> API ključ za OpenWeatherMap dobiš brezplačno na [openweathermap.org](https://openweathermap.org/api).  
> Geoapify API ključ za prenos zemljevida je vgrajen v kodi (`SloveniaMap.java`).

### Zagon

```bash
./gradlew lwjgl3:run
```

---

## 🎮 Upravljanje

### Tipkovnica

| Tipka | Akcija |
|---|---|
| `Puščice` | Premikanje po zemljevidu |
| `+` / `-` | Povečanje / pomanjšanje |
| `E` | Vklop/izklop načina urejanja |
| `P` | Vklop/izklop delcev |
| `A` | (Način urejanja) Dodaj novo mesto |
| `Delete` | (Način urejanja) Izbriši izbrano mesto |
| `ESC` | Zapri panel / prekliči akcijo |
| `Ctrl + C` | Počisti predpomnilnik |

### Miška

| Akcija | Opis |
|---|---|
| Klik na mesto | Odpre informacijski panel |
| Scroll | Povečanje / pomanjšanje |
| Klik na X v panelu | Zapre panel |

---

## 🗄️ Predpomnilnik

Aplikacija avtomatsko shranjuje:

- **Zemljevid** – `cache/slovenia_map.png`
- **Vremenske ikone** – `cache/icons/<koda_ikone>.png`
- **Seznam mest** – `cities.json`

Za brisanje predpomnilnika pritisni `Ctrl + C` v aplikaciji ali ročno izbriši mapo `cache/`.

---

## 🌤️ Načina prikaza

### Vremenski način (privzeto)
Prikazuje vremensko ikono OpenWeatherMap za vsako mesto. Informacijski panel vsebuje temperaturo, vlažnost, tlak, hitrost vetra in opis.

### Način kakovosti zraka
Preklopi z gumbom **"Air Quality Mode"** v zgornjem levem kotu. Ikone dobijo barvo glede na AQI indeks:

| AQI | Opis | Barva |
|---|---|---|
| 1 | Dobra | 🟢 Zelena |
| 2 | Zadovoljiva | 🟡 Rumena |
| 3 | Zmerna | 🟠 Oranžna |
| 4 | Slaba | 🔴 Rdeča |
| 5 | Zelo slaba | 🟣 Vijolična |

---

## ✏️ Način urejanja

Pritisni `E` za vklop načina urejanja:

- **Klik na mesto** – odpre pogovorno okno za urejanje (ime, koordinate, podatki, ikona)
- **`A` + klik na zemljevid** – doda novo mesto na izbrani lokaciji
- **`Delete`** – odpre potrditev za brisanje izbranega mesta
- **Možnost statičnih podatkov** – odkljukaj "Use Static Data" za ročni vnos vrednosti

Vsa spremembe se samodejno shranijo v `cities.json`.

---

## 🏙️ Privzeta mesta

Ob prvem zagonu se naložijo naslednja mesta:

Ljubljana, Maribor, Celje, Kranj, Koper, Novo Mesto, Velenje, Nova Gorica, Ptuj, Murska Sobota, Slovenj Gradec, Kamnik, Jesenice, Izola, Krško

---

## 🔧 Znane omejitve

- Aplikacija zahteva internetno povezavo za prvi prenos podatkov in zemljevida
- OpenWeatherMap brezplačni načrt ima omejitev zahtevkov (60/minuto)
- Koordinate mest morajo biti znotraj meja Slovenije (lat: 45.4–46.9, lon: 13.3–16.6)

---

## 👤 Avtor

**Bozinov** – Fakulteta za elektrotehniko, računalništvo in informatiko, Univerza v Mariboru
