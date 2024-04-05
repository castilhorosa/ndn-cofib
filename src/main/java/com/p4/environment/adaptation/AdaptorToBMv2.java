/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.p4.environment.adaptation;

import com.datasetsegretator.DatasetSegretator;
import com.datasetsegretator.Parameters;
import com.datasetsegretator.ShapeEngine;
import com.datasetsegretator.DualComponentHashmap;
import com.datasetsegretator.CPEntry;
import com.datasetsegretator.TemporaryTestClass;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;

/**
 * This class contains methods to adapt the control plane data, most of them
 * generated in class DatasetSegregator, to the data plane.
 */
public class AdaptorToBMv2 {

    private static void removeCrossConflictingPrefixes(DualComponentHashmap dataStructure, String filePath) throws FileNotFoundException, IOException {
        FileReader final_dataset_file = new FileReader(filePath);
        BufferedReader fileReader = new BufferedReader(final_dataset_file);
        FileWriter fileWriter = new FileWriter(Parameters.DATASET_URL + "\\non_canonical_exact_prefixes_to_traffic_without_cross_conflicting.txt");
        String line;
        while ((line = fileReader.readLine()) != null) {

        }
    }

    /*
      Generate packets following a template file and a given number of packets per second and duration.
      A template file contains packets with 1 up to 8 name components, each of which matching in all
      possible combination of ingress and egress tables. 
      The packets will be shuffled.
     */
    public static void generateFinalPacketsTxtRandom(String templateFile, int pps, int duration) throws FileNotFoundException, IOException {
        //duration per component 
        int t_chunk = duration / 8;
        int pkts_per_chunk = pps * t_chunk;

        List<String> L[] = new ArrayList[9];
        for (int i = 0; i < L.length; i++) {
            L[i] = new ArrayList<>();
        }

        FileReader file = new FileReader(templateFile);
        BufferedReader rTemplateFile = new BufferedReader(file);

        for (int i = 1; i <= 8; i++) {
            int power = (int) Math.pow(2, i);
            for (int j = 1; j <= power; j++) {
                L[i].add(rTemplateFile.readLine());
            }
        }

        file.close();
        rTemplateFile.close();

        ArrayList<String> packets_list = new ArrayList<>();
        long packets = 0;
        for (int i = 1; i <= 8; i++) {
            int power = (int) Math.pow(2, i);
            int index = 0;
            for (int j = 1; j <= pkts_per_chunk; j++) {
                if (index < power) {
                    //fileOutput.write(L[i].get(index)+"\n");   
                    packets_list.add(L[i].get(index));
                    packets++;
                } else {
                    index = 0;
                    //fileOutput.write(L[i].get(index)+"\n");
                    packets_list.add(L[i].get(index));
                    packets++;
                }
                index++;
            }
        }

        Collections.shuffle(packets_list);

        FileWriter fileOutput = new FileWriter("packets.txt");
        for (String url : packets_list) {
            fileOutput.write(url + "\n");
        }

        fileOutput.flush();
        fileOutput.close();

        System.out.println("Packets (.txt) generated: " + packets);

        //we need to print the packet time interval 
        int delay = (int) 1000000 / pps;
        System.out.println("Packets time interval to meet the traffic requirements is " + delay + "us");
    }

    /*
      Generate packets following a template file and a given number of packets per second and duration.
      A template file contains packets with 1 up to 8 name components, each of which matching in all
      possible combination of ingress and egress tables. 
      The packets are ordered in the number of name components.
     */
    public static void generateFinalPacketsTxtSorted(String templateFile, int pps, int duration) throws FileNotFoundException, IOException {
        //duration per component 
        int t_chunk = duration / 8;
        int pkts_per_chunk = pps * t_chunk;

        List<String> L[] = new ArrayList[9];
        for (int i = 0; i < L.length; i++) {
            L[i] = new ArrayList<>();
        }

        FileReader file = new FileReader(templateFile);
        BufferedReader rTemplateFile = new BufferedReader(file);

        for (int i = 1; i <= 8; i++) {
            int power = (int) Math.pow(2, i);
            for (int j = 1; j <= power; j++) {
                L[i].add(rTemplateFile.readLine());
            }
        }
        file.close();
        rTemplateFile.close();

        FileWriter fileOutput = new FileWriter("packets.txt");
        long packets = 0;
        for (int i = 1; i <= 8; i++) {
            int power = (int) Math.pow(2, i);
            int index = 0;
            for (int j = 1; j <= pkts_per_chunk; j++) {
                if (index < power) {
                    fileOutput.write(L[i].get(index) + "\n");
                    packets++;
                } else {
                    index = 0;
                    fileOutput.write(L[i].get(index) + "\n");
                    packets++;
                }
                index++;
            }
        }

        System.out.println("Packets (.txt) generated: " + packets);

        //we need to print the packet time interval 
        int delay = (int) 1000000 / pps;
        System.out.println("Packets time interval to meet the traffic requirements is " + delay + "us");
    }

    private static void generatePrefixesToDataPlane(String finalCanonicalPath, int n_prefixes, DualComponentHashmap dataStructure) throws FileNotFoundException, IOException {

        //Generating the file 1: 'prefixes_to_data_plane.txt'
        System.out.print("  1: Generating the file 'prefixes_to_data_plane.txt'..");
        ArrayList<String> prefixesList = new ArrayList<>();
        Random r = new Random();
        FileReader final_dataset_file = new FileReader(finalCanonicalPath);
        BufferedReader fileReader = new BufferedReader(final_dataset_file);
        String line;
        while ((line = fileReader.readLine()) != null) {
            prefixesList.add(line);
        }
        final_dataset_file.close();
        fileReader.close();

        FileWriter fPrefixes = new FileWriter(Parameters.DATASET_URL + "\\prefixes_to_data_plane.txt");
        int last = prefixesList.size();
        long n = 0;

        for (int i = 1; i <= n_prefixes; i++) {
            boolean found = false;
            do {
                int index = r.nextInt(last);
                line = prefixesList.get(index);
                String p[] = line.split(" ");
                String prefix = p[0];
                if (!dataStructure.isPrefixConflicting(prefix)) {
                    fPrefixes.write(line + "\n");
                    String aux = prefixesList.get(last - 1);
                    prefixesList.set(last - 1, line);
                    prefixesList.set(index, aux);
                    last--;
                    found = true;
                    n++;

                }

            } while (!found);
        }
        System.out.println(" " + n + " non-conflicting canonical prefixes extracted to prefixes_to_data_plane.txt.. OK");
        fPrefixes.close();
        prefixesList.clear();
        dataStructure.clear();   //we can clear all entries in dataStructure..
    }

