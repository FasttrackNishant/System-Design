package deployment;

public class DeploymentAppDirect {
    public static void main(String[] args) {
        DeploymentOrchestrator orchestrator = new DeploymentOrchestrator();
        orchestrator.deployApplication("main", "prod.server.example.com");

        System.out.println("\n--- Attempting another deployment (e.g., for a feature branch to staging) ---");
        // orchestrator.deployToStaging("feature/new-ux", "staging.server.example.com");
    }
}