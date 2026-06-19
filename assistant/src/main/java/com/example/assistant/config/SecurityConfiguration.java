package com.example.assistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf((csrf) -> csrf.disable())
				.authorizeHttpRequests((requests) -> requests
						.requestMatchers("/login", "/login.html", "/signup", "/signup.html", "/goodbye.html",
								"/styles.css", "/api/auth/**", "/error")
						.permitAll()
						.anyRequest().authenticated())
				.formLogin((form) -> form
						.loginPage("/login.html")
						.loginProcessingUrl("/login")
						.defaultSuccessUrl("/", true)
						.permitAll())
				.oauth2Login((oauth2) -> oauth2
						.loginPage("/login.html")
						.defaultSuccessUrl("/", true))
				.logout((logout) -> logout
						.logoutSuccessUrl("/goodbye.html")
						.permitAll())
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
