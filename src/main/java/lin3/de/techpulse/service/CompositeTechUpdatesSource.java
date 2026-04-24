package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import lin3.de.techpulse.model.UpdatesSourceHealth;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Primary
public class CompositeTechUpdatesSource implements TechUpdatesSource {

	private final HackerNewsUpdatesSource hackerNewsUpdatesSource;
	private final GitHubReleasesRssUpdatesSource gitHubReleasesRssUpdatesSource;

	public CompositeTechUpdatesSource(
		HackerNewsUpdatesSource hackerNewsUpdatesSource,
		GitHubReleasesRssUpdatesSource gitHubReleasesRssUpdatesSource
	) {
		this.hackerNewsUpdatesSource = hackerNewsUpdatesSource;
		this.gitHubReleasesRssUpdatesSource = gitHubReleasesRssUpdatesSource;
	}

	@Override
	public List<SourceUpdate> fetchLatest(int limit) {
		List<SourceUpdate> merged = new ArrayList<>();
		merged.addAll(hackerNewsUpdatesSource.fetchLatest(limit));
		merged.addAll(gitHubReleasesRssUpdatesSource.fetchLatest(limit));

		Set<String> seenUrls = new HashSet<>();
		return merged.stream()
			.filter(item -> item.url() != null && !item.url().isBlank())
			.filter(item -> seenUrls.add(item.url().trim().toLowerCase()))
			.sorted(Comparator.comparing(SourceUpdate::publishedAt).reversed())
			.limit(limit)
			.toList();
	}

	@Override
	public UpdatesSourceHealth getHealth() {
		UpdatesSourceHealth hackerNews = hackerNewsUpdatesSource.getHealth();
		UpdatesSourceHealth github = gitHubReleasesRssUpdatesSource.getHealth();
		boolean available = hackerNews.available() || github.available();

		Instant lastChecked = max(hackerNews.lastCheckedAt(), github.lastCheckedAt());
		Instant lastSuccess = max(hackerNews.lastSuccessAt(), github.lastSuccessAt());
		String lastError = available
			? null
			: "hacker-news=" + safe(hackerNews.lastError()) + "; github-releases=" + safe(github.lastError());
		return new UpdatesSourceHealth("multi-source", available, lastChecked, lastSuccess, lastError);
	}

	private Instant max(Instant left, Instant right) {
		if (left == null) {
			return right;
		}
		if (right == null) {
			return left;
		}
		return left.isAfter(right) ? left : right;
	}

	private String safe(String input) {
		return input == null ? "n/a" : input;
	}
}


