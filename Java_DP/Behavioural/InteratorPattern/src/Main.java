import java.util.ArrayList;
import java.util.Iterator;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

// ye ho gaya java ka iterator

//        ArrayList<String> companyList = new ArrayList<>();
//        companyList.add("MS");
//        companyList.add("Amazon");
//
//
//        // this is iterator for the company list
//        Iterator<String> it = companyList.iterator();
//
//        while (it.hasNext()) {
//            System.out.println(it.next());
//        }

        AirForce airForce = new AirForce();
        CustomIterator allPlanes = airForce.createIterator();

        while (allPlanes.hasNext()) {
//            allPlanes.next();
            System.out.println(allPlanes.next());
        }

    }
}