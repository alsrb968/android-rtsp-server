package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Constants;

public class LoadAddressResponse extends Response
{
	public final boolean getSucceeded()
	{
		return getBytes().length == 2 && getBytes()[0] == Constants.CmdLoadAddress && getBytes()[1] == Constants.StatusCmdOk;
	}
}
