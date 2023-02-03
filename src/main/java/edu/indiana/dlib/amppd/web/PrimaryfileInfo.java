package edu.indiana.dlib.amppd.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class PrimaryfileInfo {
	Long id;
	String name;
	String mimeType;
	String originalFilename;
}