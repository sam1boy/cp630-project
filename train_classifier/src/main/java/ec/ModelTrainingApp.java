package ec;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.meta.LogitBoost;  // Changed from AdaBoostM1 to LogitBoost
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Attribute;
import weka.core.SerializationHelper;

import java.io.*;
import java.util.*;

/**
 * Main application class for training and comparing models on the Cell Lines dataset
 */
public class ModelTrainingApp {

    public static void main(String[] args) {
        try {
            System.out.println("Starting Cell Lines ML model training");
            
            // Define paths
            String csvFilePath = locateCSVFile();
            if (csvFilePath == null) {
                System.err.println("Could not locate CSV file. Exiting.");
                return;
            }
            
            // Read and analyze the CSV file
            List<String[]> csvData = readCSVFile(csvFilePath);
            if (csvData.isEmpty() || csvData.size() < 2) {
                System.err.println("CSV file is empty or has insufficient data");
                return;
            }
            
            // Extract column names and display them
            String[] headers = csvData.get(0);
            System.out.println("\nFound " + headers.length + " columns in CSV file:");
            for (int i = 0; i < headers.length; i++) {
                System.out.println((i + 1) + ": " + headers[i]);
            }
            
            // Set target column
            String targetColumn = "Cancer Type (matching TCGA label)";
            if (!containsColumn(headers, targetColumn)) {
                System.err.println("Target column '" + targetColumn + "' not found! Please check the CSV file.");
                return;
            }
            
            System.out.println("\nUsing target column: " + targetColumn);
            
            // Select columns to keep - fixed the missing quote
            List<String> columnsToKeep = new ArrayList<>(Arrays.asList(
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
                targetColumn
            ));
            
            System.out.println("\nSelected columns for model training:");
            for (String col : columnsToKeep) {
                System.out.println("- " + col);
            }
            
            // Convert to WEKA instances, filling empty values with "unknown"
            Instances data = convertToInstances(csvData, targetColumn, columnsToKeep);
            
            // Split data into training and testing sets - changed to 0.7 for 70/30 split
            Instances[] splitData = splitData(data, 0.7);  // Changed from 0.8 to 0.7 for 70/30 split
            Instances trainingData = splitData[0];
            Instances testingData = splitData[1];
            
            // Train models
            System.out.println("\nTraining machine learning models...");
            Map<String, Classifier> models = new HashMap<>();
            models.put("Random Forest", trainRandomForest(trainingData));
            models.put("K-Nearest Neighbors", trainKNN(trainingData));
            models.put("Support Vector Machine", trainSVM(trainingData));
            models.put("XGBoost", trainXGBoost(trainingData));
            
            // Find best model
            Map.Entry<String, Classifier> bestModel = findBestModel(models, testingData);
            
            // Create directory if it doesn't exist - using absolute path
            String modelSavePath = "C:\\enterprise\\workspace\\630project\\tmp\\model";
            File dir = new File(modelSavePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Save the best model with simple name
            String fileName = modelSavePath + File.separator + "model";
            SerializationHelper.write(fileName, bestModel.getValue());
            System.out.println("Model saved to: " + fileName);
            
            System.out.println("Model training completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error in model training: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String locateCSVFile() {
        // Try different potential locations for the CSV file
        String[] file_path = {
            "../data/Cell_Lines_Details.csv",
        };
        
        for (String path : file_path) {
            File file = new File(path);
            if (file.exists()) {
                System.out.println("Found CSV file at: " + file.getAbsolutePath());
                return path;
            }
        }
        
        // If file not found at any of the expected locations, ask the user
        System.out.println("Cell_Lines_Details.csv file not found. Please specify the full path:");
        Scanner scanner = new Scanner(System.in);
        String userPath = scanner.nextLine();
        File userFile = new File(userPath);
        if (userFile.exists()) {
            return userPath;
        }
        
        return null;
    }
    
    private static List<String[]> readCSVFile(String filePath) {
        List<String[]> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Handle CSV parsing with quoted values
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
                
                data.add(tokens.toArray(new String[0]));
            }
            System.out.println("Read " + data.size() + " lines from CSV file");
            
            // Display the first few rows
            int numRowsToShow = Math.min(data.size(), 3);
            System.out.println("\nFirst " + numRowsToShow + " rows of data:");
            for (int i = 0; i < numRowsToShow; i++) {
                System.out.println(Arrays.toString(data.get(i)));
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }
    
    private static boolean containsColumn(String[] headers, String targetColumn) {
        for (String header : headers) {
            if (header.trim().equalsIgnoreCase(targetColumn.trim())) {
                return true;
            }
        }
        return false;
    }
    
    private static Instances convertToInstances(List<String[]> csvData, String targetColumn, List<String> columnsToKeep) {
        String[] headers = csvData.get(0);
        
        // Find indices of columns to keep
        List<Integer> keepIndices = new ArrayList<>();
        for (String column : columnsToKeep) {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase(column.trim())) {
                    keepIndices.add(i);
                    break;
                }
            }
        }
        
        // Find the target column index
        int targetIndex = -1;
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(targetColumn.trim())) {
                targetIndex = i;
                for (int j = 0; j < keepIndices.size(); j++) {
                    if (keepIndices.get(j) == targetIndex) {
                        targetIndex = j; // Adjust target index for the new dataset
                        break;
                    }
                }
                break;
            }
        }
        
