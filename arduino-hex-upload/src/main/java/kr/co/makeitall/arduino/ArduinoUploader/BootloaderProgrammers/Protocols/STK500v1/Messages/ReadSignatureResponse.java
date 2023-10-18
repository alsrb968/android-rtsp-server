package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Constants;

public class ReadSignatureResponse extends Response
{
	public final boolean getIsCorrectResponse()
	{
		return getBytes().length == 4 && getBytes()[3] == Constants.RespStkOk;
	}

	public final byte[] getSignature()
	{

		return new byte[] {getBytes()[0], getBytes()[1], getBytes()[2]};
	}
}
