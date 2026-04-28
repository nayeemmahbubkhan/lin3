package lin3.de.techpulse.controller;

import lin3.de.techpulse.model.TechUpdate;
import lin3.de.techpulse.model.UpdatesRefreshAllResponse;
import lin3.de.techpulse.model.UpdatesResponse;
import lin3.de.techpulse.service.UpdatesLimitParser;
import lin3.de.techpulse.service.UpdatesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
@AutoConfigureMockMvc(addFilters = false)
class UpdatesControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UpdatesService updatesService;

	@MockitoBean
	private UpdatesLimitParser updatesLimitParser;

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
				"Review changelog",
				"Could require dependency updates in the next sprint."
			)),
			false,
			null,
			false,
			null
		);

		when(updatesService.getLatest(eq(3))).thenReturn(response);

		mockMvc.perform(get("/api/updates").param("limit", "3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.source").value("hacker-news"))
			.andExpect(jsonPath("$.items[0].title").value("New release"))
			.andExpect(jsonPath("$.items[0].action").value("Review changelog"))
			.andExpect(jsonPath("$.items[0].footerInsight").value("Could require dependency updates in the next sprint."));
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
				"Patch production systems",
				"Production services may be exposed until patched."
			)),
			false,
			null,
			false,
			null
		);

		when(updatesService.refreshLatest(eq(2))).thenReturn(response);

		mockMvc.perform(post("/api/updates/refresh").param("limit", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.source").value("hacker-news"))
			.andExpect(jsonPath("$.items[0].title").value("Security patch"))
			.andExpect(jsonPath("$.items[0].action").value("Patch production systems"))
			.andExpect(jsonPath("$.items[0].footerInsight").value("Production services may be exposed until patched."));
	}

	@Test
	void refreshAllReturnsSummary() throws Exception {
		UpdatesRefreshAllResponse response = new UpdatesRefreshAllResponse(
			Instant.parse("2026-04-24T10:10:00Z"),
			List.of(5, 8),
			2,
			true
		);

		when(updatesLimitParser.parse(eq(null))).thenReturn(List.of());
		when(updatesLimitParser.parse(eq("5,8"))).thenReturn(List.of(5, 8));
		when(updatesService.refreshAllCommonLimits(eq(List.of(5, 8)))).thenReturn(response);

		mockMvc.perform(post("/api/updates/refresh-all"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.refreshedCount").value(2))
			.andExpect(jsonPath("$.limits[0]").value(5))
			.andExpect(jsonPath("$.sourceAvailable").value(true));
	}

	@Test
	void refreshAllWithCustomLimitsUsesRequestValue() throws Exception {
		UpdatesRefreshAllResponse response = new UpdatesRefreshAllResponse(
			Instant.parse("2026-04-24T10:10:00Z"),
			List.of(3, 10),
			2,
			true
		);

		when(updatesLimitParser.parse(eq("3,10"))).thenReturn(List.of(3, 10));
		when(updatesService.refreshAllCommonLimits(eq(List.of(3, 10)))).thenReturn(response);

		mockMvc.perform(post("/api/updates/refresh-all").param("limits", "3,10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.refreshedCount").value(2))
			.andExpect(jsonPath("$.limits[0]").value(3))
			.andExpect(jsonPath("$.limits[1]").value(10));
	}
}

