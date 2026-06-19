package com.example.assistant.appointment;

public record AppointmentDetails(
		String patientName,
		String reason,
		String preferredTime,
		String contact) {
}
