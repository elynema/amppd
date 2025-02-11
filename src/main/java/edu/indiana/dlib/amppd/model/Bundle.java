package edu.indiana.dlib.amppd.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Bundle is a container of primaryfiles to which similar workflows can be applied.
 * @author yingfeng
 *
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
// Lombok's impl of toString, equals, and hashCode doesn't handle circular references as in Bundle and Item and will cause StackOverflow exception.
public class Bundle extends Dataentity {

	// let Bundle owns the many to many relationship, since conceptually bundle is the container of primaryfiles, 
	// and our use case is often updating bundle's primaryfiles instead of the other way around
	@ManyToMany
    @JoinTable(name = "bundle_primaryfile", joinColumns = @JoinColumn(name = "bundle_id"), inverseJoinColumns = @JoinColumn(name = "primaryfile_id"))
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
//	@JsonManagedReference	
    private Set<Primaryfile> primaryfiles;

//	@ManyToMany(mappedBy = "bundles")
//    private Set<Item> items;
    
//    @ManyToMany(mappedBy = "bundles")
//    private Set<InputBag> bags;

}
  

