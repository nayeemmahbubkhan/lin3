package lin3.de.techpulse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_messages")
public class ContactMessageEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(nullable = false, length = 2000)
	private String message;

	@Column(nullable = false, length = 128)
	private String ipAddress;

	@Column(nullable = false, length = 512)
	private String userAgent;

	@Column(nullable = false)
	private Instant createdAt;

	public ContactMessageEntity() {
	}

	public ContactMessageEntity(UUID id, String name, String email, String message, String ipAddress, String userAgent, Instant createdAt) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.message = message;
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getMessage() {
		return message;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

