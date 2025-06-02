package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import command.*;
import persistence.*;
import service.BancaServiceClient;

/**
 * 🔒 SERVER REQUEST HANDLER THREAD-SAFE
 *
 * Versione semplificata che rimuove l'EventDispatcher
 * per eliminare le race conditions nel sistema eventi.
 */
public class ServerRequestHandler {

    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaClientiFedeli memoriaClienti;
    private final MemoriaTratte memoriaTratte;
    private final BancaServiceClient banca;

    public ServerRequestHandler(MemoriaBiglietti mb, MemoriaClientiFedeli mc, MemoriaTratte mt,
                                BancaServiceClient banca) {
        this.memoriaBiglietti = mb;
        this.memoriaClienti = mc;
        this.memoriaTratte = mt;
        this.banca = banca;
    }

    public RispostaDTO gestisci(RichiestaDTO richiesta) {
        String tipo = richiesta.getTipo().toUpperCase();

        System.out.println("🔍 DEBUG HANDLER THREAD-SAFE: " + tipo);

        try {
            ServerCommand comando = switch (tipo) {
                case "ACQUISTA" -> {
                    System.out.println("✅ DEBUG: Creando AcquistaBigliettoCommand THREAD-SAFE");
                    yield new AcquistaBigliettoCommand(richiesta, memoriaBiglietti, memoriaClienti, memoriaTratte, banca);
                }
                case "PRENOTA" -> {
                    System.out.println("✅ DEBUG: Creando PrenotaBigliettoCommand THREAD-SAFE");
                    yield new PrenotaBigliettoCommand(richiesta, memoriaBiglietti, memoriaTratte, memoriaClienti);
                }
                case "MODIFICA" -> {
                    System.out.println("✅ DEBUG: Creando ModificaBigliettoCommand THREAD-SAFE");
                    yield new ModificaBigliettoCommand(richiesta, memoriaBiglietti, memoriaClienti, memoriaTratte, banca);
                }
                case "CONFERMA" -> {
                    System.out.println("✅ DEBUG: Creando ConfermaBigliettoCommand THREAD-SAFE");
                    yield new ConfermaBigliettoCommand(richiesta, memoriaBiglietti, banca);
                }
                case "CARTA_FEDELTA" -> {
                    System.out.println("✅ DEBUG: Creando CartaFedeltaCommand THREAD-SAFE");
                    yield new CartaFedeltaCommand(richiesta, memoriaClienti, banca);
                }
                case "RICERCA_TRATTE", "FILTRA" -> {
                    System.out.println("✅ DEBUG: Creando FiltraTratteCommand");
                    yield new FiltraTratteCommand(richiesta, memoriaTratte);
                }
                default -> {
                    System.out.println("❌ DEBUG: Tipo comando non riconosciuto: " + tipo);
                    yield new ComandoErrore("❌ Tipo comando non riconosciuto: " + tipo);
                }
            };

            System.out.println("🚀 DEBUG: Eseguendo comando THREAD-SAFE: " + comando.getClass().getSimpleName());

            RispostaDTO risposta = comando.esegui(richiesta);

            System.out.println("📋 DEBUG: Comando completato");
            System.out.println("   Esito: " + risposta.getEsito());
            System.out.println("   Ha Biglietto: " + (risposta.getBiglietto() != null));

            return risposta;

        } catch (Exception e) {
            System.err.println("❌ DEBUG: Errore durante esecuzione: " + e.getMessage());
            e.printStackTrace();
            return new RispostaDTO("KO", "Errore interno del server: " + e.getMessage(), null);
        }
    }
}