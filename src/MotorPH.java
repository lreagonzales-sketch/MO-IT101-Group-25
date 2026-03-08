import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class MotorPH {
    private static Scanner sc = new Scanner(System.in);
    
    public static void main(String[] args) {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();
        
        if (!username.equals("employee") && !username.equals("payroll_staff") || !password.equals("12345")) {
            System.out.println("Incorrect username and/or password.");
            return;
        }
        
        if (username.equals("employee")) {
            employeeMenu();
        } else {
            payrollStaffMenu();
        }
    }
    
    static void employeeMenu() {
        while(true) {
            System.out.println("\nDisplay options:");
            System.out.println("1. Enter your employee number");
            System.out.println("2. Exit the program");
            System.out.print("Choice: ");
            
            String choice = sc.nextLine().trim();
            if (choice.equals("2")) return;
            if (choice.equals("1")) {
                System.out.print("Enter your employee number: ");
                showEmployeeDetails(sc.nextLine().trim());
            }
        }
    }
    
    static void payrollStaffMenu() {
        while(true) {
            System.out.println("\noptions:");
            System.out.println("1. Process Payroll");
            System.out.println("2. Exit the program");
            System.out.print("Choice: ");
            
            String choice = sc.nextLine().trim();
            if (choice.equals("2")) return;
            if (choice.equals("1")) {
                processPayrollSubMenu();
            }
        }
    }
    
    static void processPayrollSubMenu() {
        while(true) {
            System.out.println("\n1. One employee");
            System.out.println("2. All employees");
            System.out.println("3. Exit the program");
            System.out.print("Choice: ");
            
            String choice = sc.nextLine().trim();
            if (choice.equals("3")) return;
            if (choice.equals("1")) {
                System.out.print("Enter the employee number: ");
                processPayroll(sc.nextLine().trim());
            } else if (choice.equals("2")) {
                processAllEmployees();
            }
        }
    }
    
    static void showEmployeeDetails(String empNo) {
        String empFile = "MotorPH_Employee Data - Employee Details.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(empFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                if (data.length >= 4 && data[0].trim().equals(empNo)) {
                    System.out.println("\nEmployee Number: " + data[0].trim());
                    System.out.println("Employee Name: " + data[1].trim() + ", " + data[2].trim());
                    System.out.println("Birthday: " + data[3].trim());
                    return;
                }
            }
        } catch (Exception e) {}
        System.out.println("Employee number does not exist.");
    }
    
    static void processPayroll(String empNo) {
        printEmployeeHeader(empNo);
        
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("H:mm");
        double hourlyRate = getHourlyRate(empNo);
        
        // June to December - Clean monthly sections
        for (int month = 6; month <= 12; month++) {
            printMonthlySection(empNo, hourlyRate, month, timeFormat);
        }
        
        // ONLY separator AFTER complete employee payroll (June-Dec)
        System.out.println("\n" + "=".repeat(80));
    }
    
    static void processAllEmployees() {
        String empFile = "MotorPH_Employee Data - Employee Details.csv";
        int empCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(empFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                if (data.length >= 4) {
                    empCount++;
                    System.out.println("\nEMPLOYEE " + empCount);
                    processPayroll(data[0].trim());
                }
            }
        } catch (Exception e) {}
    }
    
    static void printEmployeeHeader(String empNo) {
        String empFile = "MotorPH_Employee Data - Employee Details.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(empFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                if (data.length >= 4 && data[0].trim().equals(empNo)) {
                    System.out.println("\n*** " + data[1].trim().toUpperCase() + ", " + data[2].trim().toUpperCase() + " ***");
                    System.out.println("Employee #: " + data[0].trim());
                    System.out.println("Birthday: " + data[3].trim());
                    System.out.println();
                    return;
                }
            }
        } catch (Exception e) {}
    }
    
    static void printMonthlySection(String empNo, double hourlyRate, int month, DateTimeFormatter timeFormat) {
        String attFile = "MotorPH_Employee Data - Attendance Record.csv";
        double firstHalfHours = 0, secondHalfHours = 0;
        int daysInMonth = YearMonth.of(2024, month).lengthOfMonth();
        String monthName = getMonthName(month);
        
        try (BufferedReader br = new BufferedReader(new FileReader(attFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                if (data.length < 6 || !data[0].trim().equals(empNo)) continue;
                
                try {
                    String[] dateParts = data[3].split("/");
                    int recordMonth = Integer.parseInt(dateParts[0]);
                    int day = Integer.parseInt(dateParts[1]);
                    
                    if (recordMonth == month) {
                        LocalTime login = LocalTime.parse(data[4].trim(), timeFormat);
                        LocalTime logout = LocalTime.parse(data[5].trim(), timeFormat);
                        double hours = computeHours(login, logout);
                        
                        if (day <= 15) firstHalfHours += hours;
                        else secondHalfHours += hours;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        } catch (Exception e) {}
        
        // 1ST CUTOFF
        double grossFirst = firstHalfHours * hourlyRate;
        System.out.println(monthName.toUpperCase() + " 2024");
        System.out.println("  1st Cutoff (1-15):");
        System.out.println("    Hours: " + String.format("%.2f", firstHalfHours));
        System.out.println("    Gross: " + String.format("%.2f", grossFirst));
        System.out.println("    Net:   " + String.format("%.2f", grossFirst));
        
        // 2ND CUTOFF WITH DEDUCTIONS
        double grossSecond = secondHalfHours * hourlyRate;
        System.out.println("  2nd Cutoff (16-" + daysInMonth + "):");
        System.out.println("    Hours: " + String.format("%.2f", secondHalfHours));
        System.out.println("    Gross: " + String.format("%.2f", grossSecond));
        
        double monthlyGross = grossFirst + grossSecond;
        double sss = computeSSS(monthlyGross);
        double philhealth = computePhilHealth(monthlyGross);
        double pagibig = computePagIbig(monthlyGross);
        double taxable = monthlyGross - (sss + philhealth + pagibig);
        double tax = computeWithholdingTax(taxable);
        double totalDeductions = sss + philhealth + pagibig + tax;
        double netSecond = grossSecond - totalDeductions;
        
        System.out.println("    Deductions:");
        System.out.println("      SSS:       " + String.format("%.2f", sss));
        System.out.println("      PhilHealth:" + String.format("%.2f", philhealth));
        System.out.println("      Pag-IBIG:  " + String.format("%.2f", pagibig));
        System.out.println("      Tax:       " + String.format("%.2f", tax));
        System.out.println("      Total:     " + String.format("%.2f", totalDeductions));
        System.out.println("    2nd Net:     " + String.format("%.2f", netSecond));
        System.out.println();
    }
    
    static double getHourlyRate(String empNo) {
        String empFile = "MotorPH_Employee Data - Employee Details.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(empFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                if (data.length >= 4 && data[0].trim().equals(empNo)) {
                    if (data.length > 20) {
                        try {
                            return Double.parseDouble(data[data.length - 1].trim());
                        } catch (Exception e) {
                            try {
                                return Double.parseDouble(data[data.length - 2].trim());
                            } catch (Exception ex) {}
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return 500.0;
    }
    
    static String getMonthName(int monthNum) {
        String[] months = {"", "January", "February", "March", "April", "May", 
                          "June", "July", "August", "September", "October", "November", "December"};
        if (monthNum >= 1 && monthNum <= 12) return months[monthNum];
        return "Unknown";
    }
    
    static double computeHours(LocalTime login, LocalTime logout) {
        LocalTime startWork = LocalTime.of(8, 0);
        LocalTime endWork = LocalTime.of(17, 0);
        
        if (login.isBefore(startWork)) login = startWork;
        if (logout.isAfter(endWork)) logout = endWork;
        
        long minutes = Duration.between(login, logout).toMinutes();
        if (minutes > 60) minutes -= 60;
        else minutes = 0;
        
        double hours = minutes / 60.0;
        return Math.min(hours, 8.0);
    }
    
    static double computeSSS(double gross) {
        if(gross < 3250) return 135.00;
        if(gross <= 3750) return 157.50;
        if(gross <= 4250) return 180.00;
        if(gross <= 4750) return 202.50;
        if(gross <= 5250) return 225.00;
        if(gross <= 5750) return 247.50;
        if(gross <= 6250) return 270.00;
        return 1125.00;
    }
    
    static double computePhilHealth(double gross) {
        double premium = gross <= 10000 ? 300 : Math.min(gross * 0.03, 1800);
        return premium * 0.5;
    }
    
    static double computePagIbig(double gross) {
        if(gross >= 1000 && gross <= 1500) return gross * 0.01;
        return Math.min(gross * 0.02, 100);
    }
    
    static double computeWithholdingTax(double taxable) {
        if(taxable <= 20832) return 0;
        if(taxable < 33333) return (taxable - 20833) * 0.20;
        if(taxable < 66667) return 2500 + (taxable - 33333) * 0.25;
        return 10833 + (taxable - 66667) * 0.30;
    }
}
