
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Server {

    //stores all accounts on server in an arraylist

    public static ArrayList<Account> accountList = new ArrayList<>(1);
    public static float rate = 10;

    public static void main(String[] args) throws Exception {
        try (ServerSocket listener = new ServerSocket(4242)) {
            System.out.println("Server started, waiting for connections...");
            ExecutorService pool = Executors.newFixedThreadPool(1000);
            while (true) {
                pool.execute(new Thread(listener.accept()));
            }
        }
    }

    public synchronized static ArrayList<Account> getAccountList() {
        return accountList;
    }

    public synchronized static void setAccountList(ArrayList<Account> accountList) {
        Server.accountList = accountList;
    }

    public synchronized static void addAccount(Account account) {
        Server.accountList.add(account);
    
    }
    
    

    public synchronized  static float getRate() {
        return rate;
    }

    public synchronized static void setRate(float rate) {
        Server.rate = rate;
    }

    private static class Thread implements Runnable {

        private Socket socket;
        public int numberOfAccountsFound = 0;

        Thread(Socket socket) {
            this.socket = socket;
            System.out.println("Connected: " + socket);
        }

        public void run() {
            try {
                Scanner in = new Scanner(socket.getInputStream());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                while (in.hasNextLine()) {
                    //converts command to lower case and passes print writer
                    checkCommand(in.nextLine().toLowerCase(), out);
                }
            } catch (IOException e) {
                System.out.println("ERROR: Failed to get client input/output streams.");
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    System.out.println("Closed Socket: " + socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void checkCommand(String command, PrintWriter out) {
            //splits the command into separate words
            String commandArray[] = command.split("\\s+");

            /*checks the first word of the command to see which command 
             *is required to be executed. If command is successfully executed 
             *user is informed
             */
            switch (commandArray[0]) {
                case "open":
                    if (commandArray.length == 1) {
                        out.println("Please enter account number to be opened");
                    } else {
                        openAccount(commandArray[1], out);
                    }
                    break;

                case "state":
                    printStates(out);
                    break;
                /*checks the correct length of the command in each case
                 *if the command is not the right length the correct format is 
                 *told to the user
                 */
                case "rate":
                    if (commandArray.length == 1) {
                        out.println("Please enter a value for rate.");
                    } else if (setRate(commandArray[1], out)) {
                        out.println("Rate changed");
                    }
                    break;

                case "convert":
                    if (commandArray.length < 3) {
                        out.println("please enter command in form: Convert <account no> (<a>, <p>) ");
                    } else if (convert(commandArray[1], commandArray[2], out)) {
                        out.println("Converted");
                    }
                    break;

                case "transfer":
                    if (commandArray.length < 4) {
                        out.println("Please enter command in format: Transfer <account from> <account to> (<a>, <p>)  ");
                    } else if ((transfer(commandArray[1], commandArray[2], commandArray[3], out))) {
                        out.println("Transferred");
                    }
                    break;
                //if command is not recognised user is informed
                default:
                    out.println("invalid Command");
                    break;
            }
        }

        private boolean openAccount(String accountNoString, PrintWriter out) {
            try {
                int accountNo = Integer.parseInt(accountNoString);
                //checks if accounts already exists
                if (checkIfAccountExists(accountNo)) {
                    out.println("Account number already exists");
                    return false;
                }

                out.println("Opened account " + accountNo);
                Account account = new Account(accountNo);
                Server.addAccount(account);

                return true;
            } catch (NumberFormatException e) {
                out.println("Invalid Account number - value must be numerical");
            }

            return false;
        }

        private void printStates(PrintWriter out) {
            Account curAcc = null;

            try {
                //sorts list numerically
                Collections.sort(Server.getAccountList());
                //aqcuires the lock for all accounts 

                for (int i = 0; i < Server.getAccountList().size(); i++) {
                    numberOfAccountsFound++;
                    curAcc = Server.getAccountList().get(i);
                    curAcc.lock();
                }

                //once all locks are aqquired the state of each account is printed
                for (int i = 0; i < Server.getAccountList().size(); i++) {
                    out.println(Server.getAccountList().get(i).getAccountNumber() + ":" + " Arian " + Server.accountList.get(i).getArian() + " Pres " + Server.accountList.get(i).getPres());
                    Server.getAccountList().get(i).unlock();
                }

                out.println("Rate " + Server.getRate());

            } finally {
                //if error occurs unlocks all the locks of accounts 
                //that are found to avoid deadlocks
                for (int i = 0; i < numberOfAccountsFound; i++) {
                    if (curAcc != null) {
                        curAcc.unlock();
                    }
                }
            }
        }

        private boolean setRate(String rate, PrintWriter out) {
            try {
                float rateVal = Float.valueOf(rate);
                //checks rate is not 0
                if (rateVal == 0) {
                    out.println("rate must not equal 0");
                    return false;
                }
                //
                Server.setRate(rateVal);
                return true;
            } catch (NumberFormatException e) {
                out.println("Invalid rate - value must be numerical");
            }

            return false;
        }

        private boolean convert(String accountNoString, String arianToPres, PrintWriter out) {
            Account curAcc = null;

            try {
                int accountNo = Integer.parseInt(accountNoString);

                //checks account exists and outputs error if it does not
                if (!checkIfAccountExists(accountNo)) {
                    out.println("Account number does not exist");
                    return false;
                }

                //regular expression to remove special charecters from (<a>,<p>)
                //splits arian and press to seperate values
                String arianToPresNoSpecialChar = arianToPres.replaceAll("[^0-9]", " ");
                String arianToPresArray[] = arianToPresNoSpecialChar.split("\\s+");

                //checks if a value is entered for both a and p               
                if (arianToPresArray.length < 2) {
                    out.println("No Pres or Arian input - value must be in form (<a>,<p>)");
                    return false;
                }

                float arianVal = Float.valueOf(arianToPresArray[1]);
                float presVal = Float.valueOf(arianToPresArray[2]);

                float currentArianBalance = 0;
                float currentPresBalance = 0;

                for (int i = 0; i < Server.getAccountList().size(); i++) {
                    curAcc = Server.getAccountList().get(i);

                    if (curAcc.getAccountNumber() == accountNo) {
                        curAcc.lock();
                        currentArianBalance = curAcc.getArian();
                        currentPresBalance = curAcc.getPres();

                        float newArianValue = (currentArianBalance - arianVal + presVal / rate);
                        float newPresValue = (currentPresBalance - presVal + arianVal * rate);

                        curAcc.setArian(newArianValue);
                        curAcc.setPres(newPresValue);
                        curAcc.unlock();

                        return true;
                    }
                }
            } catch (NumberFormatException e) {
                //outputs error if a or p are not valid inputs(cannot be parsed to double)
                out.println("Invalid Pres or Arian input - values must be doubles");
            } finally {
                if (curAcc != null) {
                    curAcc.unlock();
                }
            }

            return false;
        }

        private boolean transfer(String accountFromString, String accountToString, String arianToPres, PrintWriter out) {

            Account accountTo = null;
            Account accountFrom = null;
            try {
                int accountFromNo = Integer.parseInt(accountFromString);
                int accountToNo = Integer.parseInt(accountToString);
                //checks if both accounts exist.
                if (checkIfAccountExists(accountFromNo) == false || checkIfAccountExists(accountToNo) == false) {
                    out.println("At least one of the Account numbers does not exist");
                    return false;
                }

                //regular expression to remove special charecters from (<a>,<p>)
                //splits arian and press to seperate values
                String arianToPresNoSpecialChar = arianToPres.replaceAll("[^0-9]", " ");
                String arianToPresArray[] = arianToPresNoSpecialChar.split("\\s+");

                //checks if a value for both a and p are given
                if (arianToPresArray.length < 2) {
                    out.println("No Pres or Arian input - value must be in form (<a>,<p>)");
                    return false;
                }

                float arianVal = Float.valueOf(arianToPresArray[1]);
                float presVal = Float.valueOf(arianToPresArray[2]);

                //aqcuires the lock for both accounts
                for (int i = 0; i < Server.getAccountList().size(); i++) {

                    if (Server.getAccountList().get(i).getAccountNumber() == accountFromNo) {
                        accountFrom = Server.getAccountList().get(i);
                        accountFrom.lock();
                    }

                    if (Server.getAccountList().get(i).getAccountNumber() == accountToNo) {
                        accountTo = Server.getAccountList().get(i);
                        accountTo.lock();

                    }
                }

                //performs transaction and releases the locks
                accountFrom.setArian(accountFrom.getArian() - arianVal);
                accountFrom.setPres(accountFrom.getPres() - presVal);
                accountTo.setArian(accountTo.getArian() + arianVal);
                accountTo.setPres(accountTo.getPres() + presVal);
                //unlocks both accounts
                accountFrom.unlock();
                accountTo.unlock();

                return true;
            } catch (NumberFormatException e) {
                  out.println("Invalid Pres or Arian input - values must be doubles");
            } finally {
                //unlocks both accounts if error is found   
                if (accountTo != null) {
                    accountTo.unlock();
                }

                if (accountFrom != null) {
                    accountFrom.unlock();
                }
            }

            return false;
        }

        private boolean checkIfAccountExists(int accountNo) {
            //loops through all accounts to check if it exists
            for (int i = 0; i < Server.getAccountList().size(); i++) {
                if (Server.getAccountList().get(i).getAccountNumber() == accountNo) {
                    return true;
                }
            }
            return false;
        }
    }
}
