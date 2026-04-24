package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import org.springframework.stereotype.Service;

@Service
public class RuleBasedUpdatesSummarizer implements UpdatesSummarizer {

	private final LocalLlmPromptBuilder promptBuilder;

	public RuleBasedUpdatesSummarizer(LocalLlmPromptBuilder promptBuilder) {
		this.promptBuilder = promptBuilder;
	}

	@Override
	public String summarize(SourceUpdate update) {
		return "Quick brief: " + update.title() + " (" + update.source() + "). "
			+ "Read the source for technical details and assess impact on your stack.";
	}

	@Override
	public String nextAction(SourceUpdate update) {
		String title = update.title().toLowerCase();
		if (title.contains("security") || title.contains("vulnerability") || title.contains("cve")) {
			return "Check affected systems and patch priority today.";
		}
		if (title.contains("release") || title.contains("launch")) {
			return "Review changelog impact on your stack this week.";
		}
		if (title.contains("funding") || title.contains("acquisition")) {
			return "Track market impact and competitor roadmap changes.";
		}
		return "Decide whether this impacts your stack, then assign an owner or ignore it.";
	}
}

