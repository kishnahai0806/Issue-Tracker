package com.krish.issuetracker.config;

import java.util.List;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

	private final CorsProperties corsProperties;

	public SecurityConfig(CorsProperties corsProperties) {
		this.corsProperties = corsProperties;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/error").permitAll()
						.requestMatchers(EndpointRequest.to("health")).permitAll()
						.anyRequest().authenticated())
				.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		List<String> allowedOrigins = allowedOrigins();

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Idempotency-Key"));
		configuration.setExposedHeaders(List.of("Location"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private List<String> allowedOrigins() {
		List<String> origins = corsProperties.allowedOrigins();

		if (origins == null || origins.isEmpty()) {
			throw new InvalidCorsConfigurationException("app.cors.allowed-origins must contain at least one explicit origin");
		}

		List<String> normalizedOrigins = origins.stream()
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList();

		if (normalizedOrigins.isEmpty() || normalizedOrigins.contains("*")) {
			throw new InvalidCorsConfigurationException("Wildcard or empty CORS origins are not allowed");
		}

		return normalizedOrigins;
	}

}
