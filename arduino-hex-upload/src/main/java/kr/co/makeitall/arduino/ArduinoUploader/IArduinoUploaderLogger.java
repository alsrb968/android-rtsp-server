package kr.co.makeitall.arduino.ArduinoUploader;

public interface IArduinoUploaderLogger {
    void onError(String message, Exception exception);

    void onWarn(String message);

    void onInfo(String message);

    void onDebug(String message);

    void onTrace(String message);
}
