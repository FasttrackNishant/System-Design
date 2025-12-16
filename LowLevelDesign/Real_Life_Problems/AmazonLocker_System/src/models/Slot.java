package models;

import java.util.concurrent.atomic.AtomicBoolean;

public class Slot {

    private  final SlotSize size;

    private final AtomicBoolean available = new AtomicBoolean(true);

    private Package storedPackage ;

    public Slot(SlotSize size){
        this.size = size;
    }

    public boolean acquire(){
        return available.compareAndSet(true,false);
    }

    public void release(){
        available.set(true);
    }



}
