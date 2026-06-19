package com.example.assistant.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class UserAccountSchemaConfiguration {

	@Bean
	UserAccountSchemaInitializer userAccountSchemaInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
		return new UserAccountSchemaInitializer(jdbcTemplate, passwordEncoder);
	}

	static class UserAccountSchemaInitializer {

		UserAccountSchemaInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
			jdbcTemplate.execute("""
					CREATE TABLE IF NOT EXISTS app_users (
						id BIGSERIAL PRIMARY KEY,
						username TEXT NOT NULL UNIQUE,
						password TEXT NOT NULL,
						first_name TEXT NOT NULL,
						last_name TEXT NOT NULL,
						age INTEGER,
						phone_number TEXT,
						role TEXT NOT NULL DEFAULT 'USER',
						enabled BOOLEAN NOT NULL DEFAULT TRUE,
						created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
					)
					""");
			jdbcTemplate.execute("""
					ALTER TABLE app_users
					ADD COLUMN IF NOT EXISTS role TEXT NOT NULL DEFAULT 'USER'
					""");

			var adminCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM app_users WHERE username = ?",
					Integer.class,
					"admin");

			if (adminCount == 0) {
				jdbcTemplate.update("""
						INSERT INTO app_users (username, password, first_name, last_name, age, phone_number, role)
						VALUES (?, ?, ?, ?, ?, ?, ?)
						""", "admin", passwordEncoder.encode("admin"), "Local", "Admin", null, null, "ADMIN");
			}
			else {
				jdbcTemplate.update("UPDATE app_users SET role = ? WHERE username = ?", "ADMIN", "admin");
			}
		}

	}

}
