package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;

public class ReturnProgrammerTypeResponse extends Response
{
	public final char getProgrammerType()
	{
		return (char) getBytes()[0];
	}
}
