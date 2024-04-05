/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utils.bmv2;

import java.util.List;

/**
 * @author casti
 */
public class P4Runtime {
    
    private String target;
    private String p4info;
    private String bmv2_json;    
    private List<TableEntry> table_entries;

    public P4Runtime(String target, String p4info, String bmv2_json, List<TableEntry> table_entries){ 
        this.target=target;
        this.p4info=p4info;
        this.bmv2_json=bmv2_json;
        this.table_entries = table_entries;
    }

}
