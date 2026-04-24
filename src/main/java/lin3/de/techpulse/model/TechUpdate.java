package lin3.de.techpulse.model;

import java.time.Instant;

public record TechUpdate(
	String title,
	String url,
	String source,
	Instant publishedAt,
	String summary,
	String action,
	String footerInsight
) {
}

