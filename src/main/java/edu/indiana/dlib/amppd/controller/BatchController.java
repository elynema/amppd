package edu.indiana.dlib.amppd.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.indiana.dlib.amppd.config.AmppdPropertyConfig;
import edu.indiana.dlib.amppd.model.AmpUser;
import edu.indiana.dlib.amppd.service.AmpUserService;
import edu.indiana.dlib.amppd.service.BatchService;
import edu.indiana.dlib.amppd.service.BatchValidationService;
import edu.indiana.dlib.amppd.web.BatchValidationResponse;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class BatchController {
	@Autowired
	private BatchValidationService batchValidationService;
	@Autowired
	private BatchService batchService;
	@Autowired
	private AmpUserService ampUserService;
	@Autowired
	private AmppdPropertyConfig ampPropertyConfig;
	
	@PostMapping(path = "/batch/ingest", consumes = "multipart/form-data", produces = "application/json")
	public @ResponseBody BatchValidationResponse batchIngest(@RequestPart MultipartFile file, @RequestPart String unitName) {	
		
		/*
		 * THIS IS TEMPORARY UNTIL AUTHENTICATION WORKS
		 */

		String pilotUsername = ampPropertyConfig.getUsername();
		AmpUser ampUser = ampUserService.getUser(pilotUsername);
		if(ampUser==null) {
			BatchValidationResponse response  = new BatchValidationResponse();
			response.addError("Invalid user " + pilotUsername);
		}
		
		BatchValidationResponse response = batchValidationService.validateBatch(unitName, ampUser, file);
		
		if(response.isSuccess()) {
			List<String> errors = batchService.processBatch(response, ampUser.getUsername());
			boolean batchSuccess = errors.size()==0;
			response.setProcessingErrors(errors);
			response.setSuccess(batchSuccess);
		}
		
		return response;
	}
}
