package com.example.assistant.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
class AssistantController {

	private final ChatClient ai;

	AssistantController(ChatClient ai) {
		this.ai = ai;
	}

	@GetMapping("/ask")
	String ask(
			@RequestParam String question,
			@RequestParam(defaultValue = "default") String conversationId) {
		return this.ai
				.prompt()
				.advisors((advisor) -> advisor.param("chat_memory_conversation_id", conversationId))
				.user(question)
				.call()
				.content();
	}

}
