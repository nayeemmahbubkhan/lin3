package lin3.de.techpulse.controller;

import lin3.de.techpulse.config.SecurityConfig;
import lin3.de.techpulse.model.UpdatesCacheHealth;
import lin3.de.techpulse.model.UpdatesHealthResponse;
import lin3.de.techpulse.model.UpdatesSourceHealth;
import lin3.de.techpulse.service.UpdatesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UpdatesService updatesService;

	@Test
	void updatesHealthReturnsSourceAndCacheStats() throws Exception {
		UpdatesHealthResponse health = new UpdatesHealthResponse(
			Instant.parse("2026-04-24T10:10:00Z"),
			new UpdatesSourceHealth("hacker-news", true, Instant.parse("2026-04-24T10:09:00Z"), Instant.parse("2026-04-24T10:08:30Z"), null),
			new UpdatesCacheHealth(true, 300, 2, 2)
		);
		when(updatesService.getHealth()).thenReturn(health);

		mockMvc.perform(get("/api/health/updates"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.source.name").value("hacker-news"))
			.andExpect(jsonPath("$.source.available").value(true))
			.andExpect(jsonPath("$.cache.enabled").value(true))
			.andExpect(jsonPath("$.cache.entryCount").value(2));
	}
}

