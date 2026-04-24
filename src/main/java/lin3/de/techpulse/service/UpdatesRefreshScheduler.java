package lin3.de.techpulse.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UpdatesRefreshScheduler {

	private final UpdatesService updatesService;
	private final UpdatesLimitParser updatesLimitParser;
	private final boolean enabled;
	private final String commonLimits;

	public UpdatesRefreshScheduler(
		UpdatesService updatesService,
		UpdatesLimitParser updatesLimitParser,
		@Value("${techpulse.updates.auto-refresh.enabled:false}") boolean enabled,
		@Value("${techpulse.updates.common-limits:5,8}") String commonLimits
	) {
		this.updatesService = updatesService;
		this.updatesLimitParser = updatesLimitParser;
		this.enabled = enabled;
		this.commonLimits = commonLimits;
	}

	@Scheduled(
		initialDelayString = "${techpulse.updates.auto-refresh-interval-ms:300000}",
		fixedDelayString = "${techpulse.updates.auto-refresh-interval-ms:300000}"
	)
	public void refreshCommonLimits() {
		if (!enabled) {
			return;
		}
		List<Integer> limits = updatesLimitParser.parse(commonLimits);
		updatesService.refreshAllCommonLimits(limits);
	}
}

