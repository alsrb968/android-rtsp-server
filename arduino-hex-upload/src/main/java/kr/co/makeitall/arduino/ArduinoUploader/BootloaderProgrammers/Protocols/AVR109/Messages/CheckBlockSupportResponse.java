package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;

public class CheckBlockSupportResponse extends Response
{
	public final boolean getHasBlockSupport()
	{

		return getBytes()[0] == (byte) 'Y';
	}

	public final int getBufferSize()
	{
		return (getBytes()[1] << 8) + getBytes()[2];
	}
}