    private static void generatePrefixesToControlPlane(String finalNonCanonicalPath, int n_prefixes) throws IOException {

        //Generating the file 3: 'prefixes_to_control_plane.txt'
        System.out.print("  3: Generating the file 'prefixes_to_control_plane.txt'..");
        FileReader final_dataset_file = new FileReader(finalNonCanonicalPath);
        BufferedReader fileReader = new BufferedReader(final_dataset_file);
        ArrayList<String> prefixesList = new ArrayList<>();
        String line = "";
        while ((line = fileReader.readLine()) != null) {
            prefixesList.add(line);
        }
        final_dataset_file.close();
        fileReader.close();

        FileWriter fPrefixes = new FileWriter(Parameters.DATASET_URL + "\\prefixes_to_control_plane.txt");
        int n = 0;
        Random r = new Random();
        int last = prefixesList.size();
        for (int i = 1; i <= n_prefixes; i++) {
            int index = r.nextInt(last);
            line = prefixesList.get(index);
            fPrefixes.write(line + "\n");
            String aux = prefixesList.get(last - 1);
            prefixesList.set(last - 1, line);
            prefixesList.set(index, aux);
            last--;
            n++;
        }
        System.out.println(" " + n + " non-canonical prefixes extracted to prefixes_to_control_plane.txt.. OK");
        fPrefixes.close();
        prefixesList.clear();
    }

    private static void generateCanonicalExactPrefixesToTraffic(DualComponentHashmap dataStructure, int n_urls_traffic) throws FileNotFoundException, IOException {
        FileReader final_dataset_file = new FileReader(Parameters.DATASET_URL + "\\prefixes_to_data_plane.txt");
        BufferedReader fileReader = new BufferedReader(final_dataset_file);
        String line = "";
        ArrayList<String> prefixesList = new ArrayList<>();
        while ((line = fileReader.readLine()) != null) {
            prefixesList.add(line);
        }
        final_dataset_file.close();
        fileReader.close();
        //Each line in this file should contain:
        //<prefix> <expected_output_port>
        //However, the expected output port is the same as it is in 'prefixes_to_data_plane.txt' file (we dont need to call performLpm() method)
        FileWriter fPrefixes = new FileWriter(Parameters.DATASET_URL + "\\canonical_exact_prefixes_to_traffic.txt");
        int n = 0;
        int last = prefixesList.size();
        Random r = new Random();
        for (int i = 1; i <= n_urls_traffic; i++) {
            int index = r.nextInt(last);
            line = prefixesList.get(index);
            String p[] = line.split(" ");
            String prefix = p[0];

            String output_port = p[1];    //I don't need to change this to dataStructure.performLpm()

            fPrefixes.write(prefix + " " + output_port + "\n");   //stores only the prefix and the output port (we dont need the other fields..)
            String aux = prefixesList.get(last - 1);
            prefixesList.set(last - 1, line);
            prefixesList.set(index, aux);
            last--;
            n++;
        }
        System.out.println(" " + n + " exact prefixes extracted from prefixes_to_data_plane.txt to canonical_exact_prefixes_to_traffic.txt.. OK");
        fPrefixes.close();
        prefixesList.clear();
    }

