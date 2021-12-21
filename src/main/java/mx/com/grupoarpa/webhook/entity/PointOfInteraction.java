package mx.com.grupoarpa.webhook.entity;

import lombok.Data;

@Data
public class PointOfInteraction {

	private BusinessInfo business_info;
	private String type;
}
