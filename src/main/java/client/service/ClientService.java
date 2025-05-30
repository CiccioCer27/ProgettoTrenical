package service;

import Assembler.AssemblerCliente;
import dto.ClienteDTO;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import util.GrpcMapper;

public class ClientService {

    private final TrenicalServiceGrpc.TrenicalServiceBlockingStub stub;
    private final TrenicalServiceGrpc.TrenicalServiceStub asyncStub;

    private ClienteDTO cliente;

    public ClientService(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = TrenicalServiceGrpc.newBlockingStub(channel);
        this.asyncStub = TrenicalServiceGrpc.newStub(channel);
    }

    public void attivaCliente(String nome, String cognome, String email,
                              int eta, String residenza, String cellulare) {
        var clienteModel = new model.Cliente.Builder(nome, cognome, email)
                .eta(eta)
                .residenza(residenza)
                .cellulare(cellulare)
                .build();
        this.cliente = AssemblerCliente.toDTO(clienteModel);
        System.out.println("✅ Cliente attivato: " + cliente.getNome() + " (" + cliente.getId() + ")");
    }

    public ClienteDTO getCliente() {
        checkClienteAttivo();
        return cliente;
    }

    private void checkClienteAttivo() {
        if (cliente == null) {
            throw new IllegalStateException("⚠️ Cliente non attivo. Devi prima registrarti.");
        }
    }

    public RispostaDTO inviaRichiesta(RichiestaDTO richiestaDTO) {
        checkClienteAttivo();
        RichiestaGrpc grpcRequest = GrpcMapper.toGrpc(richiestaDTO);
        RispostaGrpc grpcResponse = stub.inviaRichiesta(grpcRequest);
        return GrpcMapper.fromGrpc(grpcResponse);
    }

    public void avviaNotificheTratta(TrattaDTO tratta) {
        checkClienteAttivo();

        IscrizioneNotificheGrpc richiesta = IscrizioneNotificheGrpc.newBuilder()
                .setEmailCliente(cliente.getEmail())
                .setTrattaId(tratta.getId().toString())
                .build();

        asyncStub.streamNotificheTratta(richiesta, new StreamObserver<>() {
            @Override
            public void onNext(NotificaTrattaGrpc value) {
                System.out.println("📢 Notifica tratta ricevuta: " + value.getMessaggio());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("❌ Errore ricezione notifiche tratta: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("🔚 Stream notifiche tratta completato.");
            }
        });
    }
}