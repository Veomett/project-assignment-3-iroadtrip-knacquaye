import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*;
import java.util.*;

public class IRoadTrip {

    public IRoadTrip(String[] args) {
        // Check if the correct number of files are provided
        if (args.length != 3) {
            System.out.println("Error: Incorrect number of input files. Expected 3 files.");
            System.exit(1);
        }
        String bordersFile = args[0];
        String capdistFile = args[1];
        String stateNameFile = args[2];

        // Read and process each file
        try {
            processBordersFile(bordersFile);
            processCapdistFile(capdistFile);
            processStateNameFile(stateNameFile);
        } catch (IOException e) {
            System.err.println("Error reading files: " + e.getMessage());
            System.exit(1);
        }
    }


    private Map<String, Map<String, Integer>> countryBorders;
    private void processBordersFile(String filename) throws IOException {
        countryBorders = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" = ");
                if (parts.length < 2) continue; // Skip if there's no border information

                String country = parts[0].trim();
                Map<String, Integer> borders = new HashMap<>();

                String[] borderParts = parts[1].split("; ");
                for (String border : borderParts) {
                    String[] borderInfo = border.trim().split(" km")[0].split(" ");
                    String borderCountry = String.join(" ", Arrays.copyOf(borderInfo, borderInfo.length - 1)).trim();
                    String borderLengthStr = borderInfo[borderInfo.length - 1].replaceAll("[^0-9]", ""); // Remove non-numeric characters

                    if (!borderLengthStr.isEmpty()) {
                        try {
                            int borderLength = Integer.parseInt(borderLengthStr);
                            borders.put(borderCountry, borderLength);
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing border length for " + country + " and " + borderCountry);
                        }
                    }
                }
                countryBorders.put(country, borders);

            }
        }
    }


    // Map to store distances between capitals
    private Map<String, Map<String, Integer>> capitalDistances;
    private void processCapdistFile(String filename) throws IOException {
        capitalDistances = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine(); // Read and skip the header line
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 6) continue; // Skip lines that don't have the expected number of fields

                String countryACode = parts[1].trim();
                String countryBCode = parts[3].trim();
                int distance = Integer.parseInt(parts[4].trim()); // Distance in km

                // For country A to country B
                Map<String, Integer> mapForCountryA = capitalDistances.get(countryACode);
                if (mapForCountryA == null) {
                    mapForCountryA = new HashMap<>();
                    capitalDistances.put(countryACode, mapForCountryA);
                }
                mapForCountryA.put(countryBCode, distance);

                // For country B to country A
                Map<String, Integer> mapForCountryB = capitalDistances.get(countryBCode);
                if (mapForCountryB == null) {
                    mapForCountryB = new HashMap<>();
                    capitalDistances.put(countryBCode, mapForCountryB);
                }
                mapForCountryB.put(countryACode, distance);
            }
        }
    }

    // Map to store country names
   private Map<String, String> countryNames;
    private void processStateNameFile(String filename) throws IOException {
        countryNames = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine(); // Read and skip the header line
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length < 5) continue; // Skip lines that don't have enough data

                String stateId = parts[1].trim(); // Encoded country name (state ID)
                String countryName = parts[2].trim(); // Decoded country name


                countryNames.put(countryName,stateId);
            }
        }
    }



    public int getDistance(String country1, String country2) {
        // Check if both countries exist and share a border
        if (!countryBorders.containsKey(country1) || !countryBorders.containsKey(country2) ||
                !countryBorders.get(country1).containsKey(country2)) {
            System.err.println("No shared border or country not found for: " + country1 + " and " + country2);
            return -1; // Return -1 if they do not share a border or do not exist
        }
        // Retrieve the country codes from the countryNames map
        String country1Code = countryNames.get(country1);
        //System.out.println(country1Code);
        String country2Code = countryNames.get(country2);
       // System.out.println(country2Code);

        // Check if country codes are valid and if distance data is available
        if (country1Code == null || country2Code == null || !capitalDistances.containsKey(country1Code) || !capitalDistances.get(country1Code).containsKey(country2Code)) {
            System.err.println("Invalid country codes or no distance data for: " + country1 + " and " + country2);
            return -1; // Return -1 if codes are invalid or distance data is not available
        }
        // Return the distance between the capitals of the two countries
        return capitalDistances.get(country1Code).get(country2Code);
    }

    public List<String> findPath(String country1, String country2) {
        // Check if either of the countries does not exist in the map
        if (!countryBorders.containsKey(country1) || !countryBorders.containsKey(country2)) {
            return new ArrayList<>(); // Return empty list if a country doesn't exist
        }

        // Initialize data structures for Dijkstra's Algorithm
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(Node::getDistance));
        Set<String> visited = new HashSet<>();

        // Set initial distances to all countries as infinity, except the starting country
        for (String country : countryBorders.keySet()) {
            distances.put(country, Integer.MAX_VALUE);
        }
        distances.put(country1, 0);
        priorityQueue.add(new Node(country1, 0));

        // Implementing Dijkstra's Algorithm to find the shortest path
        while (!priorityQueue.isEmpty()) {
            Node currentNode = priorityQueue.poll();
            String currentCountry = currentNode.getCountry();


            if (!visited.add(currentCountry)) {
                continue;
            }

            if (currentCountry.equals(country2)) {
                break;
            }

            // Retrieve the country code for the current country
            String currentCountryCode = countryNames.get(currentCountry);
            if (currentCountryCode == null) {
                continue; // Skip if the country code is not found
            }

            for (String adjacentCountry : countryBorders.get(currentCountry).keySet()) {
                String adjacentCountryCode = countryNames.get(adjacentCountry);
                if (adjacentCountryCode == null) {
                    continue; // Skip if the adjacent country code is not found
                }

                // Use capital distances for calculation
                if (capitalDistances.containsKey(currentCountryCode) &&
                        capitalDistances.get(currentCountryCode).containsKey(adjacentCountryCode)) {

                    int capitalDistance = capitalDistances.get(currentCountryCode).get(adjacentCountryCode);
                    int totalDistance = distances.get(currentCountry) + capitalDistance;

                    if (totalDistance < distances.getOrDefault(adjacentCountry, Integer.MAX_VALUE)) {
                        distances.put(adjacentCountry, totalDistance);
                        predecessors.put(adjacentCountry, currentCountry);
                        priorityQueue.add(new Node(adjacentCountry, totalDistance));
                    }
                }
            }
        }

        // Constructing the path from the start country to the destination
        return buildPath(predecessors, country1, country2);
    }


    /**
     * Builds the path from the predecessors map.
     *
     * @param predecessors A map of predecessors for each country on the path.
     * @param start The starting country.
     * @param end The destination country.
     * @return A list representing the path.
     */
    private List<String> buildPath(Map<String, String> predecessors, String start, String end) {
        LinkedList<String> path = new LinkedList<>();
        for (String at = end; at != null; at = predecessors.get(at)) {
            path.addFirst(at);
        }

        if (path.isEmpty() || !path.getFirst().equals(start)) {
            return new ArrayList<>(); // Return an empty list if no path is found
        }

        List<String> formattedPath = new ArrayList<>();
        String from = start;
        for (String to : path) {
            if (!from.equals(to)) { // Dont add a step for the start country itself
                int distance = countryBorders.get(from).get(to);
                formattedPath.add(from + " --> " + to + " (" + distance + " km)");
                from = to;
            }
        }

        return formattedPath;
    }


    private static class Node {
        private final String country;
        private final int distance;

        public Node(String country, int distance) {
            this.country = country;
            this.distance = distance;
        }

        public String getCountry() {
            return country;
        }

        public int getDistance()     {
            return distance;
        }
    }


    public void acceptUserInput() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Enter the name of the first country (type EXIT to quit): ");
            String country1 = scanner.nextLine().trim();

            if (country1.equalsIgnoreCase("EXIT")) {
                break;
            }

            if (!countryBorders.containsKey(country1)) {
                System.out.println("Invalid country name. Please enter a valid country name.");
                continue;
            }

            System.out.print("Enter the name of the second country (type EXIT to quit): ");
            String country2 = scanner.nextLine().trim();

            if (country2.equalsIgnoreCase("EXIT")) {
                break;
            }

            if (!countryBorders.containsKey(country2)) {
                System.out.println("Invalid country name. Please enter a valid country name.");
                continue;
            }

            List<String> path = findPath(country1, country2);
            if (path.isEmpty()) {
                System.out.println("No path found between " + country1 + " and " + country2 + ".");
            } else {
                System.out.println("Route from " + country1 + " to " + country2 + ":");
                for (String step : path) {
                    System.out.println("* " + step);
                }
            }
        }

        scanner.close();
    }
    public static void main(String[] args) {
        IRoadTrip roadTrip = new IRoadTrip(args);
        roadTrip.acceptUserInput();

    }


}


