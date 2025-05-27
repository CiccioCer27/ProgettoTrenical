package Assembler;

import dto.*;
import enums.ClasseServizio;
import enums.TipoPrezzo;

import java.time.LocalDate;
import java.util.UUID;

public class AssemblerRichiesta {

    public static RichiestaDTO buildAcquisto(String idCliente, TrattaDTO tratta,
                                             ClasseServizio classeServizio, TipoPrezzo tipoPrezzo) {
        RichiestaAcquistoDTO payload = new RichiestaAcquistoDTO(tratta, classeServizio, tipoPrezzo, idCliente.toString());
        return new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(idCliente)
                .payload(payload)
                .build();
    }

    public static RichiestaDTO buildPrenotazione(String idCliente, TrattaDTO tratta,
                                                 ClasseServizio classeServizio) {
        RichiestaPrenotazioneDTO payload = new RichiestaPrenotazioneDTO(tratta, classeServizio, idCliente.toString());
        return new RichiestaDTO.Builder()
                .tipo("PRENOTA")
                .idCliente(idCliente)
                .payload(payload)
                .build();
    }

    public static RichiestaDTO buildModifica(String idCliente, BigliettoDTO bigliettoOriginale,
                                             TrattaDTO nuovaTratta, ClasseServizio nuovaClasse, TipoPrezzo tipoPrezzo) {
        RichiestaModificaDTO payload = new RichiestaModificaDTO(
                bigliettoOriginale, nuovaTratta, nuovaClasse, tipoPrezzo, idCliente.toString()
        );
        return new RichiestaDTO.Builder()
                .tipo("MODIFICA")
                .idCliente(idCliente)
                .payload(payload)
                .build();
    }

    public static RichiestaDTO buildRichiestaFedelta(String idCliente) {
        RichiestaCartaFedeltaDTO payload = new RichiestaCartaFedeltaDTO(idCliente.toString());
        return new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(idCliente)
                .payload(payload)
                .build();
    }

    public static RichiestaDTO buildRichiestaTratte(LocalDate data, String fascia, String partenza,
                                                    String arrivo, String tipoTreno, ClasseServizio classe) {
        RichiestaTratteDTO payload = new RichiestaTratteDTO(data, fascia, partenza, arrivo, tipoTreno, classe);
        return new RichiestaDTO.Builder()
                .tipo("RICERCA_TRATTE")
                .payload(payload)
                .build();
    }
}