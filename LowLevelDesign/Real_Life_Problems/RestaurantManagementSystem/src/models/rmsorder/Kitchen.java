package models.rmsorder;

import models.rmsemployees.Chef;

import java.util.List;

public class Kitchen {
    private  int kitchenId;
    private List<Chef> chefs  ;

    public  boolean addChef(Chef chef)
    {
        chefs.add(chef);
        System.out.println("Chef Added Successfully");
        return true;
    }

}
