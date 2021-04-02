package edu.indiana.dlib.amppd.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import com.github.jmchilton.blend4j.galaxy.HistoriesClient;
import com.github.jmchilton.blend4j.galaxy.beans.Dataset;
import com.github.jmchilton.blend4j.galaxy.beans.Invocation;
import com.github.jmchilton.blend4j.galaxy.beans.InvocationDetails;
import com.github.jmchilton.blend4j.galaxy.beans.InvocationStepDetails;
import com.github.jmchilton.blend4j.galaxy.beans.Job;
import com.github.jmchilton.blend4j.galaxy.beans.JobInputOutput;
import com.github.jmchilton.blend4j.galaxy.beans.Workflow;

import edu.indiana.dlib.amppd.config.GalaxyPropertyConfig;
import edu.indiana.dlib.amppd.exception.GalaxyWorkflowException;
import edu.indiana.dlib.amppd.exception.StorageException;
import edu.indiana.dlib.amppd.model.Collection;
import edu.indiana.dlib.amppd.model.Item;
import edu.indiana.dlib.amppd.model.MgmTool;
import edu.indiana.dlib.amppd.model.Primaryfile;
import edu.indiana.dlib.amppd.model.WorkflowResult;
import edu.indiana.dlib.amppd.repository.MgmToolRepository;
import edu.indiana.dlib.amppd.repository.PrimaryfileRepository;
import edu.indiana.dlib.amppd.repository.WorkflowResultRepository;
import edu.indiana.dlib.amppd.service.JobService;
import edu.indiana.dlib.amppd.service.MediaService;
import edu.indiana.dlib.amppd.service.WorkflowResultService;
import edu.indiana.dlib.amppd.service.WorkflowService;
import edu.indiana.dlib.amppd.web.GalaxyJobState;
import edu.indiana.dlib.amppd.web.WorkflowResultResponse;
import edu.indiana.dlib.amppd.web.WorkflowResultSearchQuery;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WorkflowResultServiceImpl implements WorkflowResultService {
	public static final String WILD_CARD = "*";

	/* Note: 
	 * The two maps below are used by the standardize method (which is called by the refreshWorkflowResults method).
	 * Their values are based on current WorkflowResult table data. 
	 * Since the obsolete MGMs don't exist in the system anymore, no more obsolete IDs/names should be generated,
	 * thus the maps are inclusive for the future. Once we delete obsolete outputs from Galaxy, 
	 * we won't need to call standardize and these maps can be removed as well.
	 */   
	// map between all obsolete workflow step names to their standard current names
	private static final HashMap<String, String> STANDARD_STEPS = new HashMap<String, String>() {{
		put("adjust_timestamps", "adjust_transcript_timestamps");
		put("aws_comprehend", "aws_comprehend_ner");
		put("aws_transcribe", "aws_transcribe_stt");
		put("speech_segmenter", "ina_speech_segmenter");
		put("VTTgenerator", "vtt_generator");
		put("vtt_generator", "trasncript_to_webvtt");
	}};
	// map between all obsolete output names to their standard current names
	private static final HashMap<String, String> STANDARD_OUTPUTS = new HashMap<String, String>() {{
		put("amp_entity_extraction", "amp_entities");
		put("amp_kept_segments", "kept_segments");
		put("audio_file", "audio_extracted");
		put("amp_segmentation", "amp_segments");
		put("aws_transcribe_transcript", "aws_transcript");
		put("corrected_draftjs", "draftjs_corrected");
		put("corrected_draftjs_transcript", "draftjs_corrected");
		put("corrected_iiif", "iiif_corrected");
		put("original_draftjs", "draftjs_uncorrected");
		put("original_draftjs_transcript", "draftjs_uncorrected");
		put("original_iiif", "iiif_uncorrected");
		put("output_transcript", "amp_transcript_corrected");
		put("output_ner", "amp_entities_corrected");
		put("segmented_audio_file", "speech_audio");		
		put("webVtt", "web_vtt");
	}};
	// map between obsolete output names for certain workflow steps to their standard current names
	private static final HashMap<String, String> STANDARD_STEP_OUTPUTS = new HashMap<String, String>() {{
		put("amp_transcript", "amp_transcript_adjusted");
		put("amp_diarization", "amp_diarization_adjusted");
	}};
	// map between all standard workflow step names to maps between all obsolete output names to their standard current names
	// this is used when we need both workflow step and output name to decide what the standard output name should be, 
	// due to that some obsolete output names overlap with standard output name from other workflow steps
	private static final HashMap<String, HashMap<String, String>> STANDARD_STEPS_OUTPUTS = new HashMap<String, HashMap<String, String>>() {{
		put("adjust_transcript_timestamps", STANDARD_STEP_OUTPUTS);
		put("adjust_diarization_timestamps", STANDARD_STEP_OUTPUTS);
	}};
	// 
	private static final HashMap<String, String> FIX_OUTPUT_TYPES = new HashMap<String, String>() {{
		put("amp_diarization", "segment");
		put("amp_diarization_adjusted", "segment");
		put("amp_entities", "ner");
		put("amp_entities_corrected", "ner");
		put("amp_segments", "segment");
		put("amp_transcript", "transcript");
		put("amp_transcript_adjusted", "transcript");
		put("amp_transcript_corrected", "transcript");
		put("audio_extracted", "wav");
		put("speech_audio", "speech");
		put("web_vtt", "vtt");
	}};

	/* Note: 
	 * The three lists below are used by the hideIrrelevantWorkflowResults process.
	 * Their values are based on current WorkflowResult table data. 
	 * They are subject to change if we have new cases of irrelevant outputs in the future. 
	 * If we need to run the hideIrrelevantWorkflowResults process and change these lists frequently, 
	 * we can consider using a DB table instead of hard-coded constants.
	 * Also, only standard step/output names need to be included, thanks to the standardize method.
	 */
	// all outputs of the following workflow steps are irrelevant, disregarding its output name
	private static final List<String> HIDE_STEPS = Arrays.asList("ina_speech_segmenter", "ina_speech_segmenter_hpc", "remove_silence_music", "remove_silence_speech", "pyscenedetect_shot_detection");	
	// all outputs with the following names are irrelevant, disregarding its workflow step
	private static final List<String> HIDE_OUTPUTS = Arrays.asList("draftjs_corrected", "draftjs_uncorrected", "task_info", "iiif_corrected", "iiif_uncorrected");	
	// all outputs with the following workflowStep-outputFile tuples are irrelevant
	private static final String[][] HIDE_STEPS_OUTPUTS = { {"aws_transcribe", "amp_diarization"}, {"aws_transcribe",	"amp_transcript"} };
//	public static final List<String> HIDE_STEPS = Arrays.asList("speech_segmenter", "ina_speech_segmenter", "ina_speech_segmenter_hpc", "remove_silence_music", "remove_silence_speech", "adjust_timestamps", "adjust_transcript_timestamps", "adjust_diarization_timestamps", "pyscenedetect_shot_detection");	
//	public static final List<String> HIDE_OUTPUTS = Arrays.asList("corrected_draftjs", "draftjs_corrected", "draftjs_uncorrected", "original_draftjs", "task_info", "corrected_iiif", "iiif_corrected", "iiif_uncorrected", "original_iiif");	
//	public static final String[][] HIDE_STEPS_OUTPUTS = { {"aws_transcribe", "amp_diarization"}, {"aws_transcribe",	"amp_transcript"} };

	@Autowired
	private GalaxyPropertyConfig galaxyPropertyConfig;
	@Autowired
	private PrimaryfileRepository primaryfileRepository;
	@Autowired
	private MgmToolRepository mgmToolRepository;
	@Autowired
	private JobService jobService;
	@Autowired
	private WorkflowService workflowService;
	@Autowired
	private MediaService mediaService;
	@Autowired
	private WorkflowResultRepository workflowResultRepository;
	
	@Value("${amppd.refreshResultsStatusMinutes}")
	private int REFRESH_STATUS_MINUTES;
	
	@Value("${amppd.refreshResultsTableMinutes}")
	private int REFRESH_TABLE_MINUTES;
		
	
	/**
	 * Return true if the specified dateRefreshed is recent, i.e. within the given refreshMinutes; false otherwise.
	 */
	protected boolean isDateRefreshedRecent(Date dateRefreshed, int refreshMinutes) {		
		// This method is used by both refreshing the result status and refreshing the whole table.
		// refreshMinutes reflects the reverse of the refresh frequency, i.e. the gap between two refresh processes:
		// for the former, it should be longer than the average time Galaxy jobs takes to complete, 
		// but shorter than the gap between user accesses to the result;
		// for the latter, it should be longer than the average time refreshing the whole table takes,
		// and should be long enough to avoid picking up just refreshed records when rerun is needed in case of 
		// interruption or failure, the upper limit here is not a concern since we don't regularly need such refresh.
		return dateRefreshed != null && dateRefreshed.compareTo(DateUtils.addMinutes(new Date(), -refreshMinutes)) > 0;
	}
		
//	/**
//	 * Return true if we should refresh the status of the specified WorkflowResult from job status in galaxy.
//	 * A WorkflowResult needs refresh if it's existing status could still change (i.e. not COMPLETE or ERROR)
//	 * and its last refreshed timestamp is older than the refresh rate threshold.
//	 */
//	private boolean shouldRefreshResultStatus(WorkflowResult result) {
//		return !isDateRefreshedRecent(result.getDateRefreshed(), REFRESH_STATUS_MINUTES) && result.getStatus() != GalaxyJobState.COMPLETE && result.getStatus() != GalaxyJobState.ERROR;
//	}
	
	/**
	 * Returns true if the given dataset should be excluded from WorkflowResults, i.e. it's deleted, purged or discarded.
	 */
	protected Boolean shouldExcludeDataset(Dataset dataset) {
		return dataset == null || dataset.isDeleted() || dataset.isPurged()
				|| dataset.getState().equals("deleted") || dataset.getState().equals("discarded") ;	
		// we need to include invisible datasets in WorkflowResult table
//		return dataset == null || !dataset.getVisible() || dataset.isDeleted() || dataset.isPurged()
//				|| dataset.getState().equals("deleted") || dataset.getState().equals("discarded") ;
	}

	/**
	 * Refresh the status of the specified WorkflowResult from job status in galaxy.
	 */
	protected WorkflowResult refreshResultStatus(WorkflowResult result) {
		Dataset dataset = jobService.showJobStepOutput(result.getWorkflowId(), result.getInvocationId(), result.getStepId(), result.getOutputId());
		
		if (shouldExcludeDataset(dataset)) {
			workflowResultRepository.delete(result);
			log.warn("Deleted WorkflowResult for hidden/deleted Galaxy dataset: " + result);
		}
		else {
			// besides status, we should also update create/update timestamps from the dataset, 
			// since these could change too as the status change;
			// on the other hand, we don't set dateRefreshed here, to avoid conflict with the refresh table job, 
			// which uses this field to distinguish recently refreshed WorkflowResults from obsolete ones;
			// the dateUpdated field should be good enough to indicate the most recent update on the dataset status
			String state = dataset.getState();
			GalaxyJobState status = getJobStatus(state);			
			result.setStatus(status);
			result.setDateCreated(dataset.getCreateTime());
			result.setDateUpdated(dataset.getUpdateTime());
			workflowResultRepository.save(result);	
		}
		
		return result;		
	}
			
	/**
	 * Refresh status of the specified WorkflowResults by retrieving corresponding output status from Galaxy.
	 */
	protected List<WorkflowResult> refreshResultsStatus(List<WorkflowResult> WorkflowResults) {
		List<WorkflowResult> refreshedResults = new ArrayList<WorkflowResult>();
		
		for(WorkflowResult result : WorkflowResults) {
			try {
				refreshedResults.add(refreshResultStatus(result));
			}
			catch(Exception e) {
				throw new RuntimeException("Failed to refresh the status from Galaxy for WorkflowResult " + result.getId(), e);
			}			
		}
		
		return refreshedResults;
	}
	
	/**
	 * Refresh status for all WorkflowResults whose output status might still change by job runners in Galaxy.
	 */
	public List<WorkflowResult> refreshIncompleteWorkflowResults() {	
		// for now we exclude ERROR, PAUSED and UNKNOWN for now, 
		// as these jobs will need manual rerun for their status to be changed;
		// in most cases we likely will need to delete these outputs and resubmit whole workflow;
		// besides, all status will be updated during the nightly refresh whole table job.
		// TODO we might want to add ERROR and PAUSED to the status list for update in the future
		WorkflowResultSearchQuery query = new WorkflowResultSearchQuery();
		GalaxyJobState[] filterByStatuses = {GalaxyJobState.IN_PROGRESS, GalaxyJobState.SCHEDULED};
		query.setFilterByStatuses(filterByStatuses);
		WorkflowResultResponse response = workflowResultRepository.findByQuery(query);
		List<WorkflowResult> refreshedResults = refreshResultsStatus(response.getRows());
		log.info("Successfully refreshed status for " + refreshedResults.size() + " WorkflowResults");
		return refreshedResults;
	}
	
	/**
	 * @see edu.indiana.dlib.amppd.service.WorkflowResultService.getWorkflowResults(WorkflowResultSearchQuery)
	 */
	public WorkflowResultResponse getWorkflowResults(WorkflowResultSearchQuery query){
		WorkflowResultResponse response = workflowResultRepository.findByQuery(query);
		log.info("Successfully retrieved " + response.getTotalResults() + " WorkflowResults for search  query.");
		return response;
	}
	
	/**
	 * @see edu.indiana.dlib.amppd.service.WorkflowResultService.getFinalWorkflowResults(Long)
	 */
	public List<WorkflowResult> getFinalWorkflowResults(Long primaryfileId) {
		List<WorkflowResult> results = refreshResultsStatus(workflowResultRepository.findByPrimaryfileIdAndIsFinalTrue(primaryfileId));
		log.info("Successfully retrieved " + results.size() + " final WorkflowResults for primaryfile " + primaryfileId);
		return results;
	}
		
	/**
	 * @see edu.indiana.dlib.amppd.service.WorkflowResultService.addWorkflowResults(Invocation, Workflow, Primaryfile)
	 */
	public List<WorkflowResult> addWorkflowResults(Invocation invocation, Workflow workflow, Primaryfile primaryfile) {
		List<WorkflowResult> results = new ArrayList<WorkflowResult>();

		try {
			// this method is usually called when a new AMP job is created, in which case the passed-in invocation is a workflowOutputs 
			// instance, and we need to retrieve invocation details for the workflowOutputs, because even though 
			// workflowOutputs contains most info we need including steps, it doesn't include details of the steps
			InvocationDetails invocationDetails = invocation instanceof InvocationDetails ?
				(InvocationDetails)invocation : 
				(InvocationDetails)jobService.getWorkflowsClient().showInvocation(workflow.getId(), invocation.getId(), true);

			// add results to the table using info from the invocation
			results = refreshWorkflowResults(invocationDetails, workflow, primaryfile);
			log.info("Successfully added " + results.size() + " WorkflowResult for invocation " + invocation.getId() + ", workflow " + workflow.getId() + ", primaryfile " + primaryfile.getId());				
		}
		catch (Exception e) {
			// TODO should we rethrow exception or not 
			// if we don't rethrow the exception, results aren't added but there is no notification
			// if we do, users will see error even though jobs are submitted in success
			log.error("Failed to add results for invocation " + invocation.getId() + ", workflow " + workflow.getId() + ", primaryfile " + primaryfile.getId(), e);
//			throw new RuntimeException("Failed to add results for invocation " + invocation.getId() + ", workflow " + workflow.getId() + ", primaryfile " + primaryfile.getId());
		}

		return results;
	}
	
	/**
	 * @see edu.indiana.dlib.amppd.service.WorkflowResultService.refreshWorkflowResultsIterative()
	 */
	public List<WorkflowResult> refreshWorkflowResultsIterative() {		
		List<WorkflowResult> allResults = new ArrayList<WorkflowResult>();
		List<Primaryfile> primaryfiles = primaryfileRepository.findByHistoryIdNotNull();
		log.info("Found " + primaryfiles.size() + " primaryfiles with Galaxy history ...");

//		// clear up workflow names cache in case they have been changed on galaxy side since last refresh 
//		workflowService.clearWorkflowNamesCache();		
		// TODO replace below code with above commented code once we upgrade to Galaxy 20.*		
		// get all workflows as a work-around to retrieve invocations per workflow per primaryfile
		List<Workflow> workflows = workflowService.getWorkflowsClient().getWorkflows();
		
		// process Galaxy invocation details per primaryfile instead of retrieving all at once, in order to avoid timeout issue in Galaxy
		for (Primaryfile primaryfile : primaryfiles) {
			try {
				// skip the primaryfile if all of its results have been recently refreshed;
				// this allows rerun of the refresh to continue with unfinished primaryfiles in case of a failure
				Date oldestDateRefreshed = workflowResultRepository.findOldestDateRefreshedByPrimaryfileId(primaryfile.getId());
				if (isDateRefreshedRecent(oldestDateRefreshed, REFRESH_TABLE_MINUTES)) {
					log.info("Skipping primaryfile " + primaryfile.getId() + " as its results are recently refreshed.");
					continue;
				}

//				// get all Galaxy invocations for the primaryfile and refresh results with them
//				List<InvocationDetails> invocations = jobService.getWorkflowsClient().indexInvocationsDetails(galaxyPropertyConfig.getUsername(), null, primaryfile.getHistoryId());
//				for (InvocationDetails invocation : invocations) {
//					List<WorkflowResult> results = refreshWorkflowResults(invocation, null, primaryfile);
//					allResults.addAll(results);
//				}
				/* TODO replace below code with above commented code once we upgrade to Galaxy 20.*
				 *  retrieving all invocations for the primaryfile as above is more efficient; however
				 *  we will not get the proper workflow name using workflow ID returned from invocations,
				 *  due to the non-stored workflow ID issue in current Galaxy version;
				 *  as a work-around, we loop through all workflows and retrieve invocations per workflow for this primaryfile,
				 *  this way we have the stored workflow ID in hand
				 */
				for (Workflow workflow : workflows) {
					List<InvocationDetails> invocations = jobService.getWorkflowsClient().indexInvocationsDetails(galaxyPropertyConfig.getUsername(), workflow.getId(), primaryfile.getHistoryId());
					for (InvocationDetails invocation : invocations) {
						List<WorkflowResult> results = refreshWorkflowResults(invocation, workflow, primaryfile);
						allResults.addAll(results);
					}
				}
								
				log.info("Successfully refreshed results for primaryfile " + primaryfile.getId() + ", total of " + allResults.size() + " results refreshed so far ...");				
			}
			catch (Exception e) {
				// continue with the rest even if we fail on some primaryfile,
				// as we can rerun the refresh to continue on the failed ones
				log.error("Failed to refresh results for primaryfile " + primaryfile.getId(), e);
			}
		}
				
		log.info("Successfully refreshed " + allResults.size() + " WorkflowResults iteratively.");
		deleteObsoleteWorkflowResults();
		return allResults;
	}
	
	/**
	 * @see edu.indiana.dlib.amppd.service.WorkflowResultService.refreshWorkflowResultsLumpsum()
	 */
	public List<WorkflowResult> refreshWorkflowResultsLumpsum(){
		List<WorkflowResult> allResults = new ArrayList<WorkflowResult>();

		// clear up workflow names cache in case they have been changed on galaxy side since last refresh 
		workflowService.clearWorkflowNamesCache();
		
		// Get a list of AMP invocation details from Galaxy
		List<InvocationDetails> invocations = jobService.getWorkflowsClient().indexInvocationsDetails(galaxyPropertyConfig.getUsername());

		// refresh results for each AMP invocation
		for (InvocationDetails invocation : invocations) {
			try {
				List<WorkflowResult> results = refreshWorkflowResults(invocation, null, null);
				allResults.addAll(results);
				if (!results.isEmpty()) {
					log.info("Successfully refreshed " + results.size() + " results for invocation " + invocation.getId());
				}
				// otherwise either the invocation is not submitted via AMP or it has no output
			}
			catch(Exception e) {
				// continue with the rest even if we fail on some invocation,
				// as we can rerun the refresh to continue on the failed ones
				log.error("Failed to refresh results for invocation " + invocation.getId(), e);
			}				
		}

		log.info("Successfully refreshed " + allResults.size() + " WorkflowResults in lump sum.");
		return allResults;
	}
	
	/**
	 * Refresh WorkflowResults for the given invocation;
	 * if the workflow for the invocation is provided, use the workflow ID and name from that;
	 * if the primaryfile for the invocation is provided, use the associated entity names from that.
	 */
	protected List<WorkflowResult> refreshWorkflowResults(InvocationDetails invocation, Workflow workflow, Primaryfile primaryfile) {
		List<WorkflowResult> results = new ArrayList<WorkflowResult>();
		
		// if the passed-in primaryfile is null, get primaryfile info by its ID from the passed-in invocation
		if (primaryfile == null) {
			// Check to see if we have an associated primary file.
			List<Primaryfile> primaryfiles = primaryfileRepository.findByHistoryId(invocation.getHistoryId());
			
			// If not, skip this invocation as it is not invoked by AMP
			if (primaryfiles.isEmpty()) return results;
			
			// Grab the first primary file, although should only be one.
			primaryfile = primaryfiles.get(0);	
			
			// if we find more than one, then there is some error in the table
			if (primaryfiles.size() > 1) {
				log.warn("Error in Primaryfile table: Found more than 1 (" + primaryfiles.size() + ") primaryfiles with the same historyId " + invocation.getHistoryId());
			}
		}

		// get workflow name either from the passed-in workflow, or retrieve it by its ID from the passed-in invocation 
		String workflowName = workflow != null ? workflow.getName() : workflowService.getWorkflowName(invocation.getWorkflowId());
		String workflowId = workflow != null ? workflow.getId() : invocation.getWorkflowId(); // use the stored workflow ID if available
				
		// Iterate through each step, each of which has a list of jobs (unless it is the initial input)				
		for(InvocationStepDetails step : invocation.getSteps()) {
			// If we have no jobs, don't add any result here, skip the step
			List<Job> jobs = step.getJobs();
			if (jobs.isEmpty()) continue;
						
			// TODO confirm what Galaxy step/job timestamps represent
			// Theoretically the timestamps of a step and its job should be the same; 
			// however only the updated timestamp of a step is available in step invocation; 
			// and it differs from either the created or updated timestamp of the job;
			// for now we use the created/updated timestamp of the job in the result
			Date dateCreated = step.getUpdateTime(); // will be overwritten below
			
			// TODO check if the last job is the newest rerun to replace previously failed ones
			// It's possible to have multiple jobs for a step, likely when the step is rerun within the same invocation; iIn any case
			// the tool used should be the same, so we can just use the info from the last job assuming that's the latest one
			String stepLabel = "";
			String toolInfo = "";
			if (!jobs.isEmpty()) {
				Job job = jobs.get(jobs.size()-1);
				stepLabel = job.getToolId();
				dateCreated = job.getCreated();
				toolInfo = getMgmToolInfo(job.getToolId(), dateCreated);				
			}

			// Note that in this method, we can use either job or dataset status as the WorkflowResult status, 
			// while in refreshResultStatus, we can only use the dataset status as the WorkflowResult status;
			// the two statuses are mostly the same in Galaxy (although this remains to be confirmed);
			// except when HMGM jobs are stopped by job manager, job status is changed to error,
			// while output dataset status doesn't change to error.
			// to be consistent, we will use dataset status in both methods.
			// Similarly, for dateCreated and dateUpdated, we will use the timestamps from dataset instead of job.
			
			// For each output, create a result record.
			Map<String, JobInputOutput> outputs = step.getOutputs();
			for (String outputName : outputs.keySet()) {
				JobInputOutput output = outputs.get(outputName);
				Dataset dataset = jobService.showJobStepOutput(invocation.getWorkflowId(), invocation.getId(), step.getId(), output.getId());
				
				// retrieve the result for this output if already existing in the WorkflowResult table
				List<WorkflowResult> oldResults = workflowResultRepository.findByOutputId(output.getId());		
				
				// if the dataset becomes discarded or deleted in Galaxy, delete any existing WorkflowResult for this output,
				// then skip this output for further WorkflowResult creation or update
				if (shouldExcludeDataset(dataset)) {
					if (oldResults != null && !oldResults.isEmpty()) {
						workflowResultRepository.deleteAll(oldResults);
						log.warn("Deleted " + oldResults.size() + " WorkflowResults for hidden/deleted Galaxy datasets: " + oldResults.get(0));
					}
					continue;
				}
				
				// otherwise, initialize result as not final
				WorkflowResult result = new WorkflowResult(); 
				result.setIsFinal(false);
				
				// go though the existing WorkflowResults for this output, if any, so we can
				// preserve the isFinal field in case it has been set; also, this allows update of existing records, 
				// otherwise we have to delete all rows before adding refreshed results in order to avoid redundancy
				if (oldResults != null && !oldResults.isEmpty()) {
					// oldresults is unique throughout Galaxy, so there should only be one result per output
					result = oldResults.get(0);
					
					// if there are more than one result then there must have been some DB inconsistency,
					// scan the results to see if any is final, if so use that one, and delete all others
					if (oldResults.size() > 1) {
						log.warn("Error in WorkflowResult table: Found " + oldResults.size() + " redundant results for output: " + output.getId());						
						for (WorkflowResult oldResult : oldResults) {
							if ((oldResult.getIsFinal() != null && oldResult.getIsFinal()) && (result.getIsFinal() == null || !result.getIsFinal())) {
								// found a final result for the first time, keep this one and delete the first result which must be non-final
								workflowResultRepository.delete(result);
								log.warn("Deleted redundant WorkflowResult " + result);						
								result = oldResult;
							}
							else if (oldResult != result) {
								// delete all non-final results except the first one, which, if is final, will be kept; 
								// otherwise will be deleted as above when the first final result is found
								workflowResultRepository.delete(oldResult);
								log.warn("Deleted redundant WorkflowResult " + oldResult);						
							}
						}
					}
				}
				
				Item item = primaryfile.getItem();
				Collection collection = item.getCollection();
				
				result.setPrimaryfileId(primaryfile.getId());
				result.setPrimaryfileName(primaryfile.getName());
				result.setItemId(item.getId());
				result.setItemName(item.getName());
				result.setExternalSource(item.getExternalSource());
				result.setExternalId(item.getExternalId());
				result.setCollectionId(collection.getId());
				result.setCollectionName(collection.getName());
				
				result.setWorkflowId(workflowId);
				result.setInvocationId(invocation.getId());
				result.setStepId(step.getId());
				result.setOutputId(output.getId());
				result.setHistoryId(invocation.getHistoryId());

				result.setWorkflowName(workflowName);
				// translate possible obsolete tool ID to standardized current tool ID
				result.setWorkflowStep(standardize(stepLabel, STANDARD_STEPS)); 
				result.setToolInfo(toolInfo);

				// translate possible obsolete output name to standardized current output name
				result.setOutputName(standardize(result.getWorkflowStep(), outputName, STANDARD_STEPS_OUTPUTS, STANDARD_OUTPUTS));
				result.setOutputType(dataset.getFileExt());
				result.setOutputPath(dataset.getFileName());
				// no need to populate/overwrite outputLink here, as it is set when output is first accessed on WorkflowResult

				result.setSubmitter(galaxyPropertyConfig.getUsername());
				result.setRelevant(dataset.getVisible()); // result is relevant if and only if its Galaxy dataset is visible
				result.setStatus(getJobStatus(dataset.getState()));
				result.setDateCreated(dataset.getCreateTime());
				result.setDateUpdated(dataset.getUpdateTime());
				result.setDateRefreshed(new Date());
				results.add(result);				
			}
		}

		workflowResultRepository.saveAll(results);
		log.debug("Successfully refreshed " + results.size() + " results for invocation " + invocation.getId() + ", workflow " + workflowId + "(" + invocation.getWorkflowId() + "), primaryfile " + primaryfile.getId());
		return results;
	}
	
	/**
	 * Translate the given name to its corresponding standard name using the given obsolete-to-standard name map.
	 */
	protected String standardize(String name, HashMap<String, String> map) {
		String standardName = map.get(name);

		// if the name doesn't exist in the map, that means it's already a standard name
		if (standardName == null)
			return name;
		
		log.debug("Standardized obsolete " + name + " to " + standardName);
		return standardName;
	}
	
	/**
	 * Translate the given name to its corresponding standard name, based on its parent name, using either the given 
	 * parent 2-layer map, or the given obsolete-to-standard name map.
	 */
	protected String standardize(String nameP, String name, HashMap<String, HashMap<String, String>> mapP, HashMap<String, String> map) {	
		HashMap<String, String> mapN = mapP.get(nameP);
		
		// if the parent's name matches a key in the parent map, use the corresponding child map
		if (mapN != null) {
			return standardize(name, mapN);
		}
		// otherwise use the obsolete-to-standard name map
		else {
			return standardize(name, map);
		}
	}
	
	/**
	 * Delete obsolete WorkflowResults, i.e. those that didn't get refreshed during the most recent whole table refresh.
	 * @return a list of the deleted WorkflowResults 
	 */
	protected List<WorkflowResult> deleteObsoleteWorkflowResults() {
		Date dateObsolete = DateUtils.addMinutes(new Date(), -REFRESH_TABLE_MINUTES);
		List<WorkflowResult> deleteResults = workflowResultRepository.findObsolete(dateObsolete);	

		if (deleteResults != null && !deleteResults.isEmpty()) {
			try {
				workflowResultRepository.deleteAll(deleteResults);
				log.info("Successfully deleted " + deleteResults.size() + " obsolete WorkflowResults");
				log.info("A sample of deleted WorkflowResults: " + deleteResults.get(0));
			}
			catch (Exception e) {
				log.error("Failed to delete " + deleteResults.size() + " obsolete WorkflowResults.", e);
			}
		}
		else {
			log.info("No obsolete WorkflowResult is found after refreshing the whole table.");				
		}
		
		return deleteResults;
	}
		
	/**
	 * @see edu.indiana.dlib.amppd.service.WorkflowResultService.hideIrrelevantWorkflowResults()
	 */	
	@Deprecated
	public Set<WorkflowResult> hideIrrelevantWorkflowResults() {		
		// get all irrelevant results from WorkflowResult table
		Set<WorkflowResult> results = new HashSet<WorkflowResult>();
		results.addAll(workflowResultRepository.findByWorkflowStepIn(HIDE_STEPS));
		results.addAll(workflowResultRepository.findByOutputNameIn(HIDE_OUTPUTS));		
		for (String[] stepOutput : HIDE_STEPS_OUTPUTS ) {
			results.addAll(workflowResultRepository.findByWorkflowStepAndOutputName(stepOutput[0], stepOutput[1]));
		}		
		log.info("Found " + results.size() + " irrelevant workflowResults in AMP table to hide");
		
		// set datasets of the irrelevant results to invisible in Galaxy
		HistoriesClient historiesClient = jobService.getHistoriesClient();
		for (WorkflowResult result : results) {
			try {
				Dataset dataset = historiesClient.showDataset(result.getHistoryId(), result.getOutputId());
				
				// no need to update if the dataset is already invisible
				if (!dataset.getVisible()) continue;
				
				// otherwise set dataset invisible
				dataset.setVisible(false);
				historiesClient.updateDataset(result.getHistoryId(), dataset);
				
				// set result to irrelevant
				result.setRelevant(false);
				log.info("Successfully hid irrelevant workflowResult in Galaxy: " + result);
			} 
			catch (Exception e) {
				throw new GalaxyWorkflowException("Failed to hide irrelevant workflowResult in Galaxy: " + result, e);
			}
		}
		log.info("Successfully hid  " + results.size() + " irrelevant workflowResults in Galaxy");		
		
		// save all updated results in WorkflowResult table
		workflowResultRepository.saveAll(results);
		log.info("Successfully saved " + results.size() + " updated irrelevant workflowResults to AMP table");	
		return results;
	}
	
	/**
	 * @see edu.indiana.dlib.amppd.service.WorkflowResultService.setWorkflowResultsRelevant(List<Map<String, String>>, Boolean)
	 */	
	public Set<WorkflowResult> setWorkflowResultsRelevant(List<Map<String, String>> workflowStepOutputs, Boolean relevant) {
		// the Set of results to be updated 
		Set<WorkflowResult> updateResults = new HashSet<WorkflowResult>();
		
		// go through all search criteria map of workflowId-workflowStep-outputName
		for (Map<String, String> workflowStepOutput : workflowStepOutputs) {
			// each search criteria map contains workflowId, workflowStep, and outputName
			String workflowId = workflowStepOutput.get("workflowId");
			String workflowStep = workflowStepOutput.get("workflowStep");
			String outputName = workflowStepOutput.get("outputName");
			
			// skip this criteria if any search field is empty
			if (StringUtils.isEmpty(workflowId) || StringUtils.isEmpty(workflowStep) || StringUtils.isEmpty(outputName) ) {
				continue;
			}
			
			// get WorkflowResults matching the current criteria and the negation of relevant indicator;
			// note that we search with the negation of relevant indicator as we only want the results that  
			// need to be updated, i.e. whose relevant indicator is the opposite of the provided value
			// a wild card "*" on a search field means match all for that field
			Set<WorkflowResult> results = null;
			if (workflowId.equals(WILD_CARD) && workflowStep.equals(WILD_CARD) && outputName.equals(WILD_CARD)) {
				results = workflowResultRepository.findByRelevant(!relevant);
			}
			else if (!workflowId.equals(WILD_CARD) && workflowStep.equals(WILD_CARD) && outputName.equals(WILD_CARD)) {
				results = workflowResultRepository.findByWorkflowIdAndRelevant(workflowId, !relevant);
			}
			else if (workflowId.equals(WILD_CARD) && !workflowStep.equals(WILD_CARD) && outputName.equals(WILD_CARD)) {
				results = workflowResultRepository.findByWorkflowStepAndRelevant(workflowStep, !relevant);
			}
			else if (workflowId.equals(WILD_CARD) && workflowStep.equals(WILD_CARD) && !outputName.equals(WILD_CARD)) {
				results = workflowResultRepository.findByOutputNameAndRelevant(outputName, !relevant);
			}
			else if (!workflowId.equals(WILD_CARD) && !workflowStep.equals(WILD_CARD) && outputName.equals(WILD_CARD)) {
				results = workflowResultRepository.findByWorkflowIdAndWorkflowStepAndRelevant(workflowId, workflowStep, !relevant);
			}
			else if (!workflowId.equals(WILD_CARD) && workflowStep.equals(WILD_CARD) && !outputName.equals(WILD_CARD)) {
				results = workflowResultRepository.findByWorkflowIdAndOutputNameAndRelevant(workflowId, outputName, !relevant);
			}
			else if (workflowId.equals(WILD_CARD) && !workflowStep.equals(WILD_CARD) && !outputName.equals(WILD_CARD)) {
				results = workflowResultRepository.findByWorkflowStepAndOutputNameAndRelevant(workflowStep, outputName, !relevant);
			}
			else if (!workflowId.equals(WILD_CARD) && !workflowStep.equals(WILD_CARD) && !outputName.equals(WILD_CARD)) {
				results = workflowResultRepository.findByWorkflowIdAndWorkflowStepAndOutputNameAndRelevant(workflowId, workflowStep, outputName, !relevant);
			}			
			
			// add current results set to the updateResults set
			// note that we use Set instead of List, as it's possible that the query with multiple criteria might return
			// redundant results; Set ensures that only distinct results are kept, to avoid redundant calls to Galaxy			
			updateResults.addAll(results);
			log.info("Found " + results.size() + " workflow results for critieria: workflowId = " + workflowId + ", workflowStep = " + workflowStep + ", outputName = " + outputName);
		}		

		// update relevant field of the matching results and visibility of the associated datasets in Galaxy
		HistoriesClient historiesClient = jobService.getHistoriesClient();
		for (WorkflowResult result : updateResults) {
			try {
				Dataset dataset = historiesClient.showDataset(result.getHistoryId(), result.getOutputId());
				dataset.setVisible(relevant);
				historiesClient.updateDataset(result.getHistoryId(), dataset);
				result.setRelevant(relevant);
				log.info("Successfully updated dataset for workflowResult in Galaxy: " + result);
			} 
			catch (Exception e) {
				throw new GalaxyWorkflowException("Failed to update dataset for workflowResult in Galaxy: " + result, e);
			}
		}
		log.info("Successfully updated visible to " + relevant + " for " + updateResults.size() + " datasets in Galaxy");		
		
		// save all updated results in WorkflowResult table
		workflowResultRepository.saveAll(updateResults);
		log.info("Successfully updated relevant to "  + relevant + " for " + updateResults.size() + " workflowResults in AMP table");	
		return updateResults;
	}
	
	/**
	 * @see edu.indiana.dlib.amppd.service.WorkflowResultService.setWorkflowResultFinal(Long, Boolean)
	 */
	public WorkflowResult setWorkflowResultFinal(Long workflowResultId, Boolean isFinal) {		
		WorkflowResult result = workflowResultRepository.findById(workflowResultId).orElseThrow(() -> new StorageException("WorkflowResult <" + workflowResultId + "> does not exist!"));
		
		// no need to update if the current isFinal value is the same as the one to be set
		if (result.getIsFinal() != null && result.getIsFinal().equals(isFinal)) { 
			return result;
		}
		
		result.setIsFinal(isFinal);
		workflowResultRepository.save(result);	
		log.info("Successfully set workflow result " + workflowResultId + " isFinal to " + isFinal);	
		return result;
	}
		
	// Map the status in Galaxy to what we want on the front end.
	protected GalaxyJobState getJobStatus(String jobStatus) {
		GalaxyJobState status = GalaxyJobState.UNKNOWN;
		if(jobStatus.equals("ok")) {
			status = GalaxyJobState.COMPLETE;
		}
		else if(jobStatus.equals("running")) {
			status = GalaxyJobState.IN_PROGRESS;
		}
		else if(jobStatus.equals("scheduled")||jobStatus.equals("new")||jobStatus.equals("queued")) {
			status = GalaxyJobState.SCHEDULED;
		}
		else if(jobStatus.equals("error")) {
			status = GalaxyJobState.ERROR;
		}
		else if(jobStatus.equals("paused")) {
			status = GalaxyJobState.PAUSED;
		}
		else if(jobStatus.equals("deleted") || jobStatus.equals("discarded")) {
			status = GalaxyJobState.DELETED;
		}
		return status;
	}
	
	/*
	 * Get the latest model/version of the specified MGM tool at the point when the Galaxy job was run.
	 * @param toolId Galaxy tool ID for the MGM tool
	 * @param invocationTime the invocation time of the Galaxy job
	 * @return the latest tool information found or null if not found
	 */
	protected String getMgmToolInfo(String toolId, Date invocationTime) {
		List<MgmTool> tools = mgmToolRepository.findLatestByToolId(toolId, invocationTime);
		if (tools == null || tools.size() == 0)
			return null;
		String info = tools.get(0).getMgmName() + " " + tools.get(0).getVersion();
		return info;
	}

	@Override
	public List<WorkflowResult> exportWorkflowResults(HttpServletResponse response, WorkflowResultSearchQuery query) {
		log.info("Exporting current dashboard to CSV file ...");
		
		try {
			long totalResults = workflowResultRepository.count();
			query.setResultsPerPage((int)totalResults);
			query.setPageNum(1);
			WorkflowResultResponse wresponse = getWorkflowResults(query);
			
			ICsvMapWriter csvWriter = new CsvMapWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);			
			String[] csvHeader = {
					"WorkflowResult ID", 
					"Date", "Submitter", 
					"Collection ID", 
					"Collection", 
					"Item ID", 
					"Item", 
					"Primaryfile ID", 
					"Primaryfile", 
					"Primaryfile URL", 
					"Workflow", 
					"Workflow Step", 
					"Output ID", 
					"Output", 
					"Output URL", 
					"Status"};
			csvWriter.writeHeader(csvHeader);

			for (WorkflowResult r : wresponse.getRows()) {
				Map<String, Object> output = new HashMap<String, Object>();
				output.put(csvHeader[0], r.getId());
				output.put(csvHeader[1], r.getDateCreated());
				output.put(csvHeader[2], r.getSubmitter());
				output.put(csvHeader[3], r.getCollectionId());
				output.put(csvHeader[4], r.getCollectionName());
				output.put(csvHeader[5], r.getItemId());
				output.put(csvHeader[6], r.getItemName());
				output.put(csvHeader[7], r.getPrimaryfileId());
				output.put(csvHeader[8], r.getPrimaryfileName());
				output.put(csvHeader[9], mediaService.getPrimaryfileMediaUrl(r.getPrimaryfileId()));
				output.put(csvHeader[10], r.getWorkflowName());
				output.put(csvHeader[11], r.getWorkflowStep());
				output.put(csvHeader[12], r.getOutputId()); 
				output.put(csvHeader[13], r.getOutputName()); 
				output.put(csvHeader[14], mediaService.getWorkflowResultOutputUrl(r.getId())); 
				output.put(csvHeader[15], r.getStatus());
				csvWriter.write(output, csvHeader);				
			}
			
			csvWriter.close();
			log.info("Successfully exported current dashboard to CSV file.");
			return wresponse.getRows();
		} catch (IOException e) {
			throw new RuntimeException("Failed to export current dashboard to CSV file.", e);
		}
	}
	
}
