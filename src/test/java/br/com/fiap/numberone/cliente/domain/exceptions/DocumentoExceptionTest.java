package br.com.fiap.numberone.cliente.domain.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentoExceptionTest {

    @Test
    void deveArmazenarMensagem() {
        DocumentoException exception = new DocumentoException("erro");

        assertEquals("erro", exception.getMessage());
    }
}
