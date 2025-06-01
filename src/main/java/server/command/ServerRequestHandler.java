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

        System.out.println("🔍 DEBUG HANDLER: Ricevuta richiesta tipo: " + tipo);
        System.out.println("   ID Cliente: " + richiesta.getIdCliente());
        System.out.println("   Tratta: " + (richiesta.getTratta() != null ? richiesta.getTratta().getId() : "NULL"));
        System.out.println("   Classe: " + richiesta.getClasseServizio());
        System.out.println("   Tipo Prezzo: " + richiesta.getTipoPrezzo());

        try {
            ServerCommand comando = switch (tipo) {
                case "ACQUISTA" -> {
                    System.out.println("✅ DEBUG HANDLER: Creando AcquistaBigliettoCommand");
                    yield new AcquistaBigliettoCommand(richiesta, memoriaBiglietti, memoriaClienti, memoriaTratte, dispatcher, banca);
                }
                case "PRENOTA" -> {
                    System.out.println("✅ DEBUG HANDLER: Creando PrenotaBigliettoCommand");
                    yield new PrenotaBigliettoCommand(richiesta, memoriaBiglietti, memoriaTratte, memoriaClienti, dispatcher);
                }
                case "MODIFICA" -> {
                    System.out.println("✅ DEBUG HANDLER: Creando ModificaBigliettoCommand");
                    yield new ModificaBigliettoCommand(richiesta, memoriaBiglietti, memoriaClienti, memoriaTratte, banca, dispatcher);
                }
                case "CONFERMA" -> {
                    System.out.println("✅ DEBUG HANDLER: Creando ConfermaBigliettoCommand");
                    yield new ConfermaBigliettoCommand(richiesta, memoriaBiglietti, banca, dispatcher);
                }
                case "CARTA_FEDELTA" -> {
                    System.out.println("✅ DEBUG HANDLER: Creando CartaFedeltaCommand");
                    yield new CartaFedeltaCommand(richiesta, memoriaClienti, dispatcher, banca);
                }
                case "RICERCA_TRATTE", "FILTRA" -> {
                    System.out.println("✅ DEBUG HANDLER: Creando FiltraTratteCommand");
                    yield new FiltraTratteCommand(richiesta, memoriaTratte);
                }
                default -> {
                    System.out.println("❌ DEBUG HANDLER: Tipo comando non riconosciuto: " + tipo);
                    yield new ComandoErrore("❌ Tipo comando non riconosciuto: " + tipo);
                }
            };

            System.out.println("🚀 DEBUG HANDLER: Eseguendo comando: " + comando.getClass().getSimpleName());

            // ✅ Passa la richiesta ricevuta, non quella del costruttore
            RispostaDTO risposta = comando.esegui(richiesta);

            System.out.println("📋 DEBUG HANDLER: Comando completato");
            System.out.println("   Esito: " + risposta.getEsito());
            System.out.println("   Messaggio: " + risposta.getMessaggio());
            System.out.println("   Ha Biglietto: " + (risposta.getBiglietto() != null));
            System.out.println("   Ha Tratte: " + (risposta.getTratte() != null && !risposta.getTratte().isEmpty()));

            return risposta;

        } catch (Exception e) {
            System.err.println("❌ DEBUG HANDLER: Errore durante l'esecuzione del comando: " + e.getMessage());
            e.printStackTrace();
            return new RispostaDTO("KO", "Errore interno del server: " + e.getMessage(), null);
        }
    }
}