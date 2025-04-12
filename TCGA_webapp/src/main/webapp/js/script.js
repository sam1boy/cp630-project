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
                
                // Update explanation
                explanationText.innerHTML = `The model predicts with ${confidence}% confidence that this cell line is of <strong>${data.prediction}</strong> cancer type based on the provided characteristics.`;
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
	    const treatmentInfoSection = document.querySelector('.treatment-info');
	    const treatmentText = document.getElementById('treatmentText');
	    
	    // Show treatment section and loading message
	    treatmentInfoSection.classList.remove('d-none');
	    treatmentText.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Loading treatment information...';
	    
	    fetch('treatment-info?cancerType=' + encodeURIComponent(cancerType))
        .then(response => response.json())
        .then(data => {
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
			    
			    // Update explanation
			    explanationText.innerHTML = `The model predicts with ${confidence}% confidence that this cell line is of <strong>${data.prediction}</strong> cancer type based on the provided characteristics.`;
			    
			    // NEW: Fetch and display treatment info
			    fetchTreatmentInfo(data.prediction);
			} else {
                treatmentText.innerHTML = '<i class="fas fa-exclamation-triangle text-warning"></i> ' + 
                                          'Could not retrieve treatment information: ' + data.error;
            }
        })
        .catch(error => {
            treatmentText.innerHTML = '<i class="fas fa-exclamation-triangle text-warning"></i> ' + 
                                      'Error loading treatment information: ' + error.message;
        });
	}
});