package edu.indiana.dlib.amppd.service.impl;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import edu.indiana.dlib.amppd.config.AmppdPropertyConfig;
import edu.indiana.dlib.amppd.model.AmpUser;
import edu.indiana.dlib.amppd.model.Passwordresettoken;
import edu.indiana.dlib.amppd.repository.AmpUserRepository;
import edu.indiana.dlib.amppd.repository.PasswordTokenRepository;
import edu.indiana.dlib.amppd.service.AmpUserService;
import edu.indiana.dlib.amppd.util.MD5Encryption;
import edu.indiana.dlib.amppd.web.AuthResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AmpUserServiceImpl implements AmpUserService, UserDetailsService {

	public final int MIN_PASSWORD_LENGTH = 8;
	public final int MIN_USERNAME_LENGTH = 3;

	@Autowired
	private AmpUserRepository ampUserRepository;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private PasswordTokenRepository passwordTokenRepository;

	@Autowired
	private AmppdPropertyConfig ampPropertyConfig;

	private MessageSource messages;

	@NotNull
	private static String ampEmailId ;

	@NotNull
	private static String ampAdmin ;

	@NotNull
	private static String uiUrl ;

	@Autowired 
	public AmpUserServiceImpl(AmppdPropertyConfig amppdconfig) { 
		ampEmailId = amppdconfig.getUsername();
		uiUrl = amppdconfig.getUiUrl();
		ampAdmin = amppdconfig.getAdmin();
	}	 

	public AuthResponse validate(String email, String pswd) { 
		AuthResponse response = new AuthResponse();

		if(!passwordAcceptableLength(pswd)) {
			response.addError("Email and password do not match");
		}
		String encryptedPswd = MD5Encryption.getMd5(pswd);
		String userFound = ampUserRepository.findByApprovedUser(email, encryptedPswd);  
		if(userFound != null)
		{
			if(userFound.equals("1")) {
				response.setSuccess(true);
			}
		}
		return response;
	}

	public AmpUser getUser(String username) {
		Optional<AmpUser> userOpt = ampUserRepository.findByUsername(username);
		if(userOpt.isPresent()) return userOpt.get();

		return null;
	}

	public boolean approveUser(String username) {
		try {
			AmpUser user = ampUserRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found: " + username));
			//			  AmpUser user = ampUserRepository.findByUsername(userName).orElseThrow(() -> new RuntimeException("User not found: " + username));
			//			  if(users.size()==0) return false;
			//			  AmpUser user = users.get(0);
			user.setApproved(true);
			ampUserRepository.save(user);
		}
		catch(Exception ex) {
			System.out.println(ex.toString());
		}
		return false;		  
	}

	public AuthResponse registerAmpUser(AmpUser user) { 

		AuthResponse response = new AuthResponse();

		if(!usernameAcceptableLength(user.getUsername())) {
			response.addError("Username must be " + MIN_USERNAME_LENGTH + " characters");
		}
		else if(!usernameUnique(user.getUsername())) {
			response.addError("Username already taken.");
		}

		if(!emailUnique(user.getEmail())) {
			response.addError("Email already exists");
		}
		if(!passwordAcceptableLength(user.getPassword())) {
			response.addError("Password must be " + MIN_PASSWORD_LENGTH + " characters");
		}

		if(!validEmailAddress(user.getEmail())) {
			response.addError("Invalid email address");
		}

		if(!response.hasErrors()) {
			user.setPassword(MD5Encryption.getMd5(user.getPassword()));
			user = ampUserRepository.save(user);
			if(user!=null && user.getId() > 0) 
			{
				try {
					mailSender.send(constructRegisterEmail(uiUrl, user, "approve"));
				} 
				catch (MailException e) {
					e.printStackTrace();
				}
				response.setSuccess(true);//new GenericResponse(messages.getMessage("message.resetPasswordEmail", null, request.getLocale()));
			}
			else {
				response.addError("Error creating user account");
			}
		}

		return response;
	}

	private boolean usernameUnique(String username) {
		return !ampUserRepository.usernameExists(username);  
	}

	private boolean emailUnique(String username) {
		return !ampUserRepository.emailExists(username);  
	}

	private boolean usernameAcceptableLength(String username) {
		return username.length() >= MIN_USERNAME_LENGTH;
	}

	private boolean passwordAcceptableLength(String password) {
		return password.length() >= MIN_PASSWORD_LENGTH;
	}

	private boolean validEmailAddress(String email) {
		Pattern pattern = Pattern.compile("(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)");  

		Matcher matcher = pattern.matcher(email);

		return matcher.matches();
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AmpUser user = ampUserRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found: " + username));
		GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().name());
		return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), Arrays.asList(authority));
	}

	@Override
	public AuthResponse emailToken(String emailid) {
		// TODO Auto-generated method stub
		AuthResponse response = new AuthResponse();
		AmpUser user = ampUserRepository.findByEmail(emailid).orElseThrow(() -> new RuntimeException("User not found: " + emailid));
		if(user.getApproved())
		{
			String token = UUID.randomUUID().toString();
			createPasswordResetTokenForUser(user, token);
			try {
				mailSender.send(constructResetTokenEmail(uiUrl, token, user));
			} catch (MailException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			response.setSuccess(true);//new GenericResponse(messages.getMessage("message.resetPasswordEmail", null, request.getLocale()));
		}
		return response;
	}

	private SimpleMailMessage constructRegisterEmail(String contextPath, AmpUser user, String type) {
		String subject = null;
		String emailTo = null;
		String message = null;
		String url = null;
		if (type.equalsIgnoreCase("approve"))
		{
			url = contextPath + "/approve-user/" + user.getId();
			message = "A new user has registered and waiting approval. \n\n User Name:"+ user.getUsername()+"\n User Email: "+user.getEmail()+ "\n User ID: "+user.getId()+
					"\n\n Click the link below to view and approve the new user. \n";
			subject = "New User Approval";
			emailTo = ampAdmin;

		}
		else if (type.equalsIgnoreCase("activate"))
		{
			url = contextPath + "/activate-account/" + user.getId();
			message = "Click the link below to activate your AMP account";
			subject = "Activate Account";
			emailTo = user.getEmail();
		}
		return constructEmail(subject, message + " \r\n" + url, emailTo);
	}

	public void createPasswordResetTokenForUser(AmpUser user, String token) {

		Passwordresettoken myToken=new Passwordresettoken();
		Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
		calendar.add(Calendar.SECOND,Passwordresettoken.EXPIRATION);
		int userTokenExists = passwordTokenRepository.ifExists(user.getId());
		if(userTokenExists == 1)
		{
			passwordTokenRepository.updateToken(token, user.getId(), calendar.getTime());
		}
		else
		{
			myToken.setUser(user);
			myToken.setToken(token);
			myToken.setExpiryDate(calendar.getTime());
			passwordTokenRepository.save(myToken);
		}
	}

	private SimpleMailMessage constructResetTokenEmail(String contextPath, String token, AmpUser user) {
		String url = contextPath + "/reset-password/" + token;
		String message = "Please click the link to reset the password. The link  will be valid only for a limited time.";//messages.getMessage("message.resetPassword", null, locale);
		return constructEmail("Reset Password", message + " \r\n" + url, user.getEmail());
	}

	private SimpleMailMessage constructEmail(String subject, String body, String toEmailID) {
		SimpleMailMessage email = new SimpleMailMessage();
		email.setSubject(subject);
		email.setText(body);
		email.setTo(toEmailID);
		email.setFrom(ampEmailId);
		return email;
	}  

	@Override
	public AuthResponse resetPassword(String emailid, String new_password, String token) {
		// TODO Auto-generated method stub
		AuthResponse response = new AuthResponse();
		AmpUser user = ampUserRepository.findByEmail(emailid).orElseThrow(() -> new RuntimeException("User not found: " + emailid));
		Passwordresettoken passToken = (passwordTokenRepository.findByToken(token)).orElseThrow(() -> new RuntimeException("token not found: " + token));
		if ((passToken == null) || (user == null) || (passToken.getUser().getId() != user.getId())) {
			response.addError("Incorrect Link");
			response.setSuccess(false);
			//return response;
		}
		Calendar cal = Calendar.getInstance();
		if ((passToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
			response.addError("Link Expired");
			response.setSuccess(false);
		}

		if(!response.hasErrors()) {
			String new_encrypted_pswd = MD5Encryption.getMd5(new_password);
			int rows = ampUserRepository.updatePassword(user.getUsername(), new_encrypted_pswd, user.getId()); 
			if(rows > 0)
			{
				response.setSuccess(true);
			}
		}

		return response;
	}

	@Override 
	public AuthResponse approveUser(Long userId)
	{
		AuthResponse response = new AuthResponse();
		AmpUser user = ampUserRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found: " + userId));
		if(user == null)
		{
			response.addError("Incorrect Link");
			response.setSuccess(false);
		}
		if(!response.hasErrors()) {
			try {
				mailSender.send(constructRegisterEmail(uiUrl, user, "activate"));
			}
			catch (MailException e) 
			{
				e.printStackTrace();
			}
			int rows = ampUserRepository.updateApproved(userId);
			if(rows > 0)
			{
				response.setSuccess(true);
			}
		}
		return response;
	}

	/**
	 * @see edu.indiana.dlib.amppd.service.AmpUserService.getCurrentUsername()
	 */
	@Override
	public String getCurrentUsername() {
		// TODO replace below tmp code with logic to get the current username from User Session		
		// tmp code: return the default master AMP user for now
		return ampPropertyConfig.getUsername();
	}

	/**
	 * @see edu.indiana.dlib.amppd.service.AmpUserService.getCurrentUser()
	 */
	@Override
	public AmpUser getCurrentUser() {
		// TODO replace below tmp code with logic to get the current user from User Session		
		// tmp code: return the default master AMP user for now
		String username = ampPropertyConfig.getUsername();
		AmpUser currentUser = getUser(username);		
		if (currentUser == null) {
			throw new RuntimeException("Current user with username " + username + " doesn't exist!");
		}
		return currentUser;

//		List<AmpUser> users = ampUserRepository.findAllByUsername(username);
//		if (users.size() == 0) {
//			throw new RuntimeException("Current user with username " + username + " doesn't exist!");
//		}
//		else if (users.size() > 1) {
//			throw new RuntimeException("Current user with username " + username + " is not unique!");
//		}
//		else {
//			return users.get(0);
//		}
	}
}