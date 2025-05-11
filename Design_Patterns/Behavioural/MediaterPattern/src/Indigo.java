public class Indigo implements IAirCraft {

    ATCTower atcTower;

    public Indigo(ATCTower atcTower) {
        this.atcTower = atcTower;
    }


    // khud se land nahi kar skta
    @Override
    public void land() {
        System.out.println("Mein to land kar raha hu   ");

    }

    //permission lega ATC se
    public void requestPermission() {
        // TO DO :  ATC Tower se Permission
        atcTower.requestToLand(this);
    }


}
