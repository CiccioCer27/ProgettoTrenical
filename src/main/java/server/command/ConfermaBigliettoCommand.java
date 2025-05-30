package command;

import dto.BigliettoDTO;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import eventi.EventoGdsAcquisto;
import model.Biglietto;
import observer.EventDispatcher;
import persistence.MemoriaBiglietti;
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.UUID;

public class ConfermaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoria;
    private final BancaServiceClient banca;
    private final EventDispatcher dispatcher;

    public ConfermaBigliettoCommand(RichiestaDTO richiesta, MemoriaBiglietti memoria,
                                    BancaServiceClient banca, EventDispatcher dispatcher) {
        this.richiesta = richiesta;
        this.memoria = memoria;
        this.banca = banca;
        this.dispatcher = dispatcher;
    }

    @Override
    public RispostaDTO esegui(RichiestaDTO r) {
        BigliettoDTO bigliettoPrenotato = richiesta.getBiglietto();

        // Rimuove la prenotazione (se presente)
        memoria.rimuoviBiglietto(bigliettoPrenotato.getId());

        // Comunica con la banca
        boolean pagato = banca.paga(
                bigliettoPrenotato.getCliente().toString(),
                bigliettoPrenotato.getPrezzoEffettivo(),
                "Pagamento conferma biglietto"
        );

        if (!pagato) {
            return new RispostaDTO("KO", "❌ Pagamento non riuscito", null);
        }

        // Crea biglietto definitivo
        Biglietto confermato = new Biglietto.Builder()
                .idCliente(bigliettoPrenotato.getCliente().getId())
                .idTratta(bigliettoPrenotato.getTratta().getId())
                .classe(bigliettoPrenotato.getClasseServizio())
                .prezzoPagato(bigliettoPrenotato.getPrezzoEffettivo())
                .dataAcquisto(LocalDate.now())
                .tipoAcquisto("acquisto")
                .build();

        memoria.aggiungiBiglietto(confermato);
        dispatcher.dispatch(new EventoGdsAcquisto(confermato));

        return new RispostaDTO("OK", "✅ Biglietto confermato", confermato);
    }
}