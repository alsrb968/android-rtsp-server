package kr.co.makeitall.arduino.ArduinoUploader.Hardware;

import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory.EepromMemory;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory.FlashMemory;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory.IMemory;

import java.util.*;

public class AtMega32U4 extends Mcu {

    @Override
    public byte getDeviceCode() {
        return (byte)0x44;
    }

    @Override
    public byte getDeviceRevision() {
        return (byte)0;
    }


    @Override
    public byte getProgType() {
        return (byte)0;
    }


    @Override
    public byte getParallelMode() {
        return (byte)0;
    }


    @Override
    public byte getPolling() {
        return (byte)0;
    }

    @Override
    public byte getSelfTimed() {
        return (byte)0;
    }

    @Override
    public byte getLockBytes() {
        return (byte)0;
    }

    @Override
    public byte getFuseBytes() {
        return (byte)0;
    }

    @Override
    public byte getTimeout() {

        return (byte) 200;
    }

    @Override
    public byte getStabDelay() {
        return (byte)100;
    }


    @Override
    public byte getCmdExeDelay() {
        return (byte)25;
    }

    @Override
    public byte getSynchLoops() {
        return (byte)32;
    }


    @Override
    public byte getByteDelay() {
        return (byte)0;
    }

    @Override
    public byte getPollIndex() {
        return (byte)3;
    }


    @Override
    public byte getPollValue() {
        return (byte)0x53;
    }

    @Override
    public String getDeviceSignature() {
        return "1E-95-87";
    }


    @Override
    public Map<Command, byte[]> getCommandBytes() {

        return new HashMap<Command, byte[]>();
    }

    @Override
    public List<IMemory> getMemory() {
        FlashMemory tempVar = new FlashMemory();
        tempVar.setSize(32 * 1024);
        tempVar.setPageSize(128);
        tempVar.setPollVal1((byte) 0xff);
        tempVar.setPollVal2((byte) 0xff);
        EepromMemory tempVar2 = new EepromMemory();
        tempVar2.setSize(1024);
        tempVar2.setPollVal1((byte) 0xff);
        tempVar2.setPollVal2((byte) 0xff);
        return new ArrayList<IMemory>(Arrays.asList(tempVar, tempVar2));
    }
}
