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
public class SortByWeightRank implements Comparator<WeightedURLWithRank> {
    
    @Override
    public int compare(WeightedURLWithRank t1, WeightedURLWithRank t2) {
        return (t1.getWeight()-t2.getWeight());
    }
    
}
