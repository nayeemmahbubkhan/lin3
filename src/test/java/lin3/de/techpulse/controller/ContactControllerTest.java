package lin3.de.techpulse.controller;

import lin3.de.techpulse.config.SecurityConfig;
import lin3.de.techpulse.model.ContactRequest;
import lin3.de.techpulse.service.ContactService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
@Import(SecurityConfig.class)
class ContactControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ContactService contactService;

	@Test
	void submitReturnsCreatedForValidMessage() throws Exception {
		when(contactService.submit(ArgumentMatchers.any(ContactRequest.class), ArgumentMatchers.any(), ArgumentMatchers.any()))
			.thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

		mockMvc.perform(post("/api/contact")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "Nayeem",
					  "email": "nayeem@example.com",
					  "message": "Interested in collaboration",
					  "website": ""
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.reference").exists());
	}

	@Test
	void submitReturnsBadRequestWhenValidationFails() throws Exception {
		when(contactService.submit(ArgumentMatchers.any(ContactRequest.class), ArgumentMatchers.any(), ArgumentMatchers.any()))
			.thenThrow(new IllegalArgumentException("Please provide a valid email address."));

		mockMvc.perform(post("/api/contact")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "Nayeem",
					  "email": "bad-email",
					  "message": "Hello",
					  "website": ""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("error"));
	}
}


