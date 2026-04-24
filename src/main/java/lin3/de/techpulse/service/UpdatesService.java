package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import lin3.de.techpulse.model.TechUpdate;
import lin3.de.techpulse.model.UpdatesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UpdatesService {

	private static final int MAX_LIMIT = 20;

	private final TechUpdatesSource techUpdatesSource;
	private final UpdatesSummarizer updatesSummarizer;
	private final int defaultLimit;

	public UpdatesService(TechUpdatesSource techUpdatesSource,
		UpdatesSummarizer updatesSummarizer,
		@Value("${techpulse.updates.default-limit:5}") int defaultLimit) {
		this.techUpdatesSource = techUpdatesSource;
		this.updatesSummarizer = updatesSummarizer;
		this.defaultLimit = defaultLimit;
	}

	public UpdatesResponse getLatest(Integer requestedLimit) {
		int limit = normalizeLimit(requestedLimit);
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

		return new UpdatesResponse(Instant.now(), "hacker-news", items);
	}

	private int normalizeLimit(Integer requestedLimit) {
		if (requestedLimit == null || requestedLimit < 1) {
			return Math.min(defaultLimit, MAX_LIMIT);
		}
		return Math.min(requestedLimit, MAX_LIMIT);
	}
}

