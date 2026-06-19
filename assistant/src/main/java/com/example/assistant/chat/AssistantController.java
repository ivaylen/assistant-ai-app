package com.example.assistant.chat;

import java.util.Map;

import com.example.assistant.appointment.AppointmentRequestParser;
import com.example.assistant.appointment.AppointmentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
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
	private final ChatClient directMedicalAnswerAi;
	private final AppointmentRequestParser appointmentRequestParser;
	private final AppointmentService appointmentService;
	private final AssistantResponseSanitizer responseSanitizer;
	private final UserMessageSafetyGuard userMessageSafetyGuard;

	AssistantController(
			@Qualifier("chatClient") ChatClient ai,
			@Qualifier("directMedicalAnswerChatClient") ChatClient directMedicalAnswerAi,
			AppointmentRequestParser appointmentRequestParser,
			AppointmentService appointmentService,
			AssistantResponseSanitizer responseSanitizer,
			UserMessageSafetyGuard userMessageSafetyGuard) {
		this.ai = ai;
		this.directMedicalAnswerAi = directMedicalAnswerAi;
		this.appointmentRequestParser = appointmentRequestParser;
		this.appointmentService = appointmentService;
		this.responseSanitizer = responseSanitizer;
		this.userMessageSafetyGuard = userMessageSafetyGuard;
	}

	@GetMapping("/ask")
	String ask(
			@RequestParam String question,
			@RequestParam(defaultValue = "default") String conversationId,
			Authentication authentication) {
		var safetyResponse = userMessageSafetyGuard.check(question);
		if (safetyResponse.isPresent()) {
			return safetyResponse.get();
		}
		var username = authentication == null ? "anonymous" : authentication.getName();
		return responseSanitizer.clean(
				askAi(question, conversationId, username),
				question,
				() -> askDirectMedicalAi(question));
	}

	@PostMapping("/api/chat")
	ChatResponse chat(@RequestBody ChatRequest request, Authentication authentication) {
		var conversationId = request.conversationId() == null || request.conversationId().isBlank()
				? "default"
				: request.conversationId();
		var username = authentication.getName();
		var safetyResponse = userMessageSafetyGuard.check(request.question());
		if (safetyResponse.isPresent()) {
			return new ChatResponse(safetyResponse.get());
		}
		return appointmentRequestParser.parse(request.question())
				.map(details -> {
					var result = appointmentService.createAppointment(username, details);
					return new ChatResponse("I created appointment request " + result.id()
							+ ". This is a request, not a confirmed appointment. The clinic should confirm it.");
				})
				.orElseGet(() -> new ChatResponse(
						responseSanitizer.clean(
								askAi(request.question(), conversationId, username),
								request.question(),
								() -> askDirectMedicalAi(request.question()))));
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

	private String askDirectMedicalAi(String question) {
		return this.directMedicalAnswerAi
				.prompt()
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
