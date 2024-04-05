/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utils.bmv2;

/**
 *
 * @author casti
 */
public class ActionParams {
    
    private String dstAddr;
    private Integer port;
    private Integer cad;
    private Byte swId;
    private Integer number_name_components;
    
    public ActionParams(String dstAddr, Integer port, Integer cad, Byte swId, Integer number_name_components) {
        this.dstAddr = dstAddr;
        this.port = port;
        this.cad = cad;
        this.swId = swId;
        this.number_name_components = number_name_components;
    }
}