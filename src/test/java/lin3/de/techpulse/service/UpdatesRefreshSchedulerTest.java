package lin3.de.techpulse.service;

import lin3.de.techpulse.model.UpdatesRefreshAllResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdatesRefreshSchedulerTest {

	@Test
	void refreshCommonLimitsCallsServiceWhenEnabled() {
		UpdatesService updatesService = mock(UpdatesService.class);
		when(updatesService.refreshAllCommonLimits(eq(List.of(5, 8))))
			.thenReturn(new UpdatesRefreshAllResponse(Instant.now(), List.of(5, 8), 2, true));

		UpdatesRefreshScheduler scheduler = new UpdatesRefreshScheduler(updatesService, new UpdatesLimitParser(), true, "5,8");
		scheduler.refreshCommonLimits();

		verify(updatesService).refreshAllCommonLimits(eq(List.of(5, 8)));
	}

	@Test
	void refreshCommonLimitsSkipsWhenDisabled() {
		UpdatesService updatesService = mock(UpdatesService.class);
		UpdatesRefreshScheduler scheduler = new UpdatesRefreshScheduler(updatesService, new UpdatesLimitParser(), false, "5,8");

		scheduler.refreshCommonLimits();

		verify(updatesService, never()).refreshAllCommonLimits(eq(List.of(5, 8)));
	}
}

