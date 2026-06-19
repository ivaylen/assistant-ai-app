package com.example.assistant.config;

import javax.sql.DataSource;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ChatMemoryConfiguration {

	@Bean
	MessageChatMemoryAdvisor promptChatMemoryAdvisor(DataSource datasource) {
		var chatMemoryRepository = JdbcChatMemoryRepository.builder()
				.dataSource(datasource)
				.build();

		var chatMemory = MessageWindowChatMemory.builder()
				.chatMemoryRepository(chatMemoryRepository)
				.build();

		return MessageChatMemoryAdvisor.builder(chatMemory)
				.build();
	}

	@Bean
	ChatClient chatClient(ChatClient.Builder builder, MessageChatMemoryAdvisor promptChatMemoryAdvisor) {
		return builder
				.defaultAdvisors(promptChatMemoryAdvisor)
				.build();
	}

}
