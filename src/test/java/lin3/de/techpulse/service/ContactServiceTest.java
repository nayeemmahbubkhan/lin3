package lin3.de.techpulse.service;

import lin3.de.techpulse.model.ContactRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContactServiceTest {

	private final ContactService contactService = new ContactService(new InMemoryContactMessageStore());

	@Test
	void submitStoresMessageWhenInputIsValid() {
		var reference = contactService.submit(
			new ContactRequest("Nayeem", "nayeem@example.com", "Hello from lin3.de", ""),
			"127.0.0.1",
			"JUnit"
		);

		assertNotNull(reference);
		assertEquals(1, contactService.count());
	}

	@Test
	void submitRejectsSpamHoneypot() {
		assertThrows(IllegalArgumentException.class, () ->
			contactService.submit(
				new ContactRequest("Nayeem", "nayeem@example.com", "Hello", "http://spam.example"),
				"127.0.0.1",
				"JUnit"
			)
		);
	}
}

