import java.util.*;

public class StudentCode extends Server{

    private CountryGraph graph;

    public StudentCode() {
        super();
        graph = new CountryGraph();
        initializeElevationAndPopulationData();
    }

    private void initializeElevationAndPopulationData() {
        System.out.println("Loading elevation and population data for countries...");
        Map<String, double[]> countryCoordinates = getCountryCapitalCoordinates();
        
        for (Country country : graph.getCountrySet()) {
            // Load population data
            long population = PopulationAPI.getPopulation(country.getName());
            country.setPopulation(population);
            
            // Load elevation data - use capital and sample nearby points for better regional accuracy
            if (countryCoordinates.containsKey(country.getName())) {
                double[] coords = countryCoordinates.get(country.getName());
                
                // Sample multiple points around the country to get min/max elevation
                // This helps identify coastal regions more accurately
                double minElevation = Double.MAX_VALUE;
                double maxElevation = Double.MIN_VALUE;
                
                // Sample 9 points: center plus 8 directions
                double[][] samplePoints = {
                    coords, // Center (capital)
                    {coords[0] + 1, coords[1]}, // North
                    {coords[0] - 1, coords[1]}, // South
                    {coords[0], coords[1] + 1}, // East
                    {coords[0], coords[1] - 1}, // West
                    {coords[0] + 1, coords[1] + 1}, // NE
                    {coords[0] + 1, coords[1] - 1}, // NW
                    {coords[0] - 1, coords[1] + 1}, // SE
                    {coords[0] - 1, coords[1] - 1}  // SW
                };
                
                for (double[] point : samplePoints) {
                    double elevation = ElevationAPI.getElevation(point[0], point[1]);
                    minElevation = Math.min(minElevation, elevation);
                    maxElevation = Math.max(maxElevation, elevation);
                }
                
                // If any samples failed, use defaults
                if (minElevation == Double.MAX_VALUE) {
                    double centerElevation = ElevationAPI.getElevation(coords[0], coords[1]);
                    country.setMinElevation(Math.max(0, centerElevation - 500));
                    country.setMaxElevation(centerElevation + 500);
                } else {
                    // Use sampled min/max but ensure reasonable bounds
                    minElevation = Math.max(-100, minElevation); // Account for below sea level areas
                    maxElevation = Math.min(9000, maxElevation); // Reasonable max elevation
                    country.setMinElevation(minElevation);
                    country.setMaxElevation(maxElevation);
                }
                
                System.out.println(country.getName() + ": Pop=" + population + 
                                   ", Elev: " + String.format("%.0f", country.getMinElevation()) + 
                                   "m - " + String.format("%.0f", country.getMaxElevation()) + "m");
            }
        }
        System.out.println("Data loading complete!");
    }

