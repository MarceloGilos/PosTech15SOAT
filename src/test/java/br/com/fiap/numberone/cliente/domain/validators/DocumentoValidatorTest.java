package br.com.fiap.numberone.cliente.domain.validators;

import br.com.fiap.numberone.cliente.domain.enums.TipoDocumento;
import br.com.fiap.numberone.cliente.domain.exceptions.DocumentoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentoValidatorTest {

    @Test
    void deveAceitarCpfValidoComMascara() {
        assertDoesNotThrow(() -> DocumentoValidator.validar(TipoDocumento.PESSOA_FISICA, "529.982.247-25"));
    }

    @Test
    void deveRejeitarCpfInvalido() {
        DocumentoException exception = assertThrows(
                DocumentoException.class,
                () -> DocumentoValidator.validar(TipoDocumento.PESSOA_FISICA, "11111111111")
        );

        assertEquals("CPF inválido", exception.getMessage());
    }

    @Test
    void deveAceitarCnpjValidoComMascara() {
        assertDoesNotThrow(() -> DocumentoValidator.validar(TipoDocumento.PESSOA_JURIDICA, "45.723.174/0001-10"));
    }

    @Test
    void deveRejeitarCnpjInvalido() {
        DocumentoException exception = assertThrows(
                DocumentoException.class,
                () -> DocumentoValidator.validar(TipoDocumento.PESSOA_JURIDICA, "11.111.111/1111-11")
        );

        assertEquals("CNPJ inválido", exception.getMessage());
    }

    @Test
    void deveIgnorarQuandoTipoDocumentoNaoInformado() {
        assertDoesNotThrow(() -> DocumentoValidator.validar(null, "qualquer"));
    }
}