    private static void generateCanonicalVariationsPrefixesToTraffic(DualComponentHashmap dataStructure, HashMap<String, Boolean>[] dpShapeHashTable, HashMap<String, Boolean>[] cpShapeHashTable, int n_urls_traffic) throws IOException {
        FileReader final_dataset_file = new FileReader(Parameters.DATASET_URL + "\\prefixes_to_data_plane.txt");
        BufferedReader fileReader = new BufferedReader(final_dataset_file);
        ArrayList<String> prefixesList = new ArrayList<>();
        String line = "";
        while ((line = fileReader.readLine()) != null) {
            prefixesList.add(line);
        }
        final_dataset_file.close();
        fileReader.close();

        FileWriter fPrefixes = new FileWriter(Parameters.DATASET_URL + "\\canonical_variations_prefixes_to_traffic.txt");
        int n = 0;
        Random r = new Random();
        int last = prefixesList.size();
        for (int i = 1; i <= n_urls_traffic; i++) {
            int index = r.nextInt(last);

            line = prefixesList.get(index);
            String parts[] = line.split(" ");
            String prefix = parts[0];

            //I varies to less and more name components in 'line'..
            String prefix_varied = prefix;
            if (r.nextInt(2) == 0) {
                //Varies to less.. For example: Prefix /a/b/c is varied to /a/b or to /a
                //     /a/b/c/d/e    =>  "", "a", "b", "c", "d", "e"
                //                       0    1    2    3    4    5
                String p[] = prefix.split("/");
                int length = p.length - 1;

                if (length > 1) {
                    //E.g: /a/b/c
                    int x = length - 2;
                    index = 2 + r.nextInt(x + 1);    // 0, 1
                    prefix_varied = DatasetSegretator.subprefix(prefix, p[index]);
                }

                //-------------------------------------------------------------------------------
                //Determining the expected output port. This piece of code 
                //reflects exacly the same samantic of the P4 code 
                Byte expected_output_port;
                if (!ShapeEngine.shapeMatch(prefix_varied, dpShapeHashTable)) {
                    if (ShapeEngine.shapeMatch(prefix_varied, cpShapeHashTable)) {
                        expected_output_port = Port.CPU;  //CPU port 
                    } else {
                        expected_output_port = Port.DEFAULT;  //DEFAULT port
                    }
                } else {
                    //checking if the first name component matches.. if it doesn't,
                    //the packet should be sent to CPU (see in P4 code)
                    String p1[] = prefix_varied.split("/");
                    //Taking the first name component parts[1] and checking whether or not it exists in the correct position..
                    CPEntry value = dataStructure.getHT_C_CAD_HS().get(p1[1]);
                    if (value == null) {
                        //the word doest not exist.. the packet should be sent to CPU
                        expected_output_port = Port.CPU; //CPU port
                    } else {
                        //the word do exist but we need to check if its position is correct
                        if (value.getCad().getPosition() != 1) {
                            //if the prefix shape hits both dp_shape_table and cp_shape_table,
                            //the first word in prefix exist in the data plane table but
                            //in another position, we are sure that such prefix does not
                            //exist in the data plane. However, it may be possible that
                            //such prefix exist in the control plane. Therefore, the
                            //expected output port is CPU
                            expected_output_port = Port.CPU; //CPU port
                        } else {
                            //Since in the data plane we don't have any prefix that
                            //is subprefix of non-canonical prefixes in the control plane
                            //once the first word hit in the data plane table, the packet
                            //should be sent to the output port resulted of a NLPM..                            
                            expected_output_port = dataStructure.performLPM(prefix_varied);
                        }
                    }
                }
                //Save both url and the output port into the file assuring that its unique
                fPrefixes.write(prefix_varied + " " + expected_output_port + "\n");
                //-------------------------------------------------------------------------------

            } else {
                //Varies to more.. For example: Prefix /a/b/c is varied to /a/b/c/sxaa/dw or /a/b/c/qw/ddf/vbn, etc
                String p[] = prefix.split("/");
                int length = p.length - 1;

                String alphabet = "abcdefghijklmnopqrstyuvxzw";
                //How many name components will be added?
                int n_comps = 1 + r.nextInt(8 - length);     // 0, 1, 2, 3, 4
                for (int j = 1; j <= n_comps; j++) {
                    //Generate the name component..
                    int n_size = 1 + r.nextInt(31);
                    String w = "";
                    for (int k = 1; k <= n_size; k++) {
                        w = w + alphabet.charAt(r.nextInt(alphabet.length()));
                    }
                    prefix = prefix + "/" + w;
                }
                prefix_varied = prefix;

                //-------------------------------------------------------------------------------
                //Determining the expected output port. This piece of code 
                //reflects exacly the same samantic of the P4 code 
                Byte expected_output_port;
                if (!ShapeEngine.shapeMatch(prefix_varied, dpShapeHashTable)) {
                    if (ShapeEngine.shapeMatch(prefix_varied, cpShapeHashTable)) {
                        expected_output_port = Port.CPU;  //CPU port 
                    } else {
                        expected_output_port = Port.DEFAULT;  //DEFAULT port
                    }
                } else {
                    //checking if the first name component matches.. if it doesn't,
                    //the packet should be sent to CPU (see in P4 code)
                    String p1[] = prefix_varied.split("/");
                    //Taking the first name component parts[1] and checking whether or not it exists in the correct position..
                    CPEntry value = dataStructure.getHT_C_CAD_HS().get(p1[1]);
                    if (value == null) {
                        //the word doest not exist.. the packet should be sent to CPU
                        expected_output_port = Port.CPU; //CPU port
                    } else {
                        //the word do exist but we need to check if its position is correct
                        if (value.getCad().getPosition() != 1) {
                            //if the prefix shape hits both dp_shape_table and cp_shape_table,
                            //the first word in prefix exist in the data plane table but
                            //in another position, we are sure that such prefix does not
                            //exist in the data plane. However, it may be possible that
                            //such prefix exist in the control plane. Therefore, the
                            //expected output port is CPU
                            expected_output_port = Port.CPU; //CPU port
                        } else {
                            //Since in the data plane we don't have any prefix that
                            //is subprefix of non-canonical prefixes in the control plane
                            //once the first word hit in the data plane table, the packet
                            //should be sent to the output port resulted of a NLPM..                            
                            expected_output_port = dataStructure.performLPM(prefix_varied);
                        }
                    }
                }
                //Save both url and the output port into the file assuring that its unique
                fPrefixes.write(prefix_varied + " " + expected_output_port + "\n");
                //-------------------------------------------------------------------------------

            }

            String aux = prefixesList.get(last - 1);
            prefixesList.set(last - 1, line);
            prefixesList.set(index, aux);
            last--;
            n++;
        }
        System.out.println(" " + n + " exact prefixes extracted from prefixes_to_data_plane.txt to canonical_variations_prefixes_to_traffic.txt.. OK");
        fPrefixes.close();
        prefixesList.clear();
    }

