package models.rmsorder;

public class  MenuItem {
    private  int id;
    private  String title;
    private  String description;
    private  float price;

    public MenuItem(int id, String title , String desc , float price){
        this.id = id;
        this.title = title;
        this.description = desc;
        this.price = price;
    }

    public boolean updateItemInfo(String title ,String description,int price){
        return true;
    }
}
