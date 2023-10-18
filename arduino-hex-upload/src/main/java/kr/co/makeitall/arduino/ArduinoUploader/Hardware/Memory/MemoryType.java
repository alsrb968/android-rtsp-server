package kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory;

public enum MemoryType {
    Flash,
    Eeprom;

    public static final int SIZE = Integer.SIZE;

    public int getValue() {
        return this.ordinal();
    }

    public static MemoryType forValue(int value) {
        return values()[value];
    }
}
