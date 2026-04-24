package lin3.de.techpulse.controller;

import lin3.de.techpulse.model.UpdatesHealthResponse;
import lin3.de.techpulse.service.UpdatesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

	private final UpdatesService updatesService;
	private final boolean localLlmEnabled;

	public HealthController(
		UpdatesService updatesService,
		@Value("${techpulse.llm.enabled:false}") boolean localLlmEnabled
	) {
		this.updatesService = updatesService;
		this.localLlmEnabled = localLlmEnabled;
	}

	@GetMapping("/updates")
	public UpdatesHealthResponse updatesHealth() {
		return updatesService.getHealth();
	}

	@GetMapping("/meta")
	public Map<String, Boolean> metadata() {
		return Map.of("localLlmEnabled", localLlmEnabled);
	}
}

