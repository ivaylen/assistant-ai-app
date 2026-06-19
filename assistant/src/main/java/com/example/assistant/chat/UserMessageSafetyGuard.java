package com.example.assistant.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
class UserMessageSafetyGuard {

	private static final List<String> SENSITIVE_PHRASES = List.of(
			"kill myself",
			"killing myself",
			"suicide",
			"suicidal",
			"self harm",
			"self-harm",
			"hurt myself",
			"end my life",
			"don't want to live",
			"do not want to live",
			"kill someone",
			"hurt someone",
			"murder someone");

	private static final String SAFETY_RESPONSE = """
			I am really sorry you are dealing with this. If there is immediate danger to you or someone else, please call your local emergency number now or go to the nearest emergency department.
			If this is about thoughts of self-harm or suicide, please contact a trusted person nearby and a crisis hotline right away. You do not have to handle this alone.
			""".trim();

	Optional<String> check(String message) {
		var lower = message == null ? "" : message.toLowerCase();
		var unsafe = SENSITIVE_PHRASES.stream().anyMatch(lower::contains);
		return unsafe ? Optional.of(SAFETY_RESPONSE) : Optional.empty();
	}

}
