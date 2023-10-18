package kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory;

public interface IMemory {
    MemoryType getType();

    int getSize();

    int getPageSize();

    byte getPollVal1();

    byte getPollVal2();

    byte getDelay();

    byte[] getCmdBytesRead();

    byte[] getCmdBytesWrite();
}
