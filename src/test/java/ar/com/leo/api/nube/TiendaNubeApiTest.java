package ar.com.leo.api.nube;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TiendaNubeApiTest {

    @Test
    void esNotaImpresoAceptaLaPalabraSolaEnCualquierCaja() {
        assertTrue(TiendaNubeApi.esNotaImpreso("IMPRESO"));
        assertTrue(TiendaNubeApi.esNotaImpreso("impreso"));
        assertTrue(TiendaNubeApi.esNotaImpreso("Impreso"));
        assertTrue(TiendaNubeApi.esNotaImpreso("  IMPRESO  "));
    }

    @Test
    void esNotaImpresoDejaPasarLasDemasNotas() {
        assertFalse(TiendaNubeApi.esNotaImpreso(""));
        assertFalse(TiendaNubeApi.esNotaImpreso("   "));
        assertFalse(TiendaNubeApi.esNotaImpreso(null));
        assertFalse(TiendaNubeApi.esNotaImpreso("IMPRESO 12/3"));
        assertFalse(TiendaNubeApi.esNotaImpreso("ya impreso"));
        assertFalse(TiendaNubeApi.esNotaImpreso("falta stock"));
        assertFalse(TiendaNubeApi.esNotaImpreso("llamar al cliente"));
    }
}
