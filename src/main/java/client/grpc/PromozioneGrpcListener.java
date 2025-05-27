package grpc;

import dto.PromozioneDTO;
import eventi.EventoPromozione;
import eventi.ListaEventi;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.time.LocalDateTime;

import grpc.TrenicalServiceGrpc;
import grpc.RichiestaPromozioni;
import grpc.PromozioneGrpc;

public class PromozioneGrpcListener {

    private final ManagedChannel channel;
    private final TrenicalServiceGrpc.TrenicalServiceStub asyncStub;

    public PromozioneGrpcListener(String host, int port) {
        this.channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        this.asyncStub = TrenicalServiceGrpc.newStub(channel);
    }

    public void avviaStreamPromozioni() {
        RichiestaPromozioni richiesta = RichiestaPromozioni.newBuilder().build();

        asyncStub.streamPromozioni(richiesta, new StreamObserver<PromozioneGrpc>() {
            @Override
            public void onNext(PromozioneGrpc grpcPromo) {
                PromozioneDTO promozione = new PromozioneDTO(
                        grpcPromo.getNome(),
                        grpcPromo.getDescrizione(),
                        LocalDateTime.parse(grpcPromo.getDataInizio()),
                        LocalDateTime.parse(grpcPromo.getDataFine())
                );

                System.out.println("📥 Promozione ricevuta: " + promozione);
                ListaEventi.getInstance().notifica(new EventoPromozione(promozione));
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("❌ Errore durante lo stream promozioni: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("✅ Stream promozioni chiuso dal server.");
            }
        });
    }

    public void chiudi() {
        channel.shutdown();
    }
}