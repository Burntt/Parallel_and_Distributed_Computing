import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

public class TestPourDrink {
    @Test
    public void testApp() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(bos));
            ReplacesUtils.instance(PourDrink.class).run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.setOut(originalOut);
        }

        boolean flag = true;
        int lineCounter = 0;
        int pourCounter = 0;
        int drinkCounter = 0;
        for (String line : bos.toString().split("\\r?\\n")) {
            ++lineCounter;
            if (flag) {
                if (line.equals("Pour!")) {
                    ++pourCounter;
                    flag = !flag;
                } else {
                    fail(String.format("expected [Pour!] on line %d, got [%s]", lineCounter, line));
                }
            } else {
                if (line.equals("Drink!")) {
                    ++drinkCounter;
                    flag = !flag;
                } else {
                    fail(String.format("expected [Drink!] on line %d, got [%s]", lineCounter, line));
                }
            }
        }

        assertEquals(100_000, lineCounter, "should be 100_000 lines");
        assertEquals(50_000, pourCounter, "should pour 50_000 times");
        assertEquals(50_000, drinkCounter, "should drink 50_000 times");
        assertEquals(true, flag, "last line must be 'Drink!'");
    }
}
