package com.fitnesslife.gym.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import java.util.Map;

@Slf4j
@Service
public class EpaycoValidationService {

    private static final String EPAYCO_VALIDATION_URL = "https://secure.epayco.co/validation/v1/reference/";
    private final RestTemplate restTemplate;

    public EpaycoValidationService() {
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> getTransactionData(String refPayco) {
        try {
            log.info("Consultando API de ePayco para ref_payco: {}", refPayco);

            String url = EPAYCO_VALIDATION_URL + refPayco;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("success")) {
                Boolean success = (Boolean) response.get("success");

                if (Boolean.TRUE.equals(success)) {
                    log.info("Transacción encontrada exitosamente: {}", refPayco);
                    return response;
                } else {
                    log.warn("ePayco devolvió success=false para ref_payco: {}", refPayco);
                    return null;
                }
            }

            log.warn("Respuesta de ePayco no contiene campo 'success'");
            return null;

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP consultando ePayco: {} - {}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error inesperado consultando API de ePayco", e);
            return null;
        }
    }
}
