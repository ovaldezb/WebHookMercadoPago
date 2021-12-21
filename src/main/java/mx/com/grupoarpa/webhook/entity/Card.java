package mx.com.grupoarpa.webhook.entity;

import org.springframework.data.annotation.Transient;

import lombok.Data;

@Data
public class Card {

	private CardHolder cardholder;
	private String date_created;
	private String date_last_updated;
	@Transient
	private int expiration_month;
	@Transient
	private int expiration_year;
	private String first_six_digits;
	private String id;
	private String last_four_digits;
}
