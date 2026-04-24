package lin3.de.techpulse.service;

import lin3.de.techpulse.model.ContactMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InMemoryContactMessageStore implements ContactMessageStore {

	private final CopyOnWriteArrayList<ContactMessage> messages = new CopyOnWriteArrayList<>();

	@Override
	public ContactMessage save(ContactMessage message) {
		messages.add(message);
		return message;
	}

	@Override
	public int count() {
		return messages.size();
	}

	@Override
	public List<ContactMessage> findAll() {
		return List.copyOf(messages);
	}
}

