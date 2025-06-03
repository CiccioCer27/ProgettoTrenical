package grpc;

import dto.PromozioneDTO;
import eventi.EventoPromozione;
import eventi.ListaEventi;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import grpc.TrenicalServiceGrpc;
import grpc.RichiestaPromozioni;
import grpc.PromozioneGrpc;

public class PromozioneGrpcListener {

    private final ManagedChannel channel;
    private final TrenicalServiceGrpc.TrenicalServiceStub asyncStub;

    public PromozioneGrpcListener(String host, int port) {
        System.out.println("🔧 DEBUG: Inizializzando PromozioneGrpcListener per " + host + ":" + port);

        this.channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        this.asyncStub = TrenicalServiceGrpc.newStub(channel);
        System.out.println("✅ DEBUG: Stub gRPC creato con successo");
    }

    public void avviaStreamPromozioni() {
        System.out.println("🚀 DEBUG: Avviando stream promozioni...");

        RichiestaPromozioni richiesta = RichiestaPromozioni.newBuilder().build();

        asyncStub.streamPromozioni(richiesta, new StreamObserver<PromozioneGrpc>() {
            @Override
            public void onNext(PromozioneGrpc grpcPromo) {
                System.out.println("📥 DEBUG: Promozione gRPC ricevuta dal server");
                System.out.println("   Nome: " + grpcPromo.getNome());
                System.out.println("   Descrizione: " + grpcPromo.getDescrizione());
                System.out.println("   Data Inizio Raw: '" + grpcPromo.getDataInizio() + "'");
                System.out.println("   Data Fine Raw: '" + grpcPromo.getDataFine() + "'");

                try {
                    // ✅ FIX: Parsing intelligente delle date
                    LocalDateTime dataInizio = parseDataIntelligente(grpcPromo.getDataInizio());
                    LocalDateTime dataFine = parseDataIntelligente(grpcPromo.getDataFine());

                    System.out.println("✅ DEBUG: Date parsate con successo");
                    System.out.println("   Data Inizio: " + dataInizio);
                    System.out.println("   Data Fine: " + dataFine);

                    PromozioneDTO promozione = new PromozioneDTO(
                            grpcPromo.getNome(),
                            grpcPromo.getDescrizione(),
                            dataInizio,
                            dataFine
                    );

                    System.out.println("📥 Promozione ricevuta: " + promozione);

                    // ✅ Genera evento per WalletPromozioni
                    ListaEventi.getInstance().notifica(new EventoPromozione(promozione));
                    System.out.println("🔔 DEBUG: Evento EventoPromozione generato e notificato");

                } catch (Exception e) {
                    System.err.println("❌ DEBUG: Errore conversione promozione: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("❌ Errore durante lo stream promozioni: " + t.getMessage());
                System.err.println("🔧 DEBUG: Dettagli errore: " + t.getClass().getSimpleName());
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("✅ Stream promozioni chiuso dal server.");
            }
        });

        System.out.println("✅ DEBUG: Stream promozioni configurato e attivo");
    }

    /**
     * ✅ NUOVO: Parsing intelligente che gestisce sia LocalDate che LocalDateTime
     */
    private LocalDateTime parseDataIntelligente(String dataString) {
        if (dataString == null || dataString.trim().isEmpty()) {
            return LocalDateTime.now();
        }

        dataString = dataString.trim();

        try {
            // Strategia 1: Prova parsing come LocalDateTime (formato: 2025-06-02T10:30:00)
            if (dataString.contains("T")) {
                return LocalDateTime.parse(dataString);
            }

            // Strategia 2: Parsing come LocalDate e conversione (formato: 2025-06-02)
            if (dataString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDate data = LocalDate.parse(dataString);
                return data.atStartOfDay(); // Converte a LocalDateTime alle 00:00:00
            }

            // Strategia 3: Prova con formatter ISO
            return LocalDateTime.parse(dataString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        } catch (Exception e) {
            System.err.println("⚠️ DEBUG: Fallback parsing per data: '" + dataString + "'");
            // Fallback: usa data corrente
            return LocalDateTime.now();
        }
    }

    public void chiudi() {
        System.out.println("🛑 DEBUG: Chiudendo PromozioneGrpcListener...");
        channel.shutdown();
    }
}