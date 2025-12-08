package models.rmsorder;

import  java.util.List;

public class Restaurant {

    private String name;
    private int id;
    private List<Branch> branchList;

    public  boolean addBranch(Branch branch){
        return true;
    }

}
