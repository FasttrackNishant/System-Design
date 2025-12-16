package services;

import repository.AgentRepository;

public class NotificationService {
    private final CustomerRepository customerRepo ;
    private final AgentRepository agentRepo;

    public void notifyCustomer(Package pkg , OtpInfo otpInfo)
    {

    }

    public void notifyAgent(Package pkg)
    {
        DeliveryAgent agent = agentRepo.getById(pkg.getAgentId());

        System.out.println("Notification send to agent "+ agent.getAgentId() +
                "For Package" + pkg.getPackageId() + " To the locker " + pkg.getLockerName());
    }
}
