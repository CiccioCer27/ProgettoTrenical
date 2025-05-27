package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import service.ClientService;

public class AttivaCartaFedeltaCommand implements Command {

    private final ClientService clientService;

    public AttivaCartaFedeltaCommand(ClientService clientService) {
        this.clientService = clientService;
    }

    @Override
    public void esegui() {
        var cliente = clientService.getCliente();

        RichiestaDTO richiesta = new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(cliente.getId().toString())
                .build();

        System.out.println("➡️ Invio richiesta per attivazione carta fedeltà...");
        RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

        if ("OK".equalsIgnoreCase(risposta.getEsito())) {
            System.out.println("✅ Carta fedeltà attivata con successo!");
        } else {
            System.out.println("❌ Errore attivazione carta fedeltà: " + risposta.getMessaggio());
        }
    }
}