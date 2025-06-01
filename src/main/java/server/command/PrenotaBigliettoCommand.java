package command;

import Assembler.AssemblerTratta;
import dto.BigliettoDTO;
import dto.ClienteDTO;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import enums.StatoBiglietto;
import enums.TipoPrezzo;
import eventi.EventoGdsPrenotaz;
import model.Biglietto;
import model.Tratta;
import observer.EventDispatcher;
import persistence.MemoriaBiglietti;
import persistence.MemoriaClientiFedeli;
import persistence.MemoriaTratte;
import scheduling.PrenotazioneScheduler;

import java.time.LocalDate;
import java.util.UUID;

public class PrenotaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaTratte memoriaTratte;
    private final MemoriaClientiFedeli memoriaFedeli;
    private final EventDispatcher dispatcher;
    private final PrenotazioneScheduler scheduler;

    public PrenotaBigliettoCommand(
            RichiestaDTO richiesta,
            MemoriaBiglietti memoriaBiglietti,
            MemoriaTratte memoriaTratte,
            MemoriaClientiFedeli memoriaFedeli,
            EventDispatcher dispatcher
    ) {
        this(richiesta, memoriaBiglietti, memoriaTratte, memoriaFedeli, dispatcher, null);
    }

    // Costruttore con scheduler
    public PrenotaBigliettoCommand(
            RichiestaDTO richiesta,
            MemoriaBiglietti memoriaBiglietti,
            MemoriaTratte memoriaTratte,
            MemoriaClientiFedeli memoriaFedeli,
            EventDispatcher dispatcher,
            PrenotazioneScheduler scheduler
    ) {
        this.richiesta = richiesta;
        this.memoriaBiglietti = memoriaBiglietti;
        this.memoriaTratte = memoriaTratte;
        this.memoriaFedeli = memoriaFedeli;
        this.dispatcher = dispatcher;
        this.scheduler = scheduler;
    }

    @Override
    public RispostaDTO esegui(RichiestaDTO r) {
        UUID idCliente = UUID.fromString(richiesta.getIdCliente());
        Tratta tratta = memoriaTratte.getTrattaById(richiesta.getTratta().getId());

        if (tratta == null) {
            return new RispostaDTO("KO", "❌ Tratta non trovata", null);
        }

        // Verifica disponibilità posti
        long postiOccupati = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.getIdTratta().equals(tratta.getId()))
                .count();

        int capienza = tratta.getTreno().getCapienzaTotale();
        if (postiOccupati >= capienza) {
            return new RispostaDTO("KO", "❌ Nessun posto disponibile su questo treno", null);
        }

        // Verifica se può usare quel tipo di prezzo
        boolean isFedele = memoriaFedeli.isClienteFedele(idCliente);
        if (richiesta.getTipoPrezzo() != null) {
            switch (richiesta.getTipoPrezzo()) {
                case FEDELTA:
                    if (!isFedele) {
                        return new RispostaDTO("KO", "❌ Prezzo fedeltà non disponibile per questo cliente", null);
                    }
                    break;
            }
        }

        // Per le prenotazioni, usiamo prezzo INTERO di default
        TipoPrezzo tipoPrezzoEffettivo = richiesta.getTipoPrezzo() != null ?
                richiesta.getTipoPrezzo() : TipoPrezzo.INTERO;

        double prezzo = tratta.getPrezzi()
                .get(richiesta.getClasseServizio())
                .getPrezzo(tipoPrezzoEffettivo);

        // Crea biglietto prenotato (model)
        Biglietto biglietto = new Biglietto.Builder()
                .idCliente(idCliente)
                .idTratta(tratta.getId())
                .classe(richiesta.getClasseServizio())
                .prezzoPagato(prezzo)
                .dataAcquisto(LocalDate.now())
                .conCartaFedelta(isFedele)
                .tipoAcquisto("prenotazione")
                .build();

        // 🔧 FIX: SOLO l'evento salva il biglietto, NON salvare qui direttamente
        // RIMUOVI: memoriaBiglietti.aggiungiBiglietto(biglietto);

        // Invia evento che si occuperà del salvataggio tramite MemoriaBigliettiListener
        dispatcher.dispatch(new EventoGdsPrenotaz(biglietto));

        // 🔔 Programma rimozione automatica dopo 10 minuti
        if (scheduler != null) {
            scheduler.programmaRimozione(biglietto);
        }

        // 🔧 CONVERSIONE A DTO
        ClienteDTO clienteDTO = new ClienteDTO(
                idCliente,
                "Cliente", "Test", "cliente@test.com",
                isFedele, 0, "", 0, ""
        );

        TrattaDTO trattaDTO = AssemblerTratta.toDTO(tratta);

        BigliettoDTO bigliettoDTO = new BigliettoDTO(
                biglietto.getId(),
                clienteDTO,
                trattaDTO,
                biglietto.getClasse(),
                tipoPrezzoEffettivo,
                biglietto.getPrezzoPagato(),
                StatoBiglietto.NON_CONFERMATO // ⚠️ PRENOTAZIONE = NON_CONFERMATO
        );

        return new RispostaDTO("OK", "✅ Prenotazione effettuata", bigliettoDTO);
    }
}