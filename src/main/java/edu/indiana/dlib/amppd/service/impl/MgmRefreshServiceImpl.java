package edu.indiana.dlib.amppd.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opencsv.bean.CsvToBeanBuilder;

import edu.indiana.dlib.amppd.model.MgmCategory;
import edu.indiana.dlib.amppd.model.MgmScoringParameter;
import edu.indiana.dlib.amppd.model.MgmScoringTool;
import edu.indiana.dlib.amppd.model.MgmTool;
import edu.indiana.dlib.amppd.repository.MgmCategoryRepository;
import edu.indiana.dlib.amppd.repository.MgmScoringParameterRepository;
import edu.indiana.dlib.amppd.repository.MgmScoringToolRepository;
import edu.indiana.dlib.amppd.repository.MgmToolRepository;
import edu.indiana.dlib.amppd.service.MgmRefreshService;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of MgmRefreshService
 * @author yingfeng
 *
 */
@Service
@Slf4j
public class MgmRefreshServiceImpl implements MgmRefreshService {

	public static final String DIR = "db";
	public static final String MGM_CATEGORY = "mgm_category";
	public static final String MGM_TOOL = "mgm_tool";
	public static final String MGM_SCORING_TOOL = "mgm_scoring_tool";
	public static final String MGM_SCORING_PARAMETER = "mgm_scoring_parameter";
	 
	@Autowired
	private MgmCategoryRepository mgmCategoryRepository;
	
	@Autowired
	private MgmToolRepository mgmToolRepository;
	
	@Autowired
	private MgmScoringToolRepository mgmScoringToolRepository;
	
	@Autowired
	private MgmScoringParameterRepository mgmScoringParameterRepository;
	
	
	/**
	 * @see edu.indiana.dlib.amppd.service.MgmRefreshService.refreshMgmTables()
	 */
	@Override
    @Transactional
	public void refreshMgmTables() {
		refreshMgmCategory();
//		refreshMgmTool();
//		refreshMgmScoringTool();	
//		refreshMgmScoringParameter();	
	}
	
	/**
	 * @see edu.indiana.dlib.amppd.service.MgmRefreshService.refreshMgmCategory()
	 */
	@Override
    @Transactional
	public List<MgmCategory> refreshMgmCategory() {
		List<MgmCategory> categories = new ArrayList<MgmCategory>();
		String filename = DIR + "/" + MGM_CATEGORY + ".csv"; 
		BufferedReader breader = null;
		
		// open mgm_category.csv
		try {
			breader = new BufferedReader(new InputStreamReader(new ClassPathResource(filename).getInputStream()));
		}
		catch(Exception e) {
			throw new RuntimeException("Failed to refresh MgmCategory table: unable to open " + filename, e);
		}		
		
		// parse the csv into list of MgmCategory objects
		try {
			categories = new CsvToBeanBuilder<MgmCategory>(breader).withType(MgmCategory.class).build().parse();
		}
		catch(Exception e) {
			throw new RuntimeException("Failed to refresh MgmCategory table: invalid CSV format with " + filename, e);
		}
		
		// record the refresh start time
		Date refreshStart = new Date();
		
		// save the MgmCategory objects
		// note: we can just save all categories directly, as that would create new records in the table;
		// instead, we need to find each existing record based on ID and update it
		// TODO: check that category.sectionId exists in Galaxy and report error if not
		for (MgmCategory category : categories) {
			MgmCategory existCategory = mgmCategoryRepository.findFirstBySectionId(category.getSectionId());			
			if (existCategory != null) {
				category.setId(existCategory.getId());				
			}
			mgmCategoryRepository.save(category);	
		}		
		
		// delete all obsolete categories, i.e. those not updated during this pass of refresh;
		// based on the assumption that the csv file includes all the categories we want to keep 
		List<MgmCategory> deletedCategories = mgmCategoryRepository.deleteByModifiedDateBefore(refreshStart);
		log.info("Deleted " + deletedCategories.size() + " obsolete categories older than current refresh start time at " + refreshStart);			
				
		log.info("Successfully refreshed " + categories.size() + " categories from " + filename);
		return categories;
	}