    private static void generateNonCanonicalExactPrefixesToTraffic(DualComponentHashmap dataStructure, HashMap<String, Boolean>[] dpShapeHashTable, HashMap<String, Boolean>[] cpShapeHashTable, int n_urls_traffic) throws FileNotFoundException, IOException {
        FileReader final_dataset_file = new FileReader(Parameters.DATASET_URL + "\\prefixes_to_control_plane.txt");
        BufferedReader fileReader = new BufferedReader(final_dataset_file);
        String line = "";
        ArrayList<String> prefixesList = new ArrayList<>();
        while ((line = fileReader.readLine()) != null) {
            prefixesList.add(line);
        }
        final_dataset_file.close();
        fileReader.close();
        FileWriter fPrefixes = new FileWriter(Parameters.DATASET_URL + "\\non_canonical_exact_prefixes_to_traffic.txt");
        int n = 0;
        Random r = new Random();
        int last = prefixesList.size();
        for (int i = 1; i <= n_urls_traffic; i++) {
            int index = r.nextInt(last);
            line = prefixesList.get(index);
            // at this point line looks like this: <prefix> <number> <number> <number> 
            String p[] = line.split(" ");
            String prefix = p[0];
            String aux = prefixesList.get(last - 1);
            prefixesList.set(last - 1, line);
            prefixesList.set(index, aux);
            last--;
            n++;

            //-------------------------------------------------------------------------------
            //Determining the expected output port. This piece of code 
            //reflects exacly the same samantic of the P4 code 
            Byte expected_output_port;
            if (!ShapeEngine.shapeMatch(prefix, dpShapeHashTable)) {
                if (ShapeEngine.shapeMatch(prefix, cpShapeHashTable)) {
                    expected_output_port = Port.CPU;  //CPU port 
                } else {
                    expected_output_port = Port.DEFAULT;  //DEFAULT port
                }
            } else {
                //checking if the first name component matches.. if it doesn't,
                //the packet should be sent to CPU (see in P4 code)
                String p1[] = prefix.split("/");
                //Taking the first name component parts[1] and checking whether or not it exists in the correct position..
                CPEntry value = dataStructure.getHT_C_CAD_HS().get(p1[1]);
                if (value == null) {
                    //the word doest not exist.. the packet should be sent to CPU
                    expected_output_port = Port.CPU; //CPU port
                } else {
                    //the word do exist but we need to check if its position is correct
                    if (value.getCad().getPosition() != 1) {
                        //if the prefix shape hits both dp_shape_table and cp_shape_table,
                        //the first word in prefix exist in the data plane table but
                        //in another position, we are sure that such prefix does not
                        //exist in the data plane. However, it may be possible that
                        //such prefix exist in the control plane. Therefore, the
                        //expected output port is CPU
                        expected_output_port = Port.CPU; //CPU port
                    } else {
                        //Since in the data plane we don't have any prefix that
                        //is subprefix of non-canonical prefixes in the control plane
                        //once the first word hit in the data plane table, the packet
                        //should be sent to the output port resulted of a NLPM..                            
                        expected_output_port = dataStructure.performLPM(prefix);
                    }
                }
            }
            //Save both url and the output port into the file assuring that its unique
            fPrefixes.write(prefix + " " + expected_output_port + "\n");
            //-------------------------------------------------------------------------------
        }
        System.out.println(" " + n + " exact prefixes extracted from prefixes_to_control_plane.txt to non_canonical_exact_prefixes_to_traffic.txt.. OK");
        fPrefixes.close();
        prefixesList.clear();
    }

