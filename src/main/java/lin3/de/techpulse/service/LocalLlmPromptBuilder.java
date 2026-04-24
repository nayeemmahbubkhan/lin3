package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import org.springframework.stereotype.Component;

@Component
public class LocalLlmPromptBuilder {

	public String buildPrompt(SourceUpdate update) {
		return "Summarize this tech update in 2 short sentences and suggest one concrete action.\n"
			+ "Title: " + update.title() + "\n"
			+ "Source: " + update.source() + "\n"
			+ "URL: " + update.url();
	}
}

