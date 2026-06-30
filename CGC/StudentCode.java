import java.io.*;
import java.util.*;

public class StudentCode extends Server{

    private static final String COUNTRY_DATA_CACHE_FILE = "country-data-cache.properties";
    private CountryGraph graph;
    private Map<String, double[]> countryCoordinates = Collections.synchronizedMap(new HashMap<>());
    private Set<String> countriesWithElevation = Collections.synchronizedSet(new HashSet<>());
    private Set<String> countriesLoadingElevation = Collections.synchronizedSet(new HashSet<>());
    private Set<String> submergedCountryNames = Collections.synchronizedSet(new HashSet<>());

    public StudentCode() {
        super();
        graph = new CountryGraph();
        graph.writeActiveBordersExcluding(new HashSet<>());
        new Thread(() -> {
            initializeElevationAndPopulationData();
        }, "CountryDataLoader").start();
    }

    private void initializeElevationAndPopulationData() {
        System.out.println("Loading country data...");
        Map<String, double[]> fallbackCoordinates = getCountryCapitalCoordinates();
        Set<Country> countries = graph.getCountrySet();
        Set<String> countryNames = new HashSet<>();

        for (Country country : countries) {
            countryNames.add(country.getName());
        }

        boolean cacheComplete = loadCountryDataCache(countries);
        if (cacheComplete) {
            System.out.println("Country data loaded from " + COUNTRY_DATA_CACHE_FILE);
            return;
        }

        Set<String> missingCountryNames = new HashSet<>();
        for (Country country : countries) {
            if (country.getPopulation() == 0 || country.getCapital().isBlank() || !countryCoordinates.containsKey(country.getName())) {
                missingCountryNames.add(country.getName());
            }
        }

        Map<String, PopulationAPI.CountryData> allCountryData = new HashMap<>();
        if (!missingCountryNames.isEmpty()) {
            System.out.println("Loading population data for " + missingCountryNames.size() + " countries from API...");
            allCountryData = PopulationAPI.getAllCountryData(missingCountryNames);
        }

        for (Country country : countries) {
            if (country.getPopulation() != 0 && !country.getCapital().isBlank() && countryCoordinates.containsKey(country.getName())) {
                continue;
            }

            PopulationAPI.CountryData apiData = allCountryData.get(country.getName());
            long population = apiData == null ? 0 : apiData.getPopulation();
            country.setPopulation(population);
            if (apiData != null && !apiData.getCapital().isBlank()) {
                country.setCapital(apiData.getCapital());
            }

            double[] coords = apiData == null ? null : apiData.getCoordinates();
            if (coords == null) {
                coords = fallbackCoordinates.get(country.getName());
            }
            if (coords != null) {
                countryCoordinates.put(country.getName(), coords);
            }

            System.out.println(country.getName() + ": Pop=" + population);
        }
        System.out.println("Population data loading complete!");
        loadAllElevationData(countries);
        saveCountryDataCache(countries);
    }