    /**
     * This method generates the following 8 files to realize our experiments on
     * BMv2:
     *
     * 1) 'prefixes_to_data_plane.txt' cointaining 'n_prefixes' random
     * non-conflicting urls extracted from the file
     * 'final_canonical_dataset.txt'. At most, 'n_prefixes' could be the number
     * of non-conflicting prefixes in the 'final_canonical_dataset.txt'
     * (4107169, representing ~95% of all ~4.3M prefixes in dataset 3). The urls
     * in this file represents the prefixes that will be inserted into the data
     * plane (P4 tables).
     *
     * 2) 'dp_shape.txt' containing the shapes extracted from the file
     * 'prefixes_to_data_plane.txt' that will be used to generate the shapes to
     * be inserted in the tDataplaneShape P4 table.
     *
     * 3) 'prefixes_to_control_plane.txt' containing 'n_prefixes' random
     * non-canonical urls extracted from the file
     * 'final_non_canonical_dataset.txt'. The urls in this files will be stored
     * virtually in the control plane.
     *
     * 4) 'cp_shape.txt' containing the shapes extracted from the file
     * 'prefixes_to_control_plane.txt' that will be used to generate the shapes
     * to be inserted in the tControlPlaneShape P4 table.
     *
     * 5) 'canonical_exact_prefixes_to_traffic.txt' containing 'n_urls_traffic'
     * random non-conflicting exact prefixes extracted from the file
     * 'prefixes_to_data_plane.txt' To allow this, its necessary that
     * n_prefixes>n_urls_traffic. The urls in this file represents 25% of the
     * traffic that will be injected on BMv2.
     *
     * 6) 'canonical_variations_prefixes_to_traffic.txt' cointaining
     * 'n_urls_traffic' random non-conflicting urls extracted from the file
     * 'prefixes_to_data_plane.txt' that includes random variations. For
     * example, if the url from the file 'prefixes_to_data_plane.txt' is /a/b/c,
     * this url will be /a/b or /a/b/c/r/b in the file
     * 'canonical_variations_prefixes_to_traffic.txt' The urls in this file
     * represents 25% of the traffic that will be injected on BMv2.
     *
     * 7) 'non_canonical_exact_prefixes_to_traffic.txt' containing
     * 'n_urls_traffic' random non canonical prefixes extracted from the file
     * 'prefixes_to_control_plane.txt' The urls in this file represents 25% of
     * the traffic that will be injected on BMv2.
     *
     * 8) 'syntetic_prefixes_to_traffic.txt' containing 'n_urls_traffic'' urls
     * generated randomly using the characters from the english alphabet. The
     * urls in this file represents 25% of the traffic that will be injected on
     * BMv2.
     *
     * The reason we have this method is because so far our P4 implementation
     * only consider non-conflicting prefixes. By the time we change our P4
     * implementation to support conflicting prefixes and hashing, we will no
     * longer need this method.
     *
     * @param finalCanonicalPath: represents the url of the final canonical
     * urls.
     * @param finalNonCanonicalPath: represents the url of the final non
     * canonical urls.
     * @param n_prefixes_dataplane : represents the number of prefixes to be
     * inserted in the data plane (P4 tables)
     * @param n_prefixes_controlplane : represents the number of prefixes to be
     * inserted in the control plane (Controller)
     * @param n_urls_traffic: represents the number of prefixes for each group
     * to be used in our traffic
     * @param dataStructure: it will be used to identify the non-conflicting
     * prefixes
     * @return CPDataStructure object containing all prefixes in the file
     * 'prefixes_to_data_plane.txt'
     */
    public static DualComponentHashmap generateFinalFiles(String finalCanonicalPath, String finalNonCanonicalPath, int n_prefixes_dataplane, int n_prefixes_controlplane, int n_urls_traffic, DualComponentHashmap dataStructure) throws FileNotFoundException, IOException {

        //The prefixes in the file 'final_canonical_dataset.txt' should be 
        //syncronized with dataStructure object at this point
        if (n_prefixes_dataplane < n_urls_traffic || n_prefixes_controlplane < n_urls_traffic) {
            System.out.println("ERROR: generateFinalFiles(): n_prefixes_dataplane or n_prefixes_controlplane should be greater or equal to n_urls_traffic");
            System.exit(0);
        }

        System.out.println("Generating Final Files to be used on BMv2: ");

        generatePrefixesToDataPlane(finalCanonicalPath, n_prefixes_dataplane, dataStructure);

        //Syncronizing the dataStructure to 'prefixes_to_data_plane.txt' file
        System.out.println("  => Syncronizing the 'dataStructure' object to 'prefixes_to_data_plane.txt' file..");
        dataStructure = TemporaryTestClass.createCPDataStructure(Parameters.DATASET_URL + "\\prefixes_to_data_plane.txt");

        TemporaryTestClass.validateLpm(Parameters.DATASET_URL + "\\prefixes_to_data_plane.txt", dataStructure);

        //Generating the file 2: 'dp_shape.txt'
        System.out.print("  2: Generating the file 'dp_shape.txt'..");
        ShapeEngine.createShapeFile(Parameters.DATASET_URL + "\\prefixes_to_data_plane.txt",
                Parameters.DATASET_URL + "\\dp_shape.txt");

        HashMap<String, Boolean>[] dpShapeHashTable = ShapeEngine.getShapeHashTable(Parameters.DATASET_URL + "\\dp_shape.txt");

        generatePrefixesToControlPlane(finalNonCanonicalPath, n_prefixes_controlplane);

        //Generating the file 4: 'cp_shape.txt'
        System.out.print("  4: Generating the file 'cp_shape.txt'..");
        ShapeEngine.createShapeFile(Parameters.DATASET_URL + "\\prefixes_to_control_plane.txt",
                Parameters.DATASET_URL + "\\cp_shape.txt");

        HashMap<String, Boolean>[] cpShapeHashTable = ShapeEngine.getShapeHashTable(Parameters.DATASET_URL + "\\cp_shape.txt");

        //Generating the file 5: 'canonical_exact_prefixes_to_traffic.txt'. 
        //Each line should contain only two colums: <prefix> <expected_output_port>
        System.out.print("  5: Generating the file 'canonical_exact_prefixes_to_traffic.txt'..");
        generateCanonicalExactPrefixesToTraffic(dataStructure, n_urls_traffic * 4);

        //Generating the file 6: 'canonical_variations_prefixes_to_traffic.txt'
        //Each line in this file also should contain:
        //<prefix> <expected_output_port>
        //However, the expected output port is obtained as follows:
        // - if prefix shape matches in dp_shape
        //      output_port <- dataStructure.performLpm(prefix); (some port or DEFAULT port)
        //   else
        //      if prefix shape matches in cp_shape
        //          output_port <- CPU  
        //      else
        //          output_port <- DEFAULT port
        System.out.print("  6: Generating the file 'canonical_variations_prefixes_to_traffic.txt'..");
        generateCanonicalVariationsPrefixesToTraffic(dataStructure, dpShapeHashTable, cpShapeHashTable, /*n_urls_traffic*/ 0);

        //Generating the file 7: 'non_canonical_exact_prefixes_to_traffic.txt'
        //Each line in this file also should contain:
        //<prefix> <expected_output_port>
        //The expected output port Port.DEFAULT (the default port)
        System.out.print("  7: Generating the file 'non_canonical_exact_prefixes_to_traffic.txt'..");
        generateNonCanonicalExactPrefixesToTraffic(dataStructure, dpShapeHashTable, cpShapeHashTable, /*n_urls_traffic*/ 0);

        //Remove cross conflicting prefixes from the 'non_canonical_exact_prefixes_to_traffic.txt' file..
        removeCrossConflictingPrefixes(dataStructure, Parameters.DATASET_URL + "\\non_canonical_exact_prefixes_to_traffic.txt");

        //Generating the file 8: 'syntetic_prefixes_to_traffic.txt'
        //Instead of creating another method, we uses the generatedSynteticDataset method in
        //DatasetSegretator to generate the syntetic dataset..
        DatasetSegretator.generateSynteticDataset(n_urls_traffic, 8, 31, 0);

        //We remove the file syntetic_prefixes_to_traffic.txt
        File f = new File(Parameters.DATASET_URL + "\\syntetic_prefixes_to_traffic.txt");
        if (!f.delete()) {
            System.out.println("ERROR: in generateFinalFiles: can't delete the file syntetic_prefixes_to_traffic.txt..");
            System.exit(0);
        }

        //We have to rename the file..
        f = new File(Parameters.DATASET_URL + "\\syntetic_dataset.txt");
        if (!f.renameTo(new File(Parameters.DATASET_URL + "\\syntetic_prefixes_to_traffic.txt"))) {
            System.out.println("ERROR: in generateFinalFiles: can't rename the file syntetic_dataset.txt to syntetic_prefixes_to_traffic.txt..");
            System.exit(0);
        }
        return dataStructure;
    }

