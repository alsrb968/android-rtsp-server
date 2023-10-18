package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Constants;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;

public class ReturnSoftwareIdentifierRequest extends Request
{
	public ReturnSoftwareIdentifierRequest()
	{

		setBytes(new byte[] {Constants.CmdReturnSoftwareIdentifier});
	}
}