    private Map<String, double[]> getCountryCapitalCoordinates() {
        Map<String, double[]> coordinates = new HashMap<>();
        // Add major countries' capital coordinates [latitude, longitude]
        coordinates.put("Afghanistan", new double[]{34.5199, 69.1976}); // Kabul
        coordinates.put("Albania", new double[]{41.3275, 19.8187}); // Tirana
        coordinates.put("Algeria", new double[]{36.7538, 3.0588}); // Algiers
        coordinates.put("Angola", new double[]{-8.8383, 13.2344}); // Luanda
        coordinates.put("Argentina", new double[]{-34.6037, -58.3816}); // Buenos Aires
        coordinates.put("Australia", new double[]{-35.2809, 149.1300}); // Canberra
        coordinates.put("Austria", new double[]{48.2082, 16.3738}); // Vienna
        coordinates.put("Azerbaijan", new double[]{40.4093, 49.8671}); // Baku
        coordinates.put("Bangladesh", new double[]{23.8103, 90.4125}); // Dhaka
        coordinates.put("Belarus", new double[]{53.9045, 27.5615}); // Minsk
        coordinates.put("Belgium", new double[]{50.8503, 4.3517}); // Brussels
        coordinates.put("Benin", new double[]{6.4969, 2.6289}); // Porto-Novo
        coordinates.put("Bolivia", new double[]{-16.2902, -63.5887}); // La Paz
        coordinates.put("Brazil", new double[]{-15.8267, -47.8953}); // Brasília
        coordinates.put("Bulgaria", new double[]{42.6977, 23.3219}); // Sofia
        coordinates.put("Cambodia", new double[]{11.5564, 104.9282}); // Phnom Penh
        coordinates.put("Canada", new double[]{45.4215, -75.6972}); // Ottawa
        coordinates.put("Chad", new double[]{12.1348, 15.0557}); // N'Djamena
        coordinates.put("Chile", new double[]{-33.4489, -70.6693}); // Santiago
        coordinates.put("China", new double[]{39.9042, 116.4074}); // Beijing
        coordinates.put("Colombia", new double[]{4.7110, -74.0721}); // Bogotá
        coordinates.put("Costa Rica", new double[]{9.9281, -84.0907}); // San José
        coordinates.put("Croatia", new double[]{45.8150, 15.9819}); // Zagreb
        coordinates.put("Cuba", new double[]{23.1291, -82.3794}); // Havana
        coordinates.put("Czech Republic", new double[]{50.0755, 14.4378}); // Prague
        coordinates.put("Democratic Republic of the Congo", new double[]{-4.3376, 15.3136}); // Kinshasa
        coordinates.put("Denmark", new double[]{55.6761, 12.5683}); // Copenhagen
        coordinates.put("Ecuador", new double[]{-0.2194, -78.5125}); // Quito
        coordinates.put("Egypt", new double[]{30.0444, 31.2357}); // Cairo
        coordinates.put("El Salvador", new double[]{13.6929, -89.2182}); // San Salvador
        coordinates.put("Estonia", new double[]{59.4370, 24.7536}); // Tallinn
        coordinates.put("Ethiopia", new double[]{9.0320, 38.7469}); // Addis Ababa
        coordinates.put("Finland", new double[]{60.1695, 24.9354}); // Helsinki
        coordinates.put("France", new double[]{48.8566, 2.3522}); // Paris
        coordinates.put("Georgia", new double[]{41.7151, 44.8271}); // Tbilisi
        coordinates.put("Germany", new double[]{52.5200, 13.4050}); // Berlin
        coordinates.put("Ghana", new double[]{5.6037, -0.1870}); // Accra
        coordinates.put("Greece", new double[]{37.9838, 23.7275}); // Athens
        coordinates.put("Guatemala", new double[]{14.6343, -90.5069}); // Guatemala City
        coordinates.put("Guinea", new double[]{9.6412, -13.5784}); // Conakry
        coordinates.put("Honduras", new double[]{14.0723, -87.1921}); // Tegucigalpa
        coordinates.put("Hungary", new double[]{47.4979, 19.0402}); // Budapest
        coordinates.put("Iceland", new double[]{64.1466, -21.9426}); // Reykjavik
        coordinates.put("India", new double[]{28.6139, 77.2090}); // New Delhi
        coordinates.put("Indonesia", new double[]{-6.2088, 106.8456}); // Jakarta
        coordinates.put("Iran", new double[]{35.6892, 51.3890}); // Tehran
        coordinates.put("Iraq", new double[]{33.3157, 44.3615}); // Baghdad
        coordinates.put("Ireland", new double[]{53.3498, -6.2603}); // Dublin
        coordinates.put("Israel", new double[]{31.7683, 35.2137}); // Jerusalem
        coordinates.put("Italy", new double[]{41.9028, 12.4964}); // Rome
        coordinates.put("Jamaica", new double[]{18.0179, -76.8099}); // Kingston
        coordinates.put("Japan", new double[]{35.6762, 139.6503}); // Tokyo
        coordinates.put("Jordan", new double[]{31.9454, 35.9284}); // Amman
        coordinates.put("Kazakhstan", new double[]{51.1694, 71.4491}); // Nur-Sultan
        coordinates.put("Kenya", new double[]{-1.2921, 36.8219}); // Nairobi
        coordinates.put("Kuwait", new double[]{29.3759, 47.9774}); // Kuwait City
        coordinates.put("Latvia", new double[]{56.9496, 24.1052}); // Riga
        coordinates.put("Lebanon", new double[]{33.8738, 35.4742}); // Beirut
        coordinates.put("Libya", new double[]{32.8872, 13.1913}); // Tripoli
        coordinates.put("Lithuania", new double[]{54.6872, 25.2797}); // Vilnius
        coordinates.put("Madagascar", new double[]{-18.8792, 47.5079}); // Antananarivo
        coordinates.put("Malawi", new double[]{-13.9626, 33.7741}); // Lilongwe
        coordinates.put("Malaysia", new double[]{3.1390, 101.6869}); // Kuala Lumpur
        coordinates.put("Mali", new double[]{12.6392, -8.0029}); // Bamako
        coordinates.put("Mexico", new double[]{19.4326, -99.1332}); // Mexico City
        coordinates.put("Morocco", new double[]{33.9716, -6.8498}); // Rabat
        coordinates.put("Mozambique", new double[]{-23.8231, 35.3027}); // Maputo
        coordinates.put("Nepal", new double[]{27.7172, 85.3240}); // Kathmandu
        coordinates.put("Netherlands", new double[]{52.3676, 4.9041}); // Amsterdam
        coordinates.put("New Zealand", new double[]{-41.2865, 174.7762}); // Wellington
        coordinates.put("Nicaragua", new double[]{12.1150, -86.2362}); // Managua
        coordinates.put("Nigeria", new double[]{9.0765, 7.3986}); // Abuja
        coordinates.put("Norway", new double[]{59.9139, 10.7522}); // Oslo
        coordinates.put("Pakistan", new double[]{33.6844, 73.0479}); // Islamabad
        coordinates.put("Panama", new double[]{8.9824, -79.5199}); // Panama City
        coordinates.put("Peru", new double[]{-12.0464, -75.5278}); // Lima
        coordinates.put("Philippines", new double[]{14.5995, 120.9842}); // Manila
        coordinates.put("Poland", new double[]{52.2297, 21.0122}); // Warsaw
        coordinates.put("Portugal", new double[]{38.7223, -9.1393}); // Lisbon
        coordinates.put("Romania", new double[]{44.4268, 26.1025}); // Bucharest
        coordinates.put("Russia", new double[]{55.7558, 37.6173}); // Moscow
        coordinates.put("Rwanda", new double[]{-1.9536, 29.8739}); // Kigali
        coordinates.put("Saudi Arabia", new double[]{24.7136, 46.6753}); // Riyadh
        coordinates.put("Senegal", new double[]{14.7167, -17.4674}); // Dakar
        coordinates.put("Serbia", new double[]{44.8178, 20.4568}); // Belgrade
        coordinates.put("Singapore", new double[]{1.3521, 103.8198}); // Singapore
        coordinates.put("Slovakia", new double[]{48.1486, 17.1077}); // Bratislava
        coordinates.put("Slovenia", new double[]{46.0569, 14.5058}); // Ljubljana
        coordinates.put("South Africa", new double[]{-25.7482, 28.2293}); // Pretoria
        coordinates.put("South Korea", new double[]{37.5665, 126.9780}); // Seoul
        coordinates.put("Spain", new double[]{40.4168, -3.7038}); // Madrid
        coordinates.put("Sri Lanka", new double[]{6.9271, 80.7790}); // Colombo
        coordinates.put("Sudan", new double[]{15.5007, 32.5599}); // Khartoum
        coordinates.put("Sweden", new double[]{59.3293, 18.0686}); // Stockholm
        coordinates.put("Switzerland", new double[]{46.9479, 7.4474}); // Bern
        coordinates.put("Syria", new double[]{33.5138, 36.2765}); // Damascus
        coordinates.put("Taiwan", new double[]{25.0330, 121.5654}); // Taipei
        coordinates.put("Thailand", new double[]{13.7563, 100.5018}); // Bangkok
        coordinates.put("Tunisia", new double[]{36.8065, 10.1686}); // Tunis
        coordinates.put("Turkey", new double[]{39.9334, 32.8587}); // Ankara
        coordinates.put("Uganda", new double[]{0.3476, 32.5825}); // Kampala
        coordinates.put("Ukraine", new double[]{50.4501, 30.5234}); // Kyiv
        coordinates.put("United Arab Emirates", new double[]{24.4539, 54.3773}); // Abu Dhabi
        coordinates.put("United Kingdom", new double[]{51.5074, -0.1278}); // London
        coordinates.put("United States of America", new double[]{38.8951, -77.0369}); // Washington D.C.
        coordinates.put("Uruguay", new double[]{-34.9011, -56.1645}); // Montevideo
        coordinates.put("Venezuela", new double[]{10.4806, -66.9036}); // Caracas
        coordinates.put("Vietnam", new double[]{21.0285, 105.8542}); // Hanoi
        coordinates.put("Yemen", new double[]{15.3694, 48.5255}); // Sana'a
        coordinates.put("Zambia", new double[]{-10.3369, 28.2832}); // Lusaka
        coordinates.put("Zimbabwe", new double[]{-17.8252, 31.0335}); // Harare
        return coordinates;
    }

