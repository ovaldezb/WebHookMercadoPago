package mx.com.grupoarpa.webhook.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import mx.com.grupoarpa.webhook.entity.MercadoPagoResponse;

public interface MercadoPagoRepository extends MongoRepository<MercadoPagoResponse, String> {

	public Optional<MercadoPagoResponse> findByData(String data);
}
