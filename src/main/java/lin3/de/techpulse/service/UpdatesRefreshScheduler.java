package lin3.de.techpulse.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class UpdatesRefreshScheduler {

	private final UpdatesService updatesService;
	private final boolean enabled;
	private final String commonLimits;

	public UpdatesRefreshScheduler(
		UpdatesService updatesService,
		@Value("${techpulse.updates.auto-refresh.enabled:false}") boolean enabled,
		@Value("${techpulse.updates.common-limits:5,8}") String commonLimits
	) {
		this.updatesService = updatesService;
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
		updatesService.refreshAllCommonLimits(parseLimits(commonLimits));
	}

	private List<Integer> parseLimits(String raw) {
		return Arrays.stream(raw.split(","))
			.map(String::trim)
			.filter(value -> !value.isBlank())
			.map(value -> {
				try {
					return Integer.parseInt(value);
				} catch (NumberFormatException ex) {
					return null;
				}
			})
			.filter(Objects::nonNull)
			.toList();
	}
}

