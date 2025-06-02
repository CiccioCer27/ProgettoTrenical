package grpc;

import dto.PromozioneDTO;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import eventi.EventoPromozione;
import eventi.ListaEventi;
import grpc.TrenicalServiceGrpc.TrenicalServiceImplBase;
import io.grpc.stub.StreamObserver;
import observer.NotificaDispatcher;
import command.ServerRequestHandler;
import persistence.MemoriaPromozioni;
import util.GrpcMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 🔄 TRENICAL SERVICE IMPL - AGGIORNATO per Broadcast Promozioni Reali
 *
 * NUOVO: Supporto per broadcast promozioni create da admin console
 * MIGLIORAMENTO: Metodi pubblici per integrazione con eventi server
 */
public class TrenicalServiceImpl extends TrenicalServiceImplBase {

    private final NotificaDispatcher notificaDispatcher;
    private final ServerRequestHandler requestHandler;
    private final MemoriaPromozioni memoriaPromozioni;

    // Stream management
    private final ConcurrentHashMap<String, StreamObserver<PromozioneGrpc>> promozioniStreams = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentHashMap<String, StreamObserver<NotificaTrattaGrpc>> notificheStreams = new ConcurrentHashMap<>();

    // ✅ COSTRUTTORE PRINCIPALE (usato da ServerMain)
    public TrenicalServiceImpl(NotificaDispatcher notificaDispatcher,
                               ServerRequestHandler requestHandler,
                               MemoriaPromozioni memoriaPromozioni) {
        this.notificaDispatcher = notificaDispatcher;
        this.requestHandler = requestHandler;
        this.memoriaPromozioni = memoriaPromozioni;

        // Avvia simulatore solo per demo/testing
        //avviaSimulatorePromozioni();
    }

    // ✅ COSTRUTTORE BACKWARD-COMPATIBLE
    public TrenicalServiceImpl(NotificaDispatcher notificaDispatcher,
                               ServerRequestHandler requestHandler) {
        this(notificaDispatcher, requestHandler, new MemoriaPromozioni());
    }

    /**
     * ✅ Gestisce le richieste principali (acquisto, prenotazione, ecc.)
     */
    @Override
    public void inviaRichiesta(RichiestaGrpc request, StreamObserver<RispostaGrpc> responseObserver) {
        try {
            System.out.println("📨 Ricevuta richiesta: " + request.getTipo() + " da cliente: " + request.getIdCliente());

            RichiestaDTO richiestaDTO = GrpcMapper.toDTO(request);
            RispostaDTO rispostaDTO = requestHandler.gestisci(richiestaDTO);
            RispostaGrpc rispostaGrpc = GrpcMapper.fromDTO(rispostaDTO);

            System.out.println("📤 Risposta inviata: " + rispostaDTO.getEsito() + " - " + rispostaDTO.getMessaggio());

            responseObserver.onNext(rispostaGrpc);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Errore validazione richiesta: " + e.getMessage());
            inviaErrore(responseObserver, "Parametri richiesta non validi: " + e.getMessage());

        } catch (Exception e) {
            System.err.println("❌ Errore interno durante elaborazione richiesta: " + e.getMessage());
            e.printStackTrace();
            inviaErrore(responseObserver, "Errore interno del server: " + e.getMessage());
        }
    }

    /**
     * ✅ Gestisce lo stream delle promozioni
     */
    @Override
    public void streamPromozioni(RichiestaPromozioni request, StreamObserver<PromozioneGrpc> responseObserver) {
        final String clientId = "client_" + System.currentTimeMillis();

        try {
            System.out.println("🎉 Nuovo client collegato per stream promozioni: " + clientId);

            // Registra lo stream
            promozioniStreams.put(clientId, responseObserver);

            // Invia le promozioni attive esistenti
            inviaPromozioniEsistenti(responseObserver);

            System.out.println("✅ Stream promozioni configurato per client: " + clientId);

        } catch (Exception e) {
            System.err.println("❌ Errore setup stream promozioni: " + e.getMessage());
            promozioniStreams.remove(clientId);
            responseObserver.onError(e);
        }
    }

    /**
     * ✅ Gestisce lo stream delle notifiche per tratte specifiche
     */
    @Override
    public void streamNotificheTratta(IscrizioneNotificheGrpc request, StreamObserver<NotificaTrattaGrpc> responseObserver) {
        final String clientKey = request.getEmailCliente() + "_" + request.getTrattaId();

        try {
            System.out.println("📡 Iscrizione notifiche tratta: " + request.getEmailCliente() +
                    " per tratta: " + request.getTrattaId());

            UUID trattaId = UUID.fromString(request.getTrattaId());

            notificheStreams.put(clientKey, responseObserver);
            notificaDispatcher.registraOsservatore(trattaId, responseObserver);

            NotificaTrattaGrpc conferma = NotificaTrattaGrpc.newBuilder()
                    .setMessaggio("✅ Iscrizione completata per la tratta " + request.getTrattaId())
                    .build();
            responseObserver.onNext(conferma);

            System.out.println("✅ Notifiche tratta configurate per: " + clientKey);

        } catch (IllegalArgumentException e) {
            System.err.println("❌ ID tratta non valido: " + request.getTrattaId());
            responseObserver.onError(new IllegalArgumentException("ID tratta non valido"));

        } catch (Exception e) {
            System.err.println("❌ Errore setup stream notifiche: " + e.getMessage());
            notificheStreams.remove(clientKey);
            responseObserver.onError(e);
        }
    }

    // ================================================================================
    // 🚀 METODI PUBBLICI per Broadcast Promozioni Reali (chiamati da NotificaEventiListener)
    // ================================================================================

