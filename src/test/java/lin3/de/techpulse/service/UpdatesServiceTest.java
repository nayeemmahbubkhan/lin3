package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import lin3.de.techpulse.model.UpdatesSourceHealth;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdatesServiceTest {

	@Test
	void getLatestBuildsSummariesAndRespectsLimit() {
		TechUpdatesSource source = limit -> List.of(
			new SourceUpdate("Release 1.0 launched", "https://example.com/release", Instant.parse("2026-04-20T10:00:00Z"), "Example"),
			new SourceUpdate("Security patch available", "https://example.com/security", Instant.parse("2026-04-20T11:00:00Z"), "Example")
		);

		UpdatesSummarizer summarizer = new UpdatesSummarizer() {
			@Override
			public String summarize(SourceUpdate update) {
				return "Summary for " + update.title();
			}

			@Override
			public String nextAction(SourceUpdate update) {
				return "Action for " + update.title();
			}

			@Override
			public String footerInsight(SourceUpdate update) {
				return "Insight for " + update.title();
			}
		};

		UpdatesService updatesService = new UpdatesService(source, summarizer, 5, false, 300, true, "security,release,ai", "");
		var response = updatesService.getLatest(1);

		assertEquals(1, response.items().size());
		assertTrue(response.items().get(0).summary().startsWith("Summary for "));
		assertTrue(response.items().get(0).footerInsight().startsWith("Insight for "));
		assertFalse(response.fromCache());
	}

	@Test
	void refreshLatestUsesSamePipeline() {
		TechUpdatesSource source = limit -> List.of(
			new SourceUpdate("Release 2.0 launched", "https://example.com/release2", Instant.parse("2026-04-20T10:00:00Z"), "Example")
		);

		UpdatesSummarizer summarizer = new UpdatesSummarizer() {
			@Override
			public String summarize(SourceUpdate update) {
				return "Summary for " + update.title();
			}

			@Override
			public String nextAction(SourceUpdate update) {
				return "Action for " + update.title();
			}

			@Override
			public String footerInsight(SourceUpdate update) {
				return "Insight for " + update.title();
			}
		};

		UpdatesService updatesService = new UpdatesService(source, summarizer, 5, false, 300, true, "security,release,ai", "");
		var response = updatesService.refreshLatest(1);

		assertEquals(1, response.items().size());
		assertEquals("Summary for Release 2.0 launched", response.items().get(0).summary());
		assertEquals("Insight for Release 2.0 launched", response.items().get(0).footerInsight());
		assertFalse(response.fromCache());
	}

	@Test
	void getLatestUsesCacheWhenEnabled() {
		AtomicInteger calls = new AtomicInteger();
		TechUpdatesSource source = limit -> {
			calls.incrementAndGet();
			return List.of(new SourceUpdate("Cached update", "https://example.com/cached", Instant.parse("2026-04-20T10:00:00Z"), "Example"));
		};

		UpdatesSummarizer summarizer = simpleSummarizer();
		UpdatesService updatesService = new UpdatesService(source, summarizer, 5, true, 300, true, "security,release,ai", "");

		var first = updatesService.getLatest(1);
		var second = updatesService.getLatest(1);

		assertEquals(1, calls.get());
		assertFalse(first.fromCache());
		assertTrue(second.fromCache());
		assertNotNull(second.cachedAt());
	}

	@Test
	void refreshBypassesCacheAndRebuilds() {
		AtomicInteger calls = new AtomicInteger();
		TechUpdatesSource source = limit -> {
			calls.incrementAndGet();
			return List.of(new SourceUpdate("Fresh update", "https://example.com/fresh", Instant.parse("2026-04-20T10:00:00Z"), "Example"));
		};

		UpdatesSummarizer summarizer = simpleSummarizer();
		UpdatesService updatesService = new UpdatesService(source, summarizer, 5, true, 300, true, "security,release,ai", "");

		updatesService.getLatest(1);
		var refreshed = updatesService.refreshLatest(1);

		assertEquals(2, calls.get());
		assertFalse(refreshed.fromCache());
	}

	private UpdatesSummarizer simpleSummarizer() {
		return new UpdatesSummarizer() {
			@Override
			public String summarize(SourceUpdate update) {
				return "Summary for " + update.title();
			}

			@Override
			public String nextAction(SourceUpdate update) {
				return "Action for " + update.title();
			}

			@Override
			public String footerInsight(SourceUpdate update) {
				return "Insight for " + update.title();
			}
		};
	}

	@Test
	void getHealthReturnsSourceAndCacheStats() {
		TechUpdatesSource source = new TechUpdatesSource() {
			@Override
			public List<SourceUpdate> fetchLatest(int limit) {
				return List.of(new SourceUpdate("Health item", "https://example.com/health", Instant.parse("2026-04-20T10:00:00Z"), "Example"));
			}

			@Override
			public UpdatesSourceHealth getHealth() {
				return new UpdatesSourceHealth("hacker-news", true, Instant.parse("2026-04-24T10:00:00Z"), Instant.parse("2026-04-24T09:59:00Z"), null);
			}
		};

		UpdatesService updatesService = new UpdatesService(source, simpleSummarizer(), 5, true, 300, true, "security,release,ai", "");
		updatesService.getLatest(1);

		var health = updatesService.getHealth();
		assertEquals("hacker-news", health.source().name());
		assertTrue(health.source().available());
		assertTrue(health.cache().enabled());
		assertEquals(1, health.cache().entryCount());
		assertEquals(1, health.cache().validEntryCount());
	}

	@Test
	void refreshAllCommonLimitsNormalizesAndDeduplicates() {
		AtomicInteger calls = new AtomicInteger();
		TechUpdatesSource source = limit -> {
			calls.incrementAndGet();
			return List.of(new SourceUpdate("Item " + limit, "https://example.com/" + limit, Instant.parse("2026-04-20T10:00:00Z"), "Example"));
		};

		UpdatesService updatesService = new UpdatesService(source, simpleSummarizer(), 5, true, 300, true, "security,release,ai", "");
		var result = updatesService.refreshAllCommonLimits(List.of(5, 8, 8, 99, 0));

		assertEquals(3, result.refreshedCount());
		assertEquals(List.of(5, 8, 20), result.limits());
		assertEquals(3, calls.get());
	}

	@Test
	void getLatestDeduplicatesAndPrioritizesRelevantItems() {
		TechUpdatesSource source = limit -> List.of(
			new SourceUpdate("Security release for OSS database", "https://example.com/post/1", Instant.now().minus(Duration.ofHours(1)), "Example"),
			new SourceUpdate("Security release for OSS database", "https://example.com/post/1", Instant.now().minus(Duration.ofHours(1)), "Example"),
			new SourceUpdate("Tiny", "https://example.com/post/2", Instant.now().minus(Duration.ofHours(1)), "Example"),
			new SourceUpdate("Old release notice", "https://example.com/post/3", Instant.now().minus(Duration.ofDays(20)), "Example"),
			new SourceUpdate("AI platform launch for cloud workloads", "https://example.com/post/4", Instant.now().minus(Duration.ofHours(2)), "Example")
		);

		UpdatesService updatesService = new UpdatesService(source, simpleSummarizer(), 5, false, 300, true, "security,release,ai,cloud,database", "");
		var response = updatesService.getLatest(3);

		assertEquals(2, response.items().size());
		assertEquals("Security release for OSS database", response.items().get(0).title());
		assertEquals("AI platform launch for cloud workloads", response.items().get(1).title());
	}

	@Test
	void getLatestAppliesConfiguredSourceWeightsInRanking() {
		Instant now = Instant.now();
		TechUpdatesSource source = limit -> List.of(
			new SourceUpdate("General platform update", "https://example.com/hn", now.minus(Duration.ofHours(3)), "Hacker News"),
			new SourceUpdate("General platform update", "https://example.com/gh", now.minus(Duration.ofHours(3)), "GitHub Releases")
		);

		UpdatesService updatesService = new UpdatesService(
			source,
			simpleSummarizer(),
			5,
			false,
			300,
			true,
			"",
			"hacker-news:0.4,github-releases:1.8"
		);

		var response = updatesService.getLatest(2);

		assertEquals(2, response.items().size());
		assertEquals("https://example.com/gh", response.items().get(0).url());
		assertEquals("https://example.com/hn", response.items().get(1).url());
	}
}

