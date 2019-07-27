package list2_3;

/**
 * @author jikangwang
 */
public class Main {
    public static void main(String[] args) {
        long t1=System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                   LazyInitRace lazyInitRace=LazyInitRace.getInstance();
                    System.out.println(System.currentTimeMillis()-t1);
                }
            }).start();
        }
    }
}
