package lin3.de.techpulse.service;

import lin3.de.techpulse.model.SourceUpdate;

public interface UpdatesSummarizer {

	String summarize(SourceUpdate update);

	String nextAction(SourceUpdate update);
}

