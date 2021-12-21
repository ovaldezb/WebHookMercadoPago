package mx.com.grupoarpa.webhook.entity;

import lombok.Data;

@Data
public class TransactionDetails {

	private String acquirer_reference;
	private String external_resource_url;
	private String financial_institution;
	private double installment_amount;
	private double net_received_amount;
	private double overpaid_amount;
	private int payable_deferral_period;
	private String payment_method_reference_id;
	private double total_paid_amount;
}
