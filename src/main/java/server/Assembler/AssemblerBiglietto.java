package Assembler;

import dto.BigliettoDTO;
import dto.ClienteDTO;
import dto.TrattaDTO;
import enums.StatoBiglietto;
import enums.TipoPrezzo;
import model.Biglietto;
import model.Tratta;

import java.util.UUID;

public class AssemblerBiglietto {

    public static BigliettoDTO toDTO(Biglietto model, ClienteDTO clienteDTO, TrattaDTO trattaDTO, TipoPrezzo tipoPrezzo, StatoBiglietto stato) {
        return new BigliettoDTO(
                model.getId(),
                clienteDTO,
                trattaDTO,
                model.getClasse(),
                tipoPrezzo,
                model.getPrezzoPagato(),
                stato
        );
    }

    // Se vuoi anche il contrario, aggiungi questo metodo:
    public static Biglietto fromDTO(BigliettoDTO dto) {
        return new Biglietto.Builder()
                .idCliente(dto.getCliente().getId())
                .idTratta(dto.getTratta().getId())
                .classe(dto.getClasseServizio())
                .prezzoPagato(dto.getPrezzoEffettivo())
                .dataAcquisto(java.time.LocalDate.now())
                .tipoAcquisto("dto")
                .build();
    }
}