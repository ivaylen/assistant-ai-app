package com.example.assistant.chat;

import com.example.assistant.tool.MedicalSafetyTools;
import com.example.assistant.tool.WellbeingSupportTools;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class AssistantResponseSanitizer {

	private final MedicalSafetyTools medicalSafetyTools;
	private final WellbeingSupportTools wellbeingSupportTools;

	private static final Pattern TOOL_NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern TOOL_ARGUMENTS = Pattern.compile("\"arguments\"\\s*:\\s*\\{(.*?)}", Pattern.DOTALL);
	private static final Pattern ARGUMENT_VALUE = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");

	private static final List<String> TOOL_NAMES = List.of(
			"medical_safety_guidance",
			"wellbeing_support_guidance",
			"schedule_appointment",
			"web_search");

	AssistantResponseSanitizer(
			MedicalSafetyTools medicalSafetyTools,
			WellbeingSupportTools wellbeingSupportTools) {
		this.medicalSafetyTools = medicalSafetyTools;
		this.wellbeingSupportTools = wellbeingSupportTools;
	}

	String clean(String response) {
		return clean(response, null);
	}

	String clean(String response, String userQuestion) {
		return clean(response, userQuestion, this::fallbackGuidance);
	}

	String clean(String response, String userQuestion, Supplier<String> modelFallback) {
		if (!StringUtils.hasText(response)) {
			return "I could not prepare a clear answer right now. Please try asking again.";
		}

		var trimmed = response.trim();
		if (looksLikeToolCall(trimmed)) {
			return recoverToolGuidance(trimmed, userQuestion, modelFallback);
		}

		return response;
	}

	private String recoverToolGuidance(String response, String userQuestion, Supplier<String> modelFallback) {
		var toolCall = parseToolCall(response);
		if (toolCall == null) {
			return recoverFromRawToolName(response, userQuestion, modelFallback);
		}

		if ("medical_safety_guidance".equals(toolCall.name())) {
			var symptoms = firstText(toolCall.arguments(), "symptoms", "symptom", "health_concern", "concern");
			var context = firstText(toolCall.arguments(), "context", "extra_context", "duration", "severity");
			var effectiveSymptoms = StringUtils.hasText(symptoms) ? symptoms : userQuestion;
			if (!medicalSafetyTools.hasSpecificGuidance(effectiveSymptoms, context)) {
				return modelFallback.get();
			}
			return medicalSafetyTools.medicalSafetyGuidance(
					effectiveSymptoms,
					context);
		}

		if ("wellbeing_support_guidance".equals(toolCall.name())) {
			if (looksMedicalQuestion(userQuestion)) {
				return modelFallback.get();
			}
			var concern = firstText(toolCall.arguments(), "concern", "question", "topic");
			var context = firstText(toolCall.arguments(), "context", "extra_context", "situation");
			return wellbeingSupportTools.wellbeingSupportGuidance(
					StringUtils.hasText(concern) ? concern : userQuestion,
					context);
		}

		if ("web_search".equals(toolCall.name())) {
			var withoutToolCall = stripLeadingToolCall(response);
			return StringUtils.hasText(withoutToolCall) ? withoutToolCall : modelFallback.get();
		}

		return fallbackGuidance();
	}

	private String recoverFromRawToolName(String response, String userQuestion, Supplier<String> modelFallback) {
		var lower = response.toLowerCase();
		if (lower.contains("medical_safety_guidance")) {
			if (!medicalSafetyTools.hasSpecificGuidance(userQuestion, null)) {
				return modelFallback.get();
			}
			return medicalSafetyTools.medicalSafetyGuidance(userQuestion, null);
		}
		if (lower.contains("wellbeing_support_guidance")) {
			if (looksMedicalQuestion(userQuestion)) {
				return modelFallback.get();
			}
			return wellbeingSupportTools.wellbeingSupportGuidance(userQuestion, null);
		}
		if (lower.contains("web_search")) {
			var withoutToolCall = stripLeadingToolCall(response);
			return StringUtils.hasText(withoutToolCall) ? withoutToolCall : modelFallback.get();
		}
		return fallbackGuidance();
	}

	private String stripLeadingToolCall(String response) {
		var trimmed = response.trim();
		var jsonEnd = findLeadingJsonEnd(trimmed);
		if (jsonEnd >= 0) {
			return trimmed.substring(jsonEnd + 1).trim();
		}

		return response;
	}

	private int findLeadingJsonEnd(String text) {
		if (!text.startsWith("{") && !text.startsWith("[")) {
			return -1;
		}

		var stack = new java.util.ArrayDeque<Character>();
		var inString = false;
		var escaped = false;

		for (int i = 0; i < text.length(); i++) {
			var character = text.charAt(i);

			if (inString) {
				if (escaped) {
					escaped = false;
				}
				else if (character == '\\') {
					escaped = true;
				}
				else if (character == '"') {
					inString = false;
				}
				continue;
			}

			if (character == '"') {
				inString = true;
			}
			else if (character == '{' || character == '[') {
				stack.push(character);
			}
			else if (character == '}' || character == ']') {
				if (stack.isEmpty()) {
					return -1;
				}
				var opening = stack.pop();
				if ((opening == '{' && character != '}') || (opening == '[' && character != ']')) {
					return -1;
				}
				if (stack.isEmpty()) {
					return i;
				}
			}
		}

		return -1;
	}

	private boolean looksMedicalQuestion(String userQuestion) {
		var text = userQuestion == null ? "" : userQuestion.toLowerCase();
		return List.of(
				"acne",
				"ackne",
				"pimple",
				"skin",
				"rash",
				"pain",
				"hurt",
				"ache",
				"fever",
				"cough",
				"cold",
				"bite",
				"cut",
				"wound",
				"blood",
				"swelling",
				"headache",
				"stomach",
				"nausea",
				"vomit",
				"dizzy",
				"symptom")
				.stream()
				.anyMatch(text::contains);
	}

	private ToolCall parseToolCall(String response) {
		var nameMatcher = TOOL_NAME.matcher(response);
		if (!nameMatcher.find()) {
			return null;
		}

		var argumentsMatcher = TOOL_ARGUMENTS.matcher(response);
		if (!argumentsMatcher.find()) {
			return null;
		}

		var arguments = new HashMap<String, String>();
		var valueMatcher = ARGUMENT_VALUE.matcher(argumentsMatcher.group(1));
		while (valueMatcher.find()) {
			arguments.put(valueMatcher.group(1), valueMatcher.group(2));
		}

		return new ToolCall(nameMatcher.group(1), arguments);
	}

	private String firstText(Map<String, String> arguments, String... fieldNames) {
		for (var fieldName : fieldNames) {
			var value = arguments.get(fieldName);
			if (StringUtils.hasText(value)) {
				return value;
			}
		}
		return null;
	}

	private String fallbackGuidance() {
		return """
				I can help with general guidance, but I cannot diagnose or replace a licensed clinician.
				Please describe your symptoms, how long they have been happening, and whether anything feels severe or unusual.
				For severe pain, trouble breathing, chest pain, fainting, stroke symptoms, major bleeding, or thoughts of self-harm, seek emergency medical help immediately.
				""".trim();
	}

	private boolean looksLikeToolCall(String response) {
		var lower = response.toLowerCase();
		if (!lower.contains("\"name\"") || !lower.contains("\"arguments\"")) {
			return false;
		}
		return TOOL_NAMES.stream().anyMatch(lower::contains);
	}

	private record ToolCall(String name, Map<String, String> arguments) {
	}

}
