/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utils.bmv2;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 *
 * @author casti
 */
public class Match {
    
    @SerializedName("hdr.ipv4.dstAddr")
    private List<Object> field1_dstAddr;     
    @SerializedName("meta.crc32")
    private List<Long> field2_crc32;
    @SerializedName("meta.Fx")
    private List<Long> field3_hct;
    @SerializedName("meta.name_shape")
    private List<Long> field4_name_shape;
    
    public Match(List<Object> field1_dstAddr, List<Long> field2_crc32, List<Long> field3_hct, List<Long> field4_name_shape) {
        this.field1_dstAddr = field1_dstAddr;
        this.field2_crc32 = field2_crc32;
        this.field3_hct = field3_hct;
        this.field4_name_shape = field4_name_shape;
    }
}
