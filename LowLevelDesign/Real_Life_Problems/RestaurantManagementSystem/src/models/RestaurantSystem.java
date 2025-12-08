package models;

import models.rmsorder.Restaurant;

public class RestaurantSystem {
    private Restaurant restaurant;

    public static RestaurantSystem restaurantSystemInstance = null;

    private RestaurantSystem(){}

    public static RestaurantSystem getInstance(){
        if(restaurantSystemInstance == null){
            restaurantSystemInstance = new RestaurantSystem();
        }

        return restaurantSystemInstance;
    }
}
