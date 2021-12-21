package mx.com.grupoarpa.webhook.entity;

import lombok.Data;

@Data
public class FeeDetails {

	private double amount;
	private String fee_payer;
	private String type;
}
