package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;

public class ReadSignatureBytesResponse extends Response
{

	public final byte[] getSignature()
	{

		return new byte[] {getBytes()[2], getBytes()[1], getBytes()[0]};
	}
}
