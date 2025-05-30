package grpc;

import io.grpc.stub.StreamObserver;
import grpc.NotificaTrattaGrpc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrenicalServiceBroadcaster {

    private final Map<String, List<StreamObserver<NotificaTrattaGrpc>>> observers = new ConcurrentHashMap<>();

    public void registraOsservatore(String trattaId, StreamObserver<NotificaTrattaGrpc> observer) {
        observers.computeIfAbsent(trattaId, k -> new ArrayList<>()).add(observer);
    }

    public void invia(String trattaId, String messaggio) {
        NotificaTrattaGrpc notifica = NotificaTrattaGrpc.newBuilder().setMessaggio(messaggio).build();
        List<StreamObserver<NotificaTrattaGrpc>> lista = observers.getOrDefault(trattaId, List.of());
        for (StreamObserver<NotificaTrattaGrpc> o : lista) {
            o.onNext(notifica);
        }
    }
}