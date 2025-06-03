package command;

import service.ClientService;

import java.util.Scanner;

public class RegistraClienteCommand implements Command {
    private final ClientService clientService;
    private final Scanner scanner;

    public RegistraClienteCommand(ClientService clientService, Scanner scanner) {
        this.clientService = clientService;
        this.scanner = scanner;
    }

    @Override
    public void esegui() {
        System.out.println("\n👤 REGISTRAZIONE CLIENTE");
        System.out.println("-".repeat(25));

        try {
            System.out.print("Nome: ");
            String nome = scanner.nextLine().trim();
            if (nome.isEmpty()) throw new IllegalArgumentException("Nome non può essere vuoto");

            System.out.print("Cognome: ");
            String cognome = scanner.nextLine().trim();
            if (cognome.isEmpty()) throw new IllegalArgumentException("Cognome non può essere vuoto");

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            if (email.isEmpty() || !email.contains("@")) throw new IllegalArgumentException("Email non valida");

            System.out.print("Età: ");
            int eta = Integer.parseInt(scanner.nextLine().trim());
            if (eta < 0 || eta > 120) throw new IllegalArgumentException("Età non valida");

            System.out.print("Città: ");
            String residenza = scanner.nextLine().trim();

            System.out.print("Cellulare: ");
            String cellulare = scanner.nextLine().trim();

            clientService.attivaCliente(nome, cognome, email, eta, residenza, cellulare);
            System.out.println("✅ Cliente registrato con successo!");

        } catch (NumberFormatException e) {
            throw new RuntimeException("Età deve essere un numero!");
        } catch (Exception e) {
            throw new RuntimeException("Errore registrazione: " + e.getMessage());
        }
    }
}