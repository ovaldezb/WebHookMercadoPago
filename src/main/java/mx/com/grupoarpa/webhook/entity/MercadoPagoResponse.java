package mx.com.grupoarpa.webhook.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "responses")
public class MercadoPagoResponse {

	@Id
	private String id;
	private String idResponse;
	private boolean live_mode;
	private String date_created;
	private long application_id;
	private int version;
	private String api_version;
	private String action;
	//private DataResponse data;
	private String data;
	private String type;
	private String user_id;
	private String status;
}