        // Create attributes for selected columns
        ArrayList<Attribute> attributes = new ArrayList<>();
        Map<Integer, List<String>> uniqueValues = new HashMap<>();
        
        // Initialize unique values lists
        for (int i = 0; i < keepIndices.size(); i++) {
            uniqueValues.put(i, new ArrayList<>());
            // Add "unknown" as a possible value
            uniqueValues.get(i).add("unknown");
        }
        
        // Collect all unique values for each selected attribute
        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            for (int j = 0; j < keepIndices.size(); j++) {
                int origIndex = keepIndices.get(j);
                String value = origIndex < row.length ? row[origIndex].trim() : "";
                
                // Replace empty values with "unknown"
                if (value.isEmpty()) {
                    value = "unknown";
                }
                
                if (!uniqueValues.get(j).contains(value)) {
                    uniqueValues.get(j).add(value);
                }
            }
        }
        
        // Create attributes for selected columns
        for (int i = 0; i < keepIndices.size(); i++) {
            int origIndex = keepIndices.get(i);
            attributes.add(new Attribute(headers[origIndex], uniqueValues.get(i)));
        }
        
        // Create dataset
        Instances dataset = new Instances("CellLinesDataset", attributes, csvData.size() - 1);
        dataset.setClassIndex(targetIndex);
        
        // Add instances
        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            double[] values = new double[attributes.size()];
            
            for (int j = 0; j < attributes.size(); j++) {
                int origIndex = keepIndices.get(j);
                String value = origIndex < row.length ? row[origIndex].trim() : "";
                
                // Replace empty values with "unknown"
                if (value.isEmpty()) {
                    value = "unknown";
                }
                
                values[j] = uniqueValues.get(j).indexOf(value);
            }
            
            dataset.add(new DenseInstance(1.0, values));
        }
        
        System.out.println("Created dataset with " + dataset.numInstances() + " instances and " + 
                          dataset.numAttributes() + " attributes");
        System.out.println("Class attribute: " + dataset.classAttribute().name() + " with " + 
                          dataset.numClasses() + " unique values");
        
        return dataset;
    }
    
    private static Instances[] splitData(Instances data, double trainPercentage) {
        data.randomize(new Random(42));
        
        int trainSize = (int) Math.round(data.numInstances() * trainPercentage);
        int testSize = data.numInstances() - trainSize;
        
        Instances train = new Instances(data, 0, trainSize);
        Instances test = new Instances(data, trainSize, testSize);
        
        System.out.println("Split data into " + train.numInstances() + " training instances and " + 
                          test.numInstances() + " testing instances (70/30 split)");
        
        return new Instances[]{train, test};
    }
    
    private static Classifier trainRandomForest(Instances trainingData) throws Exception {
        System.out.println("Training Random Forest model...");
        
        RandomForest rf = new RandomForest();
        rf.setNumIterations(100);
        rf.setNumFeatures(0);
        rf.setSeed(42);
        
        rf.buildClassifier(trainingData);
        
        return rf;
    }
    
    private static Classifier trainKNN(Instances trainingData) throws Exception {
        System.out.println("Training KNN model...");
        
        IBk knn = new IBk();
        knn.setKNN(5);
        knn.setCrossValidate(true);
        
        knn.buildClassifier(trainingData);
        
        return knn;
    }
    
    private static Classifier trainSVM(Instances trainingData) throws Exception {
        System.out.println("Training SVM model...");
        
        SMO svm = new SMO();
        svm.setC(1.0);
        
        svm.buildClassifier(trainingData);
        
        return svm;
    }
    
    private static Classifier trainXGBoost(Instances trainingData) throws Exception {
        System.out.println("Training XGBoost model...");
        
        // Changed from AdaBoostM1 to LogitBoost since AdaBoostM1 cannot be resolved
        LogitBoost booster = new LogitBoost();
        booster.setNumIterations(100);
        booster.setSeed(42);
        
        booster.buildClassifier(trainingData);
        
        return booster;
    }
    
    private static double evaluateModel(Classifier model, Instances testData, String modelName) throws Exception {
        Evaluation eval = new Evaluation(testData);
        eval.evaluateModel(model, testData);
        return eval.pctCorrect();
    }
    
    private static Map.Entry<String, Classifier> findBestModel(Map<String, Classifier> models, Instances testData) throws Exception {
        System.out.println("Comparing models to find the best one...");
        
        String bestModelName = null;
        Classifier bestModel = null;
        double bestAccuracy = -1;
        
        for (Map.Entry<String, Classifier> entry : models.entrySet()) {
            String modelName = entry.getKey();
            Classifier model = entry.getValue();
            
            double accuracy = evaluateModel(model, testData, modelName);
            
            System.out.println(modelName + " accuracy: " + accuracy);
            
            if (accuracy > bestAccuracy) {
                bestAccuracy = accuracy;
                bestModelName = modelName;
                bestModel = model;
            }
        }
        
        System.out.println("Best model: " + bestModelName + " with accuracy: " + bestAccuracy + "%");
        
        Map<String, Classifier> resultMap = new HashMap<>();
        resultMap.put(bestModelName, bestModel);
        
        return resultMap.entrySet().iterator().next();
    }
}