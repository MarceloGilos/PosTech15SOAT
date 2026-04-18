package br.com.fiap.numberone.cliente.api.controllers;

import br.com.fiap.numberone.cliente.api.dtos.requests.ClienteRequest;
import br.com.fiap.numberone.cliente.api.dtos.responses.ClienteResponse;
import br.com.fiap.numberone.cliente.application.services.ClienteService;
import br.com.fiap.numberone.cliente.domain.enums.TipoDocumento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private ClienteRequest request;
    private ClienteResponse response;

    @BeforeEach
    void init() {
        request = new ClienteRequest("Fulano", TipoDocumento.PESSOA_FISICA, "52998224725", "11999998888", "Rua A", true);
        response = new ClienteResponse(1L, "Fulano", TipoDocumento.PESSOA_FISICA, "52998224725", "11999998888", "Rua A", true,
                LocalDateTime.now().minusDays(1), null);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/api/clientes");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
    }

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void deveCriarClienteComLocationHeader() {
        when(clienteService.create(request)).thenReturn(response);

        var resultado = clienteController.create(request);

        assertEquals(HttpStatus.CREATED, resultado.getStatusCode());
        assertEquals(response, resultado.getBody());
        assertNotNull(resultado.getHeaders().getLocation());
        assertTrue(resultado.getHeaders().getLocation().toString().endsWith("/api/clientes/1"));
    }

    @Test
    void deveAtualizarCliente() {
        when(clienteService.update(1L, request)).thenReturn(response);

        var resultado = clienteController.update(1L, request);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertEquals(response, resultado.getBody());
    }

    @Test
    void deveBuscarClientePorId() {
        when(clienteService.findById(1L)).thenReturn(response);

        var resultado = clienteController.findById(1L);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertEquals(response, resultado.getBody());
    }

    @Test
    void deveListarClientes() {
        when(clienteService.findAll()).thenReturn(List.of(response));

        var resultado = clienteController.findAll();

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertNotNull(resultado.getBody());
        assertEquals(1, resultado.getBody().size());
    }

    @Test
    void deveExcluirCliente() {
        var resultado = clienteController.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, resultado.getStatusCode());
        verify(clienteService).delete(1L);
    }
}
