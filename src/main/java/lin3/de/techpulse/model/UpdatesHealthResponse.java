package lin3.de.techpulse.model;

import java.time.Instant;

public record UpdatesHealthResponse(
	Instant checkedAt,
	UpdatesSourceHealth source,
	UpdatesCacheHealth cache
) {
}

