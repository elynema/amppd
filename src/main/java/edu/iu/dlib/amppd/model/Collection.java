package edu.iu.dlib.amppd.model;

import java.util.ArrayList;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Collection {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long unitId;
    
    private String name;
    private String description;
    private String creator;
    private Date dateCreated;

    private ArrayList<Item> items;
}
