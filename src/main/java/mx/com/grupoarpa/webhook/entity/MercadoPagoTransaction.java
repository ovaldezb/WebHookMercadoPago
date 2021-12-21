package mx.com.grupoarpa.webhook.entity;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document
public class MercadoPagoTransaction {

	@Id
	private String id;
	private AdditionalInfo additional_info;
	private String authorization_code;
	private boolean binary_mode;
	private String brand_id;
	private String call_for_authorize_id;
	private boolean captured;
	private Card card;
	private String[] charges_details;
	private long collector_id;
	private String corporation_id;
	private String counter_currency;
	private double coupon_amount;
	private String currency_id;
	private String date_approved;
	private String date_created;
	private String date_last_updated;
	private String date_of_expiration;
	private String deduction_schema;
	private String description;
	private String differential_pricing_id;
	private String external_reference;
	private FeeDetails[] fee_details;
	private String idTransaction;
	private int installments;
	private String integrator_id;
	private String issuer_id;
	private boolean live_mode;
	private String marketplace_owner;
	private String merchant_account_id;
	private String merchant_number;
	//private Metadata metada;
	private String money_release_date;
	private String money_release_schema;
	private String notification_url;
	private String operation_type;
	private Order order;
	private Payer payer;
	private String payment_method_id;
	private String payment_type_id;
	private String platform_id;
	private PointOfInteraction point_of_interaction;
	private String pos_id;
	private String processing_mode;
	private double[] refunds;
	private double shipping_amount;
	private String sponsor_id;
	private String statement_descriptor;
	private String status;
	private String status_detail;
	private String store_id;
	private double taxes_amount;
	private double transaction_amount;
	private double transaction_amount_refunded;
	private TransactionDetails transaction_details;
	private String encodedId;
	private boolean checked;
	private Date date_checked;
	
}
