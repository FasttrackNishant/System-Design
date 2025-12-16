package strategy.agent;

import java.util.Random;

public interface AgentAssignmentStrategy {

    public DeliveryAgent assignAgent(List<DeliveryAgent> agents){
        if(agents.isEmpty()) return null;

        return agents.get(random.nextInt(agents.size()));
    }

}
