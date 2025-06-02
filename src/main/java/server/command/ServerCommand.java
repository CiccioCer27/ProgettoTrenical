package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;

public interface ServerCommand {
    RispostaDTO esegui();  // ✅ No parametri confusing
}
