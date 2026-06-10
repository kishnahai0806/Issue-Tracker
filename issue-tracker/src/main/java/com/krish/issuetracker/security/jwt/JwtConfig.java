package com.krish.issuetracker.security.jwt;

import com.krish.issuetracker.security.TokenBlacklist;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	@Bean
	public JwtService jwtService(JwtProperties jwtProperties) {
		return new JwtService(jwtProperties);
	}

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
			JwtService jwtService,
			TokenBlacklist tokenBlacklist) {
		FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new JwtAuthenticationFilter(jwtService, tokenBlacklist));
		registrationBean.setEnabled(false);
		return registrationBean;
	}
}