    /**
     * This method merges the 04 traffic files into one (packets.txt). Each line
     * in the file packets.txt will have the following structure:
     * <id> <prefix> <input_port> <expected_output_port>
     */
    public static void mergeTrafficFiles(String canonicalExactPath, String canonicalVariationsPath, String nonCanonicalPath, String synteticPath) throws FileNotFoundException, IOException {

        System.out.print("Merging the traffic files into the 'packet.txt'.. ");

        //Merged traffic file (this is the traffic file that will be used to generate packet.bin and to validate)
        FileWriter fileWriter = new FileWriter(Parameters.DATASET_URL + "\\packets.txt");

        //Readers..
        FileReader file_reader = new FileReader(canonicalExactPath);
        BufferedReader fileReader = new BufferedReader(file_reader);
        String line;

        ArrayList<String> list = new ArrayList<>();

        //Reading the file 1 and storing into the array list..
        while ((line = fileReader.readLine()) != null) {
            list.add(line);
        }
        file_reader.close();
        fileReader.close();

        //Reading the file 2 and storing into the array list..
        file_reader = new FileReader(canonicalVariationsPath);
        fileReader = new BufferedReader(file_reader);
        line = "";

        //Reading the file 2 and storing into the array list..
        while ((line = fileReader.readLine()) != null) {
            list.add(line);
        }
        file_reader.close();
        fileReader.close();

        //Reading the file 3 and storing into the array list..
        file_reader = new FileReader(nonCanonicalPath);
        fileReader = new BufferedReader(file_reader);
        line = "";

        //Reading the file 3 and storing into the array list..
        while ((line = fileReader.readLine()) != null) {
            list.add(line);
        }
        file_reader.close();
        fileReader.close();

        //Reading the file 4 and storing into the array list..
        file_reader = new FileReader(synteticPath);
        fileReader = new BufferedReader(file_reader);
        line = "";

        //Reading the file 4 and storing into the array list..
        while ((line = fileReader.readLine()) != null) {
            list.add(line);
        }
        file_reader.close();
        fileReader.close();

        //Shuffling the list..
        Random r = new Random();
        int last = list.size();
        for (int i = 0; i < last; i++) {
            int index = r.nextInt(last);
            String packet = list.get(index);
            list.set(index, list.get(last - 1));
            list.set(last - 1, packet);
            last--;
        }

        int id = 1;

        //Merging the files into the packet.txt from the list..
        for (String entry : list) {

            String p[] = entry.split(" ");
            String prefix = p[0];
            String output_port = p[1];
            byte output_port_number = Byte.parseByte(output_port);

            //Generate the input port making sure that the input_port != output_port
            byte input_port;
            do {
                input_port = Port.generateRandomPort();
            } while (input_port == output_port_number);

            fileWriter.write(id + " " + prefix + " " + input_port + " " + output_port + "\n");
            id++;
        }

        fileWriter.close();
        System.out.println(" OK");
    }

    /**
     * This method must be call only after the method generateFinalFiles(). It
     * takes a DualComponentHashmap object sincronized with the file
     * 'prefixes_to_data_plane.txt' containing all the words to be inserted into
     * the P4 tables as well as the files containing the shapes for data plane
     * and control plane prefixes generated in the method generateFinalFiles().
     * This method consider the word itself to store instead of its hash value
     * (for words with length > 4). With these parameters, this method generates
     * the table entries commands in the file 'bmv2_entry_commands.txt' that it
     * will be used to populate the P4 tables in BMv2 by using the tool
     * runtime_CLI.
     */
    public static void generateTableEntriesCommandsFile(DualComponentHashmap dataStructure, String shapeDPPath, String shapeCPPath) throws IOException {

        System.out.print("Generating the table entries commands file ('bmv2_entry_commands.txt').. ");

        //Creating the file 'bmv2_entry_commands.txt'
        FileWriter fileWriter = new FileWriter(Parameters.DATASET_URL + "\\bmv2_entry_commands.txt");

        //Inserting the dp shape entries..
        FileReader file_reader = new FileReader(shapeDPPath);
        BufferedReader fileReader = new BufferedReader(file_reader);
        String line;
        String command = "";
        while ((line = fileReader.readLine()) != null) {
            command = generateCommandShapeTableEntry("tShapeDataplane", "shape_dp_hit", ShapeEngine.generateCLILpmEntry(line, 8));
            fileWriter.write(command + "\n");
        }
        file_reader.close();
        fileReader.close();

        //Inserting the cp shape entries..
        file_reader = new FileReader(shapeCPPath);
        fileReader = new BufferedReader(file_reader);
        while ((line = fileReader.readLine()) != null) {
            command = generateCommandShapeTableEntry("tShapeControlplane", "shape_cp_hit", ShapeEngine.generateCLILpmEntry(line, 8));
            fileWriter.write(command + "\n");
        }
        file_reader.close();
        fileReader.close();

        //Inserting the name component entries..
        Iterator<String> iterator = dataStructure.getHT_C_CAD_HS().keySet().iterator();
        while (iterator.hasNext()) {
            String word = iterator.next();
            String tableName = "tWord" + word.length();
            String actionName = "first_word_hit";
            String entry = StringConverter.convertNameToHex(word);
            String actionData = dataStructure.get(word).getCad().getCADBits();
            command = generateCommandWordTableEntry(tableName, actionName, entry, actionData);
            fileWriter.write(command + "\n");
        }
        fileWriter.close();
        System.out.println("OK");
    }

