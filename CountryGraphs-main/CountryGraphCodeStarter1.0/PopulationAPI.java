import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("deprecation")
public class PopulationAPI {
    private static final String API_URL = "https://restcountries.com/v3.1/name/";
    private static final String API_ALL_URL = "https://restcountries.com/v3.1/all?fields=name,population,capital,capitalInfo,latlng";
    private static final Map<String, CountryData> CACHE = new HashMap<>();

    public static class CountryData {
        private final long population;
        private final double[] coordinates;
        private final String capital;

        public CountryData(long population, double[] coordinates, String capital) {
            this.population = population;
            this.coordinates = coordinates;
            this.capital = capital;
        }

        public long getPopulation() {
            return population;
        }

        public double[] getCoordinates() {
            return coordinates;
        }

        public String getCapital() {
            return capital;
        }
    }

    /**
     * Fetches country data from REST Countries. This is more reliable than the
     * old population-only API because it also returns country coordinates.
     */
    public static CountryData getCountryData(String countryName) {
        if (CACHE.containsKey(countryName)) {
            return CACHE.get(countryName);
        }

        String[] namesToTry = getNamesToTry(countryName);
        for (String name : namesToTry) {
            CountryData exactData = fetchCountryData(name, true);
            if (exactData != null) {
                CACHE.put(countryName, exactData);
                return exactData;
            }
        }

        for (String name : namesToTry) {
            CountryData partialData = fetchCountryData(name, false);
            if (partialData != null) {
                CACHE.put(countryName, partialData);
                return partialData;
            }
        }

        CountryData emptyData = new CountryData(0, null, "");
        CACHE.put(countryName, emptyData);
        return emptyData;
    }

    public static Map<String, CountryData> getAllCountryData(Collection<String> countryNames) {
        Map<String, CountryData> result = new HashMap<>();
        Map<String, CountryData> dataByName = fetchAllCountryData();

        for (String countryName : countryNames) {
            CountryData data = null;
            for (String nameToTry : getNamesToTry(countryName)) {
                data = dataByName.get(normalizeName(nameToTry));
                if (data != null) {
                    break;
                }
            }

            if (data == null) {
                data = getCountryData(countryName);
            } else {
                CACHE.put(countryName, data);
            }
            result.put(countryName, data);
        }

        return result;
    }

    /**
     * Fetches the current population of a country.
     * @param countryName The name of the country
     * @return The population, or 0 if not found or error
     */
    public static long getPopulation(String countryName) {
        return getCountryData(countryName).getPopulation();
    }

    public static double[] getCoordinates(String countryName) {
        return getCountryData(countryName).getCoordinates();
    }

    private static CountryData fetchCountryData(String countryName, boolean fullText) {
        try {
            String encodedCountry = URLEncoder.encode(countryName, StandardCharsets.UTF_8).replace("+", "%20");
            String urlString = API_URL + encodedCountry
                    + "?fields=name,population,capital,capitalInfo,latlng"
                    + (fullText ? "&fullText=true" : "");

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            String responseStr = response.toString();
            long population = extractLong(responseStr, "\"population\":");
            String capital = extractString(responseStr, "\"capital\":[\"");
            double[] coordinates = extractCoordinates(responseStr, "\"capitalInfo\":", "\"latlng\":[");
            if (coordinates == null) {
                coordinates = extractCoordinates(responseStr, "", "\"latlng\":[");
            }

            if (population > 0 || coordinates != null || capital != null) {
                return new CountryData(population, coordinates, capital == null ? "" : capital);
            }
        } catch (Exception e) {
            System.err.println("Error fetching country data for " + countryName + ": " + e.getMessage());
        }
        return null;
    }

    private static Map<String, CountryData> fetchAllCountryData() {
        Map<String, CountryData> dataByName = new HashMap<>();
        try {
            URL url = new URL(API_ALL_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                System.err.println("All countries API Error: HTTP " + conn.getResponseCode());
                return dataByName;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            for (String countryJson : splitTopLevelObjects(response.toString())) {
                long population = extractLong(countryJson, "\"population\":");
                String capital = extractString(countryJson, "\"capital\":[\"");
                double[] coordinates = extractCoordinates(countryJson, "\"capitalInfo\":", "\"latlng\":[");
                if (coordinates == null) {
                    coordinates = extractCoordinates(countryJson, "", "\"latlng\":[");
                }

                CountryData data = new CountryData(population, coordinates, capital == null ? "" : capital);
                addIfPresent(dataByName, extractString(countryJson, "\"common\":\""), data);
                addIfPresent(dataByName, extractString(countryJson, "\"official\":\""), data);
            }
        } catch (Exception e) {
            System.err.println("Error fetching all country data: " + e.getMessage());
        }
        return dataByName;
    }

    private static long extractLong(String text, String key) {
        int start = text.indexOf(key);
        if (start == -1) {
            return 0;
        }

        start += key.length();
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }

        try {
            return Long.parseLong(text.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double[] extractCoordinates(String text, String sectionKey, String coordinateKey) {
        int sectionStart = sectionKey.isEmpty() ? 0 : text.indexOf(sectionKey);
        if (sectionStart == -1) {
            return null;
        }

        int coordsStart = text.indexOf(coordinateKey, sectionStart);
        if (coordsStart == -1) {
            return null;
        }

        coordsStart += coordinateKey.length();
        int coordsEnd = text.indexOf("]", coordsStart);
        if (coordsEnd == -1) {
            return null;
        }

        String[] parts = text.substring(coordsStart, coordsEnd).split(",");
        if (parts.length < 2) {
            return null;
        }

        try {
            return new double[] {
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim())
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> splitTopLevelObjects(String text) {
        List<String> objects = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        int objectStart = -1;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
            } else if (ch == '{') {
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0 && objectStart != -1) {
                    objects.add(text.substring(objectStart, i + 1));
                    objectStart = -1;
                }
            }
        }

        return objects;
    }

    private static String extractString(String text, String key) {
        int start = text.indexOf(key);
        if (start == -1) {
            return null;
        }

        start += key.length();
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escaped) {
                value.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                return value.toString();
            } else {
                value.append(ch);
            }
        }
        return null;
    }

    private static void addIfPresent(Map<String, CountryData> dataByName, String name, CountryData data) {
        if (name != null && !name.isBlank()) {
            dataByName.put(normalizeName(name), data);
        }
    }

    private static String normalizeName(String name) {
        return name.toLowerCase().replace("the ", "").replaceAll("[^a-z0-9]", "");
    }

    private static String[] getNamesToTry(String countryName) {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("Czech Republic", "Czechia");
        aliases.put("Macedonia", "North Macedonia");
        aliases.put("Republic of Serbia", "Serbia");
        aliases.put("Swaziland", "Eswatini");
        aliases.put("Ivory Coast", "Cote d'Ivoire");
        aliases.put("Democratic Republic of the Congo", "DR Congo");
        aliases.put("Republic of the Congo", "Republic of the Congo");
        aliases.put("Russia", "Russian Federation");
        aliases.put("Vietnam", "Viet Nam");

        String alias = aliases.get(countryName);
        if (alias == null || alias.equals(countryName)) {
            return new String[] { countryName };
        }
        return new String[] { countryName, alias };
    }
}
