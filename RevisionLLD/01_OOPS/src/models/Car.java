package models;

public class Car {
   private String brand;
   private String model;
   private int speed;

   public Car(String brand, String model, int speed) {
      this.brand = brand;
      this.model = model;
      this.speed = speed;
   }

   public void accelerate(int speed) {
      this.speed += speed;
   }

   public void displayStatus() {
      System.out.println(this.brand + "is running at " + this.speed + "having model" + this.model);
   }
}
