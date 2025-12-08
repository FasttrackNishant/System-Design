package models.rmsorder;

import constants.TableStatus;

import java.util.List;

public class Table {
    private  int tableId;
    private TableStatus status;
    private int maxCapacity;
    private String location;
    private List<TableSeat> seats;
}
