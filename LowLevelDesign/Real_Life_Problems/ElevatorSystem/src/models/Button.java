package models;

public abstract class Button {

    private  boolean status ;

    public abstract void press();

    public abstract boolean isPressed();

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
