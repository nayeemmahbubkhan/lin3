package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import org.springframework.stereotype.Component;

@Component
public class LocalLlmPromptBuilder {

	public String buildPrompt(SourceUpdate update) {
		return buildSummaryPrompt(update);
	}

	public String buildSummaryPrompt(SourceUpdate update) {
		return "Summarize this tech update in 2 short sentences and suggest one concrete action.\n"
			+ "Title: " + update.title() + "\n"
			+ "Source: " + update.source() + "\n"
			+ "URL: " + update.url();
	}

	public String buildActionPrompt(SourceUpdate update) {
		return "Given this tech update, provide one concise next action for an engineering team.\n"
			+ "Title: " + update.title() + "\n"
			+ "Source: " + update.source() + "\n"
			+ "URL: " + update.url();
	}

	public String buildFooterInsightPrompt(SourceUpdate update) {
		return "Write one short 'why this matters' sentence (max 18 words) for this update."
			+ " Keep it practical for engineering decisions.\n"
			+ "Title: " + update.title() + "\n"
			+ "Source: " + update.source() + "\n"
			+ "URL: " + update.url();
	}

	public String buildDidYouKnowPrompt(SourceUpdate update) {
		return "Write one concise insight line for this tech update (max 22 words)."
			+ " Do not add labels like 'Did you know?' or 'Prediction:'.\n"
			+ "Title: " + update.title() + "\n"
			+ "Source: " + update.source() + "\n"
			+ "URL: " + update.url();
	}
}

