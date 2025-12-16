package services;

import enums.PackageStatus;

public class AgentService {

    public void registerAgent(DeliveryAgent agent){
        agentRepo.save(agent);
    }

    public DeliveryAgent assignAgentForDelivery(Locker locker,Package pkg){
        String zipCode = locker.getZipCode();

        List<DeliveryAgent> agents = agentRepo.getByZip(zipCode);

        if(agents == null || agents.isEmpty()){

        }

        DeliveryAgent assignedAgent = strategy.assignAgent(agents);

        if(assignedAgent == null)
        {
            System.out.println("Agent is not assigned");
        }

        pkg.setAgentId(assignedAgent.getAgentId());
        pkg.setStatus(PackageStatus.ASSIGNED_TO_AGENT);

        // notify to user


        return assignedAgent;
    }
}
