package client.service;

import dto.ClienteDTO;
import org.junit.jupiter.api.Test;
import service.ClientService;

import static org.junit.jupiter.api.Assertions.*;

public class ClientServiceTest {

    @Test
    void testAttivaClienteGeneraDTO() {
        ClientService service = new ClientService("localhost", 1234);
        service.attivaCliente("Luca", "Bianchi", "luca@example.com", 25, "Torino", "1234567890");

        ClienteDTO cliente = service.getCliente();

        assertNotNull(cliente);
        assertEquals("Luca", cliente.getNome());
        assertEquals("Bianchi", cliente.getCognome());
        assertEquals("luca@example.com", cliente.getEmail());
        assertNotNull(cliente.getId());  // UUID
    }
}