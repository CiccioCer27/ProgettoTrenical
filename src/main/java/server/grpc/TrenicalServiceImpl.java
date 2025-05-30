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
 * Implementazione completa del servizio gRPC TreniCal
 * Gestisce tutte le operazioni: richieste, notifiche tratte e stream promozioni
 */
public class TrenicalServiceImpl extends TrenicalServiceImplBase {

    private final NotificaDispatcher notificaDispatcher;
    private final ServerRequestHandler requestHandler;
    private final MemoriaPromozioni memoriaPromozioni;

    // ✅ Gestione stream promozioni
    private final ConcurrentHashMap<String, StreamObserver<PromozioneGrpc>> promozioniStreams = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // ✅ Gestione stream notifiche tratte
    private final ConcurrentHashMap<String, StreamObserver<NotificaTrattaGrpc>> notificheStreams = new ConcurrentHashMap<>();

    public TrenicalServiceImpl(NotificaDispatcher notificaDispatcher,
                               ServerRequestHandler requestHandler,
                               MemoriaPromozioni memoriaPromozioni) {
        this.notificaDispatcher = notificaDispatcher;
        this.requestHandler = requestHandler;
        this.memoriaPromozioni = memoriaPromozioni;

        // ✅ Avvia il simulatore di promozioni
        avviaSimulatorePromozioni();
    }

    // ✅ Costruttore semplificato per compatibilità
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

            // ✅ Converte la richiesta gRPC in DTO
            RichiestaDTO richiestaDTO = GrpcMapper.toDTO(request);

            // ✅ Processa la richiesta tramite il handler
            RispostaDTO rispostaDTO = requestHandler.gestisci(richiestaDTO);

            // ✅ Converte la risposta DTO in gRPC
            RispostaGrpc rispostaGrpc = GrpcMapper.fromDTO(rispostaDTO);

            System.out.println("📤 Risposta inviata: " + rispostaDTO.getEsito() + " - " + rispostaDTO.getMessaggio());

            // ✅ Invia la risposta al client
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

            // ✅ Registra lo stream
            promozioniStreams.put(clientId, responseObserver);

            // ✅ Invia le promozioni attive esistenti
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

            // ✅ Valida l'UUID della tratta
            UUID trattaId = UUID.fromString(request.getTrattaId());

            // ✅ Registra il client per le notifiche della tratta
            notificheStreams.put(clientKey, responseObserver);
            notificaDispatcher.registraOsservatore(trattaId, responseObserver);

            // ✅ Invia messaggio di conferma iscrizione
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

    /**
     * ✅ Metodo helper per inviare errori in modo consistente
     */
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

    /**
     * ✅ Invia le promozioni attualmente attive al nuovo client
     */
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
     * ✅ Simulatore di promozioni per testing
     */
    private void avviaSimulatorePromozioni() {
        // ✅ Simula nuove promozioni ogni 60 secondi
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!promozioniStreams.isEmpty()) {
                    generaPromozioneTest();
                }
            } catch (Exception e) {
                System.err.println("❌ Errore generazione promozione test: " + e.getMessage());
            }
        }, 30, 60, TimeUnit.SECONDS);

        System.out.println("✅ Simulatore promozioni avviato (ogni 60s)");
    }

    /**
     * ✅ Genera una promozione di test
     */
    private void generaPromozioneTest() {
        String[] nomiPromo = {
                "Sconto Estivo", "Offerta Weekend", "Promo Fedeltà",
                "Super Risparmio", "Viaggio Conveniente"
        };

        String nome = nomiPromo[(int) (Math.random() * nomiPromo.length)];
        LocalDateTime ora = LocalDateTime.now();

        PromozioneDTO promo = new PromozioneDTO(
                nome,
                "Sconto speciale del " + (10 + (int)(Math.random() * 30)) + "%",
                ora,
                ora.plusDays(7)
        );

        // ✅ Notifica tramite sistema eventi
        ListaEventi.getInstance().notifica(new EventoPromozione(promo));

        // ✅ Invia a tutti i client collegati allo stream
        broadcastPromozione(promo);

        System.out.println("🎉 Promozione generata: " + nome);
    }

    /**
     * ✅ Invia una promozione a tutti i client collegati
     */
    public void broadcastPromozione(PromozioneDTO promo) {
        if (promozioniStreams.isEmpty()) {
            return;
        }

        PromozioneGrpc promoGrpc = PromozioneGrpc.newBuilder()
                .setNome(promo.getNome())
                .setDescrizione(promo.getDescrizione())
                .setDataInizio(promo.getDataInizio().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .setDataFine(promo.getDataFine().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        // ✅ Rimuovi connessioni morte e invia a quelle attive
        promozioniStreams.entrySet().removeIf(entry -> {
            try {
                entry.getValue().onNext(promoGrpc);
                return false; // Mantieni la connessione
            } catch (Exception e) {
                System.err.println("❌ Rimossa connessione promozioni inattiva: " + entry.getKey());
                return true; // Rimuovi la connessione
            }
        });

        System.out.println("📡 Promozione inviata a " + promozioniStreams.size() + " client");
    }

    /**
     * ✅ Cleanup delle risorse quando il servizio viene fermato
     */
    public void shutdown() {
        System.out.println("🛑 Shutdown TrenicalService...");

        // ✅ Chiudi tutti gli stream attivi
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

        // ✅ Ferma lo scheduler
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

    /**
     * ✅ Statistiche del servizio
     */
    public String getStats() {
        return String.format("📊 TrenicalService Stats:\n" +
                        "   - Stream promozioni attivi: %d\n" +
                        "   - Stream notifiche attivi: %d\n" +
                        "   - Scheduler running: %s",
                promozioniStreams.size(),
                notificheStreams.size(),
                !scheduler.isShutdown());
    }
}