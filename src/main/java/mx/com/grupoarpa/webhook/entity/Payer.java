package mx.com.grupoarpa.webhook.entity;

import lombok.Data;

@Data
public class Payer {

	private String first_name;
	private String last_name;
	private String email;
	private Identification identification;
	private Phone phone;
	private String type;
	private String entity_type;
	private String id;
}
