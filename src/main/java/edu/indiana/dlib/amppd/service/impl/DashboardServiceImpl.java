package edu.indiana.dlib.amppd.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.jmchilton.blend4j.galaxy.beans.InvocationDetails;
import com.github.jmchilton.blend4j.galaxy.beans.InvocationStepDetails;
import com.github.jmchilton.blend4j.galaxy.beans.Job;
import com.github.jmchilton.blend4j.galaxy.beans.JobInputOutput;

import edu.indiana.dlib.amppd.config.GalaxyPropertyConfig;
import edu.indiana.dlib.amppd.model.Primaryfile;
import edu.indiana.dlib.amppd.repository.PrimaryfileRepository;
import edu.indiana.dlib.amppd.service.DashboardService;
import edu.indiana.dlib.amppd.service.JobService;
import edu.indiana.dlib.amppd.web.DashboardResult;
import edu.indiana.dlib.amppd.web.GalaxyJobState;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService{

	@Autowired
	private GalaxyPropertyConfig galaxyPropertyConfig;
	@Autowired
	private PrimaryfileRepository primaryfileRepository;
	@Autowired
	private JobService jobService;

	public List<DashboardResult> getDashboardResults(){
		List<DashboardResult> results = new ArrayList<DashboardResult>();
		try {
			// Get a list of invocation details from Galaxy
			List<InvocationDetails> details = jobService.getWorkflowsClient().indexInvocationsDetails(galaxyPropertyConfig.getUsername());
			
			// For each detail
			for(InvocationDetails detail : details) {
				// Check to see if we have an associated primary file.
				List<Primaryfile> files = primaryfileRepository.findByHistoryId(detail.getHistoryId());
				
				// If not, skip this invocation
				if(files.isEmpty()) continue;
				
				// Grab the first primary file, although should only be one.
				Primaryfile thisFile = files.get(0);
				
				// Iterate through each step.  Each of which has a list of jobs (unless it is the initial input)				
				for(InvocationStepDetails step : detail.getSteps()) {
					// If we have no jobs, don't add a result here
					List<Job> jobs = step.getJobs();
					if(jobs.isEmpty()) continue;
					
					Date date = step.getUpdateTime();
					
					// It's possible to have more than one job per step, although we don't have any examples at the moment
					GalaxyJobState status = GalaxyJobState.UNKNOWN;
					String jobName = "";
					for(Job job : jobs) {
						// Concatenate the job names in case we have more than one. 
						jobName = jobName + job.getToolId() + " ";
						date = job.getCreated();
						status = getJobStatus(job.getState());
					}

					// For each output, create a record.
					Map<String, JobInputOutput> outputs = step.getOutputs();
					for(String key : outputs.keySet()) {
						DashboardResult result = new DashboardResult();
						result.setWorkflowStep(jobName);
						result.setSubmitter("amppd");
						result.setDate(date);
						result.setStatus(status);
						
						result.setSourceFilename(thisFile.getOriginalFilename());
						result.setSourceItem(thisFile.getItem().getName());
						
						result.setWorkflowName(detail.getWorkflowId());
						
						result.setOutputFile(key);
						results.add(result);
					}
				}
				
			}
		}
		catch(Exception ex) {
			log.error("Error getting dashboard results", ex);
		}
		
		return results;
	}
	
	// Map the status in Galaxy to what we want on the front end.
	private GalaxyJobState getJobStatus(String jobStatus) {
		GalaxyJobState status = GalaxyJobState.UNKNOWN;
		if(jobStatus.equals("ok")) {
			status = GalaxyJobState.COMPLETE;
		}
		else if(jobStatus.equals("running")) {
			status = GalaxyJobState.IN_PROGRESS;
		}
		else if(jobStatus.equals("scheduled")) {
			status = GalaxyJobState.SCHEDULED;
		}
		else if(jobStatus.equals("error")) {
			status = GalaxyJobState.ERROR;
		}
		return status;
	}
	
}