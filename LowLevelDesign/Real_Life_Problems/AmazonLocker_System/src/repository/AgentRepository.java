package repository;

import java.util.Collections;
import java.util.HashMap;

public class AgentRepository {

    private final Map<String,DeliveryAgent> agents = new HashMap<>();
    private final Map<String,List<DeliveryAgent>> zipToAgents = new HashMap<>();

    public void save(DeliveryAgent agent){

    }

    public DeliveryAgent getById(String id)
    {
        return agents.get(id);
    }

    public List<DeliveryAgent> getByZip(String zip){
        return  zipToAgents.getOrDefault(zip, Collections.emptyList());
    }


}
