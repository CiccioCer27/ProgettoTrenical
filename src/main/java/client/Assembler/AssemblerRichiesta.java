package Assembler;

import dto.*;
import enums.ClasseServizio;
import enums.TipoPrezzo;

import java.time.LocalDate;

public class AssemblerRichiesta {

    public static RichiestaDTO creaAcquisto(String idCliente, TrattaDTO tratta, ClasseServizio classe, TipoPrezzo tipoPrezzo) {
        return new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(idCliente)
                .tratta(tratta)
                .classeServizio(classe)
                .tipoPrezzo(tipoPrezzo)
                .build();
    }

    public static RichiestaDTO creaPrenotazione(String idCliente, TrattaDTO tratta, ClasseServizio classe) {
        return new RichiestaDTO.Builder()
                .tipo("PRENOTA")
                .idCliente(idCliente)
                .tratta(tratta)
                .classeServizio(classe)
                .build();
    }

    public static RichiestaDTO creaModifica(String idCliente, BigliettoDTO bigliettoVecchio, TrattaDTO nuovaTratta,
                                            ClasseServizio nuovaClasse, TipoPrezzo tipoPrezzo, double penale) {
        return new RichiestaDTO.Builder()
                .tipo("MODIFICA")
                .idCliente(idCliente)
                .biglietto(bigliettoVecchio)
                .tratta(nuovaTratta)
                .classeServizio(nuovaClasse)
                .tipoPrezzo(tipoPrezzo)
                .penale(penale)
                .build();
    }

    public static RichiestaDTO creaConferma(String idCliente, BigliettoDTO bigliettoPrenotato) {
        return new RichiestaDTO.Builder()
                .tipo("CONFERMA")
                .idCliente(idCliente)
                .biglietto(bigliettoPrenotato)
                .build();
    }

    public static RichiestaDTO creaCartaFedelta(String idCliente) {
        return new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(idCliente)
                .build();
    }

    public static RichiestaDTO creaRicercaTratte(LocalDate data, String partenza, String arrivo,
                                                 String tipoTreno, ClasseServizio classe, String fascia) {
        return new RichiestaDTO.Builder()
                .tipo("RICERCA_TRATTE")
                .data(data)
                .partenza(partenza)
                .arrivo(arrivo)
                .tipoTreno(tipoTreno)
                .classeServizio(classe)
                .fasciaOraria(fascia)
                .build();
    }
}