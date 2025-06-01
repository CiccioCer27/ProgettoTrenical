package test;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import service.ClientService;

public class ClientTestMain {
    public static void main(String[] args) {
        ClientService client = new ClientService("localhost", 50051);

        // Simula registrazione cliente
        client.attivaCliente("Francesco", "Ceraudo", "francesco@trenical.it", 25, "Rende", "3331112222");

        // 1. Filtro tratte
        RichiestaDTO filtro = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra("2025-06-01;Milano;Roma;mattina")
                .build();
        RispostaDTO rispostaFiltrata = client.inviaRichiesta(filtro);

        System.out.println("📍 Tratte trovate: " + rispostaFiltrata.getTratte().size());

        if (rispostaFiltrata.getTratte().isEmpty()) {
            System.out.println("❌ Nessuna tratta trovata.");
            return;
        }

        // 2. Acquisto biglietto sulla prima tratta
        var tratta = rispostaFiltrata.getTratte().get(0);
        RichiestaDTO acquisto = new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(client.getCliente().getId().toString())
                .tratta(tratta)
                .classeServizio(ClasseServizio.BASE)
                .tipoPrezzo(TipoPrezzo.INTERO)
                .build();

        RispostaDTO rispostaAcquisto = client.inviaRichiesta(acquisto);
        System.out.println("💳 Risposta acquisto: " + rispostaAcquisto.getMessaggio());

        if (rispostaAcquisto.getBiglietto() != null) {
            System.out.println("🎫 Biglietto ID: " + rispostaAcquisto.getBiglietto().getId());
        } else {
            System.out.println("⚠️ Nessun biglietto emesso.");
        }
    }
}