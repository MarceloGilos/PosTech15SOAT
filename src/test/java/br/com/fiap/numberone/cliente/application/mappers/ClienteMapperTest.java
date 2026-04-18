package br.com.fiap.numberone.cliente.application.mappers;

import br.com.fiap.numberone.cliente.api.dtos.requests.ClienteRequest;
import br.com.fiap.numberone.cliente.api.dtos.responses.ClienteResponse;
import br.com.fiap.numberone.cliente.domain.entities.Cliente;
import br.com.fiap.numberone.cliente.domain.enums.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ClienteMapperTest {

    private final ClienteMapper mapper = new ClienteMapper();

    @Test
    void deveMapearRequestParaEntityComAtivoDefaultTrue() {
        ClienteRequest request = new ClienteRequest(
                "Fulano",
                TipoDocumento.PESSOA_FISICA,
                "52998224725",
                "11999998888",
                "Rua X",
                null
        );

        Cliente cliente = mapper.toEntity(request);

        assertEquals("Fulano", cliente.getNome());
        assertEquals(TipoDocumento.PESSOA_FISICA, cliente.getTipoDocumento());
        assertTrue(cliente.getAtivo());
    }

    @Test
    void deveMapearRequestParaEntityComAtivoInformado() {
        ClienteRequest request = new ClienteRequest(
                "Empresa",
                TipoDocumento.PESSOA_JURIDICA,
                "45723174000110",
                "1133334444",
                "Avenida Y",
                false
        );

        Cliente cliente = mapper.toEntity(request);

        assertFalse(cliente.getAtivo());
    }

    @Test
    void deveMapearEntityParaResponse() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        Cliente cliente = Cliente.builder()
                .id(1L)
                .nome("Fulano")
                .tipoDocumento(TipoDocumento.PESSOA_FISICA)
                .documento("52998224725")
                .telefone("11999998888")
                .endereco("Rua X")
                .ativo(true)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        ClienteResponse response = mapper.toResponse(cliente);

        assertEquals(1L, response.id());
        assertEquals("Fulano", response.nome());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }
}
