package models.factory;

import models.rmsorder.MenuItem;

public class MenuManager {
    public  void addMenuItem(int id , String title , String desc , float price ){
        MenuItem item  = MenuItemFactory.createMenuItem(id,title,desc,price);
    }
}
