package edu.indiana.dlib.amppd.repository;

import java.util.List;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import edu.indiana.dlib.amppd.model.ItemSupplement;
import edu.indiana.dlib.amppd.model.projection.ItemSupplementBrief;


@RepositoryRestResource(excerptProjection = ItemSupplementBrief.class)
public interface ItemSupplementRepository extends SupplementRepository<ItemSupplement> {
	
	List<ItemSupplement> findByItemId(Long itemId);
	List<ItemSupplement> findByItemIdAndName(Long itemId, String name);
	List<ItemSupplement> findByItemIdAndCategory(Long itemId, String category); 
	List<ItemSupplement> findByItemIdAndCategoryAndOriginalFilenameEndsWith(Long itemId, String category, String format); 
	List<ItemSupplement> findByItemIdAndNameAndCategoryAndOriginalFilenameEndsWith(Long itemId, String name, String category, String format); 

}
