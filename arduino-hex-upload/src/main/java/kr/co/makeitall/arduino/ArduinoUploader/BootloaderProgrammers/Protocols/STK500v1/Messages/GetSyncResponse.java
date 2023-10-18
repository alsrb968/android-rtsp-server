package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Constants;

public class GetSyncResponse extends Response
{
	public final boolean getIsInSync()
	{
		return getBytes().length > 0 && getBytes()[0] == Constants.RespStkInsync;
	}
}
