package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import persistence.MemoriaClientiFedeli;
import service.BancaServiceClient;

import java.util.UUID;

/**
 * 🔒 CARTA FEDELTÀ THREAD-SAFE
 */
public class CartaFedeltaCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaClientiFedeli memoria;
    private final BancaServiceClient banca;

    public CartaFedeltaCommand(RichiestaDTO richiesta,
                               MemoriaClientiFedeli memoria,
                               BancaServiceClient banca) {
        this.richiesta = richiesta;
        this.memoria = memoria;
        this.banca = banca;
    }

    @Override
    public RispostaDTO esegui() {
        System.out.println("🔍 DEBUG CARTA FEDELTÀ THREAD-SAFE");

        UUID idCliente = UUID.fromString(richiesta.getIdCliente());

        // Controllo se già fedele
        if (memoria.isClienteFedele(idCliente)) {
            return new RispostaDTO("KO", "Hai già una carta fedeltà attiva", null);
        }

        // Pagamento
        boolean pagato = banca.paga(idCliente.toString(), 10.0, "Acquisto carta fedeltà");
        if (!pagato) {
            return new RispostaDTO("KO", "Pagamento carta fedeltà fallito", null);
        }

        // Registra come fedele
        memoria.registraClienteFedele(idCliente);

        System.out.println("✅ DEBUG CARTA FEDELTÀ: Cliente registrato come fedele");

        return new RispostaDTO("OK", "Carta fedeltà acquistata con successo", null);
    }
}