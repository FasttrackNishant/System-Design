package services;

import repository.AgentRepository;

public class NotificationService {
    private final CustomerRepository customerRepo ;
    private final AgentRepository agentRepo;

    public void notifyCustomer(Package pkg , OtpInfo otpInfo)
    {
        Customer customer = customerRepo.getById(pkg.getCustomerId());

        System.out.println("Noififcation to customer " + customer.getCustomerId() + "Pkg id "+ pkg.getPackageId() +
                "Is read" + otpInfo.getOtp() + "valid till "+ optInfo.getExpiryTime());

    }

    public void notifyAgent(Package pkg)
    {
        DeliveryAgent agent = agentRepo.getById(pkg.getAgentId());

        System.out.println("Notification send to agent "+ agent.getAgentId() +
                "For Package" + pkg.getPackageId() + " To the locker " + pkg.getLockerName());
    }
}
