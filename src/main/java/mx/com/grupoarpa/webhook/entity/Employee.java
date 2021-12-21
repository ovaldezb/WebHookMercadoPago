package mx.com.grupoarpa.webhook.entity;

import lombok.Data;

@Data
public class Employee {

	private long id;
	private String imageUrl;
	private String firstName;
	private String lastName;
	private String email;
	private String contactNumber;
	private int age;
	private String dob;
	private double salary;
	private String address;
}