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
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
	        throws ServletException, IOException {
	    
	    String cancerType = request.getParameter("cancerType");
	    getServletContext().log("TreatmentInfoServlet called with cancerType: " + cancerType);
	    
	    response.setContentType("application/json");
	    
	    if (cancerType == null || cancerType.isEmpty() || cancerType.equalsIgnoreCase("unknown")) {
	        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
	            .add("success", true)
	            .add("cancerType", cancerType == null ? "unknown" : cancerType)
	            .add("treatmentInfo", (String)null);
	        
	        response.getWriter().write(jsonBuilder.build().toString());
	        return;
	    }
	    
	    try {
	        getServletContext().log("Calling GeminiService.getCancerTreatmentInfo...");
	        String treatmentInfo = GeminiService.getCancerTreatmentInfo(cancerType);
	        
	        if (treatmentInfo != null) {
	            getServletContext().log("Treatment info received, length: " + treatmentInfo.length());
	        } else {
	            getServletContext().log("No treatment info returned for this cancer type");
	        }
	        
	        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
	            .add("success", true)
	            .add("cancerType", cancerType);
	        
	        if (treatmentInfo != null) {
	            jsonBuilder.add("treatmentInfo", treatmentInfo);
	        } else {
	            jsonBuilder.addNull("treatmentInfo");
	        }
	        
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