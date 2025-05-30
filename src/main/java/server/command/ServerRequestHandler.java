package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import command.*;
import persistence.*;
import observer.EventDispatcher;
import service.BancaServiceClient;

public class ServerRequestHandler {

    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaClientiFedeli memoriaClienti;
    private final MemoriaTratte memoriaTratte;
    private final EventDispatcher dispatcher;
    private final BancaServiceClient banca;

    public ServerRequestHandler(MemoriaBiglietti mb, MemoriaClientiFedeli mc, MemoriaTratte mt,
                                EventDispatcher dispatcher, BancaServiceClient banca) {
        this.memoriaBiglietti = mb;
        this.memoriaClienti = mc;
        this.memoriaTratte = mt;
        this.dispatcher = dispatcher;
        this.banca = banca;
    }

    public RispostaDTO gestisci(RichiestaDTO richiesta) {
        String tipo = richiesta.getTipo().toUpperCase();

        ServerCommand comando = switch (tipo) {
            case "ACQUISTA" -> new AcquistaBigliettoCommand(richiesta,memoriaBiglietti, memoriaClienti, memoriaTratte, dispatcher, banca);
            case "PRENOTA" -> new PrenotaBigliettoCommand(richiesta,memoriaBiglietti, memoriaTratte, memoriaClienti, dispatcher);
            case "MODIFICA" -> new ModificaBigliettoCommand(richiesta, memoriaBiglietti,memoriaClienti,memoriaTratte, banca, dispatcher);
            case "CONFERMA" -> new ConfermaBigliettoCommand(richiesta, memoriaBiglietti, banca,dispatcher);
            case "CARTA_FEDELTA" -> new CartaFedeltaCommand(richiesta, memoriaClienti, dispatcher,banca);
            case "RICERCA_TRATTE" -> new FiltraTratteCommand(richiesta,memoriaTratte);
            default -> new ComandoErrore("❌ Tipo comando non riconosciuto: " + tipo);
        };

        return comando.esegui(richiesta);
    }
}