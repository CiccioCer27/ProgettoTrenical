package command;

import dto.TrattaDTO;
import service.ClientService;

public class IscrivitiNotificheTrattaCommand implements Command {

    private final ClientService clientService;
    private final TrattaDTO tratta;

    public IscrivitiNotificheTrattaCommand(ClientService clientService, TrattaDTO tratta) {
        this.clientService = clientService;
        this.tratta = tratta;
    }

    @Override
    public void esegui() {
        System.out.println("📡 Iscrizione alle notifiche per la tratta in corso...");
        clientService.avviaNotificheTratta(tratta);
    }
}