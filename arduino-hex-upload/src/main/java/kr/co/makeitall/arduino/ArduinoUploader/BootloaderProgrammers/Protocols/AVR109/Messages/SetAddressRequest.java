package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Constants;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;

public class SetAddressRequest extends Request
{
	public SetAddressRequest(int offset)
	{

		setBytes(new byte[] {Constants.CmdSetAddress, (byte)((offset >> 8) & 0xff), (byte)(offset & 0xff)});
	}
}
