package com.example.assistant.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class MedicalSafetyTools {

	@Tool(name = "medical_safety_guidance", description = """
			Provides general safety guidance for symptom questions, including emergency red flags,
			self-care basics, and when to contact a clinician. Use for health or symptom-related questions.
			""")
	public String medicalSafetyGuidance(
			@ToolParam(description = "The symptoms or health concern described by the user") String symptoms,
			@ToolParam(required = false, description = "Any extra context, such as duration, age, trigger, or severity") String context) {
		var text = ((symptoms == null ? "" : symptoms) + " " + (context == null ? "" : context)).toLowerCase();

		if (containsAny(text, "chest pain", "trouble breathing", "shortness of breath", "stroke",
				"face drooping", "severe allergic", "anaphylaxis", "suicidal", "major bleeding",
				"loss of consciousness", "confusion", "seizure")) {
			return loadSkill("emergency-red-flags.md");
		}

		if (containsAny(text, "sun", "heat", "hot weather", "dehydration", "heat exhaustion")) {
			return loadSkill("sun-headache.md");
		}

		if (containsAny(text, "headache", "migraine")) {
			return loadSkill("headache.md");
		}

		return loadSkill("general-medical-safety.md");
	}

	private String loadSkill(String fileName) {
		try {
			var resource = new ClassPathResource("skills/" + fileName);
			return resource.getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load medical safety skill: " + fileName, ex);
		}
	}

	private boolean containsAny(String text, String... terms) {
		for (var term : terms) {
			if (text.contains(term)) {
				return true;
			}
		}
		return false;
	}

}
