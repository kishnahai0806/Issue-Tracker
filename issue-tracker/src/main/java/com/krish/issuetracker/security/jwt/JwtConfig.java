package com.krish.issuetracker.security.jwt;

import com.krish.issuetracker.repository.UserRepository;
import com.krish.issuetracker.security.TokenBlacklist;
import io.micrometer.core.instrument.MeterRegistry;
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
	public JwtAuthenticationFilter jwtAuthenticationFilter(
			JwtService jwtService,
			TokenBlacklist tokenBlacklist,
			UserRepository userRepository,
			MeterRegistry meterRegistry) {
		return new JwtAuthenticationFilter(jwtService, tokenBlacklist, userRepository, meterRegistry);
	}

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
			JwtAuthenticationFilter jwtAuthenticationFilter) {
		FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(jwtAuthenticationFilter);
		registrationBean.setEnabled(false);
		return registrationBean;
	}
}
