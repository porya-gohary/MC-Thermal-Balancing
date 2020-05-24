import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ProgressBar extends Thread {
    boolean showProgress = true;
    private int percent = 0;
    private String Method="";

    public void run() {
        Date startDateTime = new Date(System.currentTimeMillis());
        String ind = "│";
        String finished = "████████████████████";
        String unfinished = "                    ";
        String working = "...";
        String working2 = "   ";
        int x = 0;
        Instant end = Instant.now();
        while (showProgress) {
            Date endDateTime = new Date();
            Map<TimeUnit,Long> timeElapsed = computeDiff(startDateTime, endDateTime);
            System.out.print(" Processing "+Method+" " + percent + "% " + ind
                    + finished.substring(0, (percent * finished.length() / 100))
                    + unfinished.substring(0, unfinished.length() - (percent * unfinished.length() / 100))
                    + ind + "  "
                    +"( "
                    +"D"+timeElapsed.get(TimeUnit.DAYS)+" - "
                    +timeElapsed.get(TimeUnit.HOURS)+":"
                    + timeElapsed.get(TimeUnit.MINUTES)+":"
                    + timeElapsed.get(TimeUnit.SECONDS)
                    +" )"
                    +"  "+ working.substring(0, x / 10)+ working2.substring(0, 3-x / 10)
                    + "\r");
            if (x < 39) x++;
            else x = 0;
//            System.out.println("HELLO");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public void setMethod(String method) {
        Method = method;
    }

    public Map<TimeUnit,Long> computeDiff(Date date1, Date date2) {
        long diffInMilliSeconds = date2.getTime() - date1.getTime();
        List<TimeUnit> units = new ArrayList<TimeUnit>(EnumSet.allOf(TimeUnit.class));
        Collections.reverse(units);
        Map<TimeUnit,Long> result = new LinkedHashMap<TimeUnit,Long>();
        long milliSecondsRest = diffInMilliSeconds;
        for (TimeUnit unit : units) {
            long diff = unit.convert(milliSecondsRest,TimeUnit.MILLISECONDS);
            long diffInMilliSecondsForUnit = unit.toMillis(diff);
            milliSecondsRest = milliSecondsRest - diffInMilliSecondsForUnit;
            result.put(unit,diff);
        }
        return result;
    }
}
