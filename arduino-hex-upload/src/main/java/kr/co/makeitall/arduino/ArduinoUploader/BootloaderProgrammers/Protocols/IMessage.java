package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols;

public interface IMessage {
    byte[] getBytes();

    void setBytes(byte[] value);
}
