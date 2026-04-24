package lin3.de.techpulse.model;

import java.time.Instant;

public record SourceUpdate(
	String title,
	String url,
	Instant publishedAt,
	String source
) {
}

