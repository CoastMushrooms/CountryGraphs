import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ElevationAPI {
    private static final String API_URL = "https://api.open-elevation.com/api/v1/lookup";
    
    /**
     * Fetches elevation at specified coordinates
     * @param latitude The latitude
     * @param longitude The longitude
     * @return The elevation in meters, or 0 if not found
     */
    public static double getElevation(double latitude, double longitude) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            
            // Build request body manually (no JSON library needed)
            String requestBody = "{\"locations\":[{\"latitude\":" + latitude + ",\"longitude\":" + longitude + "}]}";
            
            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            if (conn.getResponseCode() != 200) {
                System.err.println("Elevation API Error: HTTP " + conn.getResponseCode());
                return 0;
            }
            
            // Read response
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            
            // Parse elevation from response manually
            String responseStr = response.toString();
            int elevationIndex = responseStr.indexOf("\"elevation\":");
            if (elevationIndex != -1) {
                int commaIndex = responseStr.indexOf(",", elevationIndex);
                int braceIndex = responseStr.indexOf("}", elevationIndex);
                int endIndex = commaIndex > 0 && commaIndex < braceIndex ? commaIndex : braceIndex;
                
                String elevationStr = responseStr.substring(elevationIndex + 12, endIndex).trim();
                try {
                    return Double.parseDouble(elevationStr);
                } catch (NumberFormatException e) {
                    System.err.println("Could not parse elevation: " + elevationStr);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching elevation: " + e.getMessage());
        }
        return 0;
    }
}

