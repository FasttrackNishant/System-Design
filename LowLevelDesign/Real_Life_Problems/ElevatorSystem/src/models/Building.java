package models;

import java.util.List;

// singleton
public class Building {

    private  List<Floor> floorList;
    private  List<Elevator> elevatorList;

    private  static Building buildingInstance = null;

    public  static Building getInstance(){
        if(buildingInstance == null)
        {
            buildingInstance = new Building();
        }
        return  buildingInstance;
    }
}