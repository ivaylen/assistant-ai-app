package com.example.assistant.appointment;

import java.time.Instant;
import java.util.UUID;

public record AppointmentCreationResult(
		UUID id,
		Instant requestedFor) {
}
