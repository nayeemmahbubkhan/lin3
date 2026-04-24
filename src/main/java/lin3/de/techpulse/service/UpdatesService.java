package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import lin3.de.techpulse.model.UpdatesCacheHealth;
import lin3.de.techpulse.model.UpdatesHealthResponse;
import lin3.de.techpulse.model.UpdatesRefreshAllResponse;
import lin3.de.techpulse.model.TechUpdate;
import lin3.de.techpulse.model.UpdatesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
	private final boolean qualityEnabled;
	private final List<String> qualityKeywords;
	private final Map<String, Double> sourceWeights;
	private final ConcurrentMap<Integer, CacheEntry> cacheByLimit = new ConcurrentHashMap<>();

	public UpdatesService(TechUpdatesSource techUpdatesSource,
		UpdatesSummarizer updatesSummarizer,
		@Value("${techpulse.updates.default-limit:5}") int defaultLimit,
		@Value("${techpulse.updates.cache.enabled:true}") boolean cacheEnabled,
		@Value("${techpulse.updates.cache-ttl-seconds:300}") long cacheTtlSeconds,
		@Value("${techpulse.updates.quality.enabled:true}") boolean qualityEnabled,
		@Value("${techpulse.updates.quality.keywords:security,release,ai,open-source,cloud,database}") String qualityKeywordsRaw,
		@Value("${techpulse.updates.source-weights:}") String sourceWeightsRaw) {
		this.techUpdatesSource = techUpdatesSource;
		this.updatesSummarizer = updatesSummarizer;
		this.defaultLimit = defaultLimit;
		this.cacheEnabled = cacheEnabled;
		this.cacheTtl = Duration.ofSeconds(Math.max(0, cacheTtlSeconds));
		this.qualityEnabled = qualityEnabled;
		this.qualityKeywords = parseKeywords(qualityKeywordsRaw);
		this.sourceWeights = parseSourceWeights(sourceWeightsRaw);
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

	public UpdatesRefreshAllResponse refreshAllCommonLimits(List<Integer> requestedLimits) {
		List<Integer> normalizedLimits = requestedLimits == null ? List.of() : requestedLimits.stream()
			.map(this::normalizeLimit)
			.distinct()
			.toList();

		List<Integer> refreshed = new ArrayList<>();
		for (Integer limit : normalizedLimits) {
			refreshLatest(limit);
			refreshed.add(limit);
		}

		return new UpdatesRefreshAllResponse(
			Instant.now(),
			refreshed,
			refreshed.size(),
			techUpdatesSource.getHealth().available()
		);
	}

	private UpdatesResponse buildLatest(int limit) {
		int fetchLimit = qualityEnabled ? Math.max(limit * 3, limit) : limit;
		List<SourceUpdate> sourceUpdates = techUpdatesSource.fetchLatest(fetchLimit);
		List<SourceUpdate> curated = qualityEnabled
			? curate(sourceUpdates, limit)
			: sourceUpdates.stream().limit(limit).toList();

		List<TechUpdate> items = curated.stream()
			.limit(limit)
			.map(update -> new TechUpdate(
				update.title(),
				update.url(),
				update.source(),
				update.publishedAt(),
				safeSummary(update),
				safeAction(update),
				safeFooterInsight(update)
			))
			.toList();

		String sourceName = techUpdatesSource.getHealth().name();
		return new UpdatesResponse(Instant.now(), sourceName, items, false, null);
	}

	private boolean isExpired(Instant cachedAt) {
		return cacheTtl.isZero() || cachedAt.plus(cacheTtl).isBefore(Instant.now());
	}

	private List<SourceUpdate> curate(List<SourceUpdate> updates, int limit) {
		Set<String> seen = new HashSet<>();
		Instant now = Instant.now();

		return updates.stream()
			.filter(this::isHighSignal)
			.filter(update -> seen.add(dedupKey(update)))
			.sorted(Comparator
				.comparingDouble((SourceUpdate update) -> score(update, now)).reversed()
				.thenComparing(SourceUpdate::publishedAt, Comparator.reverseOrder()))
			.limit(limit)
			.toList();
	}

	private String safeSummary(SourceUpdate update) {
		String summary = updatesSummarizer.summarize(update);
		if (summary == null || summary.isBlank()) {
			return "Summary is not available yet. Check the source for details.";
		}
		String normalized = summary.trim();
		return normalized.length() > 320 ? normalized.substring(0, 317) + "..." : normalized;
	}

	private String safeAction(SourceUpdate update) {
		String action = updatesSummarizer.nextAction(update);
		if (action == null || action.isBlank()) {
			return "Review the source and decide if this affects your stack.";
		}
		return action.trim();
	}

	private String safeFooterInsight(SourceUpdate update) {
		String insight = updatesSummarizer.footerInsight(update);
		if (insight == null || insight.isBlank()) {
			return "Scan this update quickly and confirm whether it changes near-term plans.";
		}
		String normalized = insight.trim();
		return normalized.length() > 140 ? normalized.substring(0, 137) + "..." : normalized;
	}

	private boolean isHighSignal(SourceUpdate update) {
		if (update == null || update.title() == null || update.url() == null || update.publishedAt() == null) {
			return false;
		}
		String title = update.title().trim();
		if (title.length() < 10) {
			return false;
		}
		// Drop stale entries older than 14 days when quality mode is enabled.
		return update.publishedAt().isAfter(Instant.now().minus(Duration.ofDays(14)));
	}

	private String dedupKey(SourceUpdate update) {
		String url = update.url().toLowerCase(Locale.ROOT).replace("https://", "").replace("http://", "").replace("/", "");
		if (!url.isBlank()) {
			return url;
		}
		return update.title().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
	}

	private double score(SourceUpdate update, Instant now) {
		String haystack = (update.title() + " " + update.url()).toLowerCase(Locale.ROOT);
		int keywordScore = 0;
		for (String keyword : qualityKeywords) {
			if (!keyword.isBlank() && haystack.contains(keyword)) {
				keywordScore += 12;
			}
		}

		if (haystack.contains("security") || haystack.contains("cve") || haystack.contains("vulnerability")) {
			keywordScore += 18;
		}
		if (haystack.contains("release") || haystack.contains("launch")) {
			keywordScore += 8;
		}

		long ageHours = Math.max(0, Duration.between(update.publishedAt(), now).toHours());
		int freshnessScore = (int) Math.max(0, 72 - ageHours);
		double baseScore = freshnessScore + keywordScore;
		double weight = resolveSourceWeight(update.source());
		return baseScore * weight;
	}

	private List<String> parseKeywords(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		return java.util.Arrays.stream(raw.split(","))
			.map(String::trim)
			.map(value -> value.toLowerCase(Locale.ROOT))
			.filter(value -> !value.isBlank())
			.distinct()
			.toList();
	}

	private Map<String, Double> parseSourceWeights(String raw) {
		if (raw == null || raw.isBlank()) {
			return Map.of();
		}
		Map<String, Double> weights = new java.util.HashMap<>();
		for (String pair : raw.split(",")) {
			String candidate = pair.trim();
			if (candidate.isBlank() || !candidate.contains(":")) {
				continue;
			}
			String[] parts = candidate.split(":", 2);
			String key = normalizeSourceKey(parts[0]);
			if (key.isBlank()) {
				continue;
			}
			try {
				double weight = Double.parseDouble(parts[1].trim());
				if (Double.isFinite(weight) && weight >= 0d) {
					weights.put(key, weight);
				}
			} catch (NumberFormatException ignored) {
				// Ignore malformed entries and keep defaults.
			}
		}
		return Map.copyOf(weights);
	}

	private double resolveSourceWeight(String source) {
		String key = normalizeSourceKey(source);
		return sourceWeights.getOrDefault(key, 1d);
	}

	private String normalizeSourceKey(String source) {
		if (source == null || source.isBlank()) {
			return "";
		}
		String normalized = source.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
		return normalized.replaceAll("(^-+|-+$)", "");
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

