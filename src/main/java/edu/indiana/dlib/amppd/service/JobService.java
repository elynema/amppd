package edu.indiana.dlib.amppd.service;

import java.util.List;
import java.util.Map;

import com.github.jmchilton.blend4j.galaxy.HistoriesClient;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.Dataset;
import com.github.jmchilton.blend4j.galaxy.beans.Invocation;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowDetails;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowOutputs;

import edu.indiana.dlib.amppd.model.Primaryfile;
import edu.indiana.dlib.amppd.web.WorkflowOutputResult;


/**
 * Service for Amppd job related functionalities. 
 * Note: unless otherwise noted, all references of job in Amppd refer to Amppd jobs. An Amppd job is an execution of a Galaxy workflow on a Amppd primaryfile.
 * @author yingfeng
 *
 */
public interface JobService {

	/**
	 * Return the WorkflowsClient instance.
	 */
	public WorkflowsClient getWorkflowsClient();
	
	/**
	 * Return the HistoriesClient instance.
	 */
	public HistoriesClient getHistoriesClient();
	
	/**
	 * Get needed job context for HMGMs when running the given workflow against the given primaryfile.
	 * @param workflowDetails the given workflow
	 * @param primaryfile the given primaryfile
	 * @return the generated JSON string for HMGM context
	 */
	public String getHmgmContext(WorkflowDetails workflowDetails, Primaryfile primaryfile); 
	
	/**
	 * Sanitize the given text by replacing single/double quotes (if any) with "%" followed by their hex code,
	 * so the result can be used in a context JSON string, which can be parsed as a valid parameter on command line.
	 * @param text text to be sanitized
	 * @return
	 */
	public String sanitizeText(String text);
	
	/**
	 * Create a new Amppd job by submitting to Galaxy the given workflow on the given primaryfile, along with the given parameters.
	 * @param workflowId ID of the given workflow
	 * @param primaryfileId ID of the given primaryfile
	 * @param parameters step parameters for running the workflow
	 * @return the WorkflowOutputs returned from Galaxy
	 */
	public WorkflowOutputResult createJob(String workflowId, Long primaryfileId, Map<String, Map<String, String>> parameters);
	
	/**
	 * Create new Amppd jobs by submitting to Galaxy the given workflow on the given primaryfiles, along with the given parameters.
	 * @param workflowId ID of the given workflow
	 * @param primaryfileIds IDs of the given primaryfiles
	 * @param parameters step parameters for running the workflow
	 * @return map between primaryfile IDs to the outputs of the jobs created successfully
	 */
	public List<WorkflowOutputResult> createJobs(String workflowId, Long[] primaryfileIds, Map<String, Map<String, String>> parameters);
	
	/**
	 * Create a bundle of multiple Amppd jobs, one job for each primaryfile included in the given bundle, to invoke the given workflow in Galaxy, with the given step parameters.
	 * @param workflowId the ID of the specified workflow 
	 * @param bundleId the ID of the specified bundle
	 * @param parameters the parameters to use for the steps in the workflow as a map {stepId: {paramName; paramValue}}
	 * @return map between primaryfile IDs to the outputs of the jobs created successfully
	 */
	public List<WorkflowOutputResult> createJobBundle(String workflowId, Long bundleId, Map<String, Map<String, String>> parameters);
	
	/**
	 * List all AMP jobs run on the specified workflow against the specified primaryfile.
	 * @param workflowId ID of the given workflow
	 * @param(primaryfileId ID of the given primaryfile 
	 * @return a list of Invocations each containing basic information of an AMP job 
	 */
	public List<Invocation> listJobs(String workflowId, Long primaryfileId);
		
	/**
	 * Show detailed information of the inquired output generated by the specified AMP job step. 
	 * @param workflowId the ID of the workflow associated with the specified AMP job 
	 * @param invocationId the ID of the Galaxy workflow invocation corresponding to the specified AMP job
	 * @param stepId the ID of the specified Galaxy workflow invocation step within the AMP job
	 * @param datasetId the ID of the inquired Galaxy workflow invocation step output dataset
	 * @return an instance of Dataset containing detailed information of the inquired AMP job step output
	 */
	public Dataset showJobStepOutput(String workflowId, String invocationId, String stepId, String datasetId);
		

}
