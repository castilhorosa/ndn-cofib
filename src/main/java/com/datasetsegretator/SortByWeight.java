/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datasetsegretator;

import java.util.Comparator;

/**
 *
 * @author user
 */
public class SortByWeight implements Comparator<WeightedURL> {
    
    @Override
    public int compare(WeightedURL t1, WeightedURL t2) {
        return (t1.getWeight()-t2.getWeight());
    }
    
}
