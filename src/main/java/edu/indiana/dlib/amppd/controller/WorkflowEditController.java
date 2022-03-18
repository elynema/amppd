package edu.indiana.dlib.amppd.controller;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.WebUtils;

import edu.indiana.dlib.amppd.config.AmppdPropertyConfig;
import edu.indiana.dlib.amppd.config.GalaxyPropertyConfig;
import edu.indiana.dlib.amppd.exception.GalaxyWorkflowException;
import edu.indiana.dlib.amppd.security.JwtTokenUtil;
import edu.indiana.dlib.amppd.web.GalaxyLoginRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller to proxy requests related to workflow edit between AMP UI and Galaxy workflow editor.
 * @author yingfeng
 */
@RestController
@Slf4j
public class WorkflowEditController {

	// workflow edit cookie name
	public static final String GALAXY_PATH = "/galaxy";
	
	// workflow edit cookie name
	public static final String WORKFLOW_EDIT_COOKIE = "workflowEdit";
	
	@Autowired
	private AmppdPropertyConfig amppdPropertyConfig;	
	
	@Autowired
	private GalaxyPropertyConfig galaxyPropertyConfig;
		
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	
	@Autowired
	private ServletContext context;
	
//	@Autowired
	private RestTemplate restTemplate = new RestTemplate();
	
	private String csrfToken = null;
	private String galaxySession = null;

	/**
	 *  Upon initialization of the controller, 
	 *  login to galaxy as AMP workflow edit user and set up galaxy session for workflow edit.
	 */
	@PostConstruct
	public ResponseEntity<String> loginGalaxy() {
		// request Galaxy server for login page
		String urlRootLogin = galaxyPropertyConfig.getBaseUrl() + "/root/login";
		ResponseEntity<String> responseRootLogin = restTemplate.getForEntity(urlRootLogin, String.class);
		galaxySession = responseRootLogin.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
		
		// retrieve CSRF token from the response body using regex
		// TODO do we need to enforce utf-8
		Pattern pattern = Pattern.compile("\"session_csrf_token\": \"(.+?)\"");
	    Matcher matcher = pattern.matcher(responseRootLogin.getBody());
	    if (!matcher.find() ) {
	    	throw new GalaxyWorkflowException("Could not get CSRF token from Galaxy for login.");
	    }
    	csrfToken = matcher.group(1);

    	// populate login request header with previously returned galaxySession cookie
    	HttpHeaders headers = new HttpHeaders();
    	headers.add(HttpHeaders.COOKIE, galaxySession);
    	headers.setContentType(MediaType.APPLICATION_JSON);
    	
    	// populate login request payload with workflow editor user credentials and CSRF token
    	String urlRedirect = "/"; //"/workflow/editor?id=" + workflowId;
    	GalaxyLoginRequest glr = new GalaxyLoginRequest(
    			galaxyPropertyConfig.getUsernameWorkflowEdit(),
    			galaxyPropertyConfig.getPasswordWorkflowEdit(),
    			null,
    			urlRedirect,
    			csrfToken,
    			null,
    			null,
    			false,
    			false
    	);
    	
    	// send Galaxy login request 
    	String urlUserLogin = galaxyPropertyConfig.getBaseUrl() + "/user/login";
    	HttpEntity<GalaxyLoginRequest> requestUserLogin = new HttpEntity<GalaxyLoginRequest>(glr, headers);
    	ResponseEntity<String> responseUserLogin = restTemplate.postForEntity(urlUserLogin, requestUserLogin, String.class);
    	
    	// retrieve the galaxySession cookie and update the stored value
    	galaxySession = responseUserLogin.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    	
    	// check response status
    	if (responseUserLogin.getStatusCode().isError() || galaxySession == null) {
    		new RuntimeException("Failed to login to Galaxy for workflow edit.");
    	}		
    	else {
    		log.info("Successfully logged in to Galaxy for workflow edit.");
    	}    	
    	return responseUserLogin;
	}
	
	/**
	 * Upon destruction of the controller,
	 * logout from galaxy as AMP workflow edit user destroy galaxy session for workflow edit.
	 */
	@PreDestroy
	public ResponseEntity<String> logoutGalaxy() {
		// TODO get logout csrf token and send logout request to galaxy

		// request Galaxy server logout with CSRF token
		String urlUserLogout = galaxyPropertyConfig.getBaseUrl() + "/user/logout";
		urlUserLogout += "?session_csrf_token=" + csrfToken + "&logout_all=false";
    	HttpHeaders headers = new HttpHeaders();
    	headers.add(HttpHeaders.COOKIE, galaxySession);
    	HttpEntity<String> requestUserLogout = new HttpEntity<String>(null, headers);
    	ResponseEntity<String> responseUserLogout = restTemplate.exchange(urlUserLogout, HttpMethod.GET, requestUserLogout, String.class);
    	
    	// retrieve the galaxySession cookie and update the stored value
    	galaxySession = responseUserLogout.getHeaders().getFirst(HttpHeaders.SET_COOKIE);	
    	
    	// check response status
    	if (responseUserLogout.getStatusCode().isError() || galaxySession == null) {
    		new RuntimeException("Failed to logout from Galaxy for workflow edit.");
    	}		
    	else {
    		log.info("Successfully logged out from Galaxy for workflow edit.");
    	}    	
		return responseUserLogout;
	}
	
