package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Constants;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory.IMemory;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory.MemoryType;

public class LoadAddressRequest extends Request {
    public LoadAddressRequest(IMemory memory, int addr) {
        int modifier = memory.getType() == MemoryType.Flash ? 0x80 : 0x00;

        setBytes(new byte[]{Constants.CmdLoadAddress, (byte) (((addr >> 24) & 0xff) | modifier), (byte) ((addr >> 16) & 0xff), (byte) ((addr >> 8) & 0xff), (byte) (addr & 0xff)});
    }
}
