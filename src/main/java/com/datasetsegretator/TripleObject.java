/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.datasetsegretator;

/**
 *
 * @author casti
 */
public class TripleObject implements Comparable<TripleObject> {
    private long frequency;
    private byte position;
    private byte size;

    public TripleObject(long frequency, byte position, byte size) {
        this.frequency = frequency;
        this.position = position;
        this.size = size;
    }

    public long getFrequency() {
        return frequency;
    }

    public byte getPosition() {
        return position;
    }

    public byte getSize() {
        return size;
    }

    @Override
    public int compareTo(TripleObject o) {
        return Long.signum(o.getFrequency() - this.getFrequency());
    }

    @Override
    public String toString() {
        return "TripleObject{" + "frequency=" + frequency + ", position=" + position + ", size=" + size + '}';
    }
    
    
}
