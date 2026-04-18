package br.com.fiap.numberone.cliente.domain.entities;

import br.com.fiap.numberone.cliente.domain.enums.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {

    @Test
    void deveAtualizarCamposPreservandoIdECreatedAt() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(3);

        Cliente clienteOriginal = Cliente.builder()
                .id(10L)
                .nome("João")
                .tipoDocumento(TipoDocumento.PESSOA_FISICA)
                .documento("52998224725")
                .telefone("11911112222")
                .endereco("Rua A")
                .ativo(true)
                .createdAt(createdAt)
                .build();

        Cliente novoCliente = Cliente.builder()
                .nome("Maria")
                .tipoDocumento(TipoDocumento.PESSOA_JURIDICA)
                .documento("45723174000110")
                .telefone("11999990000")
                .endereco("Rua B")
                .ativo(false)
                .build();

        Cliente atualizado = clienteOriginal.updateFrom(novoCliente);

        assertEquals(10L, atualizado.getId());
        assertEquals(createdAt, atualizado.getCreatedAt());
        assertEquals("Maria", atualizado.getNome());
        assertEquals(TipoDocumento.PESSOA_JURIDICA, atualizado.getTipoDocumento());
        assertEquals("45723174000110", atualizado.getDocumento());
        assertEquals("11999990000", atualizado.getTelefone());
        assertEquals("Rua B", atualizado.getEndereco());
        assertFalse(atualizado.getAtivo());
        assertNotNull(atualizado.getUpdatedAt());
    }
}
