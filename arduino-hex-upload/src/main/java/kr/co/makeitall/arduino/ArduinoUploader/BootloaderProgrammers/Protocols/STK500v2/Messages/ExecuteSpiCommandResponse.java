package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;

public class ExecuteSpiCommandResponse extends Response
{

	public final byte getAnswerId()
	{
		return getBytes()[0];
	}

	public final byte getStatus()
	{
		return getBytes()[1];
	}
}
