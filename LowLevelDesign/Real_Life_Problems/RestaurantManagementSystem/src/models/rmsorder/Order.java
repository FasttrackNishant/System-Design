package models.rmsorder;

import constants.OrderStatus;
import models.rmsemployees.Chef;
import models.rmsemployees.Waiter;

import java.util.Date;
import java.util.List;

public class Order {

    private  int orderId ;
    private OrderStatus status;
    private Date orderTimestamp;

    private List<Meal> meals;
    private Chef chef ;
    private Waiter waiter;


    // functions

    public boolean addMeal(Meal meal)
    {
        return true;
    }

    public boolean removeMeal(Meal meal)
    {
        return true;
    }

}