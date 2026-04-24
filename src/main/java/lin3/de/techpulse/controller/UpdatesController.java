package lin3.de.techpulse.controller;

import lin3.de.techpulse.model.UpdatesResponse;
import lin3.de.techpulse.service.UpdatesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/updates")
public class UpdatesController {

	private final UpdatesService updatesService;

	public UpdatesController(UpdatesService updatesService) {
		this.updatesService = updatesService;
	}

	@GetMapping
	public UpdatesResponse latest(@RequestParam(required = false) Integer limit) {
		return updatesService.getLatest(limit);
	}

	@PostMapping("/refresh")
	public UpdatesResponse refresh(@RequestParam(required = false) Integer limit) {
		return updatesService.refreshLatest(limit);
	}
}

