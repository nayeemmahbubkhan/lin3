package lin3.de.techpulse.service;

import lin3.de.techpulse.model.ContactMessage;
import lin3.de.techpulse.model.ContactMessageEntity;
import lin3.de.techpulse.repository.ContactMessageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("postgres")
public class JpaContactMessageStore implements ContactMessageStore {

	private final ContactMessageRepository contactMessageRepository;

	public JpaContactMessageStore(ContactMessageRepository contactMessageRepository) {
		this.contactMessageRepository = contactMessageRepository;
	}

	@Override
	public ContactMessage save(ContactMessage message) {
		ContactMessageEntity entity = new ContactMessageEntity(
			message.id(),
			message.name(),
			message.email(),
			message.message(),
			message.ipAddress(),
			message.userAgent(),
			message.createdAt()
		);
		ContactMessageEntity saved = contactMessageRepository.save(entity);
		return new ContactMessage(
			saved.getId(),
			saved.getName(),
			saved.getEmail(),
			saved.getMessage(),
			saved.getIpAddress(),
			saved.getUserAgent(),
			saved.getCreatedAt()
		);
	}

	@Override
	public int count() {
		long total = contactMessageRepository.count();
		return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
	}

	@Override
	public List<ContactMessage> findAll() {
		return contactMessageRepository.findAll().stream()
			.map(entity -> new ContactMessage(
				entity.getId(),
				entity.getName(),
				entity.getEmail(),
				entity.getMessage(),
				entity.getIpAddress(),
				entity.getUserAgent(),
				entity.getCreatedAt()
			))
			.toList();
	}
}

