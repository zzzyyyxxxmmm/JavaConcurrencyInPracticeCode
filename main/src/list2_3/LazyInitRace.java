package list2_3;

import java.sql.SQLOutput;

/**
 * @author jikangwang
 */
public class LazyInitRace {
    private static class LazyInitRaceHolder{
        private static final LazyInitRace instance=new LazyInitRace();
    }
    private static LazyInitRace instance = null;

    public LazyInitRace() {
        System.out.println("initialize");
    }

    public static LazyInitRace getInstance() {
        return LazyInitRaceHolder.instance;
    }
}
