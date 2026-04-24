package lin3.de.techpulse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lin3.de.techpulse.model.ContactRequest;
import lin3.de.techpulse.service.ContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

	private final ContactService contactService;

	public ContactController(ContactService contactService) {
		this.contactService = contactService;
	}

	@PostMapping
	public ResponseEntity<Map<String, String>> submit(@RequestBody ContactRequest request, HttpServletRequest httpRequest) {
		try {
			UUID reference = contactService.submit(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
			return ResponseEntity.status(HttpStatus.CREATED)
				.body(Map.of("status", "ok", "reference", reference.toString()));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("status", "error", "message", ex.getMessage()));
		}
	}

	@GetMapping("/stats")
	public Map<String, Integer> stats() {
		return Map.of("messages", contactService.count());
	}
}