	/**
	 * @see edu.indiana.dlib.amppd.service.MgmRefreshService.refreshMgmTool()
	 */
	@Override
    @Transactional
	public List<MgmTool> refreshMgmTool() {
		List<MgmTool> mgms = new ArrayList<MgmTool>();
		String filename = DIR + "/" + MGM_TOOL + ".csv"; 
		BufferedReader breader = null;
		
		// open mgm_tool.csv
		try {
			breader = new BufferedReader(new InputStreamReader(new ClassPathResource(filename).getInputStream()));
		}
		catch(Exception e) {
			throw new RuntimeException("Failed to refresh MgmTool table: unable to open " + filename, e);
		}		
		
		// parse the csv into list of MgmTool objects
		try {
			mgms = new CsvToBeanBuilder<MgmTool>(breader).withType(MgmTool.class).build().parse();
		}
		catch(Exception e) {
			throw new RuntimeException("Failed to refresh MgmTool table: invalid CSV format with " + filename, e);
		}
		
		// save the MgmTool objects
		mgmToolRepository.saveAll(mgms);		
		
		log.info("Successfully refreshed MgmTool table from " + filename);
		return mgms;
	}
	
	/**
	 *  @see edu.indiana.dlib.amppd.service.MgmRefreshService.refreshMgmScoringTool()
	 */
	@Override
    @Transactional
	public List<MgmScoringTool> refreshMgmScoringTool() {
		List<MgmScoringTool> msts = new ArrayList<MgmScoringTool>();
		String filename = DIR + "/" + MGM_SCORING_TOOL + ".csv"; 
		BufferedReader breader = null;
		
		// open mgm_scoring_tool.csv
		try {
			breader = new BufferedReader(new InputStreamReader(new ClassPathResource(filename).getInputStream()));
		}
		catch(Exception e) {
			throw new RuntimeException("Failed to refresh MgmScoringTool table: unable to open " + filename, e);
		}		
		
		// parse the csv into list of MgmScoringTool objects
		try {
			msts = new CsvToBeanBuilder<MgmScoringTool>(breader).withType(MgmScoringTool.class).build().parse();
		}
		catch(Exception e) {
			throw new RuntimeException("Failed to refresh MgmScoringTool table: invalid CSV format with " + filename, e);
		}
		
		// save the MgmScoringTool objects
		mgmScoringToolRepository.saveAll(msts);		
		
		log.info("Successfully refreshed MgmScoringTool table from " + filename);
		return msts;
	}
	
	/**
	 *  @see edu.indiana.dlib.amppd.service.MgmRefreshService.refreshMgmScoringParameter()
	 */
	@Override
    @Transactional
	public List<MgmScoringParameter> refreshMgmScoringParameter() {
		List<MgmScoringParameter> parameters = new ArrayList<MgmScoringParameter>();
		String filename = DIR + "/" + MGM_SCORING_PARAMETER + ".csv"; 
		BufferedReader breader = null;
		
		// open mgm_scoring_parameter.csv
		try {
			breader = new BufferedReader(new InputStreamReader(new ClassPathResource(filename).getInputStream()));
		}
		catch(Exception e) {
			throw new RuntimeException("Failed to refresh MgmScoringParameter table: unable to open " + filename, e);
		}		
		
		// parse the csv into list of MgmScoringParameter objects
		try {
			parameters = new CsvToBeanBuilder<MgmScoringParameter>(breader).withType(MgmScoringParameter.class).build().parse();
		}
		catch(Exception e) {
			throw new RuntimeException("Failed to refresh MgmScoringParameter table: invalid CSV format with " + filename, e);
		}
				
		// save the MgmScoringParameter objects
		mgmScoringParameterRepository.saveAll(parameters);		
		
		log.info("Successfully refreshed MgmScoringParameter table from " + filename);
		return parameters;
	}
		
}
