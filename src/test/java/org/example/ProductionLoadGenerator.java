package org.example;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.example.ClientLoadGenerator.generateLoad;

public class ProductionLoadGenerator
{
    public static void main(String[] args) throws Exception
    {
        int resourceRate = 150;
        Duration duration = Duration.ofMinutes(20);
        CountDownLatch latch = new CountDownLatch(2);
        runNetworkTest(false, duration, resourceRate, 100, 1024, "prod-false-", latch);
        runNetworkTest(true, duration, resourceRate, 100, 1024, "prod-true-", latch);

        long startTime = System.currentTimeMillis();
        while (latch.getCount() > 0) {
            long elapsedMillis = System.currentTimeMillis() - startTime;
            String formattedDuration = formatDuration(elapsedMillis);
            System.out.println("Time since start: " + formattedDuration);
            latch.await(5, TimeUnit.SECONDS);
        }

        long elapsedMillis = System.currentTimeMillis() - startTime;
        String formattedDuration = formatDuration(elapsedMillis);
        System.out.println("Completed: " + formattedDuration);
    }

    private static String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static void runNetworkTest(boolean newMode, Duration duration, int resourceRate, int numWrites, int bufferSize, String logPrefix, CountDownLatch latch)
    {
        new Thread(() ->
        {
            String host = "performance-testing-rpc-" + newMode + "-dot-jetty9-work.appspot.com";
            int port = 80;
            try
            {
                generateLoad(duration, resourceRate, host, port, numWrites, bufferSize, logPrefix);
            }
            catch (Exception e)
            {
                e.printStackTrace(System.err);
            }
            finally
            {
                System.err.println("Completed rpc=" + newMode);
                latch.countDown();
            }
        }).start();
    }
}
