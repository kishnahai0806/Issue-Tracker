package com.krish.issuetracker.logging;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class LoggingConfig {

	@Bean
	public FilterRegistrationBean<MdcLoggingFilter> mdcLoggingFilterRegistration() {
		FilterRegistrationBean<MdcLoggingFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new MdcLoggingFilter());
		registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		registrationBean.addUrlPatterns("/*");
		registrationBean.setEnabled(true);
		return registrationBean;
	}
}
