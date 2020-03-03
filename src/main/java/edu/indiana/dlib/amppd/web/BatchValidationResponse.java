package edu.indiana.dlib.amppd.web;

import java.util.ArrayList;
import java.util.List;

import edu.indiana.dlib.amppd.model.Batch;
import lombok.Data;

@Data
public class BatchValidationResponse {

	public BatchValidationResponse() {
		validationErrors = new ArrayList<String>();
		processingErrors = new ArrayList<String>();
	}
	
	boolean success;
	
	Batch batch;
	
	private List<String> validationErrors;
	
	private List<String> processingErrors;
	
	public void addError(String error) {
		validationErrors.add(error);
	}
	public void addErrors(List<String> errors) {
		if(validationErrors!=null && !errors.isEmpty()) {
			this.validationErrors.addAll(errors);
		}
	}
	
	public boolean hasErrors() {
		return !validationErrors.isEmpty();
	}
	
	public boolean hasProcessingErrors() {
		return !processingErrors.isEmpty();
	}
}
