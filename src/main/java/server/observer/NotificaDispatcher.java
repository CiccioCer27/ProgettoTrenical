package observer;

import grpc.NotificaTrattaGrpc;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public interface NotificaDispatcher {
    void inviaNotifica(UUID trattaId, String messaggio);
    void registraOsservatore(UUID trattaId, StreamObserver<NotificaTrattaGrpc> observer);
}