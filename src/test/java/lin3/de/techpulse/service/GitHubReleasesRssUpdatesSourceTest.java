package lin3.de.techpulse.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubReleasesRssUpdatesSourceTest {

	@Test
	void fetchLatestParsesAtomFeed() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://github.com/spring-projects/spring-boot/releases.atom"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				<?xml version=\"1.0\" encoding=\"utf-8\"?>
				<feed xmlns=\"http://www.w3.org/2005/Atom\">
				  <entry>
				    <title>v3.4.5</title>
				    <updated>2026-04-24T10:00:00Z</updated>
				    <link rel=\"alternate\" href=\"https://github.com/spring-projects/spring-boot/releases/tag/v3.4.5\"/>
				  </entry>
				</feed>
				""", MediaType.APPLICATION_XML));

		GitHubReleasesRssUpdatesSource source = new GitHubReleasesRssUpdatesSource(
			builder,
			"https://github.com/spring-projects/spring-boot/releases.atom",
			""
		);
		var items = source.fetchLatest(5);

		assertEquals(1, items.size());
		assertEquals("GitHub Releases", items.get(0).source());
		assertTrue(source.getHealth().available());
		server.verify();
	}

	@Test
	void fetchLatestMarksUnavailableOnError() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://github.com/spring-projects/spring-boot/releases.atom"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withServerError());

		GitHubReleasesRssUpdatesSource source = new GitHubReleasesRssUpdatesSource(
			builder,
			"https://github.com/spring-projects/spring-boot/releases.atom",
			""
		);
		var items = source.fetchLatest(5);

		assertTrue(items.isEmpty());
		assertFalse(source.getHealth().available());
		server.verify();
	}

	@Test
	void fetchLatestAggregatesAcrossConfiguredFeedsAndDeduplicates() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

		server.expect(requestTo("https://github.com/org-a/repo-a/releases.atom"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				<?xml version=\"1.0\" encoding=\"utf-8\"?>
				<feed xmlns=\"http://www.w3.org/2005/Atom\">
				  <entry>
				    <title>repo-a v1.2.0</title>
				    <updated>2026-04-24T10:00:00Z</updated>
				    <link rel=\"alternate\" href=\"https://github.com/org-a/repo-a/releases/tag/v1.2.0\"/>
				  </entry>
				</feed>
				""", MediaType.APPLICATION_XML));

		server.expect(requestTo("https://github.com/org-b/repo-b/releases.atom"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				<?xml version=\"1.0\" encoding=\"utf-8\"?>
				<feed xmlns=\"http://www.w3.org/2005/Atom\">
				  <entry>
				    <title>repo-b v2.0.0</title>
				    <updated>2026-04-24T11:00:00Z</updated>
				    <link rel=\"alternate\" href=\"https://github.com/org-b/repo-b/releases/tag/v2.0.0\"/>
				  </entry>
				  <entry>
				    <title>duplicate item</title>
				    <updated>2026-04-24T09:00:00Z</updated>
				    <link rel=\"alternate\" href=\"https://github.com/org-a/repo-a/releases/tag/v1.2.0\"/>
				  </entry>
				</feed>
				""", MediaType.APPLICATION_XML));

		GitHubReleasesRssUpdatesSource source = new GitHubReleasesRssUpdatesSource(
			builder,
			"https://github.com/spring-projects/spring-boot/releases.atom",
			"https://github.com/org-a/repo-a/releases.atom,https://github.com/org-b/repo-b/releases.atom"
		);

		var items = source.fetchLatest(10);

		assertEquals(2, items.size());
		assertEquals("repo-b v2.0.0", items.get(0).title());
		assertEquals("repo-a v1.2.0", items.get(1).title());
		assertTrue(source.getHealth().available());
		server.verify();
	}
}

