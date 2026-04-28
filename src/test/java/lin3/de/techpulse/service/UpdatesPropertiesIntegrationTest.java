package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import lin3.de.techpulse.model.UpdatesSourceHealth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = {
	"techpulse.updates.cache.enabled=false",
	"techpulse.updates.quality.enabled=true",
	"techpulse.updates.quality.keywords=",
	"techpulse.updates.github-rss-url=https://github.com/spring-projects/spring-boot/releases.atom",
	"techpulse.updates.github-rss-urls=https://github.com/org-a/repo-a/releases.atom,https://github.com/org-b/repo-b/releases.atom",
	"techpulse.updates.source-weights=hacker-news:0.4,github-releases:1.8"
})
class UpdatesPropertiesIntegrationTest {

	@Autowired
	private GitHubReleasesRssUpdatesSource gitHubReleasesRssUpdatesSource;

	@Autowired
	private UpdatesService updatesService;

	@MockitoBean
	private CompositeTechUpdatesSource compositeTechUpdatesSource;

	@MockitoBean
	private UpdatesSummarizer updatesSummarizer;

	@Test
	void githubRssUrlsPropertyBindsAsConfiguredList() {
		Object field = ReflectionTestUtils.getField(gitHubReleasesRssUpdatesSource, "rssUrls");
		assertTrue(field instanceof List<?>);
		List<?> urls = (List<?>) field;
		assertEquals(2, urls.size());
		assertEquals("https://github.com/org-a/repo-a/releases.atom", urls.get(0));
		assertEquals("https://github.com/org-b/repo-b/releases.atom", urls.get(1));
	}

	@Test
	void sourceWeightsPropertyInfluencesRankingOrder() {
		Instant now = Instant.now();
		given(compositeTechUpdatesSource.fetchLatest(anyInt())).willReturn(List.of(
			new SourceUpdate("General update alpha", "https://example.com/hn", now.minus(Duration.ofHours(2)), "Hacker News"),
			new SourceUpdate("General update alpha", "https://example.com/gh", now.minus(Duration.ofHours(2)), "GitHub Releases")
		));
		given(compositeTechUpdatesSource.getHealth()).willReturn(new UpdatesSourceHealth("multi-source", true, now, now, null));
		given(updatesSummarizer.summarize(org.mockito.ArgumentMatchers.any(SourceUpdate.class))).willAnswer(invocation -> "Summary for " + invocation.getArgument(0, SourceUpdate.class).title());
		given(updatesSummarizer.nextAction(org.mockito.ArgumentMatchers.any(SourceUpdate.class))).willReturn("Review");
		given(updatesSummarizer.didYouKnow(org.mockito.ArgumentMatchers.any(SourceUpdate.class))).willReturn("Did you know?");

		var response = updatesService.getLatest(2);

		assertEquals(2, response.items().size());
		assertEquals("https://example.com/gh", response.items().get(0).url());
		assertEquals("https://example.com/hn", response.items().get(1).url());
	}
}

