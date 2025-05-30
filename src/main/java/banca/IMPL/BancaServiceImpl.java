// src/main/java/banca/BancaServiceImpl.java
package IMPL;

import banca.grpc.BancaServiceGrpc;
import banca.grpc.RichiestaPagamento;
import banca.grpc.RispostaPagamento;
import io.grpc.stub.StreamObserver;

public class BancaServiceImpl extends BancaServiceGrpc.BancaServiceImplBase {

    @Override
    public void paga(RichiestaPagamento request, StreamObserver<RispostaPagamento> responseObserver) {
        System.out.println("✅ Pagamento ricevuto da cliente " + request.getIdCliente() +
                " per €" + request.getImporto() + " | Causale: " + request.getCausale());

        RispostaPagamento risposta = RispostaPagamento.newBuilder()
                .setSuccesso(true)
                .setMessaggio("Pagamento accettato")
                .build();

        responseObserver.onNext(risposta);
        responseObserver.onCompleted();
    }
}