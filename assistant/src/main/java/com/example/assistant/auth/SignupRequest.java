package com.example.assistant.auth;

record SignupRequest(
		String username,
		String password,
		String firstName,
		String lastName,
		Integer age,
		String phoneNumber) {
}
