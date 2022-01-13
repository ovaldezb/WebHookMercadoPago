package mx.com.grupoarpa.webhook.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import javax.mail.SendFailedException;
import javax.mail.internet.AddressException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.google.zxing.WriterException;

import mx.com.grupoarpa.webhook.entity.MercadoPagoResponse;
import mx.com.grupoarpa.webhook.entity.MercadoPagoTransaction;
import mx.com.grupoarpa.webhook.entity.Participante;
import mx.com.grupoarpa.webhook.repository.MercadoPagoRepository;
import mx.com.grupoarpa.webhook.repository.MercadoPagoTransaccionRepository;
import mx.com.grupoarpa.webhook.service.EmailConfig;
import mx.com.grupoarpa.webhook.service.QRCodeGenerator;
import mx.com.grupoarpa.webhook.utils.MercadoPagoTransactionStatus;

@RestController
@RequestMapping("/api/transaction")
@CrossOrigin(origins = "https://ready2solve.club:8086")
public class WebhookController {

	/* Dev */
	// private static final String ACCESS_TOKEN =
	// "TEST-6478429900085566-113022-088e337cb14fed28c6c30a47f9a16330-153057812";

	/* PRD */
	private static final String ACCESS_TOKEN = "APP_USR-6478429900085566-113022-63306cae1bda1b35da01505c058e6c25-153057812";
	private String url = "https://api.mercadopago.com/v1/payments/%s?access_token=" + ACCESS_TOKEN;
	private String urlParticipante = "https://ready2solve.club:8185/api/participante/%s";
	private String urlParticipanteById = "https://ready2solve.club:8185/api/participante/id/%s";

	private String PATH_QR = "/var/www/html/qr/";
	//private String PATH_QR = "/Users/macbookpro/Documents/Boletera/qr/";
	private String FILE_EXT = ".png";

	@Autowired
	MercadoPagoRepository mercadoRepo;

	@Autowired
	MercadoPagoTransaccionRepository transactionRepo;

	@Autowired
	RestTemplate restTemplate;
	
