import java.io.*;
import java.util.*;

public class CountryGraph {
    private Map<Country, Set<Country>> adjacencyList;
    private Map<String, Country> countryMap;
    private Set<Country> countries;

    public CountryGraph() {
        adjacencyList = new HashMap<>();
        countryMap = new HashMap<>();
        countries = new HashSet<>();
        loadData();
    }

    private void loadData() {
        try (BufferedReader br = new BufferedReader(new FileReader("CountryBorders.CSV"))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String name1 = parts[1].trim();
                    String name2 = parts[3].trim();
                    Country c1 = countryMap.computeIfAbsent(name1, Country::new);
                    Country c2 = countryMap.computeIfAbsent(name2, Country::new);
                    countries.add(c1);
                    countries.add(c2);
                    adjacencyList.computeIfAbsent(c1, k -> new HashSet<>()).add(c2);
                    adjacencyList.computeIfAbsent(c2, k -> new HashSet<>()).add(c1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Graph methods
    public Set<Country> getCountrySet() {
        return new HashSet<>(countries);
    }

    public Set<Border> getBorderSet() {
        Set<Border> borders = new HashSet<>();
        for (Country country : adjacencyList.keySet()) {
            for (Country neighbor : adjacencyList.get(country)) {
                if (country.getName().compareTo(neighbor.getName()) < 0) { // to avoid duplicates
                    borders.add(new Border(country, neighbor));
                }
            }
        }
        return borders;
    }

    public boolean shareBorder(Country a, Country b) {
        return adjacencyList.containsKey(a) && adjacencyList.get(a).contains(b);
    }

    public Set<Country> getNeighbors(Country a) {
        return adjacencyList.getOrDefault(a, new HashSet<>());
    }

    public boolean isConnected(Country a, Country b) {
        if (!countries.contains(a) || !countries.contains(b)) return false;
        Set<Country> visited = new HashSet<>();
        Queue<Country> queue = new LinkedList<>();
        queue.add(a);
        visited.add(a);
        while (!queue.isEmpty()) {
            Country current = queue.poll();
            if (current.equals(b)) return true;
            for (Country neighbor : adjacencyList.getOrDefault(current, new HashSet<>())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    public List<Country> findPath(Country start, Country end) {
        if (!countries.contains(start) || !countries.contains(end)) return new ArrayList<>();
        Map<Country, Country> parent = new HashMap<>();
        Set<Country> visited = new HashSet<>();
        Queue<Country> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        parent.put(start, null);
        boolean found = false;
        while (!queue.isEmpty()) {
            Country current = queue.poll();
            if (current.equals(end)) {
                found = true;
                break;
            }
            for (Country neighbor : adjacencyList.getOrDefault(current, new HashSet<>())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    parent.put(neighbor, current);
                }
            }
        }
        if (!found) return new ArrayList<>();
        List<Country> path = new ArrayList<>();
        for (Country at = end; at != null; at = parent.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    public int getDistance(Country a, Country b) {
        List<Country> path = findPath(a, b);
        return path.isEmpty() ? -1 : path.size() - 1;
    }

    public Set<Country> getWithinRadius(Country start, int radius) {
        if (!countries.contains(start) || radius < 0) return new HashSet<>();
        Set<Country> visited = new HashSet<>();
        Queue<Country> queue = new LinkedList<>();
        Map<Country, Integer> distance = new HashMap<>();
        queue.add(start);
        visited.add(start);
        distance.put(start, 0);
        while (!queue.isEmpty()) {
            Country current = queue.poll();
            int dist = distance.get(current);
            if (dist >= radius) continue;
            for (Country neighbor : adjacencyList.getOrDefault(current, new HashSet<>())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    distance.put(neighbor, dist + 1);
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }

    public Country getCountryByName(String name) {
        return countryMap.get(name);
    }

    public static class Border {
        private Country c1, c2;

        public Border(Country a, Country b) {
            if (a.getName().compareTo(b.getName()) < 0) {
                c1 = a;
                c2 = b;
            } else {
                c1 = b;
                c2 = a;
            }
        }

        public Country getC1() {
            return c1;
        }

        public Country getC2() {
            return c2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Border border = (Border) o;
            return Objects.equals(c1, border.c1) && Objects.equals(c2, border.c2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(c1, c2);
        }

        @Override
        public String toString() {
            return c1.getName() + "-" + c2.getName();
        }
    }
}