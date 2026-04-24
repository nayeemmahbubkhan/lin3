package lin3.de.techpulse.model;

public record UpdatesCacheHealth(
	boolean enabled,
	long ttlSeconds,
	int entryCount,
	int validEntryCount
) {
}

