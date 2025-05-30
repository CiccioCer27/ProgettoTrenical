package grpc;

import grpc.NotificaTrattaGrpc;
import grpc.TrenicalServiceGrpc;
import grpc.IscrizioneNotificheGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class NotificaTrattaGrpcListener {

    private final TrenicalServiceGrpc.TrenicalServiceStub stub;

    public NotificaTrattaGrpcListener(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // solo per dev!
                .build();
        this.stub = TrenicalServiceGrpc.newStub(channel);
    }

    public void iscrivi(String emailCliente, String trattaId) {
        IscrizioneNotificheGrpc richiesta = IscrizioneNotificheGrpc.newBuilder()
                .setEmailCliente(emailCliente)
                .setTrattaId(trattaId)
                .build();

        stub.streamNotificheTratta(richiesta, new StreamObserver<>() {
            @Override
            public void onNext(NotificaTrattaGrpc notifica) {
                System.out.println("📢 Notifica ricevuta: " + notifica.getMessaggio());
                // Qui puoi aggiornare GUI o inoltrare a un listener interno
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Errore ricezione notifiche: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("❗️ Stream notifiche completato");
            }
        });
    }
}