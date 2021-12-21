package mx.com.grupoarpa.webhook.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AdditionalInfo {

	private String authentication_code;
	private double available_balance;
	private String ip_address;
	private AdditionalInfoItems[] items;
	private String nsu_processadora;
	
}
