package grpc;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import grpc.TrenicalServiceGrpc.TrenicalServiceImplBase;
import io.grpc.stub.StreamObserver;
import observer.NotificaDispatcher;
import command.ServerRequestHandler;
import util.GrpcMapper;

import java.util.UUID;

public class TrenicalServiceImpl extends TrenicalServiceImplBase {

    private final NotificaDispatcher dispatcher;
    private final ServerRequestHandler handler;

    public TrenicalServiceImpl(NotificaDispatcher dispatcher, ServerRequestHandler handler) {
        this.dispatcher = dispatcher;
        this.handler = handler;
    }

    @Override
    public void streamNotificheTratta(IscrizioneNotificheGrpc request, StreamObserver<NotificaTrattaGrpc> responseObserver) {
        UUID idTratta = UUID.fromString(request.getTrattaId());
        dispatcher.registraOsservatore(idTratta, responseObserver);
    }

    @Override
    public void inviaRichiesta(RichiestaGrpc request, StreamObserver<RispostaGrpc> responseObserver) {
        try {
            RichiestaDTO richiestaDTO = GrpcMapper.toDTO(request);
            RispostaDTO rispostaDTO = handler.gestisci(richiestaDTO);
            RispostaGrpc rispostaGrpc = GrpcMapper.fromDTO(rispostaDTO);
            responseObserver.onNext(rispostaGrpc);
            responseObserver.onCompleted();
        } catch (Exception e) {
            RispostaGrpc errore = RispostaGrpc.newBuilder()
                    .setEsito("KO")
                    .setMessaggio("Errore interno: " + e.getMessage())
                    .build();
            responseObserver.onNext(errore);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void streamPromozioni(RichiestaPromozioni request, StreamObserver<PromozioneGrpc> responseObserver) {
        // eventualmente da implementare se servono stream promo
    }
}