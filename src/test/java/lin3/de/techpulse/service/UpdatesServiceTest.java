package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		};

		UpdatesService updatesService = new UpdatesService(source, summarizer, 5, false, 300);
		var response = updatesService.getLatest(1);

		assertEquals(1, response.items().size());
		assertEquals("Summary for Release 1.0 launched", response.items().get(0).summary());
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
		};

		UpdatesService updatesService = new UpdatesService(source, summarizer, 5, false, 300);
		var response = updatesService.refreshLatest(1);

		assertEquals(1, response.items().size());
		assertEquals("Summary for Release 2.0 launched", response.items().get(0).summary());
	}

	@Test
	void getLatestUsesCacheWhenEnabled() {
		AtomicInteger calls = new AtomicInteger();
		TechUpdatesSource source = limit -> {
			calls.incrementAndGet();
			return List.of(new SourceUpdate("Cached update", "https://example.com/cached", Instant.parse("2026-04-20T10:00:00Z"), "Example"));
		};

		UpdatesSummarizer summarizer = simpleSummarizer();
		UpdatesService updatesService = new UpdatesService(source, summarizer, 5, true, 300);

		updatesService.getLatest(1);
		updatesService.getLatest(1);

		assertEquals(1, calls.get());
	}

	@Test
	void refreshBypassesCacheAndRebuilds() {
		AtomicInteger calls = new AtomicInteger();
		TechUpdatesSource source = limit -> {
			calls.incrementAndGet();
			return List.of(new SourceUpdate("Fresh update", "https://example.com/fresh", Instant.parse("2026-04-20T10:00:00Z"), "Example"));
		};

		UpdatesSummarizer summarizer = simpleSummarizer();
		UpdatesService updatesService = new UpdatesService(source, summarizer, 5, true, 300);

		updatesService.getLatest(1);
		updatesService.refreshLatest(1);

		assertEquals(2, calls.get());
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
		};
	}
}

