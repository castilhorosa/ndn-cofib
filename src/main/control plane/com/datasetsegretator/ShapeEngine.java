/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datasetsegretator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang3.tuple.Triple;

/**
 *
 * @author user
 */
public class ShapeEngine {

     /**
     * This method takes a string shape and the maximum number of components and
     * generate a triple containing a decimal value to be used as match key, 
     * the mask (number of bits indicating the usuful componentes) and the number 
     * of components in that shape. 
     * For example: shape /2/2/4 will produce the triple
     * <decimal(0x0202040000000000), 24, 3>
     * This triple will be useful to generate the p4runtime friendly lpm entry. 
     */
    public static Triple generateP4RuntimeLpmEntry(String shape, int max) {                
        String hexEntry = "";
        String parts[] = shape.split("/");
        for (int i = 1; i <= max; i++) {
            if (i < parts.length) {
                int x = Integer.parseInt(parts[i]);
                String hex = String.format("%02X", x);
                hexEntry = hexEntry + hex;                
            } else {
                hexEntry = hexEntry + "00";
            }
        }        
        return Triple.of(Long.parseLong(hexEntry, 16), ((parts.length - 1) * 8), (parts.length - 1));
    }
        
    /**
     * This method takes a string shape and the maximum number of components and
     * generate a runtime_CLI friendly lpm entry. For example: shape /2/2/4 will
     * produce 0x0202040000000000/24 => 3
     */
    public static String generateCLILpmEntry(String shape, int max) {
        String lpmEntry = "";
        String parts[] = shape.split("/");
        for (int i = 1; i <= max; i++) {
            if (i < parts.length) {
                int x = Integer.parseInt(parts[i]);
                String hex = String.format("%02X", x);
                lpmEntry = lpmEntry + hex;                
            } else {
                lpmEntry = lpmEntry + "00";
            }
        }
        lpmEntry = "0x"+lpmEntry;
        return lpmEntry + "/" + ((parts.length - 1) * 8) + " => " + (parts.length - 1);
    }

    public static String generateShape(String name) {
        String shape = "";
        String parts[] = name.split("/");
        for (int i = 1; i < parts.length; i++) {
            shape = shape + "/" + parts[i].length();
        }
        return shape;
    }

    public static HashMap<String, Boolean>[] getShapeHashTable(String shapePath) throws FileNotFoundException, IOException {

        HashMap<String, Boolean>[] H = (HashMap<String, Boolean>[]) (Map<String, Boolean>[]) new HashMap<?, ?>[8];
        for (int i = 0; i < 8; i++) {
            H[i] = new HashMap<>();
        }

        FileReader dataset_file = new FileReader(shapePath);
        BufferedReader fileReader = new BufferedReader(dataset_file);
        String line;
        while ((line = fileReader.readLine()) != null) {
            String subprefix = "";
            String p[] = line.split("/");
            for (int i = 1; i < p.length; i++) {
                subprefix = subprefix + "/" + p[i];
                Boolean value = H[i - 1].get(subprefix);
                if (value == null) {                    
                    if (i < (p.length - 1)) {
                        H[i - 1].put(subprefix, false);
                    } else {
                        // i == (p.length-1)
                        H[i - 1].put(subprefix, true);
                    }
                } else {
                    if (i < (p.length - 1)) {
                        // at this point does not matter if the value is true or false
                        // I'll just keep it
                    } else {
                        // i == (p.length-1)
                        H[i-1].replace(line, true);
                    }
                }
            }
        }
        dataset_file.close();
        fileReader.close();

        return H;
    }
    
    public static boolean shapeMatch(String url, HashMap<String, Boolean>[] shapeHT){
        boolean shapeMatch=false;
        String subprefix="";
        String shape=generateShape(url);
        String p[] = shape.split("/");
        for(int i=1; i<p.length; i++){
            subprefix=subprefix+"/"+p[i];
            Boolean value = shapeHT[i-1].get(subprefix);
            if(value != null){
                if(value){
                    shapeMatch=true;
                    break;
                }
            } 
        }
        return shapeMatch;
    }
    
    /**
     * This method creates the shape file from some dataset..
     */
    public static long createShapeFile(String datasetPath, String shapePath) throws FileNotFoundException, IOException {
        
        System.out.print("Extracting shapes from the dataset file to shape file... ");

        HashMap<String, Integer> h = new HashMap<>();

        FileReader dataset_file = new FileReader(datasetPath);
        BufferedReader fileReader = new BufferedReader(dataset_file);
        String line = "";
        //String reverse_url = "";
        String shape = "";
        
        while ((line = fileReader.readLine()) != null) {
            String p[] = line.split(" ");
            shape = generateShape(p[0]);
            h.putIfAbsent(shape, shape.length());            
        }
        dataset_file.close();
        fileReader.close();
        
        FileWriter file = new FileWriter(shapePath);
        Iterator<String> iterator = h.keySet().iterator();
        while(iterator.hasNext()){
            file.write(iterator.next()+"\n");
        }
        System.out.println(" Total of shapes: "+h.size()+"   OK");
        file.close();
        return h.size();
    }
}