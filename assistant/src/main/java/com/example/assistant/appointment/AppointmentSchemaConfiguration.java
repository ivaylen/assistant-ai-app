package com.example.assistant.appointment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class AppointmentSchemaConfiguration {

	@Bean
	AppointmentSchemaInitializer appointmentSchemaInitializer(JdbcTemplate jdbcTemplate) {
		return new AppointmentSchemaInitializer(jdbcTemplate);
	}

	static class AppointmentSchemaInitializer {

		AppointmentSchemaInitializer(JdbcTemplate jdbcTemplate) {
			jdbcTemplate.execute("""
					CREATE TABLE IF NOT EXISTS appointment_requests (
						id UUID PRIMARY KEY,
						username TEXT NOT NULL DEFAULT 'anonymous',
						patient_name TEXT NOT NULL,
						reason TEXT NOT NULL,
						preferred_time TEXT NOT NULL,
						requested_for TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
						contact TEXT NOT NULL,
						status TEXT NOT NULL DEFAULT 'REQUESTED',
						created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
					)
					""");
			jdbcTemplate.execute("""
					ALTER TABLE appointment_requests
					ADD COLUMN IF NOT EXISTS requested_for TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
					""");
			jdbcTemplate.execute("""
					ALTER TABLE appointment_requests
					ADD COLUMN IF NOT EXISTS username TEXT NOT NULL DEFAULT 'anonymous'
					""");
		}

	}

}
