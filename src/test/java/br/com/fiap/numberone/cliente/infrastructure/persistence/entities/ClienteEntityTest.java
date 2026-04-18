package br.com.fiap.numberone.cliente.infrastructure.persistence.entities;

import br.com.fiap.numberone.cliente.domain.enums.TipoDocumento;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClienteEntityTest {

    @Test
    void prePersistDeveDefinirCreatedAtEAtivoDefault() {
        ClienteEntity entity = ClienteEntity.builder()
                .nome("Fulano")
                .tipoDocumento(TipoDocumento.PESSOA_FISICA)
                .documento("52998224725")
                .telefone("11999998888")
                .endereco("Rua A")
                .build();

        entity.prePersist();

        assertNotNull(entity.getCreatedAt());
        assertTrue(entity.getAtivo());
    }

    @Test
    void prePersistNaoDeveSobrescreverAtivoQuandoInformado() {
        ClienteEntity entity = ClienteEntity.builder()
                .nome("Empresa")
                .tipoDocumento(TipoDocumento.PESSOA_JURIDICA)
                .documento("45723174000110")
                .telefone("1133334444")
                .endereco("Av B")
                .ativo(false)
                .build();

        entity.prePersist();

        assertFalse(entity.getAtivo());
    }

    @Test
    void preUpdateDeveDefinirUpdatedAt() {
        ClienteEntity entity = ClienteEntity.builder().build();

        entity.preUpdate();

        assertNotNull(entity.getUpdatedAt());
    }
}
