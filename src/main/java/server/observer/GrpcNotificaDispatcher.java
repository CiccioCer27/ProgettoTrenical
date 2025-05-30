package observer;

import grpc.NotificaTrattaGrpc;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;

public class GrpcNotificaDispatcher implements NotificaDispatcher {

    // Usa una mappa concorrente + liste sicure per lettura/scrittura
    private final Map<UUID, List<StreamObserver<NotificaTrattaGrpc>>> observers = new ConcurrentHashMap<>();

    @Override
    public void registraOsservatore(UUID idTratta, StreamObserver<NotificaTrattaGrpc> observer) {
        observers.computeIfAbsent(idTratta, k -> new CopyOnWriteArrayList<>()).add(observer);
    }

    @Override
    public void inviaNotifica(UUID idTratta, String messaggio) {
        NotificaTrattaGrpc notifica = NotificaTrattaGrpc.newBuilder()
                .setMessaggio(messaggio)
                .build();

        List<StreamObserver<NotificaTrattaGrpc>> lista = observers.getOrDefault(idTratta, List.of());

        for (StreamObserver<NotificaTrattaGrpc> obs : lista) {
            try {
                obs.onNext(notifica);
            } catch (Exception e) {
                System.err.println("❌ Errore durante l'invio della notifica al client: " + e.getMessage());
            }
        }
    }
}