    private void loadAllElevationData(Set<Country> countries) {
        System.out.println("Loading elevation data for countries...");
        List<double[]> points = new ArrayList<>();
        List<Country> pointCountries = new ArrayList<>();
        Map<Country, double[]> minMaxByCountry = new HashMap<>();

        for (Country country : countries) {
            synchronized (countriesWithElevation) {
                if (countriesWithElevation.contains(country.getName())) {
                    continue;
                }
            }

            double[] coords = countryCoordinates.get(country.getName());
            if (coords == null) {
                continue;
            }

            synchronized (countriesWithElevation) {
                countriesLoadingElevation.add(country.getName());
            }
            minMaxByCountry.put(country, new double[] { Double.MAX_VALUE, -Double.MAX_VALUE });

            double[][] samplePoints = getElevationSamplePoints(coords);
            for (double[] point : samplePoints) {
                points.add(point);
                pointCountries.add(country);
            }
        }

        int batchSize = 100;
        for (int start = 0; start < points.size(); start += batchSize) {
            int end = Math.min(start + batchSize, points.size());
            double[][] batch = new double[end - start][2];
            for (int i = start; i < end; i++) {
                batch[i - start] = points.get(i);
            }

            double[] elevations = ElevationAPI.getElevations(batch);
            if (elevations.length == 0) {
                sleepBeforeNextElevationBatch();
                continue;
            }

            for (int i = 0; i < elevations.length && start + i < pointCountries.size(); i++) {
                if (Double.isNaN(elevations[i])) {
                    continue;
                }
                Country country = pointCountries.get(start + i);
                double[] minMax = minMaxByCountry.get(country);
                minMax[0] = Math.min(minMax[0], elevations[i]);
                minMax[1] = Math.max(minMax[1], elevations[i]);
            }
            sleepBeforeNextElevationBatch();
        }

        for (Map.Entry<Country, double[]> entry : minMaxByCountry.entrySet()) {
            Country country = entry.getKey();
            double[] minMax = entry.getValue();
            if (minMax[0] != Double.MAX_VALUE) {
                country.setMinElevation(Math.max(-100, minMax[0]));
                country.setMaxElevation(Math.min(9000, minMax[1]));
                System.out.println(country.getName() + ": Elev="
                        + String.format("%.0f", country.getMinElevation()) + "m - "
                        + String.format("%.0f", country.getMaxElevation()) + "m");

                synchronized (countriesWithElevation) {
                    countriesWithElevation.add(country.getName());
                }
            } else {
                System.out.println(country.getName() + ": Elev=still loading (API rate limit)");
            }

            synchronized (countriesWithElevation) {
                countriesLoadingElevation.remove(country.getName());
            }
        }
        System.out.println("Elevation data loading complete!");
    }

    private boolean loadCountryDataCache(Set<Country> countries) {
        Properties cache = new Properties();
        try (FileInputStream input = new FileInputStream(COUNTRY_DATA_CACHE_FILE)) {
            cache.load(input);
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            System.err.println("Could not read " + COUNTRY_DATA_CACHE_FILE + ": " + e.getMessage());
            return false;
        }

        boolean allCountriesComplete = true;
        for (Country country : countries) {
            String key = getCacheKey(country);

            String population = cache.getProperty(key + ".population");
            if (population != null) {
                country.setPopulation(parseLong(population, 0));
            }

            String capital = cache.getProperty(key + ".capital");
            if (capital != null) {
                country.setCapital(capital);
            }

            String latitude = cache.getProperty(key + ".latitude");
            String longitude = cache.getProperty(key + ".longitude");
            if (latitude != null && longitude != null) {
                countryCoordinates.put(country.getName(), new double[] {
                        parseDouble(latitude, 0),
                        parseDouble(longitude, 0)
                });
            }

            String minElevation = cache.getProperty(key + ".minElevation");
            String maxElevation = cache.getProperty(key + ".maxElevation");
            if (minElevation != null && maxElevation != null) {
                country.setMinElevation(parseDouble(minElevation, 0));
                country.setMaxElevation(parseDouble(maxElevation, 0));
                synchronized (countriesWithElevation) {
                    countriesWithElevation.add(country.getName());
                }
            }

            if (country.getPopulation() == 0
                    || country.getCapital().isBlank()
                    || !countryCoordinates.containsKey(country.getName())
                    || !countriesWithElevation.contains(country.getName())) {
                allCountriesComplete = false;
            }
        }
        return allCountriesComplete;
    }

    private void saveCountryDataCache(Set<Country> countries) {
        Properties cache = new Properties();
        for (Country country : countries) {
            String key = getCacheKey(country);
            cache.setProperty(key + ".population", String.valueOf(country.getPopulation()));
            cache.setProperty(key + ".capital", country.getCapital());

            double[] coords = countryCoordinates.get(country.getName());
            if (coords != null) {
                cache.setProperty(key + ".latitude", String.valueOf(coords[0]));
                cache.setProperty(key + ".longitude", String.valueOf(coords[1]));
            }

            synchronized (countriesWithElevation) {
                if (countriesWithElevation.contains(country.getName())) {
                    cache.setProperty(key + ".minElevation", String.valueOf(country.getMinElevation()));
                    cache.setProperty(key + ".maxElevation", String.valueOf(country.getMaxElevation()));
                }
            }
        }

        try (FileOutputStream output = new FileOutputStream(COUNTRY_DATA_CACHE_FILE)) {
            cache.store(output, "Cached country population, coordinates, and elevation data");
            System.out.println("Country data saved to " + COUNTRY_DATA_CACHE_FILE);
        } catch (IOException e) {
            System.err.println("Could not save " + COUNTRY_DATA_CACHE_FILE + ": " + e.getMessage());
        }
    }

