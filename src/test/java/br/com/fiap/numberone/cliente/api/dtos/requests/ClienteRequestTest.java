package br.com.fiap.numberone.cliente.api.dtos.requests;

import br.com.fiap.numberone.cliente.domain.enums.TipoDocumento;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClienteRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void deveValidarCamposObrigatorios() {
        ClienteRequest request = new ClienteRequest("", null, "", "", "", null);

        Set<ConstraintViolation<ClienteRequest>> violations = validator.validate(request);

        assertEquals(5, violations.size());
    }

    @Test
    void deveAceitarRequestValido() {
        ClienteRequest request = new ClienteRequest(
                "Fulano",
                TipoDocumento.PESSOA_FISICA,
                "52998224725",
                "11999998888",
                "Rua A",
                true
        );

        Set<ConstraintViolation<ClienteRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }
}
