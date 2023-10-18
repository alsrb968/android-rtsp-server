package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers;

import kr.co.makeitall.arduino.ArduinoUploader.ArduinoSketchUploader;
import kr.co.makeitall.arduino.ArduinoUploader.IArduinoUploaderLogger;
import kr.co.makeitall.arduino.CSharpStyle.Func0;
import kr.co.makeitall.arduino.CSharpStyle.Func3;

import java.util.concurrent.*;

public final class WaitHelper {
    private static IArduinoUploaderLogger getLogger() {
        return ArduinoSketchUploader.getLogger();
    }

    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile static Thread workingThread;

    public static <T> T WaitFor(int timeout, int interval, Func0<T> toConsider, Func3<Integer, T, Integer, String> format) {
        Future<T> task = executor.submit(() -> {
            workingThread = Thread.currentThread();
            int i = 0;
            while (!workingThread.isInterrupted()) {
                T item = toConsider.invoke();
                if (getLogger() != null) getLogger().onInfo(format.invoke(i, item, interval));
                if (item != null) return item;
                i++;
                Thread.sleep(interval);
            }
            return null;
        });
        try {
            return task.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            workingThread.interrupt();
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (CancellationException e) {
            e.printStackTrace();
        }
        boolean canceled = task.cancel(true);
        executor.shutdown();
        return null;
    }
}
