import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PopulationAPI {
    private static final String API_URL = "https://api.api-ninjas.com/v1/population";
    private static final String API_KEY = "p6XZQbTEjhRjYeX8WTUTBHwUf0NJfZdFBTcUZKmt"; // Population API key
    
    /**
     * Fetches the current population of a country
     * @param countryName The name of the country
     * @return The population, or 0 if not found or error
     */
    public static long getPopulation(String countryName) {
        try {
            String encodedCountry = URLEncoder.encode(countryName, StandardCharsets.UTF_8);
            String urlString = API_URL + "?country=" + encodedCountry;
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Api-Key", API_KEY);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() != 200) {
                System.err.println("Population API Error: HTTP " + conn.getResponseCode() + " for " + countryName);
                return 0;
            }
            
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            
            // Parse population manually from JSON response
            String responseStr = response.toString();
            
            // Find the last "population" field in historical_population
            int lastPopIndex = responseStr.lastIndexOf("\"population\":");
            if (lastPopIndex != -1) {
                int commaIndex = responseStr.indexOf(",", lastPopIndex);
                int braceIndex = responseStr.indexOf("}", lastPopIndex);
                int endIndex = commaIndex > 0 && commaIndex < braceIndex ? commaIndex : braceIndex;
                
                String populationStr = responseStr.substring(lastPopIndex + 13, endIndex).trim();
                try {
                    return Long.parseLong(populationStr);
                } catch (NumberFormatException e) {
                    System.err.println("Could not parse population for " + countryName + ": " + populationStr);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching population for " + countryName + ": " + e.getMessage());
        }
        return 0;
    }
}

