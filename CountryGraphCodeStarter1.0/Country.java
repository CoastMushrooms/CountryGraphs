import java.util.Objects;

public class Country {
    private String name;
    private long population; // placeholder for later
    private double gdp; // placeholder
    private double area; // placeholder
    private String capital; // placeholder
    private String continent; // placeholder
    private double minElevation; // minimum elevation in meters
    private double maxElevation; // maximum elevation in meters

    public Country(String name) {
        this.name = name;
        this.population = 0;
        this.gdp = 0.0;
        this.area = 0.0;
        this.capital = "";
        this.continent = "";
        this.minElevation = 0.0;
        this.maxElevation = 0.0;
    }

    // Accessor methods
    public String getName() {
        return name;
    }

    public long getPopulation() {
        return population;
    }

    public double getGdp() {
        return gdp;
    }

    public double getArea() {
        return area;
    }

    public String getCapital() {
        return capital;
    }

    public String getContinent() {
        return continent;
    }

    public double getMinElevation() {
        return minElevation;
    }

    public double getMaxElevation() {
        return maxElevation;
    }

    public String getInfoSummary() {
        String capitalDisplay = capital.isBlank() ? "unknown" : capital;
        String continentDisplay = continent.isBlank() ? "unknown" : continent;
        return "Country: " + name
                + ", Capital: " + capitalDisplay
                + ", Continent: " + continentDisplay
                + ", Population: " + population
                + ", GDP: " + gdp
                + ", Area: " + area;
    }

    // Mutator methods
    public void setPopulation(long population) {
        this.population = population;
    }

    public void setGdp(double gdp) {
        this.gdp = gdp;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public void setCapital(String capital) {
        this.capital = capital;
    }

    public void setContinent(String continent) {
        this.continent = continent;
    }

    public void setMinElevation(double minElevation) {
        this.minElevation = minElevation;
    }

    public void setMaxElevation(double maxElevation) {
        this.maxElevation = maxElevation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Country country = (Country) o;
        return Objects.equals(name, country.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
