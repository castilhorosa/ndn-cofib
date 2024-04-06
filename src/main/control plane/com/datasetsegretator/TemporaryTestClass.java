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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.CRC32;

/**
 * @author user
 */
public class TemporaryTestClass {

    public static void printHistogramTableSizes(DualComponentHashmap dataStructure) throws FileNotFoundException, IOException{
                
        FileReader file = new FileReader(Parameters.DATASET_URL+"\\bmv2_entry_commands.txt");
        BufferedReader fReader = new BufferedReader(file);
        String line;
        long dp_shape_table_size=0;
        long cp_shape_table_size=0;
        while((line=fReader.readLine())!=null){
            String p[] = line.split(" ");
            if(p[1].equals("tShapeDataplane"))
                dp_shape_table_size++;
            else 
                if(p[1].equals("tShapeControlplane"))
                    cp_shape_table_size++;
                else
                    break;
        }
        file.close();
        fReader.close();
        
        System.out.println("dp shape size: "+dp_shape_table_size);
        System.out.println("cp shape size: "+cp_shape_table_size);
        
        long frequencies[] = new long[32];
        Arrays.fill(frequencies, 0);
        Iterator<String> iterator = dataStructure.getHT_C_CAD_HS().keySet().iterator();
        while(iterator.hasNext()){
            frequencies[iterator.next().length()]++;
        }
        for(int i=0; i<frequencies.length; i++){
            System.out.println(i+" ==> "+frequencies[i]);
        }
    }
    
    /**
     * Create and returns a CPDataStructure object;
     * The CPDataStructure contains the following fields: 
     * - HashMap<String, CPEntry> HT_C_CAD_HS (Main Data Structure)
     * - HashMap<Integer,Byte> HT_CONFLICTING (Secondary Data Structure)
     * @ Input: final_canonical_dataset.txt
     * @ Output: An object of type CPDataStructure
     */
    public static DualComponentHashmap createCPDataStructure(String finalCanonicalDatasetPath) throws IOException {
        
        System.out.print("Creating the Control Plane Data Structure..");
        
        FileReader final_canonical_dataset_file = new FileReader(finalCanonicalDatasetPath);
        BufferedReader canonicalFileReader = new BufferedReader(final_canonical_dataset_file);
        
        //It contains two data structures: HT_C_CAD_HS and HT_CONFLICTING
        DualComponentHashmap HT_CP = new DualComponentHashmap();
        
        String line;
        String subprefix;

        CPEntry element;

        while ((line = canonicalFileReader.readLine()) != null) {
            String parts[] = line.split(" ");
            String url = parts[0];
            byte output_port = Byte.parseByte(parts[1]);
            
            if(HT_CP.insertPrefix(url, output_port)){
                HT_CP.incrementNumberOfPrefixes();
            } else {
                System.out.println("ERROR: createCPDataStructure: Prefix "+url+" to output port "+output_port+" can't be inserted..");
                System.exit(0);
            }       
        }

        canonicalFileReader.close();
        final_canonical_dataset_file.close();

        System.out.println("OK");

        return HT_CP;
    }

    public static void printTables(DualComponentHashmap HT_CP) {
        System.out.println("======================================================== CONTENTS OF HT_CPM_CAD_HS TABLE ========================================================");
        for (String key : HT_CP.keySet()) {
            System.out.println(key + " ==> " + HT_CP.get(key));
        }
        System.out.println("=================================================================================================================================================");

        System.out.println("======================================================== CONTENTS OF HT_CONFLICTING TABLE =======================================================");
        for (Integer key : HT_CP.getHTConflicting().keySet()) {
            System.out.println(key + " ==> " + HT_CP.getHTConflicting().get(key));
        }
        System.out.println("=================================================================================================================================================");
    }
    
    /**
     * This method validates if the packets in the file trafficPath are being forwarded
     * to the correct output port by using the method performLpm() in CPDataStructure object.
     * The file trafficPath stores both urls and the output port that such urls are 
     * suppose to be sent to, based on the prefixes in CPDataStructure object.
     * Previous tests have shown that when we use the prefixes in dataset 3 (10M names)
     * as the traffic and the CPDataStructure object contains all final prefixes in dataset 3 (10M names)
     * we expect 3 packets being forwarded to incorrect output ports, as we can see in
     * the method insertPrefix() in CPDataStructure class.
     */
    public static void validateLpm(String trafficPath, DualComponentHashmap dataStructure) throws FileNotFoundException, IOException{
        
        FileReader final_canonical_dataset_file = new FileReader(trafficPath);
        BufferedReader canonicalFileReader = new BufferedReader(final_canonical_dataset_file);
        String line;
        System.out.print("Validating the lpm..");
        boolean validate=true;
        CRC32 crc = new CRC32();
        while((line=canonicalFileReader.readLine())!=null){
            String p[] = line.split(" ");
            byte expected_output_port = Byte.parseByte(p[1]);
            byte obtained_output_port = dataStructure.performLPM(p[0]);
            if(expected_output_port!=obtained_output_port){
                String key = p[0].replaceAll("/", "");
                crc.update(key.getBytes());
                validate=false;
                System.out.println(" Prefix "+p[0]+" Hash: "+((int)crc.getValue())+" => Expected output port: "+expected_output_port+" Obtained output port: "+obtained_output_port);
                crc.reset();
            }            
        }
        if(validate)
            System.out.println("   VALIDADED !!!");
        else 
            System.out.println("   NOT VALIDADED !!!");
        final_canonical_dataset_file.close();
        canonicalFileReader.close();
    }
    
    public static void extract_10K_prefixes_from_1M_canonical_dataset() throws FileNotFoundException, IOException{
        FileReader fReader = new FileReader(Parameters.DATASET_URL+"//final_canonical_dataset.txt");
        BufferedReader bReader = new BufferedReader(fReader);
        String line;
        
        ArrayList<String> list = new ArrayList<>();
                
        long x=0;
        while((line=bReader.readLine())!=null){
            list.add(line);
        }
        fReader.close();
        bReader.close();
        
        Collections.shuffle(list);
        
        FileWriter fWriter = new FileWriter(Parameters.DATASET_URL+"//10k_dataset.txt");
        
        long n=0;
        for(String str : list){
            n++;
            fWriter.write(str+"\n");
            
            if(n==10000) 
                break;
        }
        fWriter.close();
    }
    
    public static void Main(String a[]) throws IOException, FileNotFoundException, NoSuchAlgorithmException {          
        
    }
}
