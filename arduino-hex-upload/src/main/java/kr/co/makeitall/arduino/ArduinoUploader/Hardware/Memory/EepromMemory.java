package kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory;

public class EepromMemory extends Memory {
    @Override
    public MemoryType getType() {
        return MemoryType.Eeprom;
    }
}
