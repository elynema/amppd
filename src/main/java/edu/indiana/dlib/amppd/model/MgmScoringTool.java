package edu.indiana.dlib.amppd.model;

import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.opencsv.bean.CsvBindByName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * This class contains information about an MGM Scoring Tool (MST) associated with an executable script,
 * to compute evaluation scores of its corresponding MGM.
 * Note that the table for this class is manually maintained, i.e. each time a new MST (or a new version of it) 
 * is installed, this table shall be updated with that information. 
 * @author yingfeng
 *
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(indexes = {
		@Index(columnList = "name"),
		@Index(columnList = "category_id")
})
@Data
@EqualsAndHashCode(callSuper=true, onlyExplicitlyIncluded=true)
@ToString(callSuper=true, onlyExplicitlyIncluded=true)
public class MgmScoringTool extends AmpObject {
       
    // must be uqniue within its parent category
    @NotBlank
	@CsvBindByName(column = "Name")
    private String name;
        
    @NotBlank
    @Column(columnDefinition="TEXT")
	@CsvBindByName(column = "Description")
    private String description;
        
	// current version
    @NotBlank
	@CsvBindByName(column = "Version")
    private String version;	
    
	// date when the current version is installed 
    @NotNull
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
	@CsvBindByName(column = "UpgradeDate")
    private Date upgradeDate;

	// path of the executable script of the MST, relative to the script root directory
	@NotBlank
	@CsvBindByName(column = "ScriptPath")
    private String scriptPath; 
    
    // output data type of the workflow result to be scored by the MST
    @NotBlank
	@CsvBindByName(column = "WorkflowResultType")
    private String workflowResultType; 
    
    // format (extentions) of the groundtruth file used by the MST
    @NotBlank
	@CsvBindByName(column = "GroundtruthFormat")
    private String groundtruthFormat; 
    
	// static info of the parameters
	@OneToMany(mappedBy="mst", cascade = CascadeType.REMOVE)
	@JsonBackReference(value="parameters")
    private Set<MgmScoringParameter> parameters; 
    
	// category of the applicable MGMs for evaluation, corresponding to the Galaxy tool section
	@NotNull
	@ManyToOne
    private MgmCategory category;     
        
}
