import deployment.DeploymentOrchestrator;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        DeploymentOrchestrator orchestrator = new DeploymentOrchestrator();
        orchestrator.deployApplication("main", "prod.server.example.com");

        System.out.println("\n--- Attempting another deployment (e.g., for a feature branch to staging) ---");
        // orchestrator.deployToStaging("feature/new-ux", "staging.server.example.com");

    }
}