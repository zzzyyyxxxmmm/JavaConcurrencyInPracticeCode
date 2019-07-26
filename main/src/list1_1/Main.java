package list1_1;

/**
 * @author jikangwang
 */


public class Main {

    public static void main(String[] args) {
        Sequence sequence = new Sequence();
        for (int i = 0; i < 100; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 100; i++) {
                        System.out.println(Thread.currentThread().getName() + " " + sequence.getNext());
                    }
                }
            }).start();
        }


    }
}
