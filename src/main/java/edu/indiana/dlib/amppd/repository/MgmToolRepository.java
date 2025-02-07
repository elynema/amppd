package edu.indiana.dlib.amppd.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import edu.indiana.dlib.amppd.model.MgmScoringTool;
import edu.indiana.dlib.amppd.model.MgmTool;
import edu.indiana.dlib.amppd.model.projection.MgmToolBrief;


@RepositoryRestResource(excerptProjection = MgmToolBrief.class)
public interface MgmToolRepository extends MgmMetaRepository<MgmTool> {
	
	// find all scoring tools within the given category
	List<MgmScoringTool> findByCategoryId(Long categoryId);

	// find the MGM of the given toolId
	// since toolId is unique, it's safe to findFirstBy
	MgmTool findFirstByToolId(String toolId);

	// find the MGM of the given name within the given category;
	// since name is unique within category, it's safe to findFirstBy
	MgmScoringTool findFirstByCategoryIdAndName(Long categoryId, String name);

	// delete obsolete record
	List<MgmTool> deleteByModifiedDateBefore(Date dateObsolete);
	
}
