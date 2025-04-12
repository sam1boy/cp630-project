package ec.controller;

import ec.model.CancerPredictor;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/predict")
public class PredictionServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
	private CancerPredictor predictor;
    
    @Override
    public void init() throws ServletException {
        String modelPath = "C:/enterprise/workspace/630project/tmp/model/model";
        try {
            predictor = new CancerPredictor(modelPath);
            getServletContext().log("Model loaded successfully from: " + modelPath);
        } catch (Exception e) {
            getServletContext().log("Error loading model: " + e.getMessage(), e);
            throw new ServletException("Failed to load prediction model", e);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Parse input parameters - including the two new GDSC tissue descriptors
        String wes = request.getParameter("wes");
        String cna = request.getParameter("cna");
        String geneExpression = request.getParameter("geneExpression");
        String methylation = request.getParameter("methylation");
        String drugResponse = request.getParameter("drugResponse");
        String gdscTissue1 = request.getParameter("gdscTissue1");
        String gdscTissue2 = request.getParameter("gdscTissue2");
        String msi = request.getParameter("msi");
        String screenMedium = request.getParameter("screenMedium");
        String growthProperties = request.getParameter("growthProperties");
        
        // Prepare data for prediction
        Map<String, String> features = new HashMap<>();
        features.put("Whole Exome Sequencing (WES)", wes);
        features.put("Copy Number Alterations (CNA)", cna);
        features.put("Gene Expression", geneExpression);
        features.put("Methylation", methylation);
        features.put("Drug Response", drugResponse);
        features.put("GDSC Tissue descriptor 1", gdscTissue1);
        features.put("GDSC Tissue descriptor 2", gdscTissue2);
        features.put("Microsatellite instability Status (MSI)", msi);
        features.put("Screen Medium", screenMedium);
        features.put("Growth Properties", growthProperties);
        
        response.setContentType("application/json");
        try {
            // Make prediction
            String prediction = predictor.predict(features);
            double confidence = predictor.getPredictionConfidence();
            
            // Create JSON response
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add("success", true)
                .add("prediction", prediction)
                .add("confidence", confidence);
            
            response.getWriter().write(jsonBuilder.build().toString());
        } catch (Exception e) {
            getServletContext().log("Error making prediction: " + e.getMessage(), e);
            
            // Return error response
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add("success", false)
                .add("error", e.getMessage());
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(jsonBuilder.build().toString());
        }
    }
}