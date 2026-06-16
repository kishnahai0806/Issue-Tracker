package com.krish.issuetracker.security;

import com.krish.issuetracker.repository.OrganizationMemberRepository;
import com.krish.issuetracker.security.permission.OrganizationMemberPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {

	@Bean
	public OrganizationMemberPermissionEvaluator organizationMemberPermissionEvaluator(
			OrganizationMemberRepository organizationMemberRepository,
			StringRedisTemplate redisTemplate) {
		return new OrganizationMemberPermissionEvaluator(organizationMemberRepository, redisTemplate);
	}

	@Bean
	public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
			OrganizationMemberPermissionEvaluator organizationMemberPermissionEvaluator) {
		DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
		expressionHandler.setPermissionEvaluator(organizationMemberPermissionEvaluator);
		return expressionHandler;
	}
}
