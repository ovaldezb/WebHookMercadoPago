package mx.com.grupoarpa.webhook.entity;

import lombok.Data;

@Data
public class AdditionalInfoItems {

	private String category_id;
	private String description;
	private long id;
	private String picture_url;
	private String quantity;
	private String title;
	private String unit_price;
}
