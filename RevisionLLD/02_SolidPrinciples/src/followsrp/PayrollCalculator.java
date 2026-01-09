package followsrp;

import violatesrp.Employee;

class PayrollCalculator {
    public double calculateNetPay(Employee employee) {
        double base = employee.getBaseSalary();
        double tax = base * 0.2;  // Sample tax logic
        double benefits = 1000;   // Fixed benefit deduction
        return base - tax + benefits;
    }
}