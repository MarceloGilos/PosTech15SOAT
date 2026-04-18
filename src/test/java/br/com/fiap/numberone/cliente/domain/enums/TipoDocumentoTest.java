package br.com.fiap.numberone.cliente.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TipoDocumentoTest {

    @Test
    void deveConterValoresEsperados() {
        assertEquals(TipoDocumento.PESSOA_FISICA, TipoDocumento.valueOf("PESSOA_FISICA"));
        assertEquals(TipoDocumento.PESSOA_JURIDICA, TipoDocumento.valueOf("PESSOA_JURIDICA"));
    }
}
