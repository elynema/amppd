package edu.indiana.dlib.amppd.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.jmchilton.blend4j.galaxy.beans.Workflow;

import edu.indiana.dlib.amppd.exception.GalaxyWorkflowException;
import edu.indiana.dlib.amppd.service.WorkflowService;
import lombok.extern.java.Log;

/**
 * Controller for REST operations on Workflow.
 * @author yingfeng
 *
 */
@RestController
@Log
public class WorkflowController {

	@Autowired
	private WorkflowService workflowService;
	
	/**
	 * List all workflows from Galaxy through its REST API.
	 * @return
	 */
	@GetMapping("/workflows")
	public List<Workflow> listWorkflows() {	
		List<Workflow> workflows = null;
	
		try {
			workflows = workflowService.getWorkflowsClient().getWorkflows();
			log.info("Listed " + workflows.size() + " current workflows in Galaxy: " + workflows);
		}
		catch (Exception e) {
			log.throwing("WorkflowController", "listWorkflows", new GalaxyWorkflowException("Unable to retrieve workflows from Galaxy.", e));
		}
		
		return workflows;
	}
	
	/**
	 * Retrieve details of a workflow from Galaxy through its REST API.
	 * @return
	 */
	@GetMapping("/workflows/{workflowId}")
	public List<Workflow> showWorkflow(@PathVariable("workflowId") String workflowId) {	
		List<Workflow> workflows = null;
	
		try {
			workflows = workflowService.getWorkflowsClient().getWorkflows();
			log.info("Retrieved " + workflows.size() + " current workflows in Galaxy: " + workflows);
		}
		catch (Exception e) {
			log.throwing("WorkflowController", "getWorkflows", new GalaxyWorkflowException("Unable to retrieve workflows from Galaxy.", e));
		}
		
		return workflows;
	}
	
	
}
