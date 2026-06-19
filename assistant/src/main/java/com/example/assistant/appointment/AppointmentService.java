package com.example.assistant.appointment;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {

	private final JdbcTemplate jdbcTemplate;

	AppointmentService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public AppointmentCreationResult createAppointment(String username, AppointmentDetails details) {
		var id = UUID.randomUUID();
		var requestedFor = estimateRequestedFor(details.preferredTime());
		jdbcTemplate.update("""
				INSERT INTO appointment_requests (id, username, patient_name, reason, preferred_time, requested_for, contact)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""", id, username, details.patientName().trim(), details.reason().trim(), details.preferredTime().trim(),
				Timestamp.from(requestedFor), details.contact().trim());

		return new AppointmentCreationResult(id, requestedFor);
	}

	private Instant estimateRequestedFor(String preferredTime) {
		var text = preferredTime == null ? "" : preferredTime.toLowerCase();

		if (text.contains("day after tomorrow")) {
			return Instant.now().plus(2, ChronoUnit.DAYS);
		}
		if (text.contains("tomorrow")) {
			return Instant.now().plus(1, ChronoUnit.DAYS);
		}
		if (text.contains("next week")) {
			return Instant.now().plus(7, ChronoUnit.DAYS);
		}
		return Instant.now();
	}

}
