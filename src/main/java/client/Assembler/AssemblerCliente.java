package Assembler;

import model.Cliente;
import dto.ClienteDTO;

public class AssemblerCliente {

    public static ClienteDTO toDTO(Cliente cliente) {
        return new ClienteDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCognome(),
                cliente.getEmail(),
                cliente.isFedelta(),
                cliente.getEta(),
                cliente.getResidenza(),
                cliente.getPuntiFedelta(),
                cliente.getCellulare()
        );
    }

    public static Cliente fromDTO(ClienteDTO dto) {
        return new Cliente.Builder(dto.getNome(), dto.getCognome(), dto.getEmail())
                .withId(dto.getId()) // ✅ Mantiene l’UUID originale
                .fedelta(dto.isFedelta())
                .eta(dto.getEta())
                .residenza(dto.getResidenza())
                .puntiFedelta(dto.getPuntiFedelta())
                .cellulare(dto.getCellulare())
                .build();
    }
}