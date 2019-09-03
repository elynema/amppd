package edu.indiana.dlib.amppd.service.impl;

import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.GalaxyObject;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowDetails;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputs;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputs.ExistingHistory;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputs.InputSourceType;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputs.WorkflowInput;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowOutputs;

import edu.indiana.dlib.amppd.exception.GalaxyWorkflowException;
import edu.indiana.dlib.amppd.exception.StorageException;
import edu.indiana.dlib.amppd.model.Primaryfile;
import edu.indiana.dlib.amppd.repository.PrimaryfileRepository;
import edu.indiana.dlib.amppd.service.FileStorageService;
import edu.indiana.dlib.amppd.service.GalaxyApiService;
import edu.indiana.dlib.amppd.service.GalaxyDataService;
import edu.indiana.dlib.amppd.service.JobService;
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * Implementation of JobService.
 * @author yingfeng
 *
 */
@Service
@Log
public class JobServiceImpl implements JobService {
	
	@Autowired
    private PrimaryfileRepository primaryfileRepository;

	@Autowired
    private FileStorageService fileStorageService;	

	@Autowired
	private GalaxyApiService galaxyApiService;
	
	@Autowired
	private GalaxyDataService galaxyDataService;
	
	@Getter
	private WorkflowsClient workflowsClient;
		
	/**
	 * Initialize the JobServiceImpl bean.
	 */
	@PostConstruct
	public void init() {
		workflowsClient = galaxyApiService.getGalaxyInstance().getWorkflowsClient();
	}	
	
	/**
	 * @see edu.indiana.dlib.amppd.service.JobService.buildWorkflowInputs(String,String,Map<String, Map<String, String>>)
	 */	
	@Override
	public WorkflowInputs buildWorkflowInputs(String workflowId, String datasetId, Map<String, Map<String, String>> parameters) {
		WorkflowInputs winputs = new WorkflowInputs();
		winputs.setDestination(new ExistingHistory(galaxyDataService.getSharedHistory().getId()));
		winputs.setImportInputsToHistory(false);
		winputs.setWorkflowId(workflowId);
		
		String inputId;
		try {
			WorkflowDetails wdetails = workflowsClient.showWorkflow(workflowId);
			if (wdetails == null) {
				throw new GalaxyWorkflowException("Can't find workflow with ID " + workflowId);
			}
			
			// each input in the workflow corresponds to an input step with a unique ID, the inputs of workflow detail is a map of {stepId: {label: value}}
			Set<String> inputIds = wdetails.getInputs().keySet();
			if (inputIds.size() != 1) {
				throw new GalaxyWorkflowException("Workflow " + workflowId + " has " + inputIds.size() + " inputs, while it should have exactly one input.");
			}
			
			// forAmppd, we can assume all workflows only take one primaryfile as input
			inputId = (String)inputIds.toArray()[0];
		}
		catch (Exception e) {
			throw new GalaxyWorkflowException("Exception when retrieving details for workflow " + workflowId);
		}
		
		WorkflowInput winput = new WorkflowInput(datasetId, InputSourceType.LDDA);
		winputs.setInput(inputId, winput);		
		
		parameters.forEach((stepId, stepParams) -> {
			stepParams.forEach((paramName, paramValue) -> {
				winputs.setStepParameter(stepId, paramName, paramValue);
			});
		});
		
		log.info("Successfully built job inputs, workflow ID: " + workflowId + ", datasetId: " + datasetId + " parameters: " + parameters);
		return winputs;
	}
	
	/**
	 * @see edu.indiana.dlib.amppd.service.JobService.createJob(String,Long,Map<String, Map<String, String>>)
	 */	
	@Override
	public WorkflowOutputs createJob(String workflowId, Long primaryfileId, Map<String, Map<String, String>> parameters) {
		WorkflowOutputs woutputs = null;
		String msg = "Amppd job for: workflow ID: " + workflowId + ", primaryfileId: " + primaryfileId + " parameters: " + parameters;
		
    	// at this point the primaryfile shall have been created and its media file uploaded into Amppd file system
    	Primaryfile primaryfile = primaryfileRepository.findById(primaryfileId).orElseThrow(() -> new StorageException("Primaryfile <" + primaryfileId + "> does not exist!"));    
    	if (primaryfile.getPathname() == null || primaryfile.getPathname().isEmpty()) {
    		throw new StorageException("Primaryfile " + primaryfileId + " hasn't been uploaded to AMPPD file system");
    	}
    	
    	/* Note: 
    	 * We do a lazy upload from Amppd to Galaxy, i.e. we do it when workflow is invoked in Galaxy, rather than when the file is uploaded to Amppd.
    	 * The pros is that we won't upload to Galaxy unnecessarily if the primaryfile is never going to be processed through workflow;
    	 * the cons is that it might slow down workflow execution when running in batch.
    	 */

    	// upload the primaryfile into Galaxy data library, the returned result is an GalaxyObject containing the ID and URL of the dataset uploaded
    	String pathname = fileStorageService.absolutePathName(primaryfile.getPathname());
    	GalaxyObject go = galaxyDataService.uploadFileToGalaxy(pathname);		
		
    	// invoke the workflow 
    	try {
    		WorkflowInputs winputs = buildWorkflowInputs(workflowId, go.getId(), parameters);
    		woutputs = workflowsClient.runWorkflow(winputs);
    	}
    	catch (Exception e) {
    		log.throwing("JobServiceImpl", "createJob", new GalaxyWorkflowException("Error creating " + msg, e));
    	}
    	
		log.info("Successfully created " + msg);
    	log.info("Galaxy workflow outputs: " + woutputs);
    	return woutputs;
	}

}