    /**
     * ✅ METODO PUBBLICO - Broadcast promozione creata da admin console
     *
     * Chiamato da NotificaEventiListener quando riceve EventoPromoGen/EventoPromoFedelta
     */
    public void broadcastPromozione(PromozioneDTO promo) {
        if (promozioniStreams.isEmpty()) {
            System.out.println("⚠️ Nessun client connesso per ricevere promozioni");
            return;
        }

        System.out.println("📡 BROADCAST PROMOZIONE REALE: " + promo.getNome());

        PromozioneGrpc promoGrpc = PromozioneGrpc.newBuilder()
                .setNome(promo.getNome())
                .setDescrizione(promo.getDescrizione())
                .setDataInizio(promo.getDataInizio().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .setDataFine(promo.getDataFine().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        // Rimuovi connessioni morte e invia a quelle attive
        int clientiConnessi = promozioniStreams.size();
        promozioniStreams.entrySet().removeIf(entry -> {
            try {
                entry.getValue().onNext(promoGrpc);
                return false; // Mantieni la connessione
            } catch (Exception e) {
                System.err.println("❌ Rimossa connessione promozioni inattiva: " + entry.getKey());
                return true; // Rimuovi la connessione
            }
        });

        System.out.println("✅ Promozione REALE inviata a " + promozioniStreams.size() + "/" + clientiConnessi + " client");
    }

    /**
     * ✅ METODO PUBBLICO - Broadcast promozione per tratte specifiche
     */
    public void broadcastPromozioneTratta(PromozioneDTO promo, java.util.Set<UUID> tratteDestinate) {
        System.out.println("🚂 BROADCAST PROMOZIONE TRATTA: " + promo.getNome() +
                " per " + tratteDestinate.size() + " tratte");

        // Broadcast generale a tutti i client
        broadcastPromozione(promo);

        // TODO: Se in futuro vuoi notifiche tratta-specifiche,
        // puoi usare notificaDispatcher per inviare a client iscritti a tratte specifiche
    }

    /**
     * ✅ METODO PUBBLICO - Ottieni statistiche stream attivi
     */
    public String getStreamStats() {
        return String.format("📊 Stream Attivi: Promozioni=%d, Notifiche=%d",
                promozioniStreams.size(), notificheStreams.size());
    }

    // ================================================================================
    // 🔧 METODI PRIVATI di supporto
    // ================================================================================

    private void inviaErrore(StreamObserver<RispostaGrpc> responseObserver, String messaggio) {
        RispostaGrpc errore = RispostaGrpc.newBuilder()
                .setEsito("KO")
                .setMessaggio(messaggio)
                .build();

        try {
            responseObserver.onNext(errore);
            responseObserver.onCompleted();
        } catch (Exception e) {
            System.err.println("❌ Errore invio risposta di errore: " + e.getMessage());
        }
    }

    private void inviaPromozioniEsistenti(StreamObserver<PromozioneGrpc> responseObserver) {
        try {
            var promozioniAttive = memoriaPromozioni.getPromozioniAttive();

            for (var promo : promozioniAttive) {
                if (promo.isAttiva(java.time.LocalDate.now())) {
                    PromozioneGrpc promoGrpc = PromozioneGrpc.newBuilder()
                            .setNome(promo.getDescrizione())
                            .setDescrizione(promo.getDescrizione())
                            .setDataInizio(promo.getDataInizio().toString())
                            .setDataFine(promo.getDataFine().toString())
                            .build();

                    responseObserver.onNext(promoGrpc);
                }
            }

            System.out.println("📤 Inviate " + promozioniAttive.size() + " promozioni esistenti");

        } catch (Exception e) {
            System.err.println("❌ Errore invio promozioni esistenti: " + e.getMessage());
        }
    }

    /**
     * 🧪 SIMULATORE per testing (genera promozioni fittizie)
     */
    /*private void avviaSimulatorePromozioni() {
        // Simula nuove promozioni ogni 2 minuti (solo per demo)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!promozioniStreams.isEmpty()) {
                    generaPromozioneTest();
                }
            } catch (Exception e) {
                System.err.println("❌ Errore generazione promozione test: " + e.getMessage());
            }
        }, 60, 120, TimeUnit.SECONDS); // Ogni 2 minuti

        System.out.println("🧪 Simulatore promozioni test avviato (ogni 2 min)");
    }

    private void generaPromozioneTest() {
        String[] nomiPromo = {
                "Test Sconto Estivo", "Test Offerta Weekend", "Test Promo Demo"
        };

        String nome = nomiPromo[(int) (Math.random() * nomiPromo.length)];
        LocalDateTime ora = LocalDateTime.now();

        PromozioneDTO promo = new PromozioneDTO(
                "[TEST] " + nome,
                "Promozione di test generata automaticamente",
                ora,
                ora.plusDays(3)
        );

        // Notifica sistema eventi client
        ListaEventi.getInstance().notifica(new EventoPromozione(promo));

        // Broadcast a client gRPC
        broadcastPromozione(promo);

        System.out.println("🧪 Promozione TEST generata: " + nome);
    }*/

    /**
     * ✅ Cleanup delle risorse quando il servizio viene fermato
     */
    public void shutdown() {
        System.out.println("🛑 Shutdown TrenicalService...");

        // Chiudi tutti gli stream attivi
        promozioniStreams.values().forEach(stream -> {
            try {
                stream.onCompleted();
            } catch (Exception e) {
                System.err.println("❌ Errore chiusura stream promozioni: " + e.getMessage());
            }
        });

        notificheStreams.values().forEach(stream -> {
            try {
                stream.onCompleted();
            } catch (Exception e) {
                System.err.println("❌ Errore chiusura stream notifiche: " + e.getMessage());
            }
        });

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("✅ TrenicalService shutdown completato");
    }
}