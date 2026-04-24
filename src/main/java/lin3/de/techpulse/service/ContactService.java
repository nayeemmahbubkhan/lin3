package lin3.de.techpulse.service;

import lin3.de.techpulse.model.ContactMessage;
import lin3.de.techpulse.model.ContactRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ContactService {

	private static final int MAX_NAME_LENGTH = 100;
	private static final int MAX_EMAIL_LENGTH = 255;
	private static final int MAX_MESSAGE_LENGTH = 2000;

	private final ContactMessageStore contactMessageStore;

	public ContactService(ContactMessageStore contactMessageStore) {
		this.contactMessageStore = contactMessageStore;
	}

	public UUID submit(ContactRequest request, String ipAddress, String userAgent) {
		String name = normalize(request.name());
		String email = normalize(request.email());
		String message = normalize(request.message());
		String website = normalize(request.website());

		if (!website.isEmpty()) {
			throw new IllegalArgumentException("Spam check failed.");
		}
		if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("Name is required and must be 1-100 characters.");
		}
		if (!isValidEmail(email) || email.length() > MAX_EMAIL_LENGTH) {
			throw new IllegalArgumentException("Please provide a valid email address.");
		}
		if (message.isEmpty() || message.length() > MAX_MESSAGE_LENGTH) {
			throw new IllegalArgumentException("Message is required and must be 1-2000 characters.");
		}

		ContactMessage contactMessage = new ContactMessage(
			UUID.randomUUID(),
			name,
			email,
			message,
			normalize(ipAddress),
			normalize(userAgent),
			Instant.now()
		);
		return contactMessageStore.save(contactMessage).id();
	}

	public int count() {
		return contactMessageStore.count();
	}

	public List<ContactMessage> findAll() {
		return contactMessageStore.findAll();
	}

	private String normalize(String input) {
		return input == null ? "" : input.trim();
	}

	private boolean isValidEmail(String email) {
		return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	}
}

