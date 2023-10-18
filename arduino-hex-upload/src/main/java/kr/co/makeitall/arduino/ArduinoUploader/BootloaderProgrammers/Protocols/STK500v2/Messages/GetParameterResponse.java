package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Constants;

public class GetParameterResponse extends Response {
    public final boolean getIsSuccess() {
        return getBytes().length > 2 && getBytes()[0] == Constants.CmdGetParameter && getBytes()[1] == Constants.StatusCmdOk;
    }

    public final byte getParameterValue() {
        return getBytes()[2];
    }
}
