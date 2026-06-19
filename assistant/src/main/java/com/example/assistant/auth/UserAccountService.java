package com.example.assistant.auth;

import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class UserAccountService implements UserDetailsService {

	private final JdbcTemplate jdbcTemplate;

	UserAccountService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			return jdbcTemplate.queryForObject("""
					SELECT username, password, role, enabled
					FROM app_users
					WHERE username = ?
					""", (rs, rowNum) -> new User(
					rs.getString("username"),
					rs.getString("password"),
					rs.getBoolean("enabled"),
					true,
					true,
					true,
					List.of(new SimpleGrantedAuthority("ROLE_" + rs.getString("role")))), username);
		}
		catch (EmptyResultDataAccessException ex) {
			throw new UsernameNotFoundException("User not found: " + username, ex);
		}
	}

	boolean usernameExists(String username) {
		var count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM app_users WHERE username = ?",
				Integer.class,
				username);
		return count != null && count > 0;
	}

	void createUser(SignupRequest request, String encodedPassword) {
		jdbcTemplate.update("""
				INSERT INTO app_users (username, password, first_name, last_name, age, phone_number)
				VALUES (?, ?, ?, ?, ?, ?)
				""",
				request.username().trim(),
				encodedPassword,
				request.firstName().trim(),
				request.lastName().trim(),
				request.age(),
				request.phoneNumber().trim());
	}

}
