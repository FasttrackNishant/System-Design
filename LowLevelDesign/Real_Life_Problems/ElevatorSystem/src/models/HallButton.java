package models;

import constants.DirectionStatus;

public class HallButton extends Button {

    private DirectionStatus DirectionButton ;

    @Override
    public void press() {

    }

    @Override
    public boolean isPressed() {
        return false;
    }
}
