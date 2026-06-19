package com.example.searchmcp;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
public class WebSearchTools {

	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	@McpTool(
			name = "web_search",
			description = """
					Searches the web for general information using a free instant-answer source.
					For medical questions, prefer trusted medical source names and do not treat results as diagnosis.
					""")
	public String webSearch(
			@McpToolParam(description = "The search query.") String query,
			@McpToolParam(required = false, description = "If true, adds trusted medical source names to the query.") Boolean trustedMedicalOnly)
			throws IOException, InterruptedException {
		var effectiveQuery = Boolean.TRUE.equals(trustedMedicalOnly)
				? query + " NHS Mayo Clinic Cleveland Clinic MedlinePlus"
				: query;

		var encodedQuery = URLEncoder.encode(effectiveQuery, StandardCharsets.UTF_8);
		var request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.duckduckgo.com/?q=" + encodedQuery
						+ "&format=json&no_html=1&skip_disambig=1"))
				.header("User-Agent", "assistant-search-mcp/0.1")
				.timeout(Duration.ofSeconds(15))
				.GET()
				.build();

		var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			return "Search failed with HTTP status " + response.statusCode();
		}

		return response.body();
	}

}
