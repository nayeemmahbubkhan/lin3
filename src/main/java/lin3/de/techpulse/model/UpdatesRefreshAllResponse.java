package lin3.de.techpulse.model;

import java.time.Instant;
import java.util.List;

public record UpdatesRefreshAllResponse(
	Instant refreshedAt,
	List<Integer> limits,
	int refreshedCount,
	boolean sourceAvailable
) {
}

