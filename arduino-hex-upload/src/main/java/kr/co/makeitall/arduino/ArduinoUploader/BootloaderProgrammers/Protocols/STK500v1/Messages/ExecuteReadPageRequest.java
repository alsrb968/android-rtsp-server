package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Constants;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory.MemoryType;

public class ExecuteReadPageRequest extends Request {
    public ExecuteReadPageRequest(MemoryType memType, int pageSize) {

        setBytes(new byte[5]);
        getBytes()[0] = Constants.CmdStkReadPage;
        getBytes()[1] = (byte) ((pageSize >> 8) & 0xff);
        getBytes()[2] = (byte) (pageSize & 0xff);
        getBytes()[3] = (byte) (memType == MemoryType.Eeprom ? 'E' : 'F');
        getBytes()[4] = Constants.SyncCrcEop;
    }
}
