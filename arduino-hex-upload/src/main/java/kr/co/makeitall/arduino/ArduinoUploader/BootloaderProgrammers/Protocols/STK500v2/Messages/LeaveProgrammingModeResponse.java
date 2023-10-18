package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Constants;

public class LeaveProgrammingModeResponse extends Response
{
	public final boolean getSuccess()
	{
		return getBytes().length == 2 && getBytes()[0] == Constants.CmdLeaveProgmodeIsp && getBytes()[1] == Constants.StatusCmdOk;
	}
}
