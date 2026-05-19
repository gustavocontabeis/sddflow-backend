package com.example.springia.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BirthdayMessageController.class)
class BirthdayMessageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private BirthdayMessageService birthdayMessageService;

	@Test
	void shouldGenerateBirthdayMessage() throws Exception {
		given(birthdayMessageService.generateMessage("Maria", 30))
			.willReturn("Feliz aniversario, Maria! Muitos anos de vida.");

		mockMvc.perform(post("/api/birthday-message")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "Maria",
					  "age": 30
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Feliz aniversario, Maria! Muitos anos de vida."));
	}

	@Test
	void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
		mockMvc.perform(post("/api/birthday-message")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "",
					  "age": 30
					}
					"""))
			.andExpect(status().isBadRequest());
	}
}

