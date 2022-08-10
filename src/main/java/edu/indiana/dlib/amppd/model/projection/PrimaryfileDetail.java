package edu.indiana.dlib.amppd.model.projection;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import edu.indiana.dlib.amppd.model.Primaryfile;


/**
 * Projection for a detailed view of a primaryfile.
 * @author yingfeng
 */
@Projection(name = "detail", types = {Primaryfile.class}) 
public interface PrimaryfileDetail extends PrimaryfileBrief, AssetDetail {

	public String getHistoryId();	
	public String getDatasetId();
	public Set<PrimaryfileSupplementBrief> getSupplements();

	@Value("#{target.item.id}")
	public String getItemId();
	
	@Value("#{target.item.collection.id}")
	public String getCollectionId();	
	
	@Value("#{target.item.collection.unit.id}")
	public String getUnitId();
	
}
