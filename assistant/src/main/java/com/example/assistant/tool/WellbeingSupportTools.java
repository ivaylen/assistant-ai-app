package com.example.assistant.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class WellbeingSupportTools {

	@Tool(name = "wellbeing_support_guidance", description = """
			Provides life coaching, emotional support, and psychology-informed guidance with safety boundaries.
			Use for motivation, habits, stress, relationships, mood, confidence, or life direction questions.
			""")
	public String wellbeingSupportGuidance(
			@ToolParam(description = "The user's life, coaching, emotional, or psychological concern") String concern,
			@ToolParam(required = false, description = "Any extra context such as goal, situation, emotion, or severity") String context) {
		var text = ((concern == null ? "" : concern) + " " + (context == null ? "" : context)).toLowerCase();

		if (containsAny(text, "kill myself", "suicide", "suicidal", "self harm", "self-harm",
				"hurt myself", "end my life", "don't want to live", "do not want to live")) {
			return loadSkill("crisis-support.md");
		}

		if (containsAny(text, "goal", "habit", "motivation", "discipline", "career", "study",
				"procrastination", "confidence", "decision", "life direction", "coach")) {
			return loadSkill("life-coaching.md");
		}

		return loadSkill("emotional-support.md");
	}

	private String loadSkill(String fileName) {
		try {
			var resource = new ClassPathResource("skills/" + fileName);
			return resource.getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load wellbeing support skill: " + fileName, ex);
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