    private String getCacheKey(Country country) {
        return "country." + Base64.getUrlEncoder().withoutPadding().encodeToString(country.getName().getBytes());
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void sleepBeforeNextElevationBatch() {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, double[]> getCountryCapitalCoordinates() {
        Map<String, double[]> coordinates = new HashMap<>();
        coordinates.put("Afghanistan", new double[]{34.5199, 69.1976}); 
        coordinates.put("Albania", new double[]{41.3275, 19.8187});
        coordinates.put("Algeria", new double[]{36.7538, 3.0588});
        coordinates.put("Angola", new double[]{-8.8383, 13.2344}); 
        coordinates.put("Argentina", new double[]{-34.6037, -58.3816}); 
        coordinates.put("Australia", new double[]{-35.2809, 149.1300}); 
        coordinates.put("Austria", new double[]{48.2082, 16.3738}); 
        coordinates.put("Azerbaijan", new double[]{40.4093, 49.8671}); 
        coordinates.put("Bangladesh", new double[]{23.8103, 90.4125}); 
        coordinates.put("Belarus", new double[]{53.9045, 27.5615});
        coordinates.put("Belgium", new double[]{50.8503, 4.3517}); 
        coordinates.put("Benin", new double[]{6.4969, 2.6289}); 
        coordinates.put("Bolivia", new double[]{-16.2902, -63.5887}); 
        coordinates.put("Brazil", new double[]{-15.8267, -47.8953}); 
        coordinates.put("Bulgaria", new double[]{42.6977, 23.3219});
        coordinates.put("Cambodia", new double[]{11.5564, 104.9282}); 
        coordinates.put("Canada", new double[]{45.4215, -75.6972}); 
        coordinates.put("Chad", new double[]{12.1348, 15.0557}); 
        coordinates.put("Chile", new double[]{-33.4489, -70.6693}); 
        coordinates.put("China", new double[]{39.9042, 116.4074}); 
        coordinates.put("Colombia", new double[]{4.7110, -74.0721});
        coordinates.put("Costa Rica", new double[]{9.9281, -84.0907}); 
        coordinates.put("Croatia", new double[]{45.8150, 15.9819}); 
        coordinates.put("Cuba", new double[]{23.1291, -82.3794}); 
        coordinates.put("Czech Republic", new double[]{50.0755, 14.4378}); 
        coordinates.put("Democratic Republic of the Congo", new double[]{-4.3376, 15.3136});
        coordinates.put("Denmark", new double[]{55.6761, 12.5683}); 
        coordinates.put("Ecuador", new double[]{-0.2194, -78.5125}); 
        coordinates.put("Egypt", new double[]{30.0444, 31.2357}); 
        coordinates.put("El Salvador", new double[]{13.6929, -89.2182}); 
        coordinates.put("Estonia", new double[]{59.4370, 24.7536}); 
        coordinates.put("Ethiopia", new double[]{9.0320, 38.7469}); 
        coordinates.put("Finland", new double[]{60.1695, 24.9354}); 
        coordinates.put("France", new double[]{48.8566, 2.3522}); 
        coordinates.put("Georgia", new double[]{41.7151, 44.8271});
        coordinates.put("Germany", new double[]{52.5200, 13.4050}); 
        coordinates.put("Ghana", new double[]{5.6037, -0.1870}); 
        coordinates.put("Greece", new double[]{37.9838, 23.7275}); 
        coordinates.put("Guatemala", new double[]{14.6343, -90.5069}); 
        coordinates.put("Guinea", new double[]{9.6412, -13.5784}); 
        coordinates.put("Honduras", new double[]{14.0723, -87.1921});
        coordinates.put("Hungary", new double[]{47.4979, 19.0402}); 
        coordinates.put("Iceland", new double[]{64.1466, -21.9426}); 
        coordinates.put("India", new double[]{28.6139, 77.2090}); 
        coordinates.put("Indonesia", new double[]{-6.2088, 106.8456}); 
        coordinates.put("Iran", new double[]{35.6892, 51.3890}); 
        coordinates.put("Iraq", new double[]{33.3157, 44.3615});
        coordinates.put("Ireland", new double[]{53.3498, -6.2603}); 
        coordinates.put("Israel", new double[]{31.7683, 35.2137}); 
        coordinates.put("Italy", new double[]{41.9028, 12.4964}); 
        coordinates.put("Jamaica", new double[]{18.0179, -76.8099}); 
        coordinates.put("Japan", new double[]{35.6762, 139.6503}); 
        coordinates.put("Jordan", new double[]{31.9454, 35.9284}); 
        coordinates.put("Kazakhstan", new double[]{51.1694, 71.4491}); 
        coordinates.put("Kenya", new double[]{-1.2921, 36.8219}); 
        coordinates.put("Kuwait", new double[]{29.3759, 47.9774}); 
        coordinates.put("Latvia", new double[]{56.9496, 24.1052}); 
        coordinates.put("Lebanon", new double[]{33.8738, 35.4742}); 
        coordinates.put("Libya", new double[]{32.8872, 13.1913}); 
        coordinates.put("Lithuania", new double[]{54.6872, 25.2797}); 
        coordinates.put("Madagascar", new double[]{-18.8792, 47.5079}); 
        coordinates.put("Malawi", new double[]{-13.9626, 33.7741}); 
        coordinates.put("Malaysia", new double[]{3.1390, 101.6869}); 
        coordinates.put("Mali", new double[]{12.6392, -8.0029}); 
        coordinates.put("Mexico", new double[]{19.4326, -99.1332}); 
        coordinates.put("Morocco", new double[]{33.9716, -6.8498}); 
        coordinates.put("Mozambique", new double[]{-23.8231, 35.3027}); 
        coordinates.put("Nepal", new double[]{27.7172, 85.3240}); 
        coordinates.put("Netherlands", new double[]{52.3676, 4.9041}); 
        coordinates.put("New Zealand", new double[]{-41.2865, 174.7762}); 
        coordinates.put("Nicaragua", new double[]{12.1150, -86.2362}); 
        coordinates.put("Nigeria", new double[]{9.0765, 7.3986}); 
        coordinates.put("Norway", new double[]{59.9139, 10.7522}); 
        coordinates.put("Pakistan", new double[]{33.6844, 73.0479}); 
        coordinates.put("Panama", new double[]{8.9824, -79.5199}); 
        coordinates.put("Peru", new double[]{-12.0464, -75.5278}); 
        coordinates.put("Philippines", new double[]{14.5995, 120.9842}); 
        coordinates.put("Poland", new double[]{52.2297, 21.0122}); 
        coordinates.put("Portugal", new double[]{38.7223, -9.1393}); 
        coordinates.put("Romania", new double[]{44.4268, 26.1025}); 
        coordinates.put("Russia", new double[]{55.7558, 37.6173}); 
        coordinates.put("Rwanda", new double[]{-1.9536, 29.8739}); 
        coordinates.put("Saudi Arabia", new double[]{24.7136, 46.6753}); 
        coordinates.put("Senegal", new double[]{14.7167, -17.4674}); 
        coordinates.put("Serbia", new double[]{44.8178, 20.4568}); 
        coordinates.put("Singapore", new double[]{1.3521, 103.8198}); 
        coordinates.put("Slovakia", new double[]{48.1486, 17.1077}); 
        coordinates.put("Slovenia", new double[]{46.0569, 14.5058});
        coordinates.put("South Africa", new double[]{-25.7482, 28.2293}); 
        coordinates.put("South Korea", new double[]{37.5665, 126.9780}); 
        coordinates.put("Spain", new double[]{40.4168, -3.7038});
        coordinates.put("Sri Lanka", new double[]{6.9271, 80.7790}); 
        coordinates.put("Sudan", new double[]{15.5007, 32.5599});
        coordinates.put("Sweden", new double[]{59.3293, 18.0686}); 
        coordinates.put("Switzerland", new double[]{46.9479, 7.4474}); 
        coordinates.put("Syria", new double[]{33.5138, 36.2765});
        coordinates.put("Taiwan", new double[]{25.0330, 121.5654}); 
        coordinates.put("Thailand", new double[]{13.7563, 100.5018}); 
        coordinates.put("Tunisia", new double[]{36.8065, 10.1686}); 
        coordinates.put("Turkey", new double[]{39.9334, 32.8587}); 
        coordinates.put("Uganda", new double[]{0.3476, 32.5825}); 
        coordinates.put("Ukraine", new double[]{50.4501, 30.5234});
        coordinates.put("United Arab Emirates", new double[]{24.4539, 54.3773});
        coordinates.put("United Kingdom", new double[]{51.5074, -0.1278}); 
        coordinates.put("United States of America", new double[]{38.8951, -77.0369}); 
        coordinates.put("Uruguay", new double[]{-34.9011, -56.1645}); 
        coordinates.put("Venezuela", new double[]{10.4806, -66.9036}); 
        coordinates.put("Vietnam", new double[]{21.0285, 105.8542}); 
        coordinates.put("Yemen", new double[]{15.3694, 48.5255}); 
        coordinates.put("Zambia", new double[]{-10.3369, 28.2832}); 
        coordinates.put("Zimbabwe", new double[]{-17.8252, 31.0335}); 
        return coordinates;
    }

    public static void main(String[] args) {
        Server server = new StudentCode();
        server.run(); 
        server.openURL();
    }

    @Override
    public void getInputCountries(String country1, String country2, String routeMode) {
        Country c1 = graph.getCountryByName(country1);
        Country c2 = graph.getCountryByName(country2);
        if (c1 == null || c2 == null) {
            sendMessageToUser("Country not found");
            return;
        }
        clearCountryColors();

        if ("cities".equalsIgnoreCase(routeMode)) {
            getInputCapitalCities(c1, c2);
            return;
        }

        Set<Country> submergedCountries = getSubmergedCountries();
        List<Country> path = graph.findPath(c1, c2, submergedCountries);
        if (path.isEmpty()) {
            if (submergedCountries.contains(c1) || submergedCountries.contains(c2)) {
                sendMessageToUser("No path found because " + country1 + " or " + country2 + " is submerged.");
            } else {
                sendMessageToUser("No dry path found between " + country1 + " and " + country2);
            }
        } else {
            sendMessageToUser("The shortest dry path has " + (path.size() - 1) + " borders.");
            sendMessageToUser("Capital city path: " + formatCapitalPath(path));
            for (int i = 0; i < path.size(); i++) {
                addCountryColor(path.get(i).getName(), "green");
            }
        }
        setMessage("Path calculated");
    }

    private void getInputCapitalCities(Country startCountry, Country endCountry) {
        Set<Country> submergedCountries = getSubmergedCountries();
        List<Country> cityPath = findCapitalCityPath(startCountry, endCountry, submergedCountries);
        if (cityPath.isEmpty()) {
            if (submergedCountries.contains(startCountry) || submergedCountries.contains(endCountry)) {
                sendMessageToUser("No capital city path found because the start or end country is submerged.");
            } else {
                sendMessageToUser("No dry capital city path found between "
                        + getCapitalLabel(startCountry) + " and " + getCapitalLabel(endCountry));
            }
            setMessage("City path unavailable");
            return;
        }

        for (Country country : cityPath) {
            addCountryColor(country.getName(), "green");
        }

        String cityPathMessage = formatCapitalPath(cityPath);
        sendMessageToUser("Capital city BFS path: " + cityPathMessage);
        addCountryColor("__cityPath", encodeCityPath(cityPath));
        setMessage("City path: " + cityPathMessage);
    }

    private List<Country> findCapitalCityPath(Country start, Country end, Set<Country> blockedCountries) {
        if (blockedCountries.contains(start) || blockedCountries.contains(end)) {
            return new ArrayList<>();
        }

        Map<Country, Country> parent = new HashMap<>();
        Set<Country> visited = new HashSet<>();
        Queue<Country> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        parent.put(start, null);

        while (!queue.isEmpty()) {
            Country current = queue.poll();
            if (current.equals(end)) {
                List<Country> path = new ArrayList<>();
                for (Country at = end; at != null; at = parent.get(at)) {
                    path.add(at);
                }
                Collections.reverse(path);
                return path;
            }

            for (Country neighbor : graph.getNeighbors(current)) {
                if (!blockedCountries.contains(neighbor) && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    parent.put(neighbor, current);
                }
            }
        }
        return new ArrayList<>();
    }

    private String encodeCityPath(List<Country> path) {
        List<String> points = new ArrayList<>();
        for (Country country : path) {
            double[] coords = countryCoordinates.get(country.getName());
            if (coords == null) {
                continue;
            }
            points.add(coords[0] + "," + coords[1] + "," + getCapitalLabel(country).replace(",", " "));
        }
        return String.join("|", points);
    }

    private String getCapitalLabel(Country country) {
        String capital = getCapitalDisplayName(country);
        return capital + " (" + country.getName() + ")";
    }

    private String formatCapitalPath(List<Country> path) {
        List<String> capitalStops = new ArrayList<>();
        for (Country country : path) {
            capitalStops.add(getCapitalLabel(country));
        }
        return String.join(" -> ", capitalStops);
    }

    private Set<Country> getSubmergedCountries() {
        Set<Country> submergedCountries = new HashSet<>();
        synchronized (submergedCountryNames) {
            for (String countryName : submergedCountryNames) {
                Country country = graph.getCountryByName(countryName);
                if (country != null) {
                    submergedCountries.add(country);
                }
            }
        }
        return submergedCountries;
    }

    @Override
    public void getColorPath() {

    }

    @Override
    public String getCapitalOptionsJSON() {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Country country : graph.getCountrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(escapeJSON(country.getName())).append("\":\"")
                    .append(escapeJSON(getCapitalDisplayName(country))).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private String getCapitalDisplayName(Country country) {
        String capital = country.getCapital();
        if (capital == null || capital.isBlank()) {
            capital = getFallbackCapitalNames().get(country.getName());
        }
        if (capital == null || capital.isBlank()) {
            return country.getName();
        }
        return capital;
    }

    private Map<String, String> getFallbackCapitalNames() {
        Map<String, String> capitals = new HashMap<>();
        capitals.put("Afghanistan", "Kabul");
        capitals.put("Albania", "Tirana");
        capitals.put("Algeria", "Algiers");
        capitals.put("Angola", "Luanda");
        capitals.put("Argentina", "Buenos Aires");
        capitals.put("Australia", "Canberra");
        capitals.put("Austria", "Vienna");
        capitals.put("Azerbaijan", "Baku");
        capitals.put("Bangladesh", "Dhaka");
        capitals.put("Belarus", "Minsk");
        capitals.put("Belgium", "Brussels");
        capitals.put("Benin", "Porto-Novo");
        capitals.put("Bolivia", "La Paz");
        capitals.put("Brazil", "Brasilia");
        capitals.put("Bulgaria", "Sofia");
        capitals.put("Cambodia", "Phnom Penh");
        capitals.put("Canada", "Ottawa");
        capitals.put("Chad", "N'Djamena");
        capitals.put("Chile", "Santiago");
        capitals.put("China", "Beijing");
        capitals.put("Colombia", "Bogota");
        capitals.put("Costa Rica", "San Jose");
        capitals.put("Croatia", "Zagreb");
        capitals.put("Cuba", "Havana");
        capitals.put("Czech Republic", "Prague");
        capitals.put("Democratic Republic of the Congo", "Kinshasa");
        capitals.put("Denmark", "Copenhagen");
        capitals.put("Ecuador", "Quito");
        capitals.put("Egypt", "Cairo");
        capitals.put("El Salvador", "San Salvador");
        capitals.put("Estonia", "Tallinn");
        capitals.put("Ethiopia", "Addis Ababa");
        capitals.put("Finland", "Helsinki");
        capitals.put("France", "Paris");
        capitals.put("Georgia", "Tbilisi");
        capitals.put("Germany", "Berlin");
        capitals.put("Ghana", "Accra");
        capitals.put("Greece", "Athens");
        capitals.put("Guatemala", "Guatemala City");
        capitals.put("Guinea", "Conakry");
        capitals.put("Honduras", "Tegucigalpa");
        capitals.put("Hungary", "Budapest");
        capitals.put("Iceland", "Reykjavik");
        capitals.put("India", "New Delhi");
        capitals.put("Indonesia", "Jakarta");
        capitals.put("Iran", "Tehran");
        capitals.put("Iraq", "Baghdad");
        capitals.put("Ireland", "Dublin");
        capitals.put("Israel", "Jerusalem");
        capitals.put("Italy", "Rome");
        capitals.put("Jamaica", "Kingston");
        capitals.put("Japan", "Tokyo");
        capitals.put("Jordan", "Amman");
        capitals.put("Kazakhstan", "Astana");
        capitals.put("Kenya", "Nairobi");
        capitals.put("Kuwait", "Kuwait City");
        capitals.put("Latvia", "Riga");
        capitals.put("Lebanon", "Beirut");
        capitals.put("Libya", "Tripoli");
        capitals.put("Lithuania", "Vilnius");
        capitals.put("Madagascar", "Antananarivo");
        capitals.put("Malawi", "Lilongwe");
        capitals.put("Malaysia", "Kuala Lumpur");
        capitals.put("Mali", "Bamako");
        capitals.put("Mexico", "Mexico City");
        capitals.put("Morocco", "Rabat");
        capitals.put("Mozambique", "Maputo");
        capitals.put("Nepal", "Kathmandu");
        capitals.put("Netherlands", "Amsterdam");
        capitals.put("New Zealand", "Wellington");
        capitals.put("Nicaragua", "Managua");
        capitals.put("Nigeria", "Abuja");
        capitals.put("Norway", "Oslo");
        capitals.put("Pakistan", "Islamabad");
        capitals.put("Panama", "Panama City");
        capitals.put("Peru", "Lima");
        capitals.put("Philippines", "Manila");
        capitals.put("Poland", "Warsaw");
        capitals.put("Portugal", "Lisbon");
        capitals.put("Romania", "Bucharest");
        capitals.put("Russia", "Moscow");
        capitals.put("Rwanda", "Kigali");
        capitals.put("Saudi Arabia", "Riyadh");
        capitals.put("Senegal", "Dakar");
        capitals.put("Serbia", "Belgrade");
        capitals.put("Republic of Serbia", "Belgrade");
        capitals.put("Singapore", "Singapore");
        capitals.put("Slovakia", "Bratislava");
        capitals.put("Slovenia", "Ljubljana");
        capitals.put("South Africa", "Pretoria");
        capitals.put("South Korea", "Seoul");
        capitals.put("Spain", "Madrid");
        capitals.put("Sri Lanka", "Colombo");
        capitals.put("Sudan", "Khartoum");
        capitals.put("Sweden", "Stockholm");
        capitals.put("Switzerland", "Bern");
        capitals.put("Syria", "Damascus");
        capitals.put("Taiwan", "Taipei");
        capitals.put("Thailand", "Bangkok");
        capitals.put("Tunisia", "Tunis");
        capitals.put("Turkey", "Ankara");
        capitals.put("Uganda", "Kampala");
        capitals.put("Ukraine", "Kyiv");
        capitals.put("United Arab Emirates", "Abu Dhabi");
        capitals.put("United Kingdom", "London");
        capitals.put("United States of America", "Washington, D.C.");
        capitals.put("Uruguay", "Montevideo");
        capitals.put("Venezuela", "Caracas");
        capitals.put("Vietnam", "Hanoi");
        capitals.put("Yemen", "Sana'a");
        capitals.put("Zambia", "Lusaka");
        capitals.put("Zimbabwe", "Harare");
        return capitals;
    }

    private String escapeJSON(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
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
        long timestamp = System.currentTimeMillis();
        System.out.println("Context menu on " + country + " at " + timestamp + " ms");
        long totalPopulation = c.getPopulation();
        for (Country neighbor : neighbors) {
            totalPopulation += neighbor.getPopulation();
        }
        sendMessageToUser("Total population of " + country + " and its neighbors: " + formatPopulation(totalPopulation));
    }

    @Override
    public void handleClick(String country) {
        Country c = graph.getCountryByName(country);
        if (c == null) return;

        clearCountryColors();
        addCountryColor(c.getName(), "cyan");
        startElevationLoadIfNeeded(c);
        long timestamp = System.currentTimeMillis();
        System.out.println("Clicked on " + country + " at " + timestamp + " ms");
        sendMessageToUser(country + "\n"
                + "Population: " + formatPopulation(c.getPopulation()) + "\n"
                + "Elevation: " + getElevationMessage(c));
    }

    private void startElevationLoadIfNeeded(Country country) {
        String countryName = country.getName();
        synchronized (countriesWithElevation) {
            if (countriesWithElevation.contains(countryName) || countriesLoadingElevation.contains(countryName)) {
                return;
            }
            countriesLoadingElevation.add(countryName);
        }

        new Thread(() -> {
            boolean loaded = loadElevation(country);
            synchronized (countriesWithElevation) {
                countriesLoadingElevation.remove(countryName);
                if (loaded) {
                    countriesWithElevation.add(countryName);
                }
            }
            if (loaded) {
                saveCountryDataCache(graph.getCountrySet());
                sendMessageToUser(countryName + " elevation loaded: " + getElevationMessage(country));
            } else {
                sendMessageToUser(countryName + " elevation is still unavailable. Try again in a few seconds.");
            }
        }, "ElevationLoader-" + countryName).start();
    }

    private String getElevationMessage(Country country) {
        synchronized (countriesWithElevation) {
            if (!countriesWithElevation.contains(country.getName())) {
                return "loading...";
            }
        }
        return String.format("%.0f", country.getMinElevation()) + "m - "
                + String.format("%.0f", country.getMaxElevation()) + "m";
    }

    private boolean loadElevation(Country country) {
        double[] coords = countryCoordinates.get(country.getName());
        if (coords == null) {
            return false;
        }

        double[][] samplePoints = getElevationSamplePoints(coords);

        double[] elevations = ElevationAPI.getElevations(samplePoints);
        if (elevations.length == 0) {
            return false;
        }

        double minElevation = Double.MAX_VALUE;
        double maxElevation = -Double.MAX_VALUE;
        for (double elevation : elevations) {
            if (Double.isNaN(elevation)) {
                continue;
            }
            minElevation = Math.min(minElevation, elevation);
            maxElevation = Math.max(maxElevation, elevation);
        }

        if (minElevation == Double.MAX_VALUE) {
            return false;
        }

        country.setMinElevation(Math.max(-100, minElevation));
        country.setMaxElevation(Math.min(9000, maxElevation));
        return true;
    }

    private double[][] getElevationSamplePoints(double[] coords) {
        return new double[][] {
            coords,
            {coords[0] + 1, coords[1]},
            {coords[0] - 1, coords[1]},
            {coords[0], coords[1] + 1},
            {coords[0], coords[1] - 1},
            {coords[0] + 1, coords[1] + 1},
            {coords[0] + 1, coords[1] - 1},
            {coords[0] - 1, coords[1] + 1},
            {coords[0] - 1, coords[1] - 1}
        };
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
        
        for (Country country : graph.getCountrySet()) {
            synchronized (countriesWithElevation) {
                if (!countriesWithElevation.contains(country.getName())) {
                    continue;
                }
            }

            if (country.getMaxElevation() < seaLevel) {
                underwater.add(country);
                addCountryColor(country.getName(), "#e53935");
                totalDisplacedPopulation += country.getPopulation();
            }
            else if (country.getMinElevation() < seaLevel && country.getMaxElevation() >= seaLevel) {
                coastal.add(country);
                addCountryColor(country.getName(), "#ec4899");
            }
        }

        synchronized (submergedCountryNames) {
            submergedCountryNames.clear();
            for (Country country : underwater) {
                submergedCountryNames.add(country.getName());
            }
        }
        graph.writeActiveBordersExcluding(underwater);
        
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
