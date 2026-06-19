package com.example.assistant.chat;

import java.util.Map;

import com.example.assistant.appointment.AppointmentRequestParser;
import com.example.assistant.appointment.AppointmentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
class AssistantController {

	private final ChatClient ai;
	private final AppointmentRequestParser appointmentRequestParser;
	private final AppointmentService appointmentService;
	private final AssistantResponseSanitizer responseSanitizer;

	AssistantController(
			ChatClient ai,
			AppointmentRequestParser appointmentRequestParser,
			AppointmentService appointmentService,
			AssistantResponseSanitizer responseSanitizer) {
		this.ai = ai;
		this.appointmentRequestParser = appointmentRequestParser;
		this.appointmentService = appointmentService;
		this.responseSanitizer = responseSanitizer;
	}

	@GetMapping("/ask")
	String ask(
			@RequestParam String question,
			@RequestParam(defaultValue = "default") String conversationId,
			Authentication authentication) {
		var username = authentication == null ? "anonymous" : authentication.getName();
		return responseSanitizer.clean(askAi(question, conversationId, username));
	}

	@PostMapping("/api/chat")
	ChatResponse chat(@RequestBody ChatRequest request, Authentication authentication) {
		var conversationId = request.conversationId() == null || request.conversationId().isBlank()
				? "default"
				: request.conversationId();
		var username = authentication.getName();
		return appointmentRequestParser.parse(request.question())
				.map(details -> {
					var result = appointmentService.createAppointment(username, details);
					return new ChatResponse("I created appointment request " + result.id()
							+ ". This is a request, not a confirmed appointment. The clinic should confirm it.");
				})
				.orElseGet(() -> new ChatResponse(responseSanitizer.clean(askAi(request.question(), conversationId, username))));
	}

	@GetMapping("/api/me")
	UserResponse me(Authentication authentication) {
		var admin = authentication.getAuthorities().stream()
				.anyMatch((authority) -> "ROLE_ADMIN".equals(authority.getAuthority()));
		return new UserResponse(authentication.getName(), admin);
	}

	private String askAi(String question, String conversationId, String username) {
		return this.ai
				.prompt()
				.advisors((advisor) -> advisor.param("chat_memory_conversation_id", conversationId))
				.toolContext(Map.of("username", username))
				.user(question)
				.call()
				.content();
	}

	record ChatRequest(String question, String conversationId) {
	}

	record ChatResponse(String message) {
	}

	record UserResponse(String username, boolean admin) {
	}

}
