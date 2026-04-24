package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import lin3.de.techpulse.model.UpdatesSourceHealth;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeTechUpdatesSourceTest {

	@Test
	void fetchLatestMergesAndDeduplicates() {
		HackerNewsUpdatesSource hackerNews = mock(HackerNewsUpdatesSource.class);
		GitHubReleasesRssUpdatesSource github = mock(GitHubReleasesRssUpdatesSource.class);

		when(hackerNews.fetchLatest(5)).thenReturn(List.of(
			new SourceUpdate("A", "https://example.com/a", Instant.parse("2026-04-24T10:00:00Z"), "Hacker News"),
			new SourceUpdate("B", "https://example.com/b", Instant.parse("2026-04-24T09:00:00Z"), "Hacker News")
		));
		when(github.fetchLatest(5)).thenReturn(List.of(
			new SourceUpdate("A duplicate", "https://example.com/a", Instant.parse("2026-04-24T11:00:00Z"), "GitHub Releases"),
			new SourceUpdate("C", "https://example.com/c", Instant.parse("2026-04-24T08:00:00Z"), "GitHub Releases")
		));

		CompositeTechUpdatesSource composite = new CompositeTechUpdatesSource(hackerNews, github);
		var merged = composite.fetchLatest(5);

		assertEquals(3, merged.size());
		assertEquals("https://example.com/a", merged.get(0).url());
	}

	@Test
	void getHealthAggregatesAvailability() {
		HackerNewsUpdatesSource hackerNews = mock(HackerNewsUpdatesSource.class);
		GitHubReleasesRssUpdatesSource github = mock(GitHubReleasesRssUpdatesSource.class);

		when(hackerNews.getHealth()).thenReturn(new UpdatesSourceHealth("hacker-news", false, Instant.now(), null, "down"));
		when(github.getHealth()).thenReturn(new UpdatesSourceHealth("github-releases", true, Instant.now(), Instant.now(), null));

		CompositeTechUpdatesSource composite = new CompositeTechUpdatesSource(hackerNews, github);
		var health = composite.getHealth();

		assertEquals("multi-source", health.name());
		assertTrue(health.available());
	}
}

