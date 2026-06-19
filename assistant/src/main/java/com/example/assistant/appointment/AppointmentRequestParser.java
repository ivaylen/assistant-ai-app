package com.example.assistant.appointment;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AppointmentRequestParser {

	private static final Pattern PATIENT_NAME = Pattern.compile(
			"(?i)\\bpatient\\s+name\\s+(.+?)(?=\\.?\\s+(reason|preferred\\s+time|contact)\\b|\\.?$)");
	private static final Pattern REASON = Pattern.compile(
			"(?i)\\breason\\s+(.+?)(?=\\.?\\s+(patient\\s+name|preferred\\s+time|contact)\\b|\\.?$)");
	private static final Pattern PREFERRED_TIME = Pattern.compile(
			"(?i)\\bpreferred\\s+time\\s+(.+?)(?=\\.?\\s+(patient\\s+name|reason|contact)\\b|\\.?$)");
	private static final Pattern CONTACT = Pattern.compile(
			"(?i)\\bcontact\\s+(.+?)(?=\\.?\\s+(patient\\s+name|reason|preferred\\s+time)\\b|\\.?$)");

	public Optional<AppointmentDetails> parse(String question) {
		if (!StringUtils.hasText(question) || !question.toLowerCase().contains("schedule an appointment")) {
			return Optional.empty();
		}

		var patientName = find(PATIENT_NAME, question);
		var reason = find(REASON, question);
		var preferredTime = find(PREFERRED_TIME, question);
		var contact = find(CONTACT, question);

		if (!StringUtils.hasText(patientName)
				|| !StringUtils.hasText(reason)
				|| !StringUtils.hasText(preferredTime)
				|| !StringUtils.hasText(contact)) {
			return Optional.empty();
		}

		return Optional.of(new AppointmentDetails(patientName, reason, preferredTime, contact));
	}

	private String find(Pattern pattern, String text) {
		var matcher = pattern.matcher(text);
		if (!matcher.find()) {
			return null;
		}
		return clean(matcher.group(1));
	}

	private String clean(String value) {
		if (value == null) {
			return null;
		}
		return value.replaceAll("[.\\s]+$", "").trim();
	}

}
