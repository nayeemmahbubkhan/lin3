package lin3.de.techpulse.model;

import java.time.Instant;

public record UpdatesSourceHealth(
	String name,
	boolean available,
	Instant lastCheckedAt,
	Instant lastSuccessAt,
	String lastError
) {
}

