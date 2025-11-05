package shubh.models;

import shubh.enums.GateType;

public abstract class Gate {

    protected  final String id ;

    protected Gate(String id) {
        this.id = id;
    }

    public  abstract GateType getType();
}
