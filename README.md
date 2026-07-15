# CountryGraphs

An interactive map that models the world's countries as a graph connected by shared borders, and uses that graph to simulate how rising sea levels could submerge countries and displace their populations.

Countries are represented as nodes, borders as edges. On top of that graph structure, the project layers real elevation and population data so you can drag a slider and watch which countries would go underwater at a given sea level — and how many people that would affect.

## Features

- **Interactive world map** (Leaflet-based) with every country clickable
- **Border graph** left-click a country for details, right-click for its list of bordering neighbors
- **Shortest path finder** pick two countries and find the shortest border-crossing path between them
- **Sea level slider** (0–500m) that:
  - Colors countries blue as they go "underwater" at a given elevation
  - Shows a live count of submerged countries
  - Totals the displaced population
  - Lists affected countries with their populations
- **Live data enrichment** capital elevation and population figures are fetched from external APIs on first run and cached locally for fast subsequent startups

## Tech Stack

- **Backend:** Java (standard library only — no external dependencies/build tools required)
- **Frontend:** HTML/CSS/JS with [Leaflet.js](https://leafletjs.com/) for the map
- **Data sources:**
  - [Open-Elevation API](https://api.open-elevation.com/) — capital elevation (no key required)
  - [API Ninjas Population API](https://api-ninjas.com/) — country population & capital data (free key required)
- Country border relationships are stored locally in `CountryBorders.CSV` / `CountryBordersSubmerged.CSV`

## Getting Started

### Prerequisites
- JDK installed (`javac` / `java` available on your PATH)
- A free [API Ninjas](https://api-ninjas.com/) account for a Population API key

### 1. Clone the repo
```bash
git clone https://github.com/CoastMushrooms/CountryGraphs.git
cd CountryGraphs/CGC
```

### 2. Add your API key
Open `PopulationAPI.java` and replace the placeholder with your own key:
```java
private static final String API_KEY = "YOUR_API_KEY_HERE";
```

### 3. Compile
```bash
javac *.java
```

### 4. Run
```bash
java StudentCode
```

Your browser should open automatically at `http://localhost:8000`. On first launch the app fetches elevation and population data for every country, which can take 30–60 seconds, subsequent runs load from a local cache (`country-data-cache.properties`) and start instantly.

## Project Structure

| File | Purpose |
|---|---|
| `StudentCode.java` | Application entry point; wires up the graph, data loading, and sea-level logic |
| `Server.java` | Minimal HTTP server handling all API routes and static file serving |
| `CountryGraph.java` | Graph structure connecting countries by shared borders |
| `Country.java` | Country model (name, capital, population, elevation, coordinates) |
| `ElevationAPI.java` | Client for the Open-Elevation API |
| `PopulationAPI.java` | Client for the API Ninjas Population API |
| `CountryBorders.CSV` | Source data for country-to-country border adjacency |
| `CountryBordersSubmerged.CSV` | Adjusted border data for the submerged-country scenario |
| `country-data-cache.properties` | Cached population/elevation/coordinate lookups |
| `index.html`, `leaflet.js/css` | Frontend map UI |

## Troubleshooting

- **"No API Key" errors** — make sure your API Ninjas key is set in `PopulationAPI.java`.
- **Slow first launch** — expected; the app is populating data for every country. Later runs use the cache.
- **Elevation/population data not loading** — check your internet connection and confirm your API key is valid.
