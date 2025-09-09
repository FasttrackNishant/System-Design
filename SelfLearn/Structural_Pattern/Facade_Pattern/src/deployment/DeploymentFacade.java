package deployment;

class DeploymentFacade {
    private VersionControlSystem vcs = new VersionControlSystem();
    private BuildSystem buildSystem = new BuildSystem();
    private TestingFramework testingFramework = new TestingFramework();
    private DeploymentTarget deploymentTarget = new DeploymentTarget();

    public boolean deployApplication(String branch, String serverAddress) {
        System.out.println("\nFACADE: --- Initiating FULL DEPLOYMENT for branch: " + branch + " to " + serverAddress + " ---");
        boolean success = true;

        try {
            vcs.pullLatestChanges(branch);

            if (!buildSystem.compileProject()) {
                System.err.println("FACADE: DEPLOYMENT FAILED - Build compilation failed.");
                return false;
            }

            String artifactPath = buildSystem.getArtifactPath();

            if (!testingFramework.runUnitTests()) {
                System.err.println("FACADE: DEPLOYMENT FAILED - Unit tests failed.");
                return false;
            }

            if (!testingFramework.runIntegrationTests()) {
                System.err.println("FACADE: DEPLOYMENT FAILED - Integration tests failed.");
                return false;
            }

            deploymentTarget.transferArtifact(artifactPath, serverAddress);
            deploymentTarget.activateNewVersion(serverAddress);

            System.out.println("FACADE: APPLICATION DEPLOYED SUCCESSFULLY to " + serverAddress + "!");
        } catch (Exception e) {
            System.err.println("FACADE: DEPLOYMENT FAILED - An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            success = false;
        }

        return success;
    }
}