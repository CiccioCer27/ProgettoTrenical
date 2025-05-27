package dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RichiestaDTOTest {

    @Test
    void testBuilderFunzionaCorrettamente() {
        RichiestaDTO richiesta = new RichiestaDTO.Builder()
                .tipo("TEST")
                .idCliente("abc-123")
                .messaggioExtra("Simulazione")
                .build();

        assertEquals("TEST", richiesta.getTipo());
        assertEquals("abc-123", richiesta.getIdCliente());
        assertEquals("Simulazione", richiesta.getMessaggioExtra());
    }
}