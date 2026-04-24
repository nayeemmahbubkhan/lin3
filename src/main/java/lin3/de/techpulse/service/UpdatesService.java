package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import lin3.de.techpulse.model.UpdatesCacheHealth;
import lin3.de.techpulse.model.UpdatesHealthResponse;
import lin3.de.techpulse.model.TechUpdate;
import lin3.de.techpulse.model.UpdatesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class UpdatesService {

	private static final int MAX_LIMIT = 20;

	private final TechUpdatesSource techUpdatesSource;
	private final UpdatesSummarizer updatesSummarizer;
	private final int defaultLimit;
	private final boolean cacheEnabled;
	private final Duration cacheTtl;
	private final ConcurrentMap<Integer, CacheEntry> cacheByLimit = new ConcurrentHashMap<>();

	public UpdatesService(TechUpdatesSource techUpdatesSource,
		UpdatesSummarizer updatesSummarizer,
		@Value("${techpulse.updates.default-limit:5}") int defaultLimit,
		@Value("${techpulse.updates.cache.enabled:true}") boolean cacheEnabled,
		@Value("${techpulse.updates.cache-ttl-seconds:300}") long cacheTtlSeconds) {
		this.techUpdatesSource = techUpdatesSource;
		this.updatesSummarizer = updatesSummarizer;
		this.defaultLimit = defaultLimit;
		this.cacheEnabled = cacheEnabled;
		this.cacheTtl = Duration.ofSeconds(Math.max(0, cacheTtlSeconds));
	}

	public UpdatesResponse getLatest(Integer requestedLimit) {
		int limit = normalizeLimit(requestedLimit);
		if (cacheEnabled) {
			CacheEntry cached = cacheByLimit.get(limit);
			if (cached != null && !isExpired(cached.cachedAt())) {
				return new UpdatesResponse(
					cached.response().generatedAt(),
					cached.response().source(),
					cached.response().items(),
					true,
					cached.cachedAt()
				);
			}
		}
		return refreshLatest(limit);
	}

	public UpdatesResponse refreshLatest(Integer requestedLimit) {
		int limit = normalizeLimit(requestedLimit);
		UpdatesResponse response = buildLatest(limit);
		if (cacheEnabled) {
			cacheByLimit.put(limit, new CacheEntry(response, Instant.now()));
		}
		return response;
	}

	public UpdatesHealthResponse getHealth() {
		int entryCount = cacheByLimit.size();
		int validEntryCount = (int) cacheByLimit.values().stream()
			.filter(entry -> !isExpired(entry.cachedAt()))
			.count();

		UpdatesCacheHealth cacheHealth = new UpdatesCacheHealth(
			cacheEnabled,
			cacheTtl.toSeconds(),
			entryCount,
			validEntryCount
		);

		return new UpdatesHealthResponse(Instant.now(), techUpdatesSource.getHealth(), cacheHealth);
	}

	private UpdatesResponse buildLatest(int limit) {
		List<SourceUpdate> sourceUpdates = techUpdatesSource.fetchLatest(limit);
		List<TechUpdate> items = sourceUpdates.stream()
			.limit(limit)
			.map(update -> new TechUpdate(
				update.title(),
				update.url(),
				update.source(),
				update.publishedAt(),
				updatesSummarizer.summarize(update),
				updatesSummarizer.nextAction(update)
			))
			.toList();

		return new UpdatesResponse(Instant.now(), "hacker-news", items, false, null);
	}

	private boolean isExpired(Instant cachedAt) {
		return cacheTtl.isZero() || cachedAt.plus(cacheTtl).isBefore(Instant.now());
	}

	private int normalizeLimit(Integer requestedLimit) {
		if (requestedLimit == null || requestedLimit < 1) {
			return Math.min(defaultLimit, MAX_LIMIT);
		}
		return Math.min(requestedLimit, MAX_LIMIT);
	}

	private record CacheEntry(UpdatesResponse response, Instant cachedAt) {
	}
}

