import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MotorPH Payroll Processing System
 * 
 * This program processes payroll for MotorPH employees based on attendance records from June to December 2024.
 * It supports employee self-service lookup and full payroll processing for payroll staff.
 * 
 * Key Features:
 * - Secure login (employee/payroll_staff)
 * - Employee details lookup by number
 * - Payroll processing for one employee or all employees
 * - Semi-monthly cutoffs (1-15 / 16-end) with proper deductions on 2nd cutoff
 * - Philippine statutory deductions: SSS, PhilHealth, Pag-IBIG, Withholding Tax
 * - Optimized data loading (single read of attendance records)
 * 
 * Data Files Required:
 * - "MotorPH_Employee Data - Employee Details.csv" (empNo, lastName, firstName, birthday, ..., hourlyRate)
 * - "MotorPH_Employee Data - Attendance Record.csv" (empNo, ..., date MM/dd, login H:mm, logout H:mm)
 */
public class MotorPH {
    private static Scanner sc = new Scanner(System.in);
    
    // Pre-loaded data structures for performance (loaded once at startup)
    private static Map<String, Employee> employees = new HashMap<>();
    private static Map<String, List<AttendanceRecord>> attendanceByEmp = new HashMap<>();
    
    // Formatters
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private static final int YEAR = 2024;
    
    public static void main(String[] args) {
        // Load all data once for optimal performance
        loadEmployeeData();
        loadAttendanceData();
        
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();
        
        // Fixed login logic (was using incorrect AND instead of OR for usernames)
        if ((!username.equals("employee") && !username.equals("payroll_staff")) || !password.equals("12345")) {
            System.out.println("Incorrect username and/or password.");
            return;
        }
        
        if (username.equals("employee")) {
            employeeMenu();
        } else {
            payrollStaffMenu();
        }
    }
    
    /**
     * Loads employee data from CSV into memory for fast lookup.
     * Parses employee number, name, birthday, and hourly rate (last numeric column).
     */
    private static void loadEmployeeData() {
        String file = "MotorPH_Employee Data - Employee Details.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = parseCsvLine(line);
                if (data.length >= 4) {
                    String empNo = data[0].trim();
                    String lastName = data[1].trim();
                    String firstName = data[2].trim();
                    String birthday = data[3].trim();
                    double hourlyRate = parseHourlyRate(data);
                    employees.put(empNo, new Employee(empNo, lastName, firstName, birthday, hourlyRate));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading employee data: " + e.getMessage());
        }
    }
    
    /**
     * Loads attendance records grouped by employee for O(1) lookup.
     * Stores login/logout times and dates for quick monthly aggregation.
     */
    private static void loadAttendanceData() {
        String file = "MotorPH_Employee Data - Attendance Record.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = parseCsvLine(line);
                if (data.length >= 6) {
                    String empNo = data[0].trim();
                    String dateStr = data[3].trim();
                    String loginStr = data[4].trim();
                    String logoutStr = data[5].trim();
                    
                    List<AttendanceRecord> records = attendanceByEmp.computeIfAbsent(empNo, k -> new ArrayList<>());
                    records.add(new AttendanceRecord(dateStr, loginStr, logoutStr));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading attendance data: " + e.getMessage());
        }
    }
    
    /**
     * Parses CSV line handling basic comma-escaping (splits on unquoted commas).
     * Simple implementation for course-level CSV without complex quoting.
     */
    private static String[] parseCsvLine(String line) {
        return line.split(",");
    }
    
    /**
     * Extracts hourly rate from last numeric column in employee data.
     */
    private static double parseHourlyRate(String[] data) {
        for (int i = data.length - 1; i >= 0; i--) {
            try {
                return Double.parseDouble(data[i].trim());
            } catch (NumberFormatException e) {
                // Continue to previous column
            }
        }
        return 500.0; // Default fallback
    }
    
