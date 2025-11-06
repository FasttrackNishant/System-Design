package models;

public class ElevatorSystem {

    private  Building  building;

    private static ElevatorSystem elevatorSystemInstance = null;

    private ElevatorSystem(){

    }

    public static ElevatorSystem getInstance()
    {
        if(elevatorSystemInstance == null)
        {
            elevatorSystemInstance = new ElevatorSystem();
        }
        return  elevatorSystemInstance;
    }

    public void monitoring(){
    // add custom logic here
    }

    public void dispatcherAlgo(){

    }
}
