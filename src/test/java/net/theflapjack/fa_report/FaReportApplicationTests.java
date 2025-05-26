package net.theflapjack.fa_report;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) //Production I wouldnt mock it like this
class FaReportApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void testMissingPortfolioIdReturns400() throws Exception {
		mockMvc.perform(get("/report"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testInvalidDateFormatReturns400() throws Exception {
		mockMvc.perform(get("/report")
						.param("portfolioId", "3")
						.param("startDate", "not-a-date"))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("Dates must be in ISO format")));
	}

	@Test
	void testInvalidCurrencyReturns400() throws Exception {
		mockMvc.perform(get("/report")
						.param("portfolioId", "3")
						.param("targetCurrency", "XYZ"))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("Invalid targetCurrency")));
	}

	@Test
	void testEndDateBeforeStartDateReturns400() throws Exception {
		mockMvc.perform(get("/report")
						.param("portfolioId", "3")
						.param("startDate", "2025-06-01")
						.param("endDate", "2025-05-01"))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("endDate must not be before startDate")));
	}

}
