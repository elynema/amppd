package edu.indiana.dlib.amppd.service;

import com.github.jmchilton.blend4j.galaxy.LibrariesClient;
import com.github.jmchilton.blend4j.galaxy.beans.GalaxyObject;
import com.github.jmchilton.blend4j.galaxy.beans.Library;

/**
 * Service to provide convenient helpers to operate on Galaxy data libraries, datasets, histories etc. 
 * @author yingfeng
 *
 */
public interface GalaxyDataService {

	/**
	 * Return the libraryClient instance.
	 */
	public LibrariesClient getLibrariesClient();
	
	/**
	 * Return the shared amppd data library.
	 */
	public Library getSharedLibrary();

	/**
	 * Return the data library for the given name, or null if not found.
	 * @param name the the given library name
	 */
	public Library getLibrary(String name);

	/**
	 * Upload a file/folder from AMP file system to a Galaxy data library without copying the physical file. 
	 * @param filePath the path of the source file/folder to be uploaded
	 * @param libraryName the name of the target library to upload file to  
	 */
	public GalaxyObject uploadFileToGalaxy(String filePath, String libraryName);

	/**
	 * Upload a file/folder from AMP file system to the shared amppd Galaxy data library without copying the physical file. 
	 * @param filePath the path of the source file/folder to be uploaded
	 */
	public GalaxyObject uploadFileToGalaxy(String filePath);

}