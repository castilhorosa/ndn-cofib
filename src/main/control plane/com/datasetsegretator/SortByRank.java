/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datasetsegretator;

import java.util.Comparator;

public class SortByRank implements Comparator<WeightedURLWithRank> {
    
    @Override
    public int compare(WeightedURLWithRank t1, WeightedURLWithRank t2) {        
        int v=0;        
        if((t1.getRank()-t2.getRank())<0)
            v=1;
        else {
            if((t1.getRank()-t2.getRank())>0)
                v=-1;
        }
        return v;
    }    
}
