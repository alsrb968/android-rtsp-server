package kr.co.makeitall.arduino.ArduinoUploader.Hardware;

public enum Command
{
	PgmEnable,
	ReadFlash,
	ReadEeprom;

	public static final int SIZE = Integer.SIZE;

	public int getValue()
	{
		return this.ordinal();
	}

	public static Command forValue(int value)
	{
		return values()[value];
	}
}
