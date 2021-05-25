import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;


/////////////////////////////

public class TestBankHeist {
    @Test
    public void testSpec() {
        Bank bank = ReplacesUtils.instance(Bank.class, 1, 1);
        assertEquals(1, bank.getBalance(Account.A));
        assertEquals(1, bank.getBalance(Account.B));

        assertFalse(bank.withdraw(Account.A, 4));
        assertTrue(bank.withdraw(Account.A, 2));
        assertFalse(bank.withdraw(Account.A, 1));
        assertFalse(bank.withdraw(Account.B, 1));

        assertEquals(-1, bank.getBalance(Account.A));
        assertEquals(1, bank.getBalance(Account.B));

        assertFalse(bank.transfer(Account.A, Account.B, 1));
        assertTrue(bank.transfer(Account.B, Account.A, 1));

        assertEquals(0, bank.getBalance(Account.A));
        assertEquals(0, bank.getBalance(Account.B));
    }

    @Test
    public void testMichaelDeSanta() {
        int totalMobsters = 8;
        int moneyInTheBank = 1_000_000;

        // This is the bank.
        Bank bank = ReplacesUtils.instance(Bank.class, moneyInTheBank, moneyInTheBank);

        assertEquals(moneyInTheBank, bank.getBalance(Account.A), "heist worth it!");
        assertEquals(moneyInTheBank, bank.getBalance(Account.B), "heist worth it!");

        // Prepare the heist.
        List<Runnable> heist = new ArrayList<>(8 * moneyInTheBank);
        for (int i = 0; i < moneyInTheBank; i++) {
            heist.add(() -> bank.withdraw(Account.A, 1));
            heist.add(() -> bank.withdraw(Account.B, 1));
            heist.add(() -> bank.transfer(Account.A, Account.B, 1));
            heist.add(() -> bank.transfer(Account.B, Account.A, 1));
            heist.add(() -> bank.transfer(Account.A, Account.B, 3));
            heist.add(() -> bank.transfer(Account.B, Account.A, 3));
            heist.add(() -> bank.transfer(Account.A, Account.B, 5));
            heist.add(() -> bank.transfer(Account.B, Account.A, 5));
        }
        // Secret De Santa's plan!
        Collections.shuffle(heist);

        List<Thread> mobsters = new ArrayList<>(totalMobsters);
        for (int i = 0; i < totalMobsters; ++i) {
            int beginIndex = i * heist.size() / totalMobsters;
            int endIndex = (i + 1) * heist.size() / totalMobsters;
            final List<Runnable> mobsterActions =
                    Collections.unmodifiableList(heist.subList(beginIndex, endIndex));
            mobsters.add(new Thread(() -> mobsterActions.forEach(Runnable::run)));
        }

        long start = System.currentTimeMillis();
        // Bang!
        mobsters.forEach(Thread::start);
        mobsters.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;

        System.out.println(timeElapsed);

        // Let the dust settle down.
        assertEquals(0, bank.getBalance(Account.A) + bank.getBalance(Account.B),
                "no residue in the bank -- no bugs");
        assertTrue(bank.getBalance(Account.A) <= 5, "account A is small enough");
        assertTrue(bank.getBalance(Account.B) <= 5, "account B is small enough");
    }
}
