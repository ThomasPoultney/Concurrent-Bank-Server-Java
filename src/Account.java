import java.util.concurrent.locks.ReentrantLock;

public class Account implements Comparable<Account> {
	private ReentrantLock lock;

    public int accountNumber;
    public float arian;
    public float pres;

    public Account(int accountNumber) {
		this.lock = new ReentrantLock();
        this.accountNumber = accountNumber;
        this.arian = 0;
        this.pres = 0;
    }
    
        //locks account using reentrant locks
	public void lock() {
		lock.lock();
	}
        
        //Unlocks account
	public void unlock() {
		if (lock.isLocked())
			lock.unlock();
	}

    public int getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
    }

    public float getArian() {
        return arian;
    }

    public void setArian(float arian) {
        this.arian = arian;
    }

    public float getPres() {
        return pres;
    }

    public void setPres(float pres) {
        this.pres = pres;
    }

    @Override
    //used to sort arraylist numerically
    public int compareTo(Account o) {
        if (accountNumber == o.accountNumber) {
            return 0;
        } else if (accountNumber > o.accountNumber) {
            return 1;
        } else {
            return -1;
        }
    }

}
