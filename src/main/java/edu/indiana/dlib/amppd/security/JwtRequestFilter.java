package edu.indiana.dlib.amppd.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import edu.indiana.dlib.amppd.config.AmppdUiPropertyConfig;
import edu.indiana.dlib.amppd.model.AmpUser;
import edu.indiana.dlib.amppd.service.AmpUserService;
import edu.indiana.dlib.amppd.service.AuthService;
import io.jsonwebtoken.ExpiredJwtException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

	@Autowired
	private AmpUserService ampUserService;
	
	
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	
	@Autowired
	private AmppdUiPropertyConfig amppdUIConfig;
	

	@Autowired
	private AuthService authService;
	
	
	private void createAnonymousAuth(HttpServletRequest request) {
		AmpUser userDetails = new AmpUser();
		userDetails.setEmail("none");
		userDetails.setUsername("");
	
		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
				userDetails, null, null);

		usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
	}
	
	// Determine whether referrer and url match "whitelisted" values.  This is likely temporary until the 
	// NER editor uses it's own auth.  
	private boolean validRefUrl(String referer, String uri) {
		if(referer==null) return false;
		// Only continue if it's the NER editor
		if(!uri.equals("/rest/hmgm/ner-editor")) {
			return false;
		}
		// Standardize cleaning URLs to avoid oddities
		String cleanedRef = referer.replace("https://", "").replace("http://", "").replace("#/", "").replace("#", "").replace("localhost", "127.0.0.1");
		String cleanedUiUrl = amppdUIConfig.getUrl().replace("https://", "").replace("http://", "").replace("#/", "").replace("#", "").replace("localhost", "127.0.0.1");
		logger.trace(cleanedRef + " starts with " + cleanedUiUrl + " : " + cleanedRef.startsWith(cleanedUiUrl));
		
		return cleanedRef.startsWith(cleanedUiUrl);
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		final String requestTokenHeader = request.getHeader("authorization");
			
		String username = null;
		String jwtToken = null;

		String authToken = null;
		
		// Get the referrer and URI
		String referer = request.getHeader("referer");
		String uri = request.getRequestURI();
		// If it is for the HMGM editor with a valid referrer, create anonymous auth
		if (validRefUrl(referer, uri)) {
			logger.trace("Valid referer:  " + referer  + ". Creating anonymous auth");
			createAnonymousAuth(request);
		}
		// otherwise, for AMP account registration related requests
		else if (requestTokenHeader != null && requestTokenHeader.startsWith("AMPPD ")) {
			logger.trace("Request token starts with amppd");
			authToken = requestTokenHeader.substring(6);
			String[] parts = authToken.split(";;;;");
			String editorInput = parts[0];
			String userToken = parts[1];
			String authString = parts[2];
			
			if(authService.compareAuthStrings(authString, userToken, editorInput)){
				createAnonymousAuth(request);
				logger.trace("Auth string is valid. Creating anonymous auth");
			}
			else {
				logger.warn("Auth string is invalid for authstring: " + authString + " userToken: " + userToken + " editor input: " + editorInput);
			}			
		}
		// otherwise, for AMP user authentication with JWT token
		else {
			jwtToken = jwtTokenUtil.getToken(requestTokenHeader);			
			if (StringUtils.isNotBlank(jwtToken)) {
				try {
					username = jwtTokenUtil.getUsernameFromToken(jwtToken);
					
					if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
						AmpUser userDetails = ampUserService.getUser(username);
					
						if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
							UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = 
									new UsernamePasswordAuthenticationToken(userDetails, null, null);	
							SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
							logger.debug("Authentication succeeded with valid token for user " + username);
						}
						else {
							logger.warn("Authentication failed with invalid token for user " + username);
						}		
					}
					else {
						logger.warn("Invalid JWT token: username is blank.");
					}
				} catch (IllegalArgumentException e) {
					logger.warn("Invalid JWT token: unable to get username from JWT Token");
				} catch (ExpiredJwtException e) {
					logger.warn("Invalid JWT token: token has expired");
				}
			} else {
				logger.warn("Request has no valid Authorization header with JWT Token beginning with Bearer, possibly auth is turned off.");
			}		
		}
	
		chain.doFilter(request, response);
	}

}