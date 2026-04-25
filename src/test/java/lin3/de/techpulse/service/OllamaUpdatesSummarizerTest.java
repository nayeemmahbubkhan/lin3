package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaUpdatesSummarizerTest {

	private final SourceUpdate update = new SourceUpdate(
		"Security release available",
		"https://example.com/release",
		Instant.parse("2026-04-24T10:00:00Z"),
		"Example"
	);

	@Test
	void usesFallbackWhenDisabled() {
		LocalLlmPromptBuilder promptBuilder = new LocalLlmPromptBuilder();
		RuleBasedUpdatesSummarizer fallback = new RuleBasedUpdatesSummarizer(promptBuilder);
		OllamaUpdatesSummarizer summarizer = new OllamaUpdatesSummarizer(
			RestClient.builder(),
			promptBuilder,
			fallback,
			false,
			"http://localhost:11434",
			"gemma4:latest"
		);

		assertEquals(fallback.summarize(update), summarizer.summarize(update));
		assertEquals(fallback.nextAction(update), summarizer.nextAction(update));
		assertEquals(fallback.footerInsight(update), summarizer.footerInsight(update));
	}

	@Test
	void returnsOllamaResponsesWhenEnabled() {
		LocalLlmPromptBuilder promptBuilder = new LocalLlmPromptBuilder();
		RuleBasedUpdatesSummarizer fallback = new RuleBasedUpdatesSummarizer(promptBuilder);
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

		server.expect(requestTo("http://localhost:11434/api/generate"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess("{\"response\":\"LLM summary\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo("http://localhost:11434/api/generate"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess("{\"response\":\"LLM action\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo("http://localhost:11434/api/generate"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess("{\"response\":\"LLM footer insight\"}", MediaType.APPLICATION_JSON));

		OllamaUpdatesSummarizer summarizer = new OllamaUpdatesSummarizer(
			builder,
			promptBuilder,
			fallback,
			true,
			"http://localhost:11434",
			"gemma4:latest"
		);

		assertEquals("LLM summary", summarizer.summarize(update));
		assertEquals("LLM action", summarizer.nextAction(update));
		assertEquals("LLM footer insight", summarizer.footerInsight(update));
		server.verify();
	}

	@Test
	void fallsBackWhenOllamaErrors() {
		LocalLlmPromptBuilder promptBuilder = new LocalLlmPromptBuilder();
		RuleBasedUpdatesSummarizer fallback = new RuleBasedUpdatesSummarizer(promptBuilder);
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

		server.expect(requestTo("http://localhost:11434/api/generate"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withServerError());
		server.expect(requestTo("http://localhost:11434/api/generate"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withServerError());

		OllamaUpdatesSummarizer summarizer = new OllamaUpdatesSummarizer(
			builder,
			promptBuilder,
			fallback,
			true,
			"http://localhost:11434",
			"gemma4:latest"
		);

		assertEquals(fallback.summarize(update), summarizer.summarize(update));
		assertEquals(fallback.footerInsight(update), summarizer.footerInsight(update));
		server.verify();
	}
}


