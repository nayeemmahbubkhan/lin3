package lin3.de.techpulse.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class UpdatesLimitParser {

	public List<Integer> parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
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

