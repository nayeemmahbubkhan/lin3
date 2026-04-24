package lin3.de.techpulse.model;

import java.time.Instant;
import java.util.UUID;

public record ContactMessage(
	UUID id,
	String name,
	String email,
	String message,
	String ipAddress,
	String userAgent,
	Instant createdAt
) {
}

