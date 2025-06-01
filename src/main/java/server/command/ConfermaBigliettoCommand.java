package command;

import Assembler.AssemblerTratta;
import dto.BigliettoDTO;
import dto.ClienteDTO;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import enums.StatoBiglietto;
import enums.TipoPrezzo;
import eventi.EventoGdsAcquisto;
import model.Biglietto;
import model.Tratta;
import observer.EventDispatcher;
import persistence.MemoriaBiglietti;
import persistence.MemoriaTratte;
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.UUID;

public class ConfermaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoria;
    private final BancaServiceClient banca;
    private final EventDispatcher dispatcher;
    private final MemoriaTratte memoriaTratte;

    public ConfermaBigliettoCommand(RichiestaDTO richiesta, MemoriaBiglietti memoria,
                                    BancaServiceClient banca, EventDispatcher dispatcher) {
        this.richiesta = richiesta;
        this.memoria = memoria;
        this.banca = banca;
        this.dispatcher = dispatcher;
        this.memoriaTratte = null; // Potrebbe servire se disponibile
    }

    // Costruttore alternativo con MemoriaTratte
    public ConfermaBigliettoCommand(RichiestaDTO richiesta, MemoriaBiglietti memoria,
                                    BancaServiceClient banca, EventDispatcher dispatcher,
                                    MemoriaTratte memoriaTratte) {
        this.richiesta = richiesta;
        this.memoria = memoria;
        this.banca = banca;
        this.dispatcher = dispatcher;
        this.memoriaTratte = memoriaTratte;
    }

    @Override
    public RispostaDTO esegui(RichiestaDTO r) {
        BigliettoDTO bigliettoPrenotato = richiesta.getBiglietto();

        if (bigliettoPrenotato == null) {
            return new RispostaDTO("KO", "❌ Biglietto non specificato", null);
        }

        // Trova il biglietto prenotato nel sistema
        Biglietto bigliettoModel = memoria.getById(bigliettoPrenotato.getId());
        if (bigliettoModel == null) {
            return new RispostaDTO("KO", "❌ Prenotazione non trovata", null);
        }

        // Rimuove la prenotazione
        memoria.rimuoviBiglietto(bigliettoPrenotato.getId());

        // Comunica con la banca
        boolean pagato = banca.paga(
                bigliettoModel.getIdCliente().toString(),
                bigliettoModel.getPrezzoPagato(),
                "Pagamento conferma biglietto"
        );

        if (!pagato) {
            // Ripristina la prenotazione se il pagamento fallisce
            memoria.aggiungiBiglietto(bigliettoModel);
            return new RispostaDTO("KO", "❌ Pagamento non riuscito", null);
        }

        // 🔧 CORREZIONE: Usa lo STESSO ID del biglietto prenotato
        Biglietto confermato = new Biglietto(
                bigliettoModel.getId(), // ⚠️ STESSO ID!
                bigliettoModel.getIdCliente(),
                bigliettoModel.getIdTratta(),
                bigliettoModel.getClasse(),
                bigliettoModel.isConCartaFedelta(),
                bigliettoModel.getPrezzoPagato(),
                LocalDate.now(), // Data conferma
                "acquisto" // Tipo cambia da "prenotazione" a "acquisto"
        );

        memoria.aggiungiBiglietto(confermato);
        dispatcher.dispatch(new EventoGdsAcquisto(confermato));

        // 🔧 CONVERSIONE A DTO
        ClienteDTO clienteDTO = new ClienteDTO(
                confermato.getIdCliente(),
                "Cliente", "Test", "cliente@test.com",
                confermato.isConCartaFedelta(), 0, "", 0, ""
        );

        // Crea TrattaDTO minimale o usa MemoriaTratte se disponibile
        TrattaDTO trattaDTO;
        if (memoriaTratte != null) {
            Tratta tratta = memoriaTratte.getTrattaById(confermato.getIdTratta());
            trattaDTO = tratta != null ? AssemblerTratta.toDTO(tratta) :
                    createMinimalTrattaDTO(confermato.getIdTratta());
        } else {
            trattaDTO = createMinimalTrattaDTO(confermato.getIdTratta());
        }

        BigliettoDTO bigliettoDTO = new BigliettoDTO(
                confermato.getId(),
                clienteDTO,
                trattaDTO,
                confermato.getClasse(),
                confermato.isConCartaFedelta() ? TipoPrezzo.FEDELTA : TipoPrezzo.INTERO,
                confermato.getPrezzoPagato(),
                StatoBiglietto.CONFERMATO
        );

        return new RispostaDTO("OK", "✅ Biglietto confermato", bigliettoDTO);
    }

    private TrattaDTO createMinimalTrattaDTO(UUID idTratta) {
        return new TrattaDTO(
                idTratta, "Partenza", "Arrivo",
                LocalDate.now(), java.time.LocalTime.now(), 1,
                null, null
        );
    }
}