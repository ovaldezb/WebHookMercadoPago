package mx.com.grupoarpa.webhook.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import mx.com.grupoarpa.webhook.entity.MercadoPagoTransaction;

public interface MercadoPagoTransaccionRepository extends MongoRepository<MercadoPagoTransaction, String> {
	Optional<MercadoPagoTransaction> findByEncodedId(String encodedId);
}
