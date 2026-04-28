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
		return pickVariant(update, new String[] {
			"Triaging this in backlog grooming helps avoid surprise dependencies later.",
			"Assign a quick owner to assess stack impact and recommend follow-up this sprint.",
			"Tag this signal in your roadmap notes and decide keep/watch/ignore with clear criteria.",
			"Create a lightweight spike only if this can affect reliability, security, or delivery speed."
		});
	}

	@Override
	public String footerInsight(SourceUpdate update) {
		String title = update.title().toLowerCase();
		if (title.contains("security") || title.contains("vulnerability") || title.contains("cve")) {
			return "Potential production risk; validate exposure and patch urgency.";
		}
		if (title.contains("release") || title.contains("launch")) {
			return "May change dependencies or behavior in your next delivery cycle.";
		}
		if (title.contains("funding") || title.contains("acquisition")) {
			return "Could shift vendor priorities and ecosystem direction soon.";
		}
		return pickVariant(update, new String[] {
			"Could influence planning assumptions once adjacent tooling starts adopting it.",
			"Worth tracking now to reduce migration pressure later.",
			"Low effort to monitor, potentially high impact on upcoming technical decisions.",
			"Useful early signal that can shape backlog priorities before changes become urgent."
		});
	}

	@Override
	public String didYouKnow(SourceUpdate update) {
		String haystack = (update.title() + " " + update.source() + " " + update.url()).toLowerCase();
		if (containsAny(haystack, "security", "vulnerability", "cve", "advisory", "exploit", "patch")) {
			return pickVariant(update, new String[] {
				"Did you know? Most severe incidents start with delayed patching, not zero-day exploits.",
				"Prediction: patch-lag compounds quickly; teams that rank internet-facing assets first reduce risk faster.",
				"Did you know? Exposure windows are usually created by inventory gaps, not missing patch tools.",
				"Prediction: security advisories with broad dependency chains usually trigger at least one urgent change request."
			});
		}
		if (containsAny(haystack, "release", "launch", "changelog", "version", "ga", "beta", "rc", "stable")) {
			return "Prediction: teams usually discover at least one integration edge case within the first week of a major release.";
		}
		if (containsAny(haystack, "ai", "llm", "model", "prompt", "inference", "agent")) {
			return "Did you know? Small prompt and context changes often impact output quality more than model size changes.";
		}

		String[] variants = new String[] {
			"Did you know? Platform shifts often look minor first, then compound into migration pressure over two to three quarters.",
			"Prediction: teams that track this signal monthly usually reduce surprise work during roadmap planning.",
			"Did you know? A single ecosystem change can cascade into tooling, compliance, and developer workflow updates.",
			"Prediction: low-noise signals like this often become backlog items within one or two planning cycles."
		};
		return pickVariant(update, variants);
	}

	private String pickVariant(SourceUpdate update, String[] variants) {
		int idx = Math.floorMod((update.title() + "|" + update.url() + "|" + update.source()).hashCode(), variants.length);
		return variants[idx];
	}

	private boolean containsAny(String text, String... needles) {
		for (String needle : needles) {
			if (text.contains(needle)) {
				return true;
			}
		}
		return false;
	}
}

