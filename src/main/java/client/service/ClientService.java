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

import java.util.UUID;

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

    // ✅ Attivazione una tantum
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

    // ✅ Accesso sicuro centralizzato
    public ClienteDTO getCliente() {
        checkClienteAttivo();
        return cliente;
    }

    private void checkClienteAttivo() {
        if (cliente == null) {
            throw new IllegalStateException("⚠️ Cliente non attivo. Devi prima registrarti.");
        }
    }

    // ✅ Invio richieste classiche
    public RispostaDTO inviaRichiesta(RichiestaDTO richiestaDTO) {
        checkClienteAttivo();

        RichiestaGrpc grpcRequest = RichiestaGrpc.newBuilder()
                .setTipo(richiestaDTO.getTipo())
                .setIdCliente(richiestaDTO.getIdCliente())
                .setMessaggioExtra(
                        richiestaDTO.getMessaggioExtra() != null ? richiestaDTO.getMessaggioExtra() : ""
                )
                .build();

        RispostaGrpc grpcResponse = stub.inviaRichiesta(grpcRequest);

        return new RispostaDTO(
                grpcResponse.getEsito(),
                grpcResponse.getMessaggio(),
                null
        );
    }

    // ✅ Notifiche tratta
    public void avviaNotificheTratta(TrattaDTO tratta) {
        checkClienteAttivo();

        IscrizioneNotificheGrpc richiesta = IscrizioneNotificheGrpc.newBuilder()
                .setEmailCliente(cliente.getEmail())
                .setTrattaId(tratta.getId().toString())
                .build();

        asyncStub.streamNotificheTratta(richiesta, new StreamObserver<NotificaTrattaGrpc>() {
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