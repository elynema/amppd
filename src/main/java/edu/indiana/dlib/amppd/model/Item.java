package edu.indiana.dlib.amppd.model;


import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;

/**
 * Item represents an intellectual object that contains one or more primaryfiles and none or multiple supplement files.
 * @author yingfeng
 *
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class Item extends Content {
    
	@OneToMany(mappedBy="item")
    private List<ItemSupplement> supplements;

	@ManyToOne
	private Collection collection;	
		
}