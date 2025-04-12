package ec.util;

import weka.classifiers.Classifier;
import weka.core.SerializationHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelLoader {
    
    public static Classifier loadModel(String path) throws Exception {
        File modelFile = new File(path);
        
        if (!modelFile.exists()) {
            throw new IllegalArgumentException("Model file not found at: " + modelFile.getAbsolutePath());
        }
        
        try {
            return (Classifier) SerializationHelper.read(modelFile.getAbsolutePath());
        } catch (Exception e) {
            throw new Exception("Error loading model: " + e.getMessage(), e);
        }
    }
    
    /**
     * Loads the CSV file from the specified path and retrieves the column categories
     * Adds them to the appropriate lists in attributeValues map
     * @return 
     */
    public static Map<String, List<String>> loadCategoriesFromCSV() {
    	String path = "C:/enterprise/workspace/630project/data/Cell_Lines_Details.csv";
        Map<String, List<String>> columnCategories = new HashMap<>();
        
        System.out.println("Attempting to load categories from: " + path);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            // Read header
            String headerLine = reader.readLine();
            if (headerLine == null) {
                System.err.println("CSV file is empty");
                return columnCategories;
            }
            
            // Parse header to get column names
            String[] headers = parseCSVLine(headerLine);
            
            // Initialize category lists for each column
            for (String header : headers) {
                columnCategories.put(header, new ArrayList<>());
                // Add unknown as default category
                columnCategories.get(header).add("unknown");
            }
            
            // Read data rows and collect unique values for each column
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = parseCSVLine(line);
                
                for (int i = 0; i < Math.min(values.length, headers.length); i++) {
                    String value = values[i].trim();
                    if (!value.isEmpty() && !columnCategories.get(headers[i]).contains(value)) {
                        columnCategories.get(headers[i]).add(value);
                    }
                }
            }
            
            // Print found categories for each column
            System.out.println("\n=== COLUMN CATEGORIES FROM CSV FILE ===");
            for (Map.Entry<String, List<String>> entry : columnCategories.entrySet()) {
                System.out.println("\nColumn: " + entry.getKey());
                System.out.print("Categories: [");
                List<String> cats = entry.getValue();
                for (int i = 0; i < cats.size(); i++) {
                    System.out.print(cats.get(i));
                    if (i < cats.size() - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println("]");
            }
            System.out.println("\n=========================================");
            
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            e.printStackTrace();
        }
        
        return columnCategories;
    }

    /**
     * Parses a CSV line handling quotes and commas within fields
     */
    private static String[] parseCSVLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        
        return tokens.toArray(new String[0]);
    }
}