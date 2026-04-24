package lin3.de.techpulse.controller;

import lin3.de.techpulse.config.SecurityConfig;
import lin3.de.techpulse.model.TechUpdate;
import lin3.de.techpulse.model.UpdatesResponse;
import lin3.de.techpulse.service.UpdatesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UpdatesController.class)
@Import(SecurityConfig.class)
class UpdatesControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UpdatesService updatesService;

	@Test
	void latestReturnsUpdatesPayload() throws Exception {
		UpdatesResponse response = new UpdatesResponse(
			Instant.parse("2026-04-24T10:00:00Z"),
			"hacker-news",
			List.of(new TechUpdate(
				"New release",
				"https://example.com",
				"Example",
				Instant.parse("2026-04-24T09:00:00Z"),
				"Short summary",
				"Review changelog"
			)),
			false,
			null
		);

		when(updatesService.getLatest(eq(3))).thenReturn(response);

		mockMvc.perform(get("/api/updates").param("limit", "3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.source").value("hacker-news"))
			.andExpect(jsonPath("$.items[0].title").value("New release"))
			.andExpect(jsonPath("$.items[0].action").value("Review changelog"));
	}

	@Test
	void refreshReturnsUpdatesPayload() throws Exception {
		UpdatesResponse response = new UpdatesResponse(
			Instant.parse("2026-04-24T10:00:00Z"),
			"hacker-news",
			List.of(new TechUpdate(
				"Security patch",
				"https://example.com/security",
				"Example",
				Instant.parse("2026-04-24T09:30:00Z"),
				"Short summary",
				"Patch production systems"
			)),
			false,
			null
		);

		when(updatesService.refreshLatest(eq(2))).thenReturn(response);

		mockMvc.perform(post("/api/updates/refresh").param("limit", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.source").value("hacker-news"))
			.andExpect(jsonPath("$.items[0].title").value("Security patch"))
			.andExpect(jsonPath("$.items[0].action").value("Patch production systems"));
	}
}

