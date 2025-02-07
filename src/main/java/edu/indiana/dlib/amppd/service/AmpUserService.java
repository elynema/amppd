package edu.indiana.dlib.amppd.service;

import edu.indiana.dlib.amppd.model.AmpUser;
import edu.indiana.dlib.amppd.web.AuthResponse;

/**
 * Service for AmpUser related functions.
 * @author vinitab
 *
 */

public interface AmpUserService {
	

	/**
	 * Validate the login credentials against database.
	 * @param username is the entered username
	 * @param pswd is the entered password
	 * @return the result of validation
	 */
	public AuthResponse authenticate(String username, String pswd);
	
	/**
	 * Register the new user and make entry to database.
	 * @param user contains new user data
	 * @return the result of registration
	 */
	public AuthResponse registerAmpUser(AmpUser user);
	
	/**
	 * Sets amp user as approved to login to the application
	 * @param user name
	 * @return the result of setting the user to approved as success/failure
	 */
	public boolean activateUser(String userName);
	
	/**
	 * Resets the existing password in the database for the given username
	 * @param username, new password to be updated in the DB, secure token
	 * @return the boolean result for update in the databse
	 */
	public AuthResponse resetPassword(String userName, String new_password, String token);
	
	/**
	 * Generates a token and sends it in an email to the provided email id
	 * @param email id
	 * @return the result for sending the email
	 */
	public AuthResponse emailResetPasswordToken(String emailid);
	
	/**
	 * Generates an account verification email for the new and sets the approve_status flag to the specified action in the database. 
	 * @param user id
	 * @return the response object consisting of the database update result and errors, if any.
	 */
	public AuthResponse accountAction(Long userID, String action);

	/**
	 * Fetches the user id from the database for a given token if it is not expired. 
	 * @param token
	 * @return user id for an unexpired token.
	 */
	public AuthResponse activateAccount(String token);
	
	/**
	 * Gets an amp emailid by token. It returns the emailid to the reset password page for prepopulation there. 
	 * If the email id is not found it returns error
	 * @param token
	 * @return the emailid in AuthResponse instance
	 */
	public AuthResponse resetPasswordGetEmail(String token);

  /**
	 * Gets an amp user by username.  Will return null if the amp user is not found.  This does not take into account 
	 * whether or not user is approved.
	 * @param username
	 * @return AmpUser or null, depending on whether the user was found or not
	 */
	public AmpUser getUser(String username);
	
	/**
	 * Gets an AMP user by Id. whether or not user is approved.
	 * Will throw Exception  if the AMP user is not found.
	 * @param userId
	 * @return AmpUser
	 */
	public AmpUser getUserById(Long userId);
	
	/**
	 * Gets the current username from User Session.
	 * @return the current username
	 */
	public String getCurrentUsername();
	
	/**
	 * Gets the current user from User Session.
	 * @return the current user
	 */
	public AmpUser getCurrentUser();
		
	/**
	 * Bootstrap the AMP admin user as the first AMP user upon AMP app start, by creating the user with ACTIVATED status in DB.
	 * @return the AMP admin user created
	 */
	public AmpUser bootstrapAdmin();
	
}