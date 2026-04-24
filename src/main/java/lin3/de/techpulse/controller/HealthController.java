package lin3.de.techpulse.controller;

import lin3.de.techpulse.model.UpdatesHealthResponse;
import lin3.de.techpulse.service.UpdatesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

	private final UpdatesService updatesService;

	public HealthController(UpdatesService updatesService) {
		this.updatesService = updatesService;
	}

	@GetMapping("/updates")
	public UpdatesHealthResponse updatesHealth() {
		return updatesService.getHealth();
	}
}