    /**
     * This method is called only when a final canonical dataset with no
     * collisions is defined. It takes a DualComponentHashmap object sincronized
     * with the file 'final_canonical_dataset_no_collisions.txt' containing all
     * the words to be inserted into the P4 tables as well as the files
     * containing the shapes for data plane and control plane prefixes. This
     * method consider the hash value of the components instead of its ASCII
     * values. With these parameters, this method generates the table entries
     * commands in the file 'bmv2_entry_commands.txt' that it will be used to
     * populate the P4 tables in BMv2 by using the tool runtime_CLI.
     */
    public static void generateFullTableEntriesCommandsFile(DualComponentHashmap dataStructure, String shapeDPPath, String shapeCPPath) throws IOException {

        System.out.print("Generating the full table entries commands file ('bmv2_entry_commands.txt').. ");

        //Creating the file 'bmv2_entry_commands.txt'
        FileWriter fileWriter = new FileWriter(Parameters.DATASET_URL + "\\bmv2_entry_commands.txt");

        //Inserting the dp shape entries..
        FileReader file_reader = new FileReader(shapeDPPath);
        BufferedReader fileReader = new BufferedReader(file_reader);
        String line;
        String command = "";
        while ((line = fileReader.readLine()) != null) {
            command = generateCommandShapeTableEntry("MyIngress.tDPST", "MyIngress.shape_dp_hit", ShapeEngine.generateCLILpmEntry(line, 8));
            fileWriter.write(command + "\n");
        }
        file_reader.close();
        fileReader.close();
        
        //Inserting the HCT entries
        for (Map.Entry<Integer, Byte> entry : dataStructure.getHTConflicting().entrySet()) {
            Integer key = entry.getKey();
            Byte value = entry.getValue();

            String key_hex = Integer.toHexString(key);
            key_hex = "0x"+key_hex;
            command = generateCommandHCTTableEntry("MyIngress.tHCT", "MyIngress.hct_hit", key_hex, value+"");
            fileWriter.write(command + "\n");
            command = generateCommandHCTTableEntry("MyEgress.tHCT", "MyEgress.hct_hit", key_hex, value+"");
            fileWriter.write(command + "\n");
        }

        //Inserting the name component entries..
        Iterator<String> iterator = dataStructure.getHT_C_CAD_HS().keySet().iterator();
        CRC32 crc = new CRC32();
        while (iterator.hasNext()) {
            String word = iterator.next();
            String tableName = "";
            String actionName = "";
            if (word.length() == 2 || word.length() == 3 || word.length() == 5 || word.length() == 8 || word.length() == 11 || word.length() == 13
                    || word.length() == 15 || word.length() == 16 || word.length() == 17 || word.length() == 22 || word.length() == 23 || word.length() == 24
                    || word.length() == 29) {
                tableName = "MyIngress.t" + word.length();
                actionName = "MyIngress.get_cad";
            } else {
                tableName = "MyEgress.t" + word.length();
                actionName = "MyEgress.get_cad";
            }

            crc.update(word.getBytes());
            String entry = Long.toHexString(crc.getValue());
            entry = "0x" + entry;
            //String entry = StringConverter.convertNameToHex(word);
            String actionData = dataStructure.get(word).getCad().getCADBits();
            command = generateCommandWordTableEntry(tableName, actionName, entry, actionData);
            fileWriter.write(command + "\n");
            crc.reset();
        }
        fileWriter.close();
        System.out.println("OK");
    }

    public static void shufflePacketsFile(String packetsFile) throws FileNotFoundException, IOException {

        System.out.print("Shuffling the packets file..");
        ArrayList<String> list = new ArrayList<>();

        FileReader file_reader = new FileReader(packetsFile);
        BufferedReader fileReader = new BufferedReader(file_reader);
        String line;
        while ((line = fileReader.readLine()) != null) {
            list.add(line);
        }
        file_reader.close();
        fileReader.close();

        Collections.shuffle(list);

        FileWriter fileWriter = new FileWriter(packetsFile);
        for (String str : list) {
            fileWriter.write(str + "\n");
        }
        fileWriter.close();
        System.out.println("OK");
    }

    /**
     * This method takes a file containing all urls that we are generating
     * packets from (file packets.txt, which each line is structure as: <id>
     * <prefix> <input_port> <expected_output_port>) and generates a file called
     * 'packets.bin' that contains the packets for our traffic generator in
     * linux. The file 'packets.bin' is binary and its structure is a sequence
     * of 3-tuple:
     * <1byte=input_port><2bytes=size_packet><packet obtained from convertUrlIntoScapyFriendlyPacket() method>
     * For example: Let's suppose we have a canonical dataset as follows: 1
     * /a/b/c 1 2 2 /a/b 3 4 3 /a 5 6
     *
     * The first packet (/a/b/c) will be represented as [1 9 0 1 3 1 1 1 97 98
     * 99] (input port = 1; packet size = 9; packet from
     * convertUrlIntoScapyFriendlyPacket() method) The second packet (/a/b) will
     * be represented as [3 7 0 2 2 1 1 97 98] (input port = 3; packet size = 7;
     * packet from convertUrlIntoScapyFriendlyPacket() method) The third packet
     * (/a) will be represented as [5 5 0 3 1 1 97] (input port = 5; packet size
     * = 5; packet from convertUrlIntoScapyFriendlyPacket() method)
     *
     * Then, the 'packet.bin' is going to be as follows:
     *
     * [1 9 0 1 3 1 1 1 97 98 99] [3 7 0 2 2 1 1 97 98] [5 5 0 3 1 1 97]
     *
     * The reason we use 2 bytes to store the packet size is because the maximum
     * size packet is 2+1+8+(31*8) = 259 bytes, considering a name with 8 name
     * components and each name component with the maximum of 31 characters.
     *
     */
    public static void generateScapyPacketsOldVersion(String packetsPath) throws FileNotFoundException, IOException {

        FileReader dataset_file = new FileReader(packetsPath);
        BufferedReader fileReader = new BufferedReader(dataset_file);

        String line = "";

        System.out.println("Generating the scapy packets file (packets.bin) from the file 'packets.txt'...");

        //Delete the old 'packets.bin'
        Path path = Paths.get(Parameters.DATASET_URL + "\\packets.bin");
        Files.delete(path);

        //Open a new file 'packets.bin'
        FileOutputStream file = new FileOutputStream(Parameters.DATASET_URL + "\\packets.bin");
        DataOutputStream filePackets = new DataOutputStream(file);

        long n = 0;
        while ((line = fileReader.readLine()) != null) {

            String p[] = line.split(" ");
            String id = p[0];
            String prefix = p[1];
            String input_port = p[2];

            //we ignore p[3] that stores the output port
            byte packet[] = StringConverter.convertUrlIntoScapyFriendlyPacketOldVersion(Short.parseShort(id), prefix);

            if (packet.length > Parameters.MAX_PACKET_SIZE) {
                System.out.println("ERROR: in generateScapyPackets() for prefix " + prefix + ". Packet size is too long (" + packet.length + " bytes)");
                System.exit(0);
            }

            filePackets.writeByte(Integer.parseInt(input_port));   //writing the input port as 1 byte
            filePackets.writeShort(packet.length);  //writing the packet size as 2 bytes
            filePackets.write(packet);   //writing the packet bytes

            n++;
        }
        fileReader.close();
        file.close();
        filePackets.close();

        System.out.println(" " + n + " packets generated.. OK");
    }

