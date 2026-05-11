# Sea Level Rise Visualization - Setup Instructions

## Project Overview
This project visualizes how rising sea levels affect different countries by:
1. Fetching elevation data for country capitals using the Open-Elevation API
2. Displaying population affected by rising sea levels
3. Using an interactive slider to simulate sea level rise

## Required Setup

### 1. Population API Key (REQUIRED)
You need to sign up for the API Ninjas Population API:

1. Go to https://api-ninjas.com/ and create a free account
2. Get your API key from your dashboard
3. Open `PopulationAPI.java` and replace `YOUR_API_KEY_HERE` with your actual API key:
```java
private static final String API_KEY = "YOUR_API_KEY_HERE"; // Replace with your actual API key
```

### 2. Open-Elevation API
This project uses the free public Open-Elevation API which doesn't require authentication:
- API Endpoint: https://api.open-elevation.com/api/v1/lookup
- No API key needed!

## How to Run

1. Ensure all `.java` files are in the same directory:
   - `StudentCode.java`
   - `Server.java`
   - `CountryGraph.java`
   - `Country.java`
   - `ElevationAPI.java` (new)
   - `PopulationAPI.java` (new)

2. **No external libraries needed** - everything uses only Java standard library!

3. Compile all Java files:
```bash
javac *.java
```

4. Run the program:
```bash
java StudentCode
```

5. The browser should open automatically to `http://localhost:8000`

## Features

### Sea Level Slider
- Located in the bottom-left control panel
- Range: 0 to 500 meters
- Moving the slider will:
  - Show which countries are underwater (in blue, matching water color)
  - Display statistics about displaced population
  - Show the list of affected countries with their populations

### Statistics Display
- Shows current sea level in meters
- Count of underwater countries
- Total displaced population (formatted as millions/thousands)
- List of affected countries with population details

### Country Information
- Right-click on countries for neighbor information
- Left-click for country details
- Use the country selectors to find the shortest border-crossing path

## API Details

### Open-Elevation API
- Free public API
- Returns elevation (in meters) for given coordinates
- POST endpoint: https://api.open-elevation.com/api/v1/lookup

### Population API (Ninjas)
- Requires free registration at https://api-ninjas.com/
- Returns current and historical population data
- GET endpoint: https://api.api-ninjas.com/v1/population

## Troubleshooting

### "No API Key" errors
Make sure you've added your API Ninjas key to `PopulationAPI.java`

### Slow initial startup
The program loads elevation and population data for all countries on startup. This may take 30-60 seconds on first run.

### Missing elevation data
If elevation data isn't loading, check:
1. Internet connection (needs to reach open-elevation.com)
2. API Ninjas key is set correctly
3. Check Java console for error messages

## Code Structure

- `StudentCode.java`: Main application logic, handles sea level changes
- `Server.java`: HTTP server framework
- `CountryGraph.java`: Country border graph data structure
- `Country.java`: Country object with elevation and population data
- `ElevationAPI.java`: Wrapper for Open-Elevation API
- `PopulationAPI.java`: Wrapper for Population API

## Future Enhancements
- Load elevation data asynchronously to avoid startup delay
- Add more detailed elevation data (min/max for entire country)
- Display affected cities and coastal regions
- Add historical/projected sea level data
- Include economic impact calculations
