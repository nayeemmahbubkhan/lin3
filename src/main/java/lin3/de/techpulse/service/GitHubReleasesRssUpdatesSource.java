package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import lin3.de.techpulse.model.UpdatesSourceHealth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GitHubReleasesRssUpdatesSource implements TechUpdatesSource {

	private final RestClient restClient;
	private final List<String> rssUrls;
	private volatile boolean available;
	private volatile Instant lastCheckedAt;
	private volatile Instant lastSuccessAt;
	private volatile String lastError;

	public GitHubReleasesRssUpdatesSource(
		RestClient.Builder restClientBuilder,
		@Value("${techpulse.updates.github-rss-url:https://github.com/spring-projects/spring-boot/releases.atom}") String rssUrl,
		@Value("${techpulse.updates.github-rss-urls:}") String rssUrlsRaw
	) {
		this.restClient = restClientBuilder.build();
		this.rssUrls = parseRssUrls(rssUrlsRaw, rssUrl);
	}


	@Override
	public List<SourceUpdate> fetchLatest(int limit) {
		try {
			List<SourceUpdate> updates = new ArrayList<>();
			List<String> errors = new ArrayList<>();

			for (String rssUrl : rssUrls) {
				try {
					String xml = restClient.get()
						.uri(rssUrl)
						.retrieve()
						.body(String.class);
					if (xml == null || xml.isBlank()) {
						errors.add("Empty RSS response: " + rssUrl);
						continue;
					}

					Document document = parseXml(xml);
					NodeList entries = document.getElementsByTagName("entry");

					for (int i = 0; i < entries.getLength(); i++) {
						Element entry = (Element) entries.item(i);
						String title = text(entry, "title");
						String updated = text(entry, "updated");
						String link = firstLinkHref(entry);
						if (title.isBlank() || link.isBlank()) {
							continue;
						}

						Instant publishedAt;
						try {
							publishedAt = Instant.parse(updated);
						} catch (Exception ignored) {
							publishedAt = Instant.now();
						}
						updates.add(new SourceUpdate(title.trim(), link.trim(), publishedAt, "GitHub Releases"));
					}
				} catch (Exception ex) {
					errors.add(rssUrl + " -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
				}
			}

			if (updates.isEmpty()) {
				if (errors.isEmpty()) {
					markFailure("No valid entries in RSS feed");
				} else {
					markFailure(String.join(" | ", errors));
				}
				return List.of();
			}

			updates = deduplicate(updates);
			updates.sort(Comparator.comparing(SourceUpdate::publishedAt).reversed());
			markSuccess();
			return updates.stream().limit(limit).toList();
		} catch (Exception ex) {
			markFailure(ex.getClass().getSimpleName() + ": " + ex.getMessage());
			return List.of();
		}
	}

	@Override
	public UpdatesSourceHealth getHealth() {
		return new UpdatesSourceHealth("github-releases", available, lastCheckedAt, lastSuccessAt, lastError);
	}

	private Document parseXml(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setExpandEntityReferences(false);
		return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
	}

	private String text(Element element, String tag) {
		NodeList nodes = element.getElementsByTagName(tag);
		if (nodes.getLength() == 0 || nodes.item(0) == null) {
			return "";
		}
		String value = nodes.item(0).getTextContent();
		return value == null ? "" : value;
	}

	private String firstLinkHref(Element entry) {
		NodeList links = entry.getElementsByTagName("link");
		for (int i = 0; i < links.getLength(); i++) {
			Element link = (Element) links.item(i);
			String rel = link.getAttribute("rel");
			String href = link.getAttribute("href");
			if (href != null && !href.isBlank() && (rel == null || rel.isBlank() || "alternate".equals(rel))) {
				return href;
			}
		}
		return "";
	}

	private List<String> parseRssUrls(String rssUrlsRaw, String fallbackRssUrl) {
		if (rssUrlsRaw == null || rssUrlsRaw.isBlank()) {
			return List.of(fallbackRssUrl);
		}
		List<String> urls = java.util.Arrays.stream(rssUrlsRaw.split(","))
			.map(String::trim)
			.filter(value -> !value.isBlank())
			.distinct()
			.toList();
		return urls.isEmpty() ? List.of(fallbackRssUrl) : urls;
	}

	private List<SourceUpdate> deduplicate(List<SourceUpdate> updates) {
		Set<String> seen = new HashSet<>();
		List<SourceUpdate> deduped = new ArrayList<>();
		for (SourceUpdate update : updates) {
			String key = update.url().trim().toLowerCase(Locale.ROOT);
			if (key.isBlank()) {
				continue;
			}
			if (seen.add(key)) {
				deduped.add(update);
			}
		}
		return deduped;
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
}