    /**
     * This method takes a file containing all urls that we are generating
     * packets from (file packets.txt, each of which is structured as follows:
     * <prefix> <expected_output_port>) and generates a file called
     * 'packets.bin' that contains the packets for our traffic generator in
     * linux. The file 'packets.bin' is binary and its structure is a sequence
     * of tuples:
     * <2bytes=packet_size><packet obtained from convertUrlIntoScapyFriendlyPacket() method>
     *
     * The reason we use 2 bytes to store the packet size is because the maximum
     * size packet is 2+1+8+(31*8) = 259 bytes, considering a name with 8 name
     * components and each name component with the maximum of 31 characters.
     *
     */
    public static void generateScapyPackets(String packetsPath, String packetsTraffic, boolean add_payload) throws FileNotFoundException, IOException {

        FileReader dataset_file = new FileReader(packetsPath);
        BufferedReader fileReader = new BufferedReader(dataset_file);

        String line = "";

        System.out.println("Generating the binary packets file (packets.bin) from the file 'packets.txt'...");

        //Delete the old 'packets.bin'
        Path path = Paths.get(packetsTraffic);
        Files.delete(path);

        //Open a new file 'packets.bin'
        FileOutputStream file = new FileOutputStream(packetsTraffic);
        DataOutputStream filePackets = new DataOutputStream(file);

        long n = 0;
        while ((line = fileReader.readLine()) != null) {

            String p[] = line.split(" ");
            String prefix = p[0];

            //we ignore p[1] for now (expected output port)
            byte packet[] = StringConverter.convertUrlIntoScapyFriendlyPacket(prefix, add_payload);

            filePackets.writeShort(packet.length);  //writing the packet size as 2 bytes
            filePackets.write(packet);   //writing the packet bytes

            n++;
        }
        fileReader.close();
        file.close();
        filePackets.close();

        System.out.println("Packets (.bin) generated: " + n);
    }

    public static void printScapyPackets(String packetsBinPath) throws FileNotFoundException, IOException {
        FileInputStream file = new FileInputStream(Parameters.DATASET_URL + "\\packets.bin");
        DataInputStream filePackets = new DataInputStream(file);
        int n = 0;

        try {
            while (true) {
                byte input_port = filePackets.readByte();
                short packet_size = filePackets.readShort();
                byte packet[] = new byte[packet_size];
                filePackets.read(packet);

                byte id_bytes[] = new byte[2];
                id_bytes[0] = packet[0];
                id_bytes[1] = packet[1];
                BigInteger b = new BigInteger(id_bytes);
                short packet_id = b.shortValueExact();

                System.out.println(packet_id + " " + input_port + " " + packet_size);

                n++;
            }
        } catch (EOFException ex) {
            System.out.println(n + " packets read from packets.bin..");
            file.close();
            filePackets.close();
        }
    }

    /**
     * This method takes the table name (e.g tWord1), action name (e.g
     * first_word_hit), and the encoded DP or CP shape together with the action
     * data (e.g 0x0102000000000000/16 => 16) and generate the command used by
     * runtime_CLI to insert an entry into a P4 table
     */
    public static String generateCommandShapeTableEntry(String tableName, String actionName, String hexEntryWithActionData) {
        return "table_add " + tableName + " " + actionName + " " + hexEntryWithActionData;
    }

    public static String generateCommandHCTTableEntry(String tableName, String actionName, String key, String port) {
        return "table_add " + tableName + " " + actionName + " " + key + " => " + port;
    }
    
    /**
     * This method takes the table name (e.g tWord1), action name (e.g
     * first_word_hit), the matching key (e.g com), and the action data as
     * output port(s) (e.g 16) and generate the command used by runtime_CLI to
     * insert an entry into a P4 table
     */
    public static String generateCommandWordTableEntry(String tableName, String actionName, String hexEntry, String actionData) {
        return "table_add " + tableName + " " + actionName + " " + hexEntry + " => " + actionData;
    }

    public static long countConflictingPrefixes(DualComponentHashmap dataStructure, String datasetPath) throws IOException, FileNotFoundException {
        FileReader file = new FileReader(datasetPath);
        BufferedReader fReader = new BufferedReader(file);
        String line;
        long n = 0;
        while ((line = fReader.readLine()) != null) {
            String parts[] = line.split(" ");
            String url = parts[0];
            if (dataStructure.isPrefixConflicting(url)) {
                n++;
            }
        }
        file.close();
        fReader.close();
        return n;
    }
}
