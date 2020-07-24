package edu.indiana.dlib.amppd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

import edu.indiana.dlib.amppd.model.Bundle;
import edu.indiana.dlib.amppd.model.Item;

@CrossOrigin(origins = "*")
@RepositoryRestResource(collectionResourceRel = "bundles", path = "bundles")
public interface BundleRepository extends DataentityRepository<Bundle> {
	
	@Query(value = "select i from Item i where lower(i.name) like lower(concat('%', :keyword,'%')) or lower(i.description) like lower(concat('%', :keyword,'%'))")
	List<Bundle> findByNameAndCreatedBy(String name, String createdBy);
	
}
