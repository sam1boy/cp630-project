package ec.service;

import ec.config.ApiConfig;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiService {

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    
    public static String getCancerTreatmentInfo(String cancerType) throws IOException {
        String apiKey = ApiConfig.getGeminiApiKey();
        
        if (apiKey == null || apiKey.isEmpty()) {
            return "API key not configured. Please set the Gemini API key in config.properties.";
        }
        
        String prompt = "Provide a concise paragraph (maximum 100 words) about the current treatment approaches for " + 
                        cancerType + " cancer. Include standard treatments and any recent advances. Format as an HTML paragraph.";
        
        String requestBody = Json.createObjectBuilder()
                .add("contents", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("parts", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("text", prompt)))))
                .build().toString();
        
        URL url = new URL(GEMINI_API_URL + "?key=" + apiKey);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000); // 10 seconds timeout
        connection.setReadTimeout(30000); // 30 seconds read timeout
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        
        if (responseCode != 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    errorResponse.append(responseLine.trim());
                }
                System.err.println("Error from Gemini API: " + errorResponse.toString());
                return "Unable to retrieve treatment information at this time.";
            }
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        return extractTextFromResponse(response.toString());
    }
    
    private static String extractTextFromResponse(String responseBody) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(responseBody))) {
            JsonObject jsonObject = jsonReader.readObject();
            return jsonObject
                    .getJsonArray("candidates")
                    .getJsonObject(0)
                    .getJsonObject("content")
                    .getJsonArray("parts")
                    .getJsonObject(0)
                    .getString("text");
        } catch (Exception e) {
            System.err.println("Error parsing Gemini API response: " + e.getMessage());
            return "Unable to parse treatment information.";
        }
    }
}