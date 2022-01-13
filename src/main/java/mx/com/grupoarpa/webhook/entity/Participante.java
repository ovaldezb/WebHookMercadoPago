package mx.com.grupoarpa.webhook.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document("users")
public class Participante {

	@Id
	private String id;
	private String nombres;
	private String apellidos;
	private String correo;
	private String edad;
	private String fechaNacimiento;
	private String genero;
	private String pais;
	private String telefono;
	private String contactoEmerg;
	private String nombreEquipo;
	private String tipoSangre;
	private String talla;
	private String marcaBici;
	private String modelo;
	private String ano;
	private String rodada;
	private String alergias;
	private String tickets;
	private String categoryAvalanche;
	private String categoryTrail;
	private String terms;
	private String privacy;
	private String responsal;
	private String externalReference;
}