	@GetMapping("/confirm/{idData}/{email}")
	public ResponseEntity<?> getReponseByData(@PathVariable final String idData, @PathVariable final String email) {
		Optional<MercadoPagoResponse> mpResponse = mercadoRepo.findByData("{'id': '" + idData + "'}");
		MercadoPagoTransaction mpTransactionBody = null;
		String emailSend = null;
		try {
				if (mpResponse.isPresent()) {
					MercadoPagoResponse mpRespFound = mpResponse.get();
					ResponseEntity<MercadoPagoTransaction> mpTransaction = restTemplate.exchange(String.format(url, idData),
						HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), MercadoPagoTransaction.class);
					mpTransactionBody = mpTransaction.getBody();
					if (mpTransactionBody.getStatus().equals(MercadoPagoTransactionStatus.approved.toString())
						&& mpTransaction.getStatusCode() == HttpStatus.OK) {
					
						ResponseEntity<Participante> participante = restTemplate.exchange(
							String.format(urlParticipante, mpTransactionBody.getPayer().getEmail()), HttpMethod.GET,
							new HttpEntity<>(new HttpHeaders()), Participante.class);
					
						String md5Hex = DigestUtils.md5DigestAsHex(mpTransactionBody.getId().getBytes());
						String encoded = md5Hex + Base64.getEncoder().encodeToString(String.valueOf(Calendar.getInstance().getTime().getTime()).getBytes());
						mpTransactionBody.setIdTransaction(mpTransaction.getBody().getId());
						QRCodeGenerator.generateQRCodeImage(encoded, 250, 250,PATH_QR + mpTransactionBody.getIdTransaction()+"q" + FILE_EXT);
					
						mpTransactionBody.setEncodedId(encoded);					
						mpTransactionBody.setId(null);
						//este se usa para validar el ticket
						mpTransactionBody.setChecked(false);
						mpRespFound.setStatus(mpTransactionBody.getStatus());
					
						double costoTicket = mpTransactionBody.getDescription().contains("MTB")  || mpTransactionBody.getDescription().contains("AVALANCHE") || mpTransactionBody.getDescription().contains("Inscripcion Carrera")  ? 600.00 : 400.00;
						double costoTotal = mpTransactionBody.getTransaction_details().getTotal_paid_amount();
						double comision = 0.0;//mpTransactionBody.getDescription().contains("Avalance MTB") || mpTransactionBody.getDescription().contains("AVALANCHE") || mpTransactionBody.getDescription().contains("Inscripcion Carrera") ? 96.00 : 64.00;
						double porcentaje = mpTransactionBody.getDescription().contains("10") ? 0.10 : mpTransactionBody.getDescription().contains("20") ? 0.20 : 0.00;
						double descuento = costoTicket * porcentaje;
						comision = Math.abs(costoTicket - descuento - costoTotal);
						String evento = mpTransactionBody.getDescription().contains("Avalance MTB") || mpTransactionBody.getDescription().contains("Inscripcion Carrera") ? "OPEN MTB AVALANCHE" : mpTransactionBody.getDescription();
						emailSend = email.equals("null") ? mpTransactionBody.getPayer().getEmail() : email;
						EmailConfig.sendmail(emailSend,
							mpTransactionBody.getIdTransaction()+"q" + FILE_EXT, 
							participante.getBody().getNombres(),
							participante.getBody().getApellidos(), 
							evento.contains("%") ? evento.substring(0,evento.indexOf('%')-3) : evento,
							"$"+(costoTicket)+" MXN",
							"$"+(comision)+" MXN",
							"$"+(costoTotal)+" MXN",
							"$"+(descuento)+" MXN",
							participante.getBody().getCategoryAvalanche() != null ? participante.getBody().getCategoryAvalanche() : "",
									mpTransactionBody.getDescription().contains("MTB") || mpTransactionBody.getDescription().contains("AVALANCHE") || mpTransactionBody.getDescription().contains("Inscripcion Carrera"));
						transactionRepo.save(mpTransactionBody);
						mercadoRepo.save(mpRespFound);
					}
				}
			
		} catch (IOException | WriterException |  AddressException | SendFailedException  e) {
			return ResponseEntity.badRequest().body(e.toString());

		} catch (HttpClientErrorException badReq) {
			badReq.printStackTrace();
			return ResponseEntity.badRequest().body("Venta no disponible");
		}
		return ResponseEntity.ok().body("La transaccion fue: " + mpTransactionBody.getStatus()+" se envio a "+emailSend);
	}
	
	
	/*
	 * Este metodo va a relacionar la info de MercadoPago con Participantes por el ID de ExternalReference
	 * **/
	@GetMapping("/confirm/id/{idData}")
	public ResponseEntity<?> getReponseByDataById(@PathVariable final String idData) {
		Optional<MercadoPagoResponse> mpResponse = mercadoRepo.findByData("{'id': '" + idData + "'}");
		MercadoPagoTransaction mpTransactionBody = null;
		try {
				if (mpResponse.isPresent()) {
					MercadoPagoResponse mpRespFound = mpResponse.get();
					ResponseEntity<MercadoPagoTransaction> mpTransaction = restTemplate.exchange(String.format(url, idData),
						HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), MercadoPagoTransaction.class);
					mpTransactionBody = mpTransaction.getBody();
					if (mpTransactionBody.getStatus().equals(MercadoPagoTransactionStatus.approved.toString())
						&& mpTransaction.getStatusCode() == HttpStatus.OK) {
					
						ResponseEntity<Participante> participante = restTemplate.exchange(
							String.format(urlParticipanteById, mpTransactionBody.getExternal_reference()), HttpMethod.GET,
							new HttpEntity<>(new HttpHeaders()), Participante.class);
					
						String md5Hex = DigestUtils.md5DigestAsHex(mpTransactionBody.getId().getBytes());
						String encoded = md5Hex + Base64.getEncoder().encodeToString(String.valueOf(Calendar.getInstance().getTime().getTime()).getBytes());
						mpTransactionBody.setIdTransaction(mpTransaction.getBody().getId());
						QRCodeGenerator.generateQRCodeImage(encoded, 250, 250,PATH_QR + mpTransactionBody.getIdTransaction()+"q" + FILE_EXT);
					
						mpTransactionBody.setEncodedId(encoded);					
						mpTransactionBody.setId(null);
						//este se usa para validar el ticket
						mpTransactionBody.setChecked(false);
						mpRespFound.setStatus(mpTransactionBody.getStatus());
					
						double costoTicket = mpTransactionBody.getDescription().contains("Producto")  || mpTransactionBody.getDescription().contains("undefined")  || mpTransactionBody.getDescription().contains("MTB")  || mpTransactionBody.getDescription().contains("AVALANCHE") || mpTransactionBody.getDescription().contains("Inscripcion Carrera") ? 600.00 : 400.00;
						double costoTotal = mpTransactionBody.getTransaction_details().getTotal_paid_amount();
						double comision = 0.0;//mpTransactionBody.getDescription().contains("Avalance MTB") || mpTransactionBody.getDescription().contains("AVALANCHE") || mpTransactionBody.getDescription().contains("Inscripcion Carrera") ? 96.00 : 64.00;
						double porcentaje = mpTransactionBody.getDescription().contains("10") ? 0.10 : mpTransactionBody.getDescription().contains("20") ? 0.20 : 0.00;
						double descuento = costoTicket * porcentaje;
						comision = Math.abs(costoTicket - descuento - costoTotal);
						String evento = mpTransactionBody.getDescription().contains("Producto")  || mpTransactionBody.getDescription().contains("undefined") || mpTransactionBody.getDescription().contains("Avalance MTB") || mpTransactionBody.getDescription().contains("Inscripcion Carrera") ? "OPEN MTB AVALANCHE" : mpTransactionBody.getDescription();

						EmailConfig.sendmail(participante.getBody().getCorreo(),
							mpTransactionBody.getIdTransaction()+"q" + FILE_EXT, 
							participante.getBody().getNombres(),
							participante.getBody().getApellidos(), 
							evento.contains("%") ? evento.substring(0,evento.indexOf('%')-3) : evento,
							"$"+(costoTicket)+" MXN",
							"$"+(comision)+" MXN",
							"$"+(costoTotal)+" MXN",
							"$"+(descuento)+" MXN",
							participante.getBody().getCategoryAvalanche() != null ? participante.getBody().getCategoryAvalanche() : "",
									mpTransactionBody.getDescription().contains("Producto")  || mpTransactionBody.getDescription().contains("undefined") || mpTransactionBody.getDescription().contains("MTB") || mpTransactionBody.getDescription().contains("AVALANCHE") || mpTransactionBody.getDescription().contains("Inscripcion Carrera"));
						transactionRepo.save(mpTransactionBody);
						mercadoRepo.save(mpRespFound);
					}
				}
			
		} catch (IOException | WriterException |  AddressException | SendFailedException  e) {
			return ResponseEntity.badRequest().body(e.toString());

		} catch (HttpClientErrorException badReq) {
			badReq.printStackTrace();
			return ResponseEntity.badRequest().body("Venta no disponible");
		}
		return ResponseEntity.ok().body("La transaccion fue: " + mpTransactionBody.getStatus());
	}
	
	/*
	 * Este metodo va a relacionar la info de MercadoPago con Participantes por el ID de ExternalReference
	 * **/
	@GetMapping("/externalreference/{extref}")
	public ResponseEntity<?> getReponseByExternalReference(@PathVariable final String extref) {
		try {
			ResponseEntity<Participante> participante = restTemplate.exchange(
				String.format(urlParticipanteById, extref), HttpMethod.GET,
				new HttpEntity<>(new HttpHeaders()), Participante.class);
			String dataParticipantQR = "Cortesia "+ extref +"-"+ participante.getBody().getNombres()+" "+participante.getBody().getApellidos()+"-"+
				(participante.getBody().getCategoryAvalanche() != null ? participante.getBody().getCategoryAvalanche() : participante.getBody().getCategoryTrail() )+"-"+participante.getBody().getFechaNacimiento();
		
			QRCodeGenerator.generateQRCodeImage(dataParticipantQR, 250, 250,PATH_QR + extref+"q" + FILE_EXT);
		
			double costoTicket = 0.00;
			double costoTotal = 0.00;
			double comision = 0.0;
			double descuento = 0.00;
			String evento = participante.getBody().getCategoryAvalanche() != null ? participante.getBody().getCategoryAvalanche() : "Trail Running";//participante.getBody().getCategoryTrail();
			EmailConfig.sendmail(participante.getBody().getCorreo(),
				extref+"q" + FILE_EXT, 
				participante.getBody().getNombres(),
				participante.getBody().getApellidos(), 
				evento.contains("%") ? evento.substring(0,evento.indexOf('%')-3) : evento,
				"$"+(costoTicket)+" MXN",
				"$"+(comision)+" MXN",
				"$"+(costoTotal)+" MXN",
				"$"+(descuento)+" MXN",
				participante.getBody().getCategoryAvalanche() != null ? participante.getBody().getCategoryAvalanche() : participante.getBody().getCategoryTrail(),
						evento.contains("MTB") || evento.contains("AVALANCHE"));
			
			
		} catch (IOException | WriterException |  AddressException | SendFailedException  e) {
			return ResponseEntity.badRequest().body(e.toString());

		} catch (HttpClientErrorException badReq) {
			badReq.printStackTrace();
			return ResponseEntity.badRequest().body("Venta no disponible");
		}
		return ResponseEntity.ok().body("Se envio el correo: " + extref );
	}

	@GetMapping("/{idTransaction}")
	public ResponseEntity<?> validateTicket(@PathVariable final String idTransaction) {
		//LocalDateTime raceDateTime = LocalDateTime.parse("2022-01-08T06:00:00.000");
		//LocalDateTime currentDateTime = LocalDateTime.now();
		//boolean isCurrentBeforeRace = currentDateTime.isBefore(raceDateTime);
		/*if (isCurrentBeforeRace) {
			return ResponseEntity.ok().body("Aun no se abre el horario de validacion de tickets");
		}*/
		Optional<MercadoPagoTransaction> mpTransaction = transactionRepo.findByEncodedId(idTransaction);
		if (mpTransaction.isPresent() && !mpTransaction.get().isChecked()) {
			MercadoPagoTransaction mpTransactionUpdate = mpTransaction.get();
			mpTransactionUpdate.setChecked(true);
			mpTransactionUpdate.setDate_checked(new Date());
			return ResponseEntity.ok(transactionRepo.save(mpTransactionUpdate));
		} else {
			return ResponseEntity.badRequest()
					.body("La ficha de inscripcion ya fue validad el " + mpTransaction.get().getDate_checked());
		}
	}
	
	@GetMapping("/generaqr/{idTransaction}")
	public ResponseEntity<?> generaQR(@PathVariable final String idTransaction) {
		Optional<MercadoPagoTransaction> transaction = transactionRepo.findByIdTransaction(idTransaction);
		if(transaction.isPresent()) {
			try {
				QRCodeGenerator.generateQRCodeImage(transaction.get().getEncodedId(), 250, 250,PATH_QR + idTransaction+"q" + FILE_EXT);
			} catch (WriterException | IOException e) {
				// TODO Auto-generated catch block
				return ResponseEntity.badRequest().build();
			}
			return ResponseEntity.ok(transaction.get());	
		}else {
			return ResponseEntity.notFound().build();
		}
	}
	
	@GetMapping("/entregakit/{encodedId}")
	public ResponseEntity<?> entregaKits(@PathVariable final String encodedId){
			Optional<MercadoPagoTransaction> mpTransaction = transactionRepo.findByEncodedId(encodedId);
		
			if(mpTransaction.isPresent()) {
				MercadoPagoTransaction mpTransactionBody = mpTransaction.get();
				ResponseEntity<Participante> participante = restTemplate.exchange(
						String.format(urlParticipanteById, mpTransactionBody.getExternal_reference()), HttpMethod.GET,
						new HttpEntity<>(new HttpHeaders()), Participante.class);
				return ResponseEntity.ok(participante.getBody());
			}else {
				return ResponseEntity.noContent().build();
			}
		}
	
	
}
