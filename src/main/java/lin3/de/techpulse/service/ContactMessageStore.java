package lin3.de.techpulse.service;

import lin3.de.techpulse.model.ContactMessage;

import java.util.List;

public interface ContactMessageStore {

	ContactMessage save(ContactMessage message);

	int count();

	List<ContactMessage> findAll();
}

