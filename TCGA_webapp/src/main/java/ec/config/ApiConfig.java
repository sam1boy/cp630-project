package ec.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApiConfig {
    
    private static Properties properties;
    
    static {
        properties = new Properties();
        try (InputStream input = ApiConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                properties.setProperty("gemini.api.key", "");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            properties.setProperty("gemini.api.key", "");
        }
    }
    
    public static String getGeminiApiKey() {
        return properties.getProperty("gemini.api.key");
    }
}