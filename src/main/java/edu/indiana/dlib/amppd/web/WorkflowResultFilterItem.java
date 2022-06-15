package edu.indiana.dlib.amppd.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
//not using superclass because lombok @AllArgsConstructor doesn't handle super fields
public class WorkflowResultFilterItem { // extends WorkflowResultFilterCollection {	
	Long unitId;
	String unitName;	
	Long collectionId;
	String collectionName;	
	Long itemId;
	String itemName;	
	String externalId;
	String externalSource;	
}
