package kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory;

public class FlashMemory extends Memory {
    @Override
    public MemoryType getType() {
        return MemoryType.Flash;
    }
}
