/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.p4.environment.adaptation;

import java.util.Random;

/**
 * @author user
 */
public class Port {
    
    public static int NUM_PORTS          = 255; 
    public static byte CPU               = 1;
    public static byte DEFAULT           = 0;
    public static byte TRAFFIC_INJECTION = 2;
    
    /**
    * Generate a random port between 1 and max (inclusive)
    */
    public static byte generateRandomPort(){
        Random r = new Random();
        //byte v[] = {2, 3, 4, 5, 6, 7, 8, 9};
        byte v[] = {2, 4};
        return v[r.nextInt(v.length)];
        
        //return value between 2 and 254 (1 is CPU and 255 is DEFAULT)
        //return (byte)(2+r.nextInt(NUM_PORTS-2));
    }
    
    public static byte generatePowerOfTwoPort(){
        //(byte)0b00000000 =>  CPU PORT
        //(byte)0b00001111 =>  DEFAULT PORT
        byte possible_ports[] = {(byte)0b00000001,
                                 (byte)0b00000010,
                                 (byte)0b00000100,
                                 (byte)0b00001000,
                                 (byte)0b00010000,
                                 (byte)0b00100000,
                                 (byte)0b01000000,
                                 (byte)0b10000000};
        Random r = new Random();
        return possible_ports[r.nextInt(possible_ports.length)];         
    }
}
