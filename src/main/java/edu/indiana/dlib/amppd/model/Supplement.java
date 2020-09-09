package edu.indiana.dlib.amppd.model;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Supplement is a file (either media or annotation) used as supplemental material to assist metadata retrieval for a primaryfile through a workflow.
 * @author yingfeng
 *
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public abstract class Supplement extends Asset {
	
	// TODO double check the relationship
//	@ManyToMany
//	private Set<InputBag> bags;
	
}
