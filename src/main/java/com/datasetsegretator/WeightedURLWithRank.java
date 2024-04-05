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
public class WeightedURLWithRank {
    
    private String url;
    private byte output_port;
    private int weight;
    private long rank;
    private double openPageRank;
    
    public WeightedURLWithRank(String url, byte output_port, int weight, long rank, double openPageRank) {
        this.url = url;
        this.output_port=output_port;
        this.weight = weight;
        this.rank = rank;
        this.openPageRank = openPageRank;
    }

    public double getOpenPageRank() {
        return openPageRank;
    }

    public void setOpenPageRank(double openPageRank) {
        this.openPageRank = openPageRank;
    }
    
    public int getWeight() {
        return weight;
    }

    public String getUrl() {
        return url;
    }

    public long getRank() {
        return rank;
    }

    public void setRank(long rank) {
        this.rank = rank;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public byte getOutputPort() {
        return output_port;
    }   
    
 
    public String toString(){
        return url+", "+weight;
    }
}
