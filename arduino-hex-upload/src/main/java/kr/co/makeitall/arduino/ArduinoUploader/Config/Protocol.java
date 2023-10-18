package kr.co.makeitall.arduino.ArduinoUploader.Config;


public enum Protocol {
    Stk500v1,
    Stk500v2,
    Avr109;

    public static final int SIZE = Integer.SIZE;

    public int getValue() {
        return this.ordinal();
    }

    public static Protocol forValue(int value) {
        return values()[value];
    }
}
