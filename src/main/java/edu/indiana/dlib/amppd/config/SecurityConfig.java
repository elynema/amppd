package edu.indiana.dlib.amppd.config;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
/*
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity 

public class SecurityConfig extends WebSecurityConfigurerAdapter {  

	@Override
	protected void configure(HttpSecurity http) throws Exception 
	{
		http.authorizeRequests().antMatchers("/").permitAll().and().authorizeRequests().antMatchers("/console/**").permitAll();
		http.authorizeRequests()
	    	.anyRequest()
	        .permitAll()
	        .and().csrf().disable();
		http.headers().frameOptions().disable();
	}
}
*/
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.google.common.net.HttpHeaders;

import edu.indiana.dlib.amppd.security.JwtAuthenticationEntryPoint;
import edu.indiana.dlib.amppd.security.JwtRequestFilter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private AmppdUiPropertyConfig amppduiPropertyConfig;	
	
	@Autowired
	private AmppdPropertyConfig amppdPropertyConfig;
	
	@Autowired
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	
	@Autowired
	private UserDetailsService jwtUserDetailsService;
	
	@Autowired
	private JwtRequestFilter jwtRequestFilter;
	
//	@Value( "${amppdui.url}" )
//	private String amppduiUrl;
	
	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(jwtUserDetailsService).passwordEncoder(passwordEncoder());
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
	    CorsConfiguration config = new CorsConfiguration();

	    // only allow AMP UI as origin, can add target systems as needed
	    String origin = StringUtils.substringBeforeLast(amppduiPropertyConfig.getUrl(), "/");
	    config.setAllowedOrigins(Arrays.asList(origin));
	    
	    // allow clients other than the AMP UI for CORS requests;
	    // one use case is for AMP frontend development, we allow AMP local UI to point to AMP Rest Test.
	    String origins = amppdPropertyConfig.getCorsOriginPattern();
	    if (StringUtils.isNotBlank(origins)) {
	    	config.addAllowedOriginPattern(origins);
		    log.info("Added " + origins + " to allowed origins for CORS requests");
	    }
	    
	    // all AMP update requests use PATCH instead of PUT, but PUT is still needed as Galaxy workflow editor requests
	    config.setAllowedMethods(Arrays.asList("HEAD", "GET", "POST", "PUT", "PATCH", "DELETE"));

	    // 'Location' header is checked by HMGM NER editor (Timeliner), if not exposed, browser may throw error
	    // "Authorization" header is needed by most AMP UI requests;
	    // for Galaxy workflow editor requests, Set-Cookie header is needed
	    config.setExposedHeaders(Arrays.asList(HttpHeaders.AUTHORIZATION, HttpHeaders.LOCATION, HttpHeaders.SET_COOKIE));
	    
	    config.setAllowedHeaders(Arrays.asList(CorsConfiguration.ALL));
	    config.setAllowCredentials(true);
	    
	    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/**", config.applyPermitDefaultValues());
	    return source;
	}
	
	@Override
	protected void configure(HttpSecurity httpSecurity) throws Exception {
		// if authentication is turned on (either auth property is not defined or is true), add JWT token filter
		if (amppdPropertyConfig.getAuth() == null || amppdPropertyConfig.getAuth()) {
			// TODO recover X-Frame-Options to sameOrigin
			// below is a temp tweak to remove X-Frame-Options to allow local AMP UI to connect to AMP Test Workflow Editor
//			httpSecurity.cors().and().csrf().disable().headers().frameOptions().sameOrigin().and().authorizeRequests()
			httpSecurity.cors().and().csrf().disable().headers().frameOptions().disable().and().authorizeRequests()
			.antMatchers(HttpMethod.POST, "/account/register").permitAll()
			.antMatchers(HttpMethod.POST, "/account/authenticate").permitAll()
			.antMatchers(HttpMethod.POST, "/account/forgot-password").permitAll()
			.antMatchers(HttpMethod.POST, "/account/reset-password").permitAll()
			.antMatchers(HttpMethod.POST, "/account/activate").permitAll()
			.antMatchers(HttpMethod.POST, "/account/reset-password-getEmail").permitAll()
			.antMatchers(HttpMethod.GET, "/hmgm/authorize-editor").permitAll()
			// bypass /galaxy/* requests, which will be handled by the galaxy workflow edit proxy
			.antMatchers("/galaxy/**").permitAll()
			// TODO 
			// Below two lines are for access media or output files;
			// auth is bypassed possibly due to the need to allow users to access these links without login;
			// however, we probably should not allow such.
			.antMatchers(HttpMethod.GET, "/primaryfiles/*/media").permitAll()
			.antMatchers(HttpMethod.GET, "/workflow-results/*/output").permitAll()
			
			// TODO remove below hmgm paths after we done development with HMGM
			.anyRequest().authenticated().and().
			exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and().sessionManagement()
			.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		}
		// otherwise permit all requests
		else {
			httpSecurity.cors().and().csrf().disable().headers().frameOptions().disable().and().authorizeRequests()
			.antMatchers("/**").permitAll()
			.anyRequest().authenticated().and().
			exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and().sessionManagement()
			.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		}
		httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
	}

}
