package com.krish.issuetracker.notification;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
@EnableAsync
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

	private final MailProperties mailProperties;

	public MailConfig(MailProperties mailProperties) {
		this.mailProperties = mailProperties;
	}

	@Bean
	public JavaMailSender javaMailSender() {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(mailProperties.host());
		mailSender.setPort(mailProperties.port());
		mailSender.setUsername(mailProperties.username());
		mailSender.setPassword(mailProperties.password());
		mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

		Properties properties = mailSender.getJavaMailProperties();
		properties.put("mail.transport.protocol", "smtp");

		return mailSender;
	}

	@Bean
	public ThreadPoolTaskExecutor emailTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(mailProperties.threadPoolCoreSize());
		executor.setMaxPoolSize(mailProperties.threadPoolMaxSize());
		executor.setQueueCapacity(mailProperties.threadPoolQueueCapacity());
		executor.setThreadNamePrefix("email-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		executor.initialize();
		return executor;
	}

	@Bean
	public SpringTemplateEngine emailTemplateEngine() {
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setPrefix("templates/email/");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
		templateResolver.setCacheable(true);

		SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		templateEngine.setTemplateResolver(templateResolver);
		return templateEngine;
	}
}
