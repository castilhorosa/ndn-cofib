/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datasetsegretator;

import com.p4.environment.adaptation.Port;
import com.p4.environment.adaptation.StringConverter;

/**
 * @CAD = Component Action Data
 */
public class CAD {

    private boolean _continue;
    private boolean _end_prefix;
    private boolean _confliting;
    private byte _position;
    private byte _output_ports;
    private String _hs;

    public CAD() {
        this._continue = true;
        this._end_prefix = false;
        this._confliting = false;
        this._position = 1;
        this._output_ports = Port.DEFAULT;
        this._hs = "0000000000";
    }

    /*
    public CAD(boolean _continue, boolean _end_prefix, boolean _confliting, byte _position, byte _output_ports, String _hs) {
        this._continue = _continue;
        this._end_prefix = _end_prefix;
        this._confliting = _confliting;
        this._position = _position;
        this._output_ports = _output_ports;
        if (_hs.length() == 10) {
            this._hs = _hs;
        } else {
            System.out.println("Error in CAD constructor. hs parameter should be 10-bit long.");
            System.exit(0);
        }
    }*/

    public void setHs(String _hs) {
        if (_hs.length() == 10) {
            this._hs = _hs;
        }else {
            System.out.println("Error in CAD setHs() method. hs parameter should be 10-bit long.");
            System.exit(0);
        }
    }

    public String getHs() {
        return _hs;
    }

    public boolean isContinue() {
        return _continue;
    }

    public void setContinue(boolean _continue) {
        this._continue = _continue;
    }

    public boolean isEndPrefix() {
        return _end_prefix;
    }

    public void setEndPrefix(boolean _end_prefix) {
        this._end_prefix = _end_prefix;
    }

    public boolean isConfliting() {
        return _confliting;
    }

    public void setConfliting(boolean _confliting) {
        this._confliting = _confliting;
    }

    public byte getPosition() {
        return _position;
    }

    public void setPosition(byte _position) {
        this._position = _position;
    }

    public byte getOutputPorts() {
        return _output_ports;
    }

    public void setOutputPorts(byte _output_ports) {
        this._output_ports = _output_ports;
    }

    public int getCADInteger(){
        return Integer.parseInt(getCADBits(), 2);
    }
    
    public String getCADBits() {
        String bits = "";
        if (this._continue) {
            bits = "1";
        } else {
            bits = "0";
        }
        if (this._end_prefix) {
            bits = bits + "1";
        } else {
            bits = bits + "0";
        }
        if (this._confliting) {
            bits = bits + "1";
        } else {
            bits = bits + "0";
        }
        bits = bits + positionNumberToPositionString();
        
        //String ports = Integer.toBinaryString(_output_ports);
        String ports = StringConverter.convertByteToBitstring(_output_ports);
        if(ports.length() != 8){
            System.out.println("Error in getCADBits(). 'ports' should be 8-bit long.");
            System.exit(0);
        }
        
        bits = bits+ports+_hs;
        
        //the length of bits is suppose to be 24
        if (bits.length() != 24) {
            System.out.println("ERROR: getCADBits() is suppose to return a 24-length string but its value is " + bits.length() + "-length. " + bits);
            System.exit(0);
        }
        //return bits;
        return "0b" + bits;
    }

    private String positionNumberToPositionString() {
        String positionString = "";
        switch (this.getPosition()) {
            case 1:
                positionString = "000";
                break;
            case 2:
                positionString = "001";
                break;
            case 3:
                positionString = "010";
                break;
            case 4:
                positionString = "011";
                break;
            case 5:
                positionString = "100";
                break;
            case 6:
                positionString = "101";
                break;
            case 7:
                positionString = "110";
                break;
            case 8:
                positionString = "111";
        }
        return positionString;
    }
}