    public static void main(String[] args) {
        Server server = new StudentCode(); // Initialize server on default port (8000).
        server.run(); // Start the server.
        server.openURL(); // Open url in browser.
    }

    @Override
    public void getInputCountries(String country1, String country2) {
        Country c1 = graph.getCountryByName(country1);
        Country c2 = graph.getCountryByName(country2);
        if (c1 == null || c2 == null) {
            sendMessageToUser("Country not found");
            return;
        }
        List<Country> path = graph.findPath(c1, c2);
        if (path.isEmpty()) {
            sendMessageToUser("No path found between " + country1 + " and " + country2);
        } else {
            sendMessageToUser("The shortest path has " + (path.size() - 1) + " borders.");
            for (int i = 0; i < path.size(); i++) {
                String color = (i == 0) ? "red" : (i == path.size() - 1) ? "blue" : "green";
                addCountryColor(path.get(i).getName(), color);
            }
        }
        setMessage("Path calculated");
    }

    @Override
    public void getColorPath() {

    }

    @Override
    public void handleContextMenu(String country) {
        Country c = graph.getCountryByName(country);
        if (c == null) return;

        clearCountryColors();
        addCountryColor(c.getName(), "yellow");
        Set<Country> neighbors = graph.getNeighbors(c);
        for (Country neighbor : neighbors) {
            addCountryColor(neighbor.getName(), "orange");
        }
        // Record the click
        long timestamp = System.currentTimeMillis();
        System.out.println("Context menu on " + country + " at " + timestamp + " ms");
        sendMessageToUser("Neighbors of " + country + ": " + neighbors);
    }

