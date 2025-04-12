package ec.model;

import ec.util.ModelLoader;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CancerPredictor {
    
    private Classifier model;
    private ArrayList<Attribute> attributes;
    private Map<String, List<String>> attributeValues;
    private Map<String, Integer> attributeIndexMap; // For fast attribute lookups
    private double lastPredictionConfidence = 0.0;
    
    public CancerPredictor(String modelPath) throws Exception {
        // Load the model
        model = ModelLoader.loadModel(modelPath);
        
        // Define the attributes (must match the model's training data)
        attributes = new ArrayList<>();
        attributeValues = new HashMap<>();
        attributeIndexMap = new HashMap<>();
        
        // Initialize with exact values as specified
        initializeAttributes(modelPath);
    }
    
    private void initializeAttributes(String modelPath) {
        // Set up attribute values for each feature as specified
        
        Map<String, List<String>> columnCategories = ModelLoader.loadCategoriesFromCSV();
        attributeValues = new HashMap<>();
        List<String> columns = Arrays.asList(
        	    "Whole Exome Sequencing (WES)",
        	    "Copy Number Alterations (CNA)",
        	    "Gene Expression",
        	    "Methylation",
        	    "Drug Response",
        	    "GDSC Tissue descriptor 1",
        	    "GDSC Tissue descriptor 2",
        	    "Microsatellite instability Status (MSI)",
        	    "Screen Medium",
        	    "Growth Properties",
        	    "Cancer Type (matching TCGA label)"
    	);

    	int index = 0;
    	for (String column : columns) {
    	    if (columnCategories.containsKey(column)) {
    	        attributeValues.put(column, columnCategories.get(column));
    	        attributes.add(createAttribute(column, index++));
    	    }
    	}
    }
    
    // Helper method to create attribute and store its index
    private Attribute createAttribute(String name, int index) {
        Attribute attr = new Attribute(name, attributeValues.get(name));
        attributeIndexMap.put(name, index);
        return attr;
    }
    
    public String predict(Map<String, String> features) throws Exception {
        // Create dataset structure
        Instances dataStructure = new Instances("TestInstances", attributes, 0);
        dataStructure.setClassIndex(attributes.size() - 1); // Set last attribute as class
        
        // Create instance with the feature values
        Instance instance = new DenseInstance(attributes.size());
        instance.setDataset(dataStructure);
        
        // Fill with default values first to prevent any null values
        for (int i = 0; i < attributes.size() - 1; i++) { // Skip class attribute
            instance.setValue(i, attributeValues.get(attributes.get(i).name()).indexOf("unknown"));
        }
        
        // Set attribute values from features map
        for (Map.Entry<String, String> entry : features.entrySet()) {
            String attrName = entry.getKey();
            String attrValue = entry.getValue();
            
            // Make sure we handle unknown values
            if (attrValue == null || attrValue.isEmpty()) {
                attrValue = "unknown";
            }
            
            // Use our index map to quickly find the attribute index
            Integer attrIndex = attributeIndexMap.get(attrName);
            
            if (attrIndex != null && attrIndex < attributes.size()) {
                Attribute attr = attributes.get(attrIndex);
                List<String> values = attributeValues.get(attrName);
                
                // Check if entered value is valid - this applies to all fields including tissue descriptors
                if (values.contains(attrValue)) {
                    instance.setValue(attrIndex, values.indexOf(attrValue));
                } else {
                    // Use unknown for values not in our list
                    System.out.println("Warning: Unknown value '" + attrValue + "' for attribute '" + 
                                      attrName + "'. Using 'unknown' instead.");
                    instance.setValue(attrIndex, values.indexOf("unknown"));
                }
            }
        }
        
        // Make the prediction
        double[] distribution = model.distributionForInstance(instance);
        int predictionIndex = (int) model.classifyInstance(instance);
        
        // Get confidence
        if (distribution.length > 0) {
            lastPredictionConfidence = distribution[predictionIndex] * 100.0;
        }
        
        // Return the predicted class name
        Attribute classAttr = dataStructure.classAttribute();
        return classAttr.value(predictionIndex);
    }
    
    public double getPredictionConfidence() {
        return lastPredictionConfidence;
    }
}