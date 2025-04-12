package ec.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.io.File;

@WebServlet("/health")
public class HealthCheckServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        
        String modelPath = "C:/enterprise/workspace/630project/tmp/model/best.model";
        File modelFile = new File(modelPath);
        
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
            .add("status", "UP")
            .add("serverTime", System.currentTimeMillis())
            .add("modelExists", modelFile.exists())
            .add("modelPath", modelPath);
        
        if (modelFile.exists()) {
            jsonBuilder.add("modelSize", modelFile.length());
            jsonBuilder.add("modelLastModified", modelFile.lastModified());
        }
        
        response.getWriter().write(jsonBuilder.build().toString());
    }
}