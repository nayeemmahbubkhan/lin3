package lin3.de.techpulse.model;

import java.time.Instant;
import java.util.List;

public record UpdatesResponse(
	Instant generatedAt,
	String source,
	List<TechUpdate> items
) {
}

