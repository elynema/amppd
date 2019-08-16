package edu.indiana.dlib.amppd.service.impl;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputs;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputs.InputSourceType;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowInputs.WorkflowDestination;

import edu.indiana.dlib.amppd.service.GalaxyApiService;
import edu.indiana.dlib.amppd.service.WorkflowService;
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * Implementation of WorkflowService.
 * @author yingfeng
 *
 */
@Service
@Log
public class WorkflowServiceImpl implements WorkflowService {
	
	public static final String SHARED_HISTORY_NAME = "amppd";
	
	@Autowired
	private GalaxyApiService galaxyApiService;
	
	private GalaxyInstance galaxyInstance;
	
	@Getter
	private WorkflowsClient workflowsClient;
	
	@Getter
	private WorkflowDestination sharedHistory;
	
	/**
	 *  initialize Galaxy data library, which is shared by all AMPPD users.
	 */
	@PostConstruct
	public void init() {
		galaxyInstance = galaxyApiService.getGalaxyInstance();
		workflowsClient = galaxyInstance.getWorkflowsClient();
		sharedHistory = new WorkflowInputs.NewHistory(SHARED_HISTORY_NAME);
	}
	
	/**
	 * @see edu.indiana.dlib.amppd.service.WorkflowService.buildWorkflowInputs(String,String)
	 */	
	public WorkflowInputs buildWorkflowInputs(String workflowId, String datasetId) {
		WorkflowInputs winputs = new WorkflowInputs();
		winputs.setDestination(sharedHistory);
		winputs.setImportInputsToHistory(false);
		winputs.setWorkflowId(workflowId);
		
		WorkflowInputs.WorkflowInput winput = new WorkflowInputs.WorkflowInput(datasetId, InputSourceType.LDDA);
		winputs.setInput(inputName, winput);		
		
		return winputs;
	}

}
