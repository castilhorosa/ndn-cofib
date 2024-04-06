/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.p4.environment.adaptation;

/**
 *
 * @author user
 */
public class NumberConverter {
    
    //takes a byte and returns its unsigned value (value between 0 and 255)
    public static int unsignedByte(byte value){
        if(value>=0)
            return value;
        return (255+value+1);
    }
    
    
}
