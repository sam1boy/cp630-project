document.addEventListener('DOMContentLoaded', function() {
    const predictionForm = document.getElementById('predictionForm');
    const resultsSection = document.getElementById('resultsSection');
    const waitingMessage = resultsSection.querySelector('.waiting-message');
    const resultsContainer = resultsSection.querySelector('.results-container');
    const predictionResult = document.getElementById('predictionResult');
    const confidenceBar = document.getElementById('confidenceBar');
    const explanationText = document.getElementById('explanationText');
    
    // Fetch categories from the server
    fetchCategories();
    
    // Submit form event handler
    predictionForm.addEventListener('submit', function(e) {
		e.preventDefault();

		// Hide treatment card when starting a new prediction
		document.getElementById('treatmentCard').classList.add('d-none');
        
        // Show loading state
        waitingMessage.innerHTML = '<p><i class="fas fa-spinner fa-spin fa-3x mb-3"></i></p><p>Processing your request...</p>';
        waitingMessage.classList.remove('d-none');
        resultsContainer.classList.add('d-none');
        
        // Collect form data
        const formData = new FormData(predictionForm);
        const queryString = new URLSearchParams(formData).toString();
        
        // Make AJAX request
        fetch('predict', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: queryString
        })
        .then(response => response.json())
        .then(data => {
            // Hide loading message
            waitingMessage.classList.add('d-none');
            resultsContainer.classList.remove('d-none');
            
            if (data.success) {
                // Display prediction
                predictionResult.textContent = data.prediction;
                
                // Update confidence bar
                const confidence = Math.round(data.confidence);
                confidenceBar.style.width = confidence + '%';
                confidenceBar.setAttribute('aria-valuenow', confidence);
                confidenceBar.textContent = confidence + '%';
                
                // Set color based on confidence
                if (confidence >= 70) {
                    confidenceBar.classList.remove('bg-warning', 'bg-danger');
                    confidenceBar.classList.add('bg-success');
                } else if (confidence >= 40) {
                    confidenceBar.classList.remove('bg-success', 'bg-danger');
                    confidenceBar.classList.add('bg-warning');
                } else {
                    confidenceBar.classList.remove('bg-success', 'bg-warning');
                    confidenceBar.classList.add('bg-danger');
                }
                
                explanationText.innerHTML = `The model predicts with ${confidence}% confidence that this cell line is of <strong>${data.prediction}</strong> cancer type based on the provided characteristics.`;
            
				fetchTreatmentInfo(data.prediction);
	
			} else {
                // Show error
                predictionResult.textContent = 'ERROR';
                confidenceBar.style.width = '0%';
                confidenceBar.setAttribute('aria-valuenow', 0);
                confidenceBar.textContent = '0%';
                explanationText.textContent = 'An error occurred: ' + data.error;
            }
        })
        .catch(error => {
            waitingMessage.classList.add('d-none');
            resultsContainer.classList.remove('d-none');
            predictionResult.textContent = 'ERROR';
            confidenceBar.style.width = '0%';
            confidenceBar.setAttribute('aria-valuenow', 0);
            confidenceBar.textContent = '0%';
            explanationText.textContent = 'An error occurred: ' + error.message;
        });
    });
    
    // Function to fetch categories and populate the dropdowns
    function fetchCategories() {
        fetch('categories')
        .then(response => response.json())
        .then(data => {
            // Populate each select/datalist
            populateSelectOptions('wes', data.wes);
            populateSelectOptions('cna', data.cna);
            populateSelectOptions('geneExpression', data.geneExpression);
            populateSelectOptions('methylation', data.methylation);
            populateSelectOptions('drugResponse', data.drugResponse);
            populateDatalist('gdscTissue1List', data.gdscTissue1);
            populateDatalist('gdscTissue2List', data.gdscTissue2);
            populateSelectOptions('msi', data.msi);
            populateSelectOptions('screenMedium', data.screenMedium);
            populateSelectOptions('growthProperties', data.growthProperties);
            
            console.log('Form options loaded successfully');
        })
        .catch(error => {
            console.error('Error loading form options:', error);
            // Show error message in the form
            document.querySelectorAll('select').forEach(select => {
                select.innerHTML = '<option value="">Error loading options</option>';
            });
        });
    }
    
    // Helper function to populate a select element
    function populateSelectOptions(elementId, options) {
        const select = document.getElementById(elementId);
        if (!select) return;
        
        // Clear current options
        select.innerHTML = '';
        
        // Add blank option
        const blankOption = document.createElement('option');
        blankOption.value = '';
        blankOption.textContent = 'Select...';
        blankOption.selected = true;
        select.appendChild(blankOption);
        
        // Sort options alphabetically, but keep 'unknown' at the end
        const sortedOptions = [...options].sort((a, b) => {
            if (a === 'unknown') return 1;
            if (b === 'unknown') return -1;
            return a.localeCompare(b);
        });
        
        // Add options
        sortedOptions.forEach(option => {
            const optionElement = document.createElement('option');
            optionElement.value = option;
            optionElement.textContent = option === 'Y' ? 'Yes' : 
                                       option === 'N' ? 'No' : 
                                       option;
            select.appendChild(optionElement);
        });
    }
    
    // Helper function to populate a datalist
    function populateDatalist(elementId, options) {
        const datalist = document.getElementById(elementId);
        if (!datalist) return;
        
        // Clear current options
        datalist.innerHTML = '';
        
        // Sort options alphabetically, but keep 'unknown' at the end
        const sortedOptions = [...options].sort((a, b) => {
            if (a === 'unknown') return 1;
            if (b === 'unknown') return -1;
            return a.localeCompare(b);
        });
        
        // Add options
        sortedOptions.forEach(option => {
            const optionElement = document.createElement('option');
            optionElement.value = option;
            datalist.appendChild(optionElement);
        });
    }
	
	function fetchTreatmentInfo(cancerType) {
	    // Don't show treatment info for unknown cancer types
	    if (!cancerType || cancerType.toLowerCase().includes('unknown')) {
	        console.log("Unknown cancer type - not displaying treatment info");
	        document.getElementById('treatmentCard').classList.add('d-none');
	        return;
	    }
	    
	    console.log("Fetching treatment info for: " + cancerType);
	    const treatmentCard = document.getElementById('treatmentCard');
	    const treatmentText = document.getElementById('treatmentText');
	    const treatmentTitle = document.getElementById('treatmentTitle');
	    
	    // Show treatment card and set title
	    treatmentCard.classList.remove('d-none');
	    treatmentTitle.textContent = `Treatment Approaches for ${cancerType} Cancer`;
	    treatmentText.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Loading treatment information...';
	    
	    // Log the API URL for debugging
	    const apiUrl = 'treatment-info?cancerType=' + encodeURIComponent(cancerType);
	    console.log("Calling API endpoint: " + apiUrl);
	    
	    fetch(apiUrl)
	        .then(response => {
	            console.log("API Response status: " + response.status);
	            if (!response.ok) {
	                throw new Error('API returned status: ' + response.status);
	            }
	            return response.json();
	        })
	        .then(data => {
	            console.log("API data received:", data);
	            if (data.success) {
	                // Check if we have treatment info
	                if (data.treatmentInfo) {
	                    treatmentText.innerHTML = data.treatmentInfo;
	                    console.log("Treatment info displayed successfully");
	                } else {
	                    // If no treatment info was returned (for unknown types)
	                    treatmentCard.classList.add('d-none');
	                }
	            } else {
	                treatmentText.innerHTML = '<i class="fas fa-exclamation-triangle text-warning"></i> ' + 
	                                          'Could not retrieve treatment information: ' + data.error;
	                console.error("API error: " + data.error);
	            }
	        })
	        .catch(error => {
	            console.error("Error fetching treatment info: " + error);
	            treatmentText.innerHTML = '<i class="fas fa-exclamation-triangle text-warning"></i> ' + 
	                                      'Error loading treatment information: ' + error.message;
	            // Fall back to basic information if API fails
	            showFallbackTreatmentInfo(cancerType);
	        });
	}
	
	function showFallbackTreatmentInfo(cancerType) {
	    const treatmentText = document.getElementById('treatmentText');
	    
	    switch(cancerType) {
	        case "BRCA":
	            treatmentText.innerHTML = "<p>Breast cancer treatments typically include surgery, radiation therapy, chemotherapy, hormone therapy, and targeted therapies. Recent advances include immunotherapies and personalized medicine approaches.</p>";
	            break;
	        case "LUAD":
	        case "LUSC":
	            treatmentText.innerHTML = "<p>Lung cancer treatments may include surgery, chemotherapy, radiation therapy, targeted drug therapy, and immunotherapy. Treatment depends on cancer type, stage, and patient health status.</p>";
	            break;
	        default:
	            treatmentText.innerHTML = "<p>Treatment typically involves a combination of surgery, radiation therapy, chemotherapy, targeted therapy, or immunotherapy, depending on cancer stage and patient factors.</p>";
	    }
	    
	    treatmentText.innerHTML += "<p><small class='text-muted'>(Basic information provided - API currently unavailable)</small></p>";
	}
});