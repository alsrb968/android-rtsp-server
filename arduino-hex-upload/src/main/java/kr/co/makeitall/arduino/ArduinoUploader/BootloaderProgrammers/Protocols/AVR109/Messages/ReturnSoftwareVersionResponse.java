package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;

public class ReturnSoftwareVersionResponse extends Response
{
	public final char getMajorVersion()
	{
		return (char) getBytes()[0];
	}

	public final char getMinorVersion()
	{
		return (char) getBytes()[1];
	}
}
