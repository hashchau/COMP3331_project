import java.util.*;

public class Test {
    public static void main (String[] args) {
        int seed = 50;
        Random random = new Random(seed);
        System.err.println(random.nextFloat());
        System.err.println(random.nextFloat());
        System.err.println(random.nextFloat());
    }
}
