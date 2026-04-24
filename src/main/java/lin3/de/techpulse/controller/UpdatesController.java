package lin3.de.techpulse.controller;

import lin3.de.techpulse.model.UpdatesResponse;
import lin3.de.techpulse.model.UpdatesRefreshAllResponse;
import lin3.de.techpulse.service.UpdatesLimitParser;
import lin3.de.techpulse.service.UpdatesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/updates")
public class UpdatesController {

	private final UpdatesService updatesService;
	private final UpdatesLimitParser updatesLimitParser;
	private final String commonLimits;

	public UpdatesController(
		UpdatesService updatesService,
		UpdatesLimitParser updatesLimitParser,
		@Value("${techpulse.updates.common-limits:5,8}") String commonLimits
	) {
		this.updatesService = updatesService;
		this.updatesLimitParser = updatesLimitParser;
		this.commonLimits = commonLimits;
	}

	@GetMapping
	public UpdatesResponse latest(@RequestParam(required = false) Integer limit) {
		return updatesService.getLatest(limit);
	}

	@PostMapping("/refresh")
	public UpdatesResponse refresh(@RequestParam(required = false) Integer limit) {
		return updatesService.refreshLatest(limit);
	}

	@PostMapping("/refresh-all")
	public UpdatesRefreshAllResponse refreshAll(@RequestParam(required = false) String limits) {
		List<Integer> parsed = updatesLimitParser.parse(limits);
		if (parsed.isEmpty()) {
			parsed = updatesLimitParser.parse(commonLimits);
		}
		return updatesService.refreshAllCommonLimits(parsed);
	}
}