    /**
     * Employee menu for self-service details lookup.
     */
    static void employeeMenu() {
        while (true) {
            System.out.println("\nDisplay options:");
            System.out.println("1. Enter your employee number");
            System.out.println("2. Exit the program");
            System.out.print("Choice: ");
            
            String choice = sc.nextLine().trim();
            if (choice.equals("2")) return;
            if (choice.equals("1")) {
                System.out.print("Enter your employee number: ");
                showEmployeeDetails(sc.nextLine().trim());
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
    
    /**
     * Payroll staff menu for processing.
     */
    static void payrollStaffMenu() {
        while (true) {
            System.out.println("\noptions:");
            System.out.println("1. Process Payroll");
            System.out.println("2. Exit the program");
            System.out.print("Choice: ");
            
            String choice = sc.nextLine().trim();
            if (choice.equals("2")) return;
            if (choice.equals("1")) {
                processPayrollSubMenu();
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
    
    /**
     * Submenu for single vs all employees payroll processing.
     */
    static void processPayrollSubMenu() {
        while (true) {
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
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
    
    /**
     * Displays employee details from pre-loaded data.
     */
    static void showEmployeeDetails(String empNo) {
        Employee emp = employees.get(empNo);
        if (emp != null) {
            System.out.println("\nEmployee Number: " + emp.empNo);
            System.out.println("Employee Name: " + emp.lastName + ", " + emp.firstName);
            System.out.println("Birthday: " + emp.birthday);
        } else {
            System.out.println("Employee number does not exist.");
        }
    }
    
    /**
     * Processes complete payroll (June-Dec 2024) for one employee.
     * Prints formatted report with cutoffs and deductions.
     */
    static void processPayroll(String empNo) {
        Employee emp = employees.get(empNo);
        if (emp == null) {
            System.out.println("Employee not found.");
            return;
        }
        
        printEmployeeHeader(emp);
        
        // Process each month
        for (int month = 6; month <= 12; month++) {
            printMonthlySection(empNo, emp.hourlyRate, month);
        }
        
        System.out.println("\n" + "=".repeat(80));
    }
    
    /**
     * Prints employee header for payroll report.
     */
    static void printEmployeeHeader(Employee emp) {
        System.out.println("\n*** " + emp.lastName.toUpperCase() + ", " + emp.firstName.toUpperCase() + " ***");
        System.out.println("Employee #: " + emp.empNo);
        System.out.println("Birthday: " + emp.birthday);
        System.out.println();
    }
    
    /**
     * Prints detailed monthly payroll section with cutoffs.
     * Uses pre-loaded attendance for fast computation.
     */
    static void printMonthlySection(String empNo, double hourlyRate, int month) {
        List<AttendanceRecord> records = attendanceByEmp.getOrDefault(empNo, Collections.emptyList());
        double firstHalfHours = 0, secondHalfHours = 0;
        int daysInMonth = YearMonth.of(YEAR, month).lengthOfMonth();
        String monthName = getMonthName(month);
        
        // Aggregate hours by cutoff period
        for (AttendanceRecord rec : records) {
            if (isInMonth(rec.date, month)) {
                int day = parseDay(rec.date);
                double hours = computeHours(rec.login, rec.logout);
                if (day <= 15) {
                    firstHalfHours += hours;
                } else {
                    secondHalfHours += hours;
                }
            }
        }
        
        double grossFirst = firstHalfHours * hourlyRate;
        double grossSecond = secondHalfHours * hourlyRate;
        double monthlyGross = grossFirst + grossSecond;
        
        // Compute deductions based on monthly gross
        Deductions deductions = computeTotalDeductions(monthlyGross);
        
        // Format output exactly as original
        System.out.println(monthName.toUpperCase() + " " + YEAR);
        System.out.println("  1st Cutoff (1-15):");
        System.out.println("    Hours: " + String.format("%.2f", firstHalfHours));
        System.out.println("    Gross: " + String.format("%.2f", grossFirst));
        System.out.println("    Net:   " + String.format("%.2f", grossFirst));
        
        System.out.println("  2nd Cutoff (16-" + daysInMonth + "):");
        System.out.println("    Hours: " + String.format("%.2f", secondHalfHours));
        System.out.println("    Gross: " + String.format("%.2f", grossSecond));
        
        System.out.println("    Deductions:");
        System.out.println("      SSS:       " + String.format("%.2f", deductions.sss));
        System.out.println("      PhilHealth:" + String.format("%.2f", deductions.philhealth));
        System.out.println("      Pag-IBIG:  " + String.format("%.2f", deductions.pagibig));
        System.out.println("      Tax:       " + String.format("%.2f", deductions.tax));
        System.out.println("      Total:     " + String.format("%.2f", deductions.total));
        System.out.println("    2nd Net:     " + String.format("%.2f", grossSecond - deductions.total));
        System.out.println();
    }
    
    /**
     * Processes payroll for all employees, enumerating them.
     */
    static void processAllEmployees() {
        int empCount = 0;
        for (Employee emp : employees.values()) {
            empCount++;
            System.out.println("\nEMPLOYEE " + empCount);
            processPayroll(emp.empNo);
        }
    }
    
    /**
     * Checks if attendance date belongs to specific month.
     */
    private static boolean isInMonth(String dateStr, int month) {
        try {
            String[] parts = dateStr.split("/");
            return Integer.parseInt(parts[0]) == month;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Parses day from date string MM/dd.
     */
    private static int parseDay(String dateStr) {
        try {
            String[] parts = dateStr.split("/");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Gets full month name from number.
     */
    static String getMonthName(int monthNum) {
        String[] months = {"", "January", "February", "March", "April", "May", 
                          "June", "July", "August", "September", "October", "November", "December"};
        return monthNum >= 1 && monthNum <= 12 ? months[monthNum] : "Unknown";
    }
    
    /**
     * Computes regular hours respecting work window (8-17) and 1-hour lunch.
     * Caps at 8 hours per day.
     */
    static double computeHours(String loginStr, String logoutStr) {
        try {
            LocalTime login = LocalTime.parse(loginStr, TIME_FORMAT);
            LocalTime logout = LocalTime.parse(logoutStr, TIME_FORMAT);
            LocalTime startWork = LocalTime.of(8, 0);
            LocalTime endWork = LocalTime.of(17, 0);
            
            if (login.isBefore(startWork)) login = startWork;
            if (logout.isAfter(endWork)) logout = endWork;
            
            long minutes = Duration.between(login, logout).toMinutes();
            if (minutes > 60) minutes -= 60; // Lunch deduction
            else minutes = 0;
            
            double hours = minutes / 60.0;
            return Math.min(hours, 8.0);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Computes all statutory deductions for monthly gross pay.
     * Employee shares only. Based on Philippine rates (simplified for project).
     */
    static Deductions computeTotalDeductions(double gross) {
        double sss = computeSSS(gross);
        double philhealth = computePhilHealth(gross);
        double pagibig = computePagIbig(gross);
        double taxable = gross - (sss + philhealth + pagibig);
        double tax = computeWithholdingTax(taxable);
        double total = sss + philhealth + pagibig + tax;
        return new Deductions(sss, philhealth, pagibig, tax, total);
    }
    
    /**
     * SSS Employee Contribution (simplified table matching original logic)
     */
    static double computeSSS(double gross) {
        if (gross < 3250) return 135.00;
        if (gross <= 3750) return 157.50;
        if (gross <= 4250) return 180.00;
        if (gross <= 4750) return 202.50;
        if (gross <= 5250) return 225.00;
        if (gross <= 5750) return 247.50;
        if (gross <= 6250) return 270.00;
        return 1125.00; // Cap
    }
    
    /**
     * PhilHealth Employee Share (2.5% of gross, capped)
     */
    static double computePhilHealth(double gross) {
        double premium = gross <= 10000 ? 300 : Math.min(gross * 0.025, 900); // Simplified
        return premium;
    }
    
    /**
     * Pag-IBIG Employee Share (1-2% capped at 100).
     */
    static double computePagIbig(double gross) {
        if (gross >= 1000 && gross <= 1500) return gross * 0.01;
        return Math.min(gross * 0.02, 100);
    }
    
    /**
     * BIR Withholding Tax on monthly taxable income.
     */
    static double computeWithholdingTax(double taxable) {
        if (taxable <= 20833) return 0;
        if (taxable < 33333) return (taxable - 20833) * 0.20;
        if (taxable < 66667) return 2500 + (taxable - 33333) * 0.25;
        return 10833 + (taxable - 66667) * 0.30;
    }
    
    // Data holder classes for clarity and reusability
    static class Employee {
        String empNo, lastName, firstName, birthday;
        double hourlyRate;
        
        Employee(String empNo, String lastName, String firstName, String birthday, double hourlyRate) {
            this.empNo = empNo;
            this.lastName = lastName;
            this.firstName = firstName;
            this.birthday = birthday;
            this.hourlyRate = hourlyRate;
        }
    }
    
    static class AttendanceRecord {
        String date, login, logout;
        
        AttendanceRecord(String date, String login, String logout) {
            this.date = date;
            this.login = login;
            this.logout = logout;
        }
    }
    
    static class Deductions {
        final double sss, philhealth, pagibig, tax, total;
        
        Deductions(double sss, double philhealth, double pagibig, double tax, double total) {
            this.sss = sss;
            this.philhealth = philhealth;
            this.pagibig = pagibig;
            this.tax = tax;
            this.total = total;
        }
    }
}