    @Override
    public void handleClick(String country) {
        Country c = graph.getCountryByName(country);
        if (c == null) return;

        clearCountryColors();
        addCountryColor(c.getName(), "cyan");
        // Record the click
        long timestamp = System.currentTimeMillis();
        System.out.println("Clicked on " + country + " at " + timestamp + " ms");
        sendMessageToUser("Clicked on " + country);
    }
     
    @Override
    public void handleMenuOption(String country, String menuOption) {
        Country c = graph.getCountryByName(country);
        if (c == null) return;

        switch (menuOption.toLowerCase()) {
            case "population":
                sendMessageToUser("Population of " + country + ": " + c.getPopulation());
                break;
            case "gdp":
                sendMessageToUser("GDP of " + country + ": " + c.getGdp());
                break;
            case "area":
                sendMessageToUser("Area of " + country + ": " + c.getArea());
                break;
            case "capital":
                sendMessageToUser("Capital of " + country + ": " + c.getCapital());
                break;
            case "continent":
                sendMessageToUser("Continent of " + country + ": " + c.getContinent());
                break;
            default:
                sendMessageToUser("Unknown menu option: " + menuOption);
        }
    }

    @Override
    public void handleSeaLevelChange(double seaLevel) {
        clearCountryColors();
        Set<Country> underwater = new HashSet<>();
        Set<Country> coastal = new HashSet<>();
        long totalDisplacedPopulation = 0;
        
        // Determine which countries are affected by sea level rise
        for (Country country : graph.getCountrySet()) {
            // Check if country's lowest elevation is below sea level (fully underwater)
            if (country.getMinElevation() < seaLevel) {
                underwater.add(country);
                addCountryColor(country.getName(), "#000000"); // Black for underwater
                totalDisplacedPopulation += country.getPopulation();
            }
            // Check if country's max elevation is below sea level but min is above
            // This identifies countries with coastal flooding
            else if (country.getMaxElevation() < seaLevel) {
                underwater.add(country);
                addCountryColor(country.getName(), "#000000");
                totalDisplacedPopulation += country.getPopulation();
            }
            // Coastal regions: min elevation above sea level, but max indicates coastal areas
            else if (country.getMinElevation() < seaLevel && country.getMaxElevation() >= seaLevel) {
                coastal.add(country);
                // Highlight coastal regions with a darker shade
                addCountryColor(country.getName(), "#2d2d2d"); // Very dark gray for partial submersion
            }
        }
        
        // Display statistics
        setMessage("Sea Level: " + String.format("%.1f", seaLevel) + "m | " +
                   "Fully Underwater: " + underwater.size() + " | " +
                   "Coastal Flooding: " + coastal.size() + " | " +
                   "Displaced: " + formatPopulation(totalDisplacedPopulation));
        
        sendMessageToUser("Sea level set to " + String.format("%.1f", seaLevel) + " meters");
        
        if (!underwater.isEmpty()) {
            StringBuilder underwater_list = new StringBuilder("Fully Submerged: ");
            int count = 0;
            for (Country c : underwater) {
                if (count > 0) underwater_list.append(", ");
                underwater_list.append(c.getName()).append(" (").append(formatPopulation(c.getPopulation())).append(")");
                count++;
                if (count >= 5) {
                    underwater_list.append("...");
                    break;
                }
            }
            sendMessageToUser(underwater_list.toString());
        }
        
        if (!coastal.isEmpty()) {
            StringBuilder coastal_list = new StringBuilder("Coastal Flooding Risk: ");
            int count = 0;
            for (Country c : coastal) {
                if (count > 0) coastal_list.append(", ");
                coastal_list.append(c.getName());
                count++;
                if (count >= 5) {
                    coastal_list.append("...");
                    break;
                }
            }
            sendMessageToUser(coastal_list.toString());
        }
    }
    
    private String formatPopulation(long pop) {
        if (pop >= 1_000_000) {
            return String.format("%.1fM", pop / 1_000_000.0);
        } else if (pop >= 1_000) {
            return String.format("%.1fK", pop / 1_000.0);
        }
        return String.valueOf(pop);
    }
}
