package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import lin3.de.techpulse.model.UpdatesSourceHealth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class HackerNewsUpdatesSource implements TechUpdatesSource {

	private final RestClient restClient;
	private final String sourceUrl;
	private volatile boolean available;
	private volatile Instant lastCheckedAt;
	private volatile Instant lastSuccessAt;
	private volatile String lastError;

	public HackerNewsUpdatesSource(RestClient.Builder restClientBuilder,
		@Value("${techpulse.updates.source-url:https://hn.algolia.com/api/v1/search_by_date?tags=story}") String sourceUrl) {
		this.restClient = restClientBuilder.build();
		this.sourceUrl = sourceUrl;
	}

	@Override
	public List<SourceUpdate> fetchLatest(int limit) {
		try {
			Map<String, Object> payload = restClient.get()
				.uri(sourceUrl)
				.retrieve()
				.body(Map.class);
			if (payload == null) {
				markFailure("Empty source response");
				return fallbackItems();
			}

			Object hits = payload.get("hits");
			if (!(hits instanceof List<?> hitList)) {
				markFailure("Source payload missing hits");
				return fallbackItems();
			}

			List<SourceUpdate> updates = new ArrayList<>();
			for (Object hit : hitList) {
				if (!(hit instanceof Map<?, ?> item)) {
					continue;
				}

				String title = value(item, "title", "story_title");
				String url = value(item, "url", "story_url");
				String createdAt = value(item, "created_at");
				if (title.isBlank() || url.isBlank()) {
					continue;
				}

				Instant publishedAt;
				try {
					publishedAt = Instant.parse(createdAt);
				} catch (Exception ignored) {
					publishedAt = Instant.now();
				}

				updates.add(new SourceUpdate(title, url, publishedAt, "Hacker News"));
			}

			updates.sort(Comparator.comparing(SourceUpdate::publishedAt).reversed());
			if (updates.isEmpty()) {
				markFailure("No valid items from source");
				return fallbackItems();
			}
			markSuccess();
			return updates.stream().limit(limit).toList();
		} catch (Exception ex) {
			markFailure(ex.getClass().getSimpleName() + ": " + ex.getMessage());
			return fallbackItems();
		}
	}

	@Override
	public UpdatesSourceHealth getHealth() {
		return new UpdatesSourceHealth("hacker-news", available, lastCheckedAt, lastSuccessAt, lastError);
	}

	private void markSuccess() {
		Instant now = Instant.now();
		available = true;
		lastCheckedAt = now;
		lastSuccessAt = now;
		lastError = null;
	}

	private void markFailure(String error) {
		available = false;
		lastCheckedAt = Instant.now();
		lastError = error;
	}

	private String value(Map<?, ?> item, String... keys) {
		for (String key : keys) {
			Object value = item.get(key);
			if (value instanceof String text && !text.isBlank()) {
				return text.trim();
			}
		}
		return "";
	}

	private List<SourceUpdate> fallbackItems() {
		Instant now = Instant.now();
		return List.of(
			new SourceUpdate("Fallback: Local feed active while source is unavailable", "https://news.ycombinator.com", now, "Tech Pulse"),
			new SourceUpdate("Fallback: Connect your own source URL via techpulse.updates.source-url", "https://hn.algolia.com/api", now.minusSeconds(300), "Tech Pulse")
		);
	}
}

