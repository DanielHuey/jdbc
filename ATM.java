import java.util.Scanner;
import java.sql.*;

public class ATM {
    static Customer aCustomer;
    static ATMdb atmdb;

    // inputs
    static Scanner s = new Scanner(System.in);

    static String sInput() {
        return s.next();
    }

    static float fInput() {
        return s.nextFloat();
    }

    // stuff
    static String a, pin;
    static float bal;

    static void register() {
        boolean x = true, y = true;
        while (x) {
            System.out.print("5-Character account code: ");
            a = sInput();
            try {
                atmdb.addCustomer(a, "", 0.0f);
                atmdb.rmov(a);
                x = false;
            } catch (Exception z) {
                System.out.println("Account code taken!");
            }
        }
        System.out.print("PIN (Hidden, you may include letters; max length=20): ");
        pin = new String(System.console().readPassword());

        while (y) {
            System.out.print("Account Deposit ( >0.00 BTC and <100.00 BTC ): ");
            bal = fInput();
            if (0.0 <= bal && bal <= 100.00) {
                y = false;
            } else
                System.out.println("Check Deposit range!!");
        }
        try {
            atmdb.addCustomer(a, pin, bal);
        } catch (SQLException sqlException) {
            System.out.println("Account code already taken.");
            System.out.println("Failed to add you to our database.");
        }
        System.out.println("\n-> Proceeding to login...");
        login();
    }

    static void login() {
        if (a == null && pin == null) {
            System.out.print("Account code: ");
            a = sInput();
            System.out.print("PIN: ");
            pin = new String(System.console().readPassword());
            System.out.println();
        }
        try {
            atmdb.addCustomer(a, "", 0.0f);
            System.out.println("Account code doesn't exist!");
            atmdb.rmov(a);
        } catch (Exception z) {
            aCustomer = new Customer(a, pin, bal);
            aCustomer.mainLoop();
        }
    }

    public static void main(String[] args) {
        atmdb = new ATMdb();
        atmdb.init();
        System.out.println("Welcome to Sbantic bank ATM.\nThe world's first decentralized ATM");
        mainLoop();
    }

    static void mainLoop() {
        boolean a = true;
        while (a) {
            System.out.println("\n 1) Register\n 2) Login\n 3) Quit");
            System.out.print("-> ");
            int opt = (int) fInput();
            System.out.println();
            if (opt == 1) {
                register();
            }
            if (opt == 2) {
                login();
            }
            if (opt == 3) {
                a = false;
                try {
                    atmdb.sysResourceClr();
                } catch (SQLException s) {
                    System.out.println(s.getMessage());
                }
            }
        }
    }
}

class Customer {
    String a;
    String p;
    float b;
    boolean login = true;

    Customer(String ac, String pin, float bal) {
        System.out.println("Login Successful!");
        this.a = ac;
        this.p = pin;
        this.b = bal;
    }

    void mainLoop() {
        while (login) {
            System.out.println("\n 1) Make Deposit\n 2) Withdraw\n 3) Check Balance\n 4) Logout");
            System.out.print("-> ");
            int opt = (int) ATM.fInput();
            System.out.println();
            if (opt == 1) {
                uDeposit();
            }
            if (opt == 2) {
                uWithdraw();
            }
            if (opt == 3) {
                uBalance();
            }
            if (opt == 4) {
                uLogout();
            }
        }
    }

    void uDeposit() {
        System.out.print("Deposit Amount: ");
        float f = ATM.fInput();
        try {
            ATM.atmdb.uDeposit(a, f);
        } catch (SQLException s) {
            s.printStackTrace();
        }
    }

    void uBalance() {
        try {
            ATM.atmdb.uBalance(a);
        } catch (SQLException s) {
            s.printStackTrace();
        }
    }

    void uWithdraw() {
        System.out.print("Withdraw Amount: ");
        float f = ATM.fInput();
        try {
            ATM.atmdb.uWithdraw(a, f);
        } catch (SQLException s) {
            s.printStackTrace();
        }
    }

    void uLogout() {
        login = false;
        ATM.a = null;
        ATM.pin = null;
        ATM.bal = 0;
        System.out.println("Logout Success.");
    }
}

class ATMdb {
    Connection atmConnection;
    Statement atmStatement;
    ResultSet uSet;

    void init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            atmConnection = DriverManager.getConnection("jdbc:mysql://localhost", "root", null);
            atmStatement = atmConnection.createStatement();
            atmStatement.executeUpdate("create database IF NOT EXISTS atmdb");
            atmStatement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `atmdb`.`customer` (`accountCode` CHAR(5) NOT NULL , `pin` CHAR(20) NOT NULL , `balance` FLOAT NOT NULL , PRIMARY KEY (`accountCode`)) ENGINE = InnoDB;");

        } catch (ClassNotFoundException x) {
            System.out.println("!Error! This Class Not Found --> " + x.getMessage());
        } catch (SQLException x) {
            System.out.println("!Error! --> " + x.getMessage());
        }
    }

    void addCustomer(String ac, String pin, float bal) throws SQLException {
        String theQuery = "insert into `atmdb`.`customer` (`accountCode`, `pin`, `balance`) VALUES ('" + ac + "', '"
                + pin + "', '" + bal + "')";
        atmStatement.executeUpdate(theQuery);
    }

    void rmov(String ac) throws SQLException {
        String theQuery = "delete from `atmdb`.`customer` WHERE accountCode = '" + ac + "'";
        atmStatement.executeUpdate(theQuery);
    }

    // interactions
    void initUsr() throws SQLException {
        uSet = atmStatement
                .executeQuery("select * from `atmdb`.`customer` WHERE accountCode='" + ATM.aCustomer.a + "'");
    }

    void uDeposit(String a, float amt) throws SQLException {
        initUsr();
        if (uSet.next()) {
            float b4 = uSet.getFloat("balance");
            float afta = b4 + amt;
            ATM.aCustomer.b = afta;
            atmStatement.executeUpdate(
                    "UPDATE `atmdb`.`customer` SET `balance` = '" + afta + "' WHERE accountCode = '" + a + "'");
            System.out.println("Success.");
        }
    }

    void uBalance(String a) throws SQLException {
        initUsr();
        if (uSet.next()) {
            float bal = uSet.getFloat("balance");
            System.out.println("Balance is "+bal + " Bitcoins");
        }
    }

    void uWithdraw(String a, float amt) throws SQLException {
        initUsr();
        if (uSet.next()) {
            float b4 = uSet.getFloat("balance");
            if (amt <= b4) {
                float afta = b4 - amt;
                ATM.aCustomer.b = afta;
                atmStatement.executeUpdate(
                        "UPDATE `atmdb`.`customer` SET `balance` = '" + afta + "' WHERE accountCode = '" + a + "'");
                System.out.println("Success.");
            } else {
                System.out.println("Your balance is lower than " + amt);
                System.out.println("Operation Unsuccessful.");
            }
        }
    }
    
    //final
    void sysResourceClr() throws SQLException{
        atmConnection.close();
    }
}