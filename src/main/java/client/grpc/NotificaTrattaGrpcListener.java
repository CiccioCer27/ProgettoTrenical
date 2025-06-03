package grpc;

import grpc.NotificaTrattaGrpc;
import grpc.TrenicalServiceGrpc;
import grpc.IscrizioneNotificheGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import observer.CentroAvvisi;

/**
 * 📡 NOTIFICA TRATTA gRPC LISTENER - INTEGRATO CON CENTRO AVVISI
 */
public class NotificaTrattaGrpcListener {

    private final TrenicalServiceGrpc.TrenicalServiceStub stub;
    private final CentroAvvisi centroAvvisi;

    public NotificaTrattaGrpcListener(String host, int port, CentroAvvisi centroAvvisi) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = TrenicalServiceGrpc.newStub(channel);
        this.centroAvvisi = centroAvvisi;

        System.out.println("📡 NotificaTrattaGrpcListener collegato al Centro Avvisi");
    }

    public void iscrivi(String emailCliente, String trattaId) {
        IscrizioneNotificheGrpc richiesta = IscrizioneNotificheGrpc.newBuilder()
                .setEmailCliente(emailCliente)
                .setTrattaId(trattaId)
                .build();

        stub.streamNotificheTratta(richiesta, new StreamObserver<>() {
            @Override
            public void onNext(NotificaTrattaGrpc notifica) {
                System.out.println("📡 Notifica tratta ricevuta: " + notifica.getMessaggio());
                processaNotificaTratta(notifica, trattaId);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("❌ Errore notifiche: " + t.getMessage());
                centroAvvisi.aggiungiAvvisoSistema(
                        "Errore Notifiche",
                        "Connessione interrotta: " + t.getMessage(),
                        CentroAvvisi.Priorita.MEDIA
                );
            }

            @Override
            public void onCompleted() {
                System.out.println("✅ Stream notifiche completato");
                centroAvvisi.aggiungiAvvisoSistema(
                        "Notifiche Disconnesse",
                        "Stream terminato per tratta " + trattaId.substring(0, 8),
                        CentroAvvisi.Priorita.BASSA
                );
            }
        });

        centroAvvisi.aggiungiConfermaOperazione(
                "Iscrizione notifiche tratta " + trattaId.substring(0, 8), true
        );
    }

    /**
     * ✅ ELABORAZIONE INTELLIGENTE DELLE NOTIFICHE
     */
    private void processaNotificaTratta(NotificaTrattaGrpc notifica, String trattaId) {
        String messaggio = notifica.getMessaggio();

        // ✅ ANALISI DEL TIPO DI NOTIFICA
        if (messaggio.toLowerCase().contains("binario")) {
            processaCambioBinario(messaggio, trattaId);
        } else if (messaggio.toLowerCase().contains("ritardo")) {
            processaRitardo(messaggio, trattaId);
        } else if (messaggio.toLowerCase().contains("cancell")) {
            processaCancellazione(messaggio, trattaId);
        } else if (messaggio.toLowerCase().contains("modifica")) {
            processaModificaGenerica(messaggio, trattaId);
        } else {
            // Notifica generica
            centroAvvisi.aggiungiNotificaTratta(
                    messaggio,
                    trattaId,
                    CentroAvvisi.Priorita.MEDIA
            );
        }
    }

    private void processaCambioBinario(String messaggio, String trattaId) {
        try {
            // Cerca pattern "binario X" nel messaggio
            String[] parti = messaggio.split(" ");
            String nuovoBinario = "N/A";

            for (int i = 0; i < parti.length - 1; i++) {
                if (parti[i].toLowerCase().contains("binario")) {
                    nuovoBinario = parti[i + 1];
                    break;
                }
            }

            centroAvvisi.aggiungiCambioBinario(trattaId, nuovoBinario, "precedente");

        } catch (Exception e) {
            // Fallback a notifica generica
            centroAvvisi.aggiungiNotificaTratta(
                    messaggio, trattaId, CentroAvvisi.Priorita.ALTA
            );
        }
    }

    private void processaRitardo(String messaggio, String trattaId) {
        try {
            // Cerca minuti di ritardo nel messaggio
            int minutiRitardo = estraiMinutiRitardo(messaggio);
            String nuovoOrario = estraiNuovoOrario(messaggio);

            centroAvvisi.aggiungiRitardo(trattaId, minutiRitardo, nuovoOrario);

        } catch (Exception e) {
            centroAvvisi.aggiungiNotificaTratta(
                    messaggio, trattaId, CentroAvvisi.Priorita.ALTA
            );
        }
    }

    private void processaCancellazione(String messaggio, String trattaId) {
        centroAvvisi.aggiungiNotificaTratta(
                "🚫 CANCELLAZIONE: " + messaggio,
                trattaId,
                CentroAvvisi.Priorita.CRITICA
        );
    }

    private void processaModificaGenerica(String messaggio, String trattaId) {
        centroAvvisi.aggiungiNotificaTratta(
                messaggio,
                trattaId,
                CentroAvvisi.Priorita.ALTA
        );
    }

    // ✅ UTILITY METHODS
    private int estraiMinutiRitardo(String messaggio) {
        try {
            // Cerca pattern numerico seguito da "minut"
            String[] parole = messaggio.split(" ");
            for (String parola : parole) {
                if (parola.matches("\\d+") && messaggio.toLowerCase().contains("minut")) {
                    return Integer.parseInt(parola);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return 10; // Default
    }

    private String estraiNuovoOrario(String messaggio) {
        try {
            // Cerca pattern HH:mm nel messaggio
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{1,2}:\\d{2}");
            java.util.regex.Matcher matcher = pattern.matcher(messaggio);
            if (matcher.find()) {
                return matcher.group();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "da definire";
    }
}