	/**
	 * Start a workflow edit session within an authenticated AMP user session.
	 * @param authHeader Authorization header from the request
	 * @param workflowId ID of the workflow for edit
	 * @param response HTTP response to send
	 * @return empty body response upon success
	 */
	@PostMapping("/workflows/{workflowId}/editStart")
	public ResponseEntity<String> startEdit(@RequestHeader("Authorization") String authHeader, @PathVariable("workflowId") String workflowId, HttpServletResponse response) {
		/* Note:
		 * We use AMP authorization token instead of username to identify workflow edit session, as the former corresponds to
		 * an AMP user session, and there could be multiple sessions from multiple clients for the same user.
		 * Also, with current implementation, AMP server does allow user to edit multiple workflows in the same session;
		 * however, AMP client can only take one workflow edit cookie at a time, so it should be disallowed by AMP client.
		 */

		// since the request is through AMP user authentication, the authorization token must be valid at this point
		String authToken = jwtTokenUtil.retrieveToken(authHeader);
		
		// generate a workflow edit token corresponding to the authorization token and workflow ID
		String wfeToken = jwtTokenUtil.generateWorkflowEditToken(authToken, workflowId);
		
    	// wrap the workflow edit token in a cookie 
		Cookie cookie = new Cookie(WORKFLOW_EDIT_COOKIE, wfeToken);
	    cookie.setSecure(true);
	    cookie.setHttpOnly(true);
	    cookie.setPath(context.getContextPath() + GALAXY_PATH);
	    cookie.setMaxAge(amppdPropertyConfig.getWorkflowEditMinutes() * 60);
		
		// send the cookie to AMP client to authenticate future workflow edit requests
		response.addCookie(cookie);
		log.info("Successfully started the edit session for workflow " + workflowId);
		return ResponseEntity.status(HttpStatus.OK).body(null);
	}	
	
	/**
	 * Start a workflow edit session within an authenticated AMP user session.
	 * @param authHeader Authorization header from the request
	 * @param workflowId ID of the workflow for edit
	 * @param response HTTP response to send
	 * @return empty body response upon success
	 */
	@PostMapping("/workflows/{workflowId}/editEnd")
	public ResponseEntity<String> endEdit(@PathVariable("workflowId") String workflowId, HttpServletResponse response) {		
    	// unset the workflow edit cookie 
		Cookie cookie = new Cookie(WORKFLOW_EDIT_COOKIE, null);
	    cookie.setSecure(true);
	    cookie.setHttpOnly(true);
	    cookie.setPath(GALAXY_PATH);
	    cookie.setMaxAge(0);
		
		// send the unset cookie to AMP client to delete it
		response.addCookie(cookie);
		log.info("Successfully ended the edit session for workflow " + workflowId);
		return ResponseEntity.status(HttpStatus.OK).body(null);
	}	
	
	/**
	 * 
	 */
	@RequestMapping(value = GALAXY_PATH)
	public ResponseEntity<String> proxyEdit(
			@RequestHeader Map<String, String> headers,
			@RequestBody String body,
			HttpServletRequest request, HttpServletResponse response) {
	    
		// retrieve workflow edit cookie and validate it
		Cookie wfeCookie = WebUtils.getCookie(request, WORKFLOW_EDIT_COOKIE);
		if (!validateWorkflowEditCookie(wfeCookie)) {
			
		}
		
		// process all headers
    	HttpHeaders gheaders = new HttpHeaders();
    	boolean valid = false;
		headers.forEach((headerName, headerValue) -> {
	        // add all headers to galaxy request except workflow edit cookie
			if (!isHeaderWorkflowEditCookie(headerName, headerValue)) {
	        	gheaders.add(headerName,  headerValue);
	        }
	    });
		
		return null;
	}
	
	private boolean isHeaderWorkflowEditCookie(String key, String value) {
		return key.equalsIgnoreCase(HttpHeaders.COOKIE) && value.startsWith(WORKFLOW_EDIT_COOKIE);
	}
	
	public boolean validateWorkflowEditCookie(Cookie wfeCookie) {
		if (wfeCookie == null) {
			log.error("Workflow Edit Cookie is not provided in the request.");
			return false;
		}
			
		String wfeToken = wfeCookie.getValue();
		
		boolean valid = false;
		return valid;
	}
	
	
	
}
