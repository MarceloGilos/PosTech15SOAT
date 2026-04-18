package br.com.fiap.numberone.cliente.infrastructure.persistence.mappers;

import br.com.fiap.numberone.cliente.domain.entities.Cliente;
import br.com.fiap.numberone.cliente.domain.enums.TipoDocumento;
import br.com.fiap.numberone.cliente.infrastructure.persistence.entities.ClienteEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClienteEntityMapperTest {

    private final ClienteEntityMapper mapper = new ClienteEntityMapper();

    @Test
    void deveMapearDomainParaEntity() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(2);
        LocalDateTime updatedAt = LocalDateTime.now().minusDays(1);

        Cliente domain = Cliente.builder()
                .id(1L)
                .nome("Fulano")
                .tipoDocumento(TipoDocumento.PESSOA_FISICA)
                .documento("52998224725")
                .telefone("11999998888")
                .endereco("Rua A")
                .ativo(true)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        ClienteEntity entity = mapper.toEntity(domain);

        assertEquals(domain.getId(), entity.getId());
        assertEquals(domain.getNome(), entity.getNome());
        assertEquals(domain.getTipoDocumento(), entity.getTipoDocumento());
        assertEquals(domain.getDocumento(), entity.getDocumento());
        assertEquals(domain.getTelefone(), entity.getTelefone());
        assertEquals(domain.getEndereco(), entity.getEndereco());
        assertEquals(domain.getAtivo(), entity.getAtivo());
        assertEquals(domain.getCreatedAt(), entity.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), entity.getUpdatedAt());
    }

    @Test
    void deveMapearEntityParaDomain() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(2);
        LocalDateTime updatedAt = LocalDateTime.now().minusDays(1);

        ClienteEntity entity = ClienteEntity.builder()
                .id(1L)
                .nome("Empresa")
                .tipoDocumento(TipoDocumento.PESSOA_JURIDICA)
                .documento("45723174000110")
                .telefone("1133334444")
                .endereco("Av B")
                .ativo(false)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        Cliente domain = mapper.toDomain(entity);

        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getNome(), domain.getNome());
        assertEquals(entity.getTipoDocumento(), domain.getTipoDocumento());
        assertEquals(entity.getDocumento(), domain.getDocumento());
        assertEquals(entity.getTelefone(), domain.getTelefone());
        assertEquals(entity.getEndereco(), domain.getEndereco());
        assertEquals(entity.getAtivo(), domain.getAtivo());
        assertEquals(entity.getCreatedAt(), domain.getCreatedAt());
        assertEquals(entity.getUpdatedAt(), domain.getUpdatedAt());
    }
}
