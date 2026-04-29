package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Primary
public class OllamaUpdatesSummarizer implements UpdatesSummarizer {

	private final RestClient restClient;
	private final LocalLlmPromptBuilder promptBuilder;
	private final RuleBasedUpdatesSummarizer fallback;
	private final boolean enabled;
	private final String model;

	public OllamaUpdatesSummarizer(
		RestClient.Builder restClientBuilder,
		LocalLlmPromptBuilder promptBuilder,
		RuleBasedUpdatesSummarizer fallback,
		@Value("${techpulse.llm.enabled:false}") boolean enabled,
		@Value("${techpulse.llm.base-url:http://localhost:11434}") String baseUrl,
		@Value("${techpulse.llm.model:gemma4:latest}") String model
	) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
		this.promptBuilder = promptBuilder;
		this.fallback = fallback;
		this.enabled = enabled;
		this.model = model;
	}

	@Override
	public String summarize(SourceUpdate update) {
		if (!enabled) {
			return fallback.summarize(update);
		}
		try {
			String generated = generate(promptBuilder.buildSummaryPrompt(update));
			return generated.isBlank() ? fallback.summarize(update) : generated;
		} catch (Exception ex) {
			return fallback.summarize(update);
		}
	}

	@Override
	public String nextAction(SourceUpdate update) {
		if (!enabled) {
			return fallback.nextAction(update);
		}
		try {
			String generated = generate(promptBuilder.buildActionPrompt(update));
			return generated.isBlank() ? fallback.nextAction(update) : generated;
		} catch (Exception ex) {
			return fallback.nextAction(update);
		}
	}

	@Override
	public String footerInsight(SourceUpdate update) {
		// Footer text is not rendered in the current UI; avoid spending an LLM call per item.
		return fallback.footerInsight(update);
	}

	@Override
	public String didYouKnow(SourceUpdate update) {
		if (!enabled) {
			return fallback.didYouKnow(update);
		}
		try {
			String generated = generate(promptBuilder.buildDidYouKnowPrompt(update));
			return generated.isBlank() ? fallback.didYouKnow(update) : generated;
		} catch (Exception ex) {
			return fallback.didYouKnow(update);
		}
	}

	private String generate(String prompt) {
		Map<String, Object> request = Map.of(
			"model", model,
			"prompt", prompt,
			"stream", false
		);

		Map<String, Object> response = restClient.post()
			.uri("/api/generate")
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.body(Map.class);

		if (response == null) {
			return "";
		}
		Object value = response.get("response");
		return value instanceof String text ? text.trim() : "";
	}
}
