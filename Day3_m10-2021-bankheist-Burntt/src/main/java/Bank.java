// Copyleft of Javashire Getaway, 2020.
////////////////////////////////////////////////////////////////////////////////

class Bank {

    private int balanceA;
    private int balanceB;

    public Bank(int initialBalanceA, int initialBalanceB) {
        this.balanceA = initialBalanceA;
        this.balanceB = initialBalanceB;

    }

    // Transfer funds between the accounts.
    // Transaction succeeds iff the source account is non-negative after the transfer.
    // Returns true iff the transaction succeeds.
    public boolean transfer(Account source, Account target, int amount) {

        synchronized (this) {
            // If the source is account A, and the balance of A is greater or equal than the requested amount
            if ((source == Account.A) && (this.balanceA >= amount)) {

                // Transfer A -> B
                this.balanceA -= amount;
                this.balanceB += amount;
                return true;
            }
        }

        synchronized (this) {
            // If the source is account B, and the balance of B is greater or equal than the requested amount
            if ((source == Account.B) && (this.balanceB >= amount)) {

                // Transfer B -> A
                this.balanceB -= amount;
                this.balanceA += amount;
                return true;
            }
        }
        return false;

    }

    // Withdraw funds from the account.
    // Transaction succeeds iff A + B is non-negative after the withdrawal.
    // Returns true iff the transaction succeeds.
    public boolean withdraw(Account account, int amount) {

        int residualBalance = 0;
        synchronized (this) {
            // If money is retrieved from account A, and there is enough total balance
            if ((account == Account.A) && (this.balanceA + this.balanceB >= amount)) {
                this.balanceA -= amount;
                return true;
            }
        }

        synchronized (this) {
            // If money is retrieved from account B, and there is enough total balance
            if ((account == Account.B) && (this.balanceA + this.balanceB >= amount)) {
                this.balanceB -= amount;
                return true;
            }
        }

        // If for any other reason withdrawal cannot be done
        return false;
    }

    // Returns the account's balance.
    public int getBalance(Account account) {

        if (account == Account.A && (this.balanceA >= 0)) {
            return this.balanceA;
        }

        if (account == Account.B && (this.balanceB >= 0)) {
            return this.balanceB;
        }
        else {
            return -1;
        }
    }
}

class StartBanking {
    public static void main(final String[] args) {

        int totalMobsters = 8;
        int moneyInTheBank = 1_000_000;
        Bank bank = new Bank(1,1);

        // Test
        System.out.println( bank.getBalance(Account.A) );
        System.out.println( bank.getBalance(Account.B) );

        System.out.println( bank.withdraw(Account.A, 4));
        System.out.println( bank.withdraw(Account.A, 2));
        System.out.println( bank.withdraw(Account.A, 1));
        System.out.println( bank.withdraw(Account.B, 1));



    }
}
