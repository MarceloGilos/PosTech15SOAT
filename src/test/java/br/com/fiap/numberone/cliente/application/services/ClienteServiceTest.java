package br.com.fiap.numberone.cliente.application.services;

import br.com.fiap.numberone.cliente.api.dtos.requests.ClienteRequest;
import br.com.fiap.numberone.cliente.api.dtos.responses.ClienteResponse;
import br.com.fiap.numberone.cliente.application.mappers.ClienteMapper;
import br.com.fiap.numberone.cliente.domain.entities.Cliente;
import br.com.fiap.numberone.cliente.domain.enums.TipoDocumento;
import br.com.fiap.numberone.cliente.infrastructure.persistence.entities.ClienteEntity;
import br.com.fiap.numberone.cliente.infrastructure.persistence.mappers.ClienteEntityMapper;
import br.com.fiap.numberone.cliente.infrastructure.repositories.ClienteRepository;
import br.com.fiap.numberone.shared.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ClienteMapper clienteMapper;
    @Mock
    private ClienteEntityMapper clienteEntityMapper;
    @InjectMocks
    private ClienteService clienteService;

    private ClienteRequest request;
    private Cliente domain;
    private ClienteEntity entity;
    private ClienteResponse response;

    @BeforeEach
    void setUp() {
        request = new ClienteRequest("Fulano", TipoDocumento.PESSOA_FISICA, "52998224725", "11999998888", "Rua A", true);
        domain = Cliente.builder().id(1L).nome("Fulano").tipoDocumento(TipoDocumento.PESSOA_FISICA).documento("52998224725")
                .telefone("11999998888").endereco("Rua A").ativo(true).createdAt(LocalDateTime.now().minusDays(1)).build();
        entity = ClienteEntity.builder().id(1L).nome("Fulano").tipoDocumento(TipoDocumento.PESSOA_FISICA).documento("52998224725")
                .telefone("11999998888").endereco("Rua A").ativo(true).createdAt(LocalDateTime.now().minusDays(1)).build();
        response = new ClienteResponse(1L, "Fulano", TipoDocumento.PESSOA_FISICA, "52998224725", "11999998888", "Rua A", true, LocalDateTime.now().minusDays(1), null);
    }

    @Test
    void deveCriarCliente() {
        when(clienteMapper.toEntity(request)).thenReturn(domain);
        when(clienteEntityMapper.toEntity(domain)).thenReturn(entity);
        when(clienteRepository.save(entity)).thenReturn(entity);
        when(clienteEntityMapper.toDomain(entity)).thenReturn(domain);
        when(clienteMapper.toResponse(domain)).thenReturn(response);

        ClienteResponse resultado = clienteService.create(request);

        assertEquals(response, resultado);
        verify(clienteRepository).save(entity);
    }

    @Test
    void deveAtualizarCliente() {
        Cliente novo = Cliente.builder()
                .nome("Novo Nome")
                .tipoDocumento(TipoDocumento.PESSOA_FISICA)
                .documento("52998224725")
                .telefone("11911110000")
                .endereco("Rua B")
                .ativo(false)
                .build();
        Cliente atualizado = domain.updateFrom(novo);
        ClienteEntity entityAtualizado = ClienteEntity.builder().id(1L).nome("Novo Nome").tipoDocumento(TipoDocumento.PESSOA_FISICA)
                .documento("52998224725").telefone("11911110000").endereco("Rua B").ativo(false).build();
        ClienteResponse responseAtualizado = new ClienteResponse(1L, "Novo Nome", TipoDocumento.PESSOA_FISICA, "52998224725", "11911110000", "Rua B", false, domain.getCreatedAt(), atualizado.getUpdatedAt());

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(clienteEntityMapper.toDomain(entity)).thenReturn(domain);
        when(clienteMapper.toEntity(request)).thenReturn(novo);
        when(clienteEntityMapper.toEntity(any(Cliente.class))).thenReturn(entityAtualizado);
        when(clienteRepository.save(entityAtualizado)).thenReturn(entityAtualizado);
        when(clienteEntityMapper.toDomain(entityAtualizado)).thenReturn(atualizado);
        when(clienteMapper.toResponse(atualizado)).thenReturn(responseAtualizado);

        ClienteResponse resultado = clienteService.update(1L, request);

        assertEquals(responseAtualizado, resultado);
        verify(clienteRepository).findById(1L);
        verify(clienteRepository).save(entityAtualizado);
    }

    @Test
    void deveLancarExcecaoAoAtualizarClienteInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.update(99L, request));

        assertEquals("Cliente não encontrado com id: 99", exception.getMessage());
    }

    @Test
    void deveBuscarPorId() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(clienteEntityMapper.toDomain(entity)).thenReturn(domain);
        when(clienteMapper.toResponse(domain)).thenReturn(response);

        ClienteResponse resultado = clienteService.findById(1L);

        assertEquals(response, resultado);
    }

    @Test
    void deveLancarExcecaoAoBuscarPorIdInexistente() {
        when(clienteRepository.findById(98L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.findById(98L));

        assertEquals("Cliente não encontrado com id: 98", exception.getMessage());
    }

    @Test
    void deveListarTodos() {
        when(clienteRepository.findAll()).thenReturn(List.of(entity));
        when(clienteEntityMapper.toDomain(entity)).thenReturn(domain);
        when(clienteMapper.toResponse(domain)).thenReturn(response);

        List<ClienteResponse> resultado = clienteService.findAll();

        assertEquals(1, resultado.size());
        assertEquals(response, resultado.getFirst());
    }

    @Test
    void deveExcluirCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(entity));

        clienteService.delete(1L);

        verify(clienteRepository).delete(entity);
    }

    @Test
    void deveLancarExcecaoAoExcluirClienteInexistente() {
        when(clienteRepository.findById(7L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.delete(7L));

        assertEquals("Cliente não encontrado com id: 7", exception.getMessage());
    }
}
