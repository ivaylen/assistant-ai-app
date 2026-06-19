package com.example.assistant.chat;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class AssistantResponseSanitizer {

	private static final List<String> TOOL_NAMES = List.of(
			"medical_safety_guidance",
			"wellbeing_support_guidance",
			"schedule_appointment");

	String clean(String response) {
		if (!StringUtils.hasText(response)) {
			return "I could not prepare a clear answer right now. Please try asking again.";
		}

		var trimmed = response.trim();
		if (looksLikeToolCall(trimmed)) {
			return """
					I can help with general guidance, but I cannot diagnose or replace a licensed clinician.
					Please describe your symptoms, how long they have been happening, and whether anything feels severe or unusual.
					For severe pain, trouble breathing, chest pain, fainting, stroke symptoms, major bleeding, or thoughts of self-harm, seek emergency medical help immediately.
					""".trim();
		}

		return response;
	}

	private boolean looksLikeToolCall(String response) {
		var lower = response.toLowerCase();
		if (!lower.contains("\"name\"") || !lower.contains("\"arguments\"")) {
			return false;
		}
		return TOOL_NAMES.stream().anyMatch(lower::contains);
	}

}
