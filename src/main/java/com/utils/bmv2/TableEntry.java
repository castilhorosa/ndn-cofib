/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utils.bmv2;

/**
 *
 * @author casti
 */
public class TableEntry {
    
    private String table;    
    private Match match;
    private String action_name;
    private ActionParams action_params;

    public TableEntry(String table, Match match, String action_name, ActionParams action_params) {
        this.table = table;
        this.match = match;
        this.action_name = action_name;
        this.action_params = action_params;
    }      
}