package mx.com.grupoarpa.webhook.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
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
public class WebhookController {

	/* Dev */
	// private static final String ACCESS_TOKEN =
	// "TEST-6478429900085566-113022-088e337cb14fed28c6c30a47f9a16330-153057812";

	/* PRD */
	private static final String ACCESS_TOKEN = "APP_USR-6478429900085566-113022-63306cae1bda1b35da01505c058e6c25-153057812";
	private String url = "https://api.mercadopago.com/v1/payments/%s?access_token=" + ACCESS_TOKEN;
	private String urlParticipante = "https://ready2solve.club:8185/api/participante/%s";

	private String PATH_QR = "/var/www/html/qr/";
	//private String PATH_QR = "/Users/macbookpro/Documents/Boletera/";
	private String FILE_EXT = ".png";

	@Autowired
	MercadoPagoRepository mercadoRepo;

	@Autowired
	MercadoPagoTransaccionRepository transactionRepo;

	@Autowired
	RestTemplate restTemplate;

	/*@PostMapping
	public ResponseEntity<String> addResponseMercadoPago(@RequestBody final MercadoPagoResponse mercadoPagoResponse) {
		mercadoPagoResponse.setIdResponse(mercadoPagoResponse.getId());
		mercadoPagoResponse.setId(null);
		mercadoRepo.save(mercadoPagoResponse);
		MercadoPagoTransaction mpTransactionBody;
		try {
			ResponseEntity<MercadoPagoTransaction> mpTransaction = restTemplate.exchange(
					// hacer el substring
					String.format(url, mercadoPagoResponse.getData()), HttpMethod.GET,
					new HttpEntity<>(new HttpHeaders()), MercadoPagoTransaction.class);
			mpTransactionBody = mpTransaction.getBody();
			mpTransactionBody.setIdTransaction(mpTransaction.getBody().getId());
			mpTransactionBody.setId(null);
			mpTransactionBody.setChecked(false);
			MercadoPagoTransaction mpTransactionSaved = transactionRepo.save(mpTransactionBody);

			if (mpTransactionSaved.getStatus().equals(MercadoPagoTransactionStatus.approved.toString())
					&& mpTransaction.getStatusCode() == HttpStatus.OK) {
				String md5Hex = DigestUtils.md5DigestAsHex(mpTransactionSaved.getId().getBytes());
				String encoded = md5Hex + Base64.getEncoder().encodeToString(String.valueOf(Calendar.getInstance().getTime().getTime()).getBytes());
				QRCodeGenerator.generateQRCodeImage(URL_VERIFICACION + encoded, 250, 250,PATH_QR + mpTransactionBody.getIdTransaction() + FILE_EXT);
				mpTransactionSaved.setEncodedId(encoded);
				transactionRepo.save(mpTransactionSaved);
				EmailConfig.sendmail(mpTransactionSaved.getPayer().getEmail(),
						PATH_QR + mpTransactionBody.getIdTransaction() + FILE_EXT, mpTransactionBody.getPayer().getFirst_name()+mpTransactionBody.getPayer().getLast_name(), mpTransactionBody.getDescription(),"600.00","96.00","696.00");
			}
		} catch (IOException | WriterException | MessagingException e) {
			return ResponseEntity.badRequest().body("Error");

		} catch (HttpClientErrorException badReq) {
			return ResponseEntity.badRequest().body("Venta no disponible");
		}
		return ResponseEntity.ok().body("La transaccion fue: " + mpTransactionBody.getStatus());
	}*/
	
	/*@PostMapping
	public ResponseEntity<?> addResponseMercadoPago(@RequestBody final MercadoPagoResponse mercadoPagoResponse) {
		return ResponseEntity.ok(mercadoRepo.save(mercadoPagoResponse)); 
	}*/
	
	
	@GetMapping("/confirm/{idData}")
	public ResponseEntity<?> getReponseByData(@PathVariable final String idData) {
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
					
					double costoTicket = mpTransactionBody.getDescription().contains("Avalance MTB") || mpTransactionBody.getDescription().contains("Inscripcion Carrera") ? 600.00 : 400.00;
					double costoTotal = mpTransactionBody.getTransaction_details().getTotal_paid_amount();
					double comision = mpTransactionBody.getDescription().contains("Avalance MTB") || mpTransactionBody.getDescription().contains("Inscripcion Carrera") ? 96.00 : 64.00;
					double porcentaje = mpTransactionBody.getDescription().contains("10") ? 0.10 : 0.00;
					double descuento = costoTicket * porcentaje;
					/*if(mpTransactionBody.getPayer().getEmail().equals("tomas.jerry@hotmail.com")) {
						costoTotal = 696.00;
						costoTicket = 600.00;
						comision = 96.00;
					}*/
					String evento = mpTransactionBody.getDescription().contains("Inscripcion Carrera") ? "Avalance MTB" : mpTransactionBody.getDescription();
					
					EmailConfig.sendmail(mpTransactionBody.getPayer().getEmail(),
							mpTransactionBody.getIdTransaction()+"q" + FILE_EXT, 
							participante.getBody().getNombres(),
							participante.getBody().getApellidos(), 
							evento.contains("%") ? evento.substring(0,evento.indexOf('%')-3) : evento,
							"$"+(costoTicket)+" MXN",
							"$"+(comision)+" MXN",
							"$"+(costoTotal)+" MXN",
							"$"+(descuento)+" MXN",
							participante.getBody().getCategoryAvalanche() != null ? participante.getBody().getCategoryAvalanche() : "",
							mpTransactionBody.getDescription().contains("Avalance MTB") || mpTransactionBody.getDescription().contains("Inscripcion Carrera"));
					transactionRepo.save(mpTransactionBody);
					mercadoRepo.save(mpRespFound);
				}
			}
		} catch (IOException | WriterException | MessagingException e) {
			return ResponseEntity.badRequest().body("Error");

		} catch (HttpClientErrorException badReq) {
			badReq.printStackTrace();
			return ResponseEntity.badRequest().body("Venta no disponible");
		}
		return ResponseEntity.ok().body("La transaccion fue: " + mpTransactionBody.getStatus());
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
	
}
