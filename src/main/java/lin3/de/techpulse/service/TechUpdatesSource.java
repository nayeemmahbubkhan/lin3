package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;
import lin3.de.techpulse.model.UpdatesSourceHealth;

import java.util.List;

public interface TechUpdatesSource {

	List<SourceUpdate> fetchLatest(int limit);

	default UpdatesSourceHealth getHealth() {
		return new UpdatesSourceHealth("unknown", false, null, null, "No source health implementation");
	}
}

