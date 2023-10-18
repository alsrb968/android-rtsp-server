package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Constants;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory.MemoryType;

public class StartBlockLoadRequest extends Request {

    public StartBlockLoadRequest(MemoryType memType, int blockSize, byte[] bytes) {

        setBytes(new byte[blockSize + 4]);
        getBytes()[0] = Constants.CmdStartBlockLoad;

        getBytes()[1] = (byte) (blockSize >> 8);

        getBytes()[2] = (byte) (blockSize & 0xff);

        getBytes()[3] = (byte) (memType == MemoryType.Flash ? 'F' : 'E');
        System.arraycopy(bytes, 0, getBytes(), 4, blockSize);
    }
}
