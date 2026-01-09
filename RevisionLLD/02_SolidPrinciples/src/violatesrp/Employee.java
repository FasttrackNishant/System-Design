package violatesrp;

public class Employee {
    private String name;
    private String email;
    private double salary;

    // Constructor, getters, setters...

    public void calculateSalary() {
        // Complex salary calculation logic
        // Includes tax calculations
    }

    public void saveToDatabase() {
        // Connect to database
        // Prepare SQL
        // Execute query
    }

    public void generatePayslip() {
        // Format payslip
        // Add company logo
        // Convert to PDF
    }

    public void sendPayslipEmail() {
        // Connect to email server
        // Create email with attachment
        // Send email
    }

    public double getBaseSalary(){
        return 0.0d;
    }
}