package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import command.*;
import persistence.*;
import service.BancaServiceClient;

/**
 * 🔒 SERVER REQUEST HANDLER THREAD-SAFE - OBSERVER REFACTORED
 *
 * DESIGN DECISION: Rimosso EventDispatcher complesso per persistenza core.
 *
 * RATIONALE:
 * - Commands si prendono responsabilità diretta della persistenza (atomicità)
 * - Observer Pattern mantenuto solo per cross-cutting concerns via ListaEventiS
 * - Eliminata duplicazione di responsabilità Command ↔ Observer
 * - Ridotte race conditions e complessità architettuale
 */
public class ServerRequestHandler {

    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaClientiFedeli memoriaClienti;
    private final MemoriaTratte memoriaTratte;
    private final BancaServiceClient banca;

    /**
     * Constructor SEMPLIFICATO - Senza EventDispatcher per persistenza
     *
     * I Command ricevono solo le dipendenze necessarie per le loro responsabilità core.
     * Gli eventi per notifiche cross-domain sono gestiti via ListaEventiS quando necessario.
     */
    public ServerRequestHandler(MemoriaBiglietti mb, MemoriaClientiFedeli mc, MemoriaTratte mt,
                                BancaServiceClient banca) {
        this.memoriaBiglietti = mb;
        this.memoriaClienti = mc;
        this.memoriaTratte = mt;
        this.banca = banca;
    }

    /**
     * Gestisce le richieste creando i Command appropriati
     *
     * NOTA: Command Pattern con responsabilità diretta per persistenza.
     * Observer events generati solo quando necessario per notifiche cross-domain.
     */
    public RispostaDTO gestisci(RichiestaDTO richiesta) {
        String tipo = richiesta.getTipo().toUpperCase();

        System.out.println("🔍 DEBUG HANDLER THREAD-SAFE (Observer Refactored): " + tipo);

        try {
            ServerCommand comando = switch (tipo) {
                case "ACQUISTA" -> {
                    System.out.println("✅ DEBUG: Creando AcquistaBigliettoCommand THREAD-SAFE");
                    // ✅ REFACTORED: Senza EventDispatcher - Command ha responsabilità diretta
                    yield new AcquistaBigliettoCommand(
                            richiesta, memoriaBiglietti, memoriaClienti, memoriaTratte, banca
                    );
                }
                case "PRENOTA" -> {
                    System.out.println("✅ DEBUG: Creando PrenotaBigliettoCommand THREAD-SAFE");
                    // ✅ REFACTORED: Senza EventDispatcher - Command ha responsabilità diretta
                    yield new PrenotaBigliettoCommand(
                            richiesta, memoriaBiglietti, memoriaTratte, memoriaClienti
                    );
                }
                case "MODIFICA" -> {
                    System.out.println("✅ DEBUG: Creando ModificaBigliettoCommand THREAD-SAFE");
                    // ✅ REFACTORED: Senza EventDispatcher - Command ha responsabilità diretta
                    yield new ModificaBigliettoCommand(
                            richiesta, memoriaBiglietti, memoriaClienti, memoriaTratte, banca
                    );
                }
                case "CONFERMA" -> {
                    System.out.println("✅ DEBUG: Creando ConfermaBigliettoCommand THREAD-SAFE");
                    // ✅ REFACTORED: Senza EventDispatcher - Command ha responsabilità diretta
                    yield new ConfermaBigliettoCommand(richiesta, memoriaBiglietti, banca);
                }
                case "CARTA_FEDELTA" -> {
                    System.out.println("✅ DEBUG: Creando CartaFedeltaCommand THREAD-SAFE");
                    // ✅ REFACTORED: Senza EventDispatcher - Command ha responsabilità diretta
                    yield new CartaFedeltaCommand(richiesta, memoriaClienti, banca);
                }
                case "RICERCA_TRATTE", "FILTRA" -> {
                    System.out.println("✅ DEBUG: Creando FiltraTratteCommand");
                    // ✅ Query command - nessuna persistenza necessaria
                    yield new FiltraTratteCommand(richiesta, memoriaTratte);
                }
                default -> {
                    System.out.println("❌ DEBUG: Tipo comando non riconosciuto: " + tipo);
                    yield new ComandoErrore("❌ Tipo comando non riconosciuto: " + tipo);
                }
            };

            System.out.println("🚀 DEBUG: Eseguendo comando THREAD-SAFE: " + comando.getClass().getSimpleName());
            System.out.println("🎯 ARCHITETTURA: Command con responsabilità diretta (no observer persistenza)");

            RispostaDTO risposta = comando.esegui();

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

    /**
     * Metodo di utilità per diagnostica
     */
    public String getStatistiche() {
        return String.format("Handler Stats: Biglietti=%d, Tratte=%d, ClientiFedeli=%s",
                memoriaBiglietti.getTuttiIBiglietti().size(),
                memoriaTratte.getTutteTratte().size(),
                "N/A" // MemoriaClientiFedeli non ha metodo getSize()
        );
    }
}