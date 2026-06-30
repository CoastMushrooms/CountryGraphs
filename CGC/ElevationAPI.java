import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class ElevationAPI {
    private static final String API_URL = "https://api.open-meteo.com/v1/elevation";
    
    public static double getElevation(double latitude, double longitude) {
        double[] elevations = getElevations(new double[][] {{ latitude, longitude }});
        return elevations.length == 0 || Double.isNaN(elevations[0]) ? 0 : elevations[0];
    }

    public static double[] getElevations(double[][] locations) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            double[] elevations = getElevationsOnce(locations);
            if (elevations.length > 0) {
                return elevations;
            }
            waitBeforeRetry(attempt);
        }
        return new double[0];
    }

    private static double[] getElevationsOnce(double[][] locations) {
        try {
            URL url = new URL(buildUrl(locations));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            
            if (conn.getResponseCode() != 200) {
                System.err.println("Elevation API Error: HTTP " + conn.getResponseCode());
                return new double[0];
            }
            
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            
            String responseStr = response.toString().replace(" ", "");
            double[] elevations = new double[locations.length];
            int count = 0;
            int arrayStart = responseStr.indexOf("\"elevation\":[");
            if (arrayStart == -1) {
                return new double[0];
            }

            arrayStart += "\"elevation\":[".length();
            int arrayEnd = responseStr.indexOf("]", arrayStart);
            if (arrayEnd == -1) {
                return new double[0];
            }

            String[] elevationStrings = responseStr.substring(arrayStart, arrayEnd).split(",");
            for (String elevationStr : elevationStrings) {
                if (count >= elevations.length) {
                    break;
                }

                if (elevationStr.equals("null") || elevationStr.isBlank()) {
                    elevations[count] = Double.NaN;
                    count++;
                    continue;
                }

                try {
                    elevations[count] = Double.parseDouble(elevationStr);
                } catch (NumberFormatException e) {
                    elevations[count] = Double.NaN;
                    System.err.println("Could not parse elevation: " + elevationStr);
                }
                count++;
            }

            if (count == elevations.length) {
                return elevations;
            }

            double[] foundElevations = new double[count];
            System.arraycopy(elevations, 0, foundElevations, 0, count);
            return foundElevations;
        } catch (Exception e) {
            System.err.println("Error fetching elevation: " + e.getMessage());
        }
        return new double[0];
    }

    private static void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(attempt * 1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String buildUrl(double[][] locations) {
        StringBuilder latitudes = new StringBuilder();
        StringBuilder longitudes = new StringBuilder();
        for (int i = 0; i < locations.length; i++) {
            if (i > 0) {
                latitudes.append(",");
                longitudes.append(",");
            }
            latitudes.append(locations[i][0]);
            longitudes.append(locations[i][1]);
        }
        return API_URL + "?latitude=" + latitudes + "&longitude=" + longitudes;
    }
}
