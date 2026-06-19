package com.example.assistant.auth;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class AuthController {

	private final ClientRegistrationRepository clientRegistrationRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserAccountService userAccountService;

	AuthController(
			ClientRegistrationRepository clientRegistrationRepository,
			PasswordEncoder passwordEncoder,
			UserAccountService userAccountService) {
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.passwordEncoder = passwordEncoder;
		this.userAccountService = userAccountService;
	}

	@GetMapping("/login")
	String login() {
		return "redirect:/login.html";
	}

	@GetMapping("/signup")
	String signup() {
		return "redirect:/signup.html";
	}

	@PostMapping("/signup")
	String register(SignupRequest request, RedirectAttributes redirectAttributes) {
		if (userAccountService.usernameExists(request.username().trim())) {
			redirectAttributes.addAttribute("error", "username");
			return "redirect:/signup.html";
		}

		userAccountService.createUser(request, passwordEncoder.encode(request.password()));
		redirectAttributes.addAttribute("registered", "true");
		return "redirect:/login.html";
	}

	@GetMapping("/api/auth/providers")
	@ResponseBody
	AuthProviders providers() {
		var googleEnabled = clientRegistrationRepository instanceof InMemoryClientRegistrationRepository registrations
				&& registrations.findByRegistrationId("google") != null;
		return new AuthProviders(googleEnabled);
	}

	record AuthProviders(boolean googleEnabled) {
	}

}
