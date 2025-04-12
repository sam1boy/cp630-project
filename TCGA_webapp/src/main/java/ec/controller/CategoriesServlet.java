package ec.controller;

import ec.util.ModelLoader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/categories")
public class CategoriesServlet extends HttpServlet {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        
        try {
            // Load categories from CSV file
            Map<String, List<String>> columnCategories = ModelLoader.loadCategoriesFromCSV();
            
            // Build JSON response
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
            
            for (Map.Entry<String, List<String>> entry : columnCategories.entrySet()) {
                JsonArrayBuilder categoryArray = Json.createArrayBuilder();
                
                for (String value : entry.getValue()) {
                    categoryArray.add(value);
                }
                
                // The key in the JSON will be a simplified version of the column name for easy use in JavaScript
                String simplifiedKey = getSimplifiedKey(entry.getKey());
                jsonBuilder.add(simplifiedKey, categoryArray);
            }
            
            response.getWriter().write(jsonBuilder.build().toString());
            
        } catch (Exception e) {
            getServletContext().log("Error retrieving categories: " + e.getMessage(), e);
            
            // Return error response
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add("error", e.getMessage());
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(jsonBuilder.build().toString());
        }
    }
    
    // Helper method to convert column names to JavaScript-friendly keys
    private String getSimplifiedKey(String columnName) {
        // Map the column names to the form field IDs
        switch (columnName) {
            case "Whole Exome Sequencing (WES)":
                return "wes";
            case "Copy Number Alterations (CNA)":
                return "cna";
            case "Gene Expression":
                return "geneExpression";
            case "Methylation":
                return "methylation";
            case "Drug Response":
                return "drugResponse";
            case "GDSC Tissue descriptor 1":
                return "gdscTissue1";
            case "GDSC Tissue descriptor 2":
                return "gdscTissue2";
            case "Microsatellite instability Status (MSI)":
                return "msi";
            case "Screen Medium":
                return "screenMedium";
            case "Growth Properties":
                return "growthProperties";
            case "Cancer Type (matching TCGA label)":
                return "cancerType";
            default:
                // Convert to camelCase as a fallback - fixed version
                return toCamelCase(columnName);
        }
    }
    
    // Helper method to convert strings to camelCase
    private String toCamelCase(String input) {
        // First make everything lowercase
        String result = input.toLowerCase();
        
        // Then use regex to find patterns and convert to camelCase
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9]+(.)");
        Matcher matcher = pattern.matcher(result);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
}