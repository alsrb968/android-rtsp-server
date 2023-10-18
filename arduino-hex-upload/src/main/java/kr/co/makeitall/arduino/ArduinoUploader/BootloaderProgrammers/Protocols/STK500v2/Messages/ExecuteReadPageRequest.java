package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory.IMemory;

public class ExecuteReadPageRequest extends Request {

    public ExecuteReadPageRequest(byte readCmd, IMemory memory) {
        int pageSize = memory.getPageSize();
        byte cmdByte = memory.getCmdBytesRead()[0];
        setBytes(new byte[]{readCmd, (byte) (pageSize >> 8), (byte) (pageSize & 0xff), cmdByte});
    }
}
