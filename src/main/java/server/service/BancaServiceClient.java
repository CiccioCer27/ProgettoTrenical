// src/main/java/server/service/BancaServiceClient.java
package service;

import banca.grpc.BancaServiceGrpc;
import banca.grpc.RichiestaPagamento;
import banca.grpc.RispostaPagamento;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class BancaServiceClient {

    private final BancaServiceGrpc.BancaServiceBlockingStub stub;

    public BancaServiceClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = BancaServiceGrpc.newBlockingStub(channel);
    }

    public boolean paga(String idCliente, double importo, String causale) {
        RichiestaPagamento richiesta = RichiestaPagamento.newBuilder()
                .setIdCliente(idCliente)
                .setImporto(importo)
                .setCausale(causale)
                .build();

        RispostaPagamento risposta = stub.paga(richiesta);
        return risposta.getSuccesso(); // true in ogni caso per ora
    }
}