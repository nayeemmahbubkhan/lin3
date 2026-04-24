package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;

import java.util.List;

public interface TechUpdatesSource {

	List<SourceUpdate> fetchLatest(int limit);
}

