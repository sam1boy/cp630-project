package ec.controller;

import ec.service.GeminiService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.IOException;

@WebServlet("/treatment-info")
public class TreatmentInfoServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String cancerType = request.getParameter("cancerType");
        response.setContentType("application/json");
        
        if (cancerType == null || cancerType.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add("success", false)
                .add("error", "Cancer type parameter is required");
            
            response.getWriter().write(jsonBuilder.build().toString());
            return;
        }
        
        try {
            String treatmentInfo = GeminiService.getCancerTreatmentInfo(cancerType);
            
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add("success", true)
                .add("cancerType", cancerType)
                .add("treatmentInfo", treatmentInfo);
            
            response.getWriter().write(jsonBuilder.build().toString());
            
        } catch (Exception e) {
            getServletContext().log("Error retrieving treatment info: " + e.getMessage(), e);
            
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add("success", false)
                .add("error", "Failed to retrieve treatment information: " + e.getMessage());
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(jsonBuilder.build().toString());
        }
    }
}