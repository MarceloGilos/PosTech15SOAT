package br.com.fiap.numberone.cliente.api.dtos.responses;

import br.com.fiap.numberone.cliente.domain.enums.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClienteResponseTest {

    @Test
    void deveExporCamposDoRecord() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        ClienteResponse response = new ClienteResponse(1L, "Fulano", TipoDocumento.PESSOA_FISICA, "52998224725",
                "11999998888", "Rua A", true, createdAt, updatedAt);

        assertEquals(1L, response.id());
        assertEquals("Fulano", response.nome());
        assertEquals(TipoDocumento.PESSOA_FISICA, response.tipoDocumento());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }
}
