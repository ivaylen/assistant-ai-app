package com.example.assistant.appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
class AppointmentController {

	private final JdbcTemplate jdbcTemplate;

	AppointmentController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/api/appointments")
	List<AppointmentResponse> appointments(Authentication authentication) {
		if (isAdmin(authentication)) {
			return jdbcTemplate.query("""
					SELECT id, username, patient_name, reason, preferred_time, requested_for, contact, status, created_at
					FROM appointment_requests
					ORDER BY created_at DESC
					LIMIT 50
					""", this::mapAppointment);
		}

		return jdbcTemplate.query("""
				SELECT id, username, patient_name, reason, preferred_time, requested_for, contact, status, created_at
				FROM appointment_requests
				WHERE username = ?
				ORDER BY created_at DESC
				LIMIT 20
				""", this::mapAppointment, authentication.getName());
	}

	@PostMapping("/api/appointments/{id}/confirm")
	AppointmentResponse confirm(@PathVariable UUID id, Authentication authentication) {
		if (!isAdmin(authentication)) {
			throw new ResponseStatusException(FORBIDDEN, "Only admins can confirm appointments.");
		}

		var updated = jdbcTemplate.update("""
				UPDATE appointment_requests
				SET status = 'CONFIRMED'
				WHERE id = ?
				""", id);

		if (updated == 0) {
			throw new ResponseStatusException(NOT_FOUND, "Appointment was not found.");
		}

		return jdbcTemplate.queryForObject("""
				SELECT id, username, patient_name, reason, preferred_time, requested_for, contact, status, created_at
				FROM appointment_requests
				WHERE id = ?
				""", this::mapAppointment, id);
	}

	private AppointmentResponse mapAppointment(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		return new AppointmentResponse(
				rs.getObject("id", UUID.class),
				rs.getString("username"),
				rs.getString("patient_name"),
				rs.getString("reason"),
				rs.getString("preferred_time"),
				rs.getTimestamp("requested_for").toLocalDateTime(),
				rs.getString("contact"),
				rs.getString("status"),
				rs.getTimestamp("created_at").toLocalDateTime());
	}

	private boolean isAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch((authority) -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}

	record AppointmentResponse(
			UUID id,
			String username,
			String patientName,
			String reason,
			String preferredTime,
			LocalDateTime requestedFor,
			String contact,
			String status,
			LocalDateTime createdAt) {
	}

}
