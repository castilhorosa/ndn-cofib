/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datasetsegretator;

/**
 *
 * @author user
 */
public class WeightedURL {
    
    private String url;
    private byte output_port;
    private int weight;
        
    public WeightedURL(String url, byte output_port, int weight) {
        this.url = url;
        this.output_port=output_port;
        this.weight = weight;        
    }

    public byte getOutputPort() {
        return output_port;
    }
 
    public int getWeight() {
        return weight;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
 
    public String toString(){
        return url+", "+weight;
    }
}
