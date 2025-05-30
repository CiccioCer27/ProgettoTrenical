package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import eventi.EventoAcquistoCartaFed;
import observer.EventDispatcher;
import persistence.MemoriaClientiFedeli;
import service.BancaServiceClient;

import java.util.UUID;

public class CartaFedeltaCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaClientiFedeli memoria;
    private final EventDispatcher dispatcher;
    private final BancaServiceClient banca;

    public CartaFedeltaCommand(RichiestaDTO richiesta,
                               MemoriaClientiFedeli memoria,
                               EventDispatcher dispatcher,
                               BancaServiceClient banca) {
        this.richiesta = richiesta;
        this.memoria = memoria;
        this.dispatcher = dispatcher;
        this.banca = banca;
    }

    @Override
    public RispostaDTO esegui(RichiestaDTO ignored) {
        UUID idCliente = UUID.fromString(richiesta.getIdCliente());

        // 🔍 Controlla se è già fedele
        if (memoria.isClienteFedele(idCliente)) {
            return new RispostaDTO("KO", "Hai già una carta fedeltà attiva", null);
        }

        // 💳 Pagamento (es. 10€)
        boolean pagato = banca.paga(idCliente.toString(), 10.0, "Acquisto carta fedeltà");
        if (!pagato) {
            return new RispostaDTO("KO", "Pagamento carta fedeltà fallito", null);
        }

        // ✅ Registra come fedele
        memoria.registraClienteFedele(idCliente);
        dispatcher.dispatch(new EventoAcquistoCartaFed(idCliente));

        return new RispostaDTO("OK", "Carta fedeltà acquistata con successo", null);
    }
}