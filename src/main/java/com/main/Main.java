/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.utils.bmv2.ActionParams;
import com.utils.bmv2.Match;
import com.utils.bmv2.P4Runtime;
import com.utils.bmv2.TableEntry;
import com.datasetsegretator.Parameters;
import com.datasetsegretator.ShapeEngine;
import com.datasetsegretator.CPEntry;
import com.datasetsegretator.DualComponentHashmap;
import com.datasetsegretator.TripleObject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.zip.CRC32;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.lang3.tuple.Triple;
import com.p4.environment.adaptation.AdaptorToBMv2;
import com.p4.environment.adaptation.StringConverter;

/**
 *
 * @author casti
 */
public class Main {

    public static boolean hasCollision(String dataset, boolean with_ports) throws FileNotFoundException, IOException {
        FileReader fReader = new FileReader(dataset);
        BufferedReader bReader = new BufferedReader(fReader);

        HashMap<String, Integer> h = new HashMap<>();

        String line;

        while ((line = bReader.readLine()) != null) {
            String p[];
            if (!with_ports) {
                p = line.split("/");
            } else {
                String p1[] = line.split(" ");
                p = p1[0].split("/");
            }
            for (int i = 1; i < p.length; i++) {
                Integer value = h.putIfAbsent(p[i], p[i].length());
            }
        }
        fReader.close();
        bReader.close();

        //hash, <component, position>
        //H[1] = components with 1 character
        //H[31] = components with 31 characters
        HashMap<Long, Entry<String, Integer>> H[] = new HashMap[32];
        for (int i = 0; i < H.length; i++) {
            H[i] = new HashMap<>();
        }

        CRC32 crc = new CRC32();

        boolean r = false;
        int i = 0;

        Iterator<Entry<String, Integer>> iterator = h.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Integer> entry = iterator.next();
            crc.update(entry.getKey().getBytes());
            Entry e = new AbstractMap.SimpleEntry(entry.getKey(), entry.getValue());
            Entry value = H[entry.getKey().length()].putIfAbsent(crc.getValue(), e);

            if (value != null) {
                //we only consider collision if two components with same size produces the same hash 
                if (entry.getValue() == ((int) value.getValue())) {
                    i++;
                    System.out.println(" " + i + ": hash of component '" + entry.getKey() + "' with size " + entry.getKey().length() + " collides with '" + (String) value.getKey() + " => crc32: " + crc.getValue());
                    r = true;
                } else {
                    System.out.println("--------------------- key=" + entry.getKey() + " [" + crc.getValue() + "]" + " entry.getValue()=" + entry.getValue() + " ((int) value.getValue())=" + ((int) value.getValue()));
                }
            }
            crc.reset();
            iterator.remove();
        }
        return r;
    }

    public static boolean isCanonicalDataset(String dataset, boolean with_ports) throws FileNotFoundException, IOException {
        FileReader fReader = new FileReader(dataset);
        BufferedReader bReader = new BufferedReader(fReader);

        HashMap<String, Integer> h = new HashMap<>();

        boolean r = true;
        String line;

        while ((line = bReader.readLine()) != null) {
            String p[];
            if (!with_ports) {
                p = line.split("/");
            } else {
                String p1[] = line.split(" ");
                p = p1[0].split("/");
            }
            for (int i = 1; i < p.length; i++) {
                Integer value = h.putIfAbsent(p[i], i);
                if (value != null) {
                    if (value != i) {
                        System.out.println(p[i] + " appears at position " + value + " and " + i);
                        r = false;
                    }
                }
            }
        }
        fReader.close();
        bReader.close();
        return r;
    }

    /**
     * Load the canonical prefixes from file and provide an interative prompt to
     * perfom operations
     */
    public static void loadPrefixes(String datasetFileName) throws FileNotFoundException, IOException {

        if (isCanonicalDataset(datasetFileName, true)
                && !hasCollision(datasetFileName, true)) {

            DualComponentHashmap d = new DualComponentHashmap();
            FileReader fReader = new FileReader(datasetFileName);
            BufferedReader bReader = new BufferedReader(fReader);
            String line;

            while ((line = bReader.readLine()) != null) {
                String p[] = line.split(" ");
                String prefix = p[0];
                byte port = Byte.parseByte(p[1]);
                d.insertPrefix(prefix, port);
            }
            System.out.println("Prefixes loaded: " + d.getNumberOfPrefixes());
            d.iterativeTesting();
        } else {
            System.out.println("The prefixes in " + datasetFileName + " are not canonical or there are hash collisions.");
        }
    }

    /**
     * Generate p4runtime entries for switch 1. It will include entries for t1,
     * ..., t31 tables and also for the HCT, and DPST
     */
    public static void generateP4RuntimeEntries(String inputDatasetFileName, String outputEntriesFileName, int ingressTables[], int egressTables[]) throws IOException {

        //Entries for the IP table
        List<Object> ip_matching_values1 = new ArrayList<>();
        ip_matching_values1.add("10.0.1.1");
        ip_matching_values1.add(32);
        Match ip_match1 = new Match(ip_matching_values1, null, null, null);

        List<Object> ip_matching_values2 = new ArrayList<>();
        ip_matching_values2.add("10.0.2.2");
        ip_matching_values2.add(32);
        Match ip_match2 = new Match(ip_matching_values2, null, null, null);

        List<Object> ip_matching_values3 = new ArrayList<>();
        ip_matching_values3.add("10.0.4.4");
        ip_matching_values3.add(32);
        Match ip_match3 = new Match(ip_matching_values3, null, null, null);

        List<TableEntry> table_entries = new ArrayList<>();
        table_entries.add(new TableEntry("MyIngress.ipv4_lpm", ip_match1, "MyIngress.ipv4_forward", new ActionParams("00:00:00:00:00:01", 1, null, null, null)));
        table_entries.add(new TableEntry("MyIngress.ipv4_lpm", ip_match2, "MyIngress.ipv4_forward", new ActionParams("00:00:00:00:00:02", 2, null, null, null)));
        table_entries.add(new TableEntry("MyIngress.ipv4_lpm", ip_match3, "MyIngress.ipv4_forward", new ActionParams("00:00:00:00:00:04", 3, null, null, null)));

        //Loading the canonical prefixes into the DCH ...
        FileReader fReader = new FileReader(inputDatasetFileName);
        BufferedReader bReader = new BufferedReader(fReader);
        String line;

        DualComponentHashmap dch = null;

        if (isCanonicalDataset(inputDatasetFileName, true)
                && !hasCollision(inputDatasetFileName, true)) {

            dch = new DualComponentHashmap();

            while ((line = bReader.readLine()) != null) {
                String p[] = line.split(" ");
                String url = p[0];
                byte port = Byte.parseByte(p[1]);
                dch.insertPrefix(url, port);
            }
            System.out.println("Prefixes inserted: " + dch.getNumberOfPrefixes());
        } else {
            System.out.println("Can't generate entries because the dataset is non-canonical or it has collisions..");
            System.exit(0);
        }

        //cofib_tables[1] = table 1 (1 character)
        //cofib_tables[2] = table 2 (2 character)
        //...
        //cofib_tables[31] = table 31 (31 character)        
        HashMap<Long, Integer> cofib_tables[] = new HashMap[32];
        for (int i = 0; i < cofib_tables.length; i++) {
            cofib_tables[i] = new HashMap<>();
        }

        //Entries for the CoFIB tables
        HashMap<String, CPEntry> entries = dch.getHT_C_CAD_HS();
        CRC32 crc = new CRC32();

        Iterator<Entry<String, CPEntry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, CPEntry> entry = it.next();
            crc.update(entry.getKey().getBytes());

            if (cofib_tables[entry.getKey().length()].putIfAbsent(crc.getValue(), entry.getValue().getCad().getCADInteger()) == null) {
                //the name component hash has been stored sucessfully...
            } else {
                System.out.println("Error in generateEntries(). A collision has occured for name component " + entry.getKey() + "[" + crc.getValue() + "] with value " + entry.getValue().getCad().getCADInteger());
                System.exit(0);
            }
            crc.reset();
        }

        for (int i = 1; i <= 31; i++) {
            Iterator<Entry<Long, Integer>> iterator = cofib_tables[i].entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<Long, Integer> entry = iterator.next();
                List<Long> list = new ArrayList<>();
                list.add(entry.getKey());

                //if (i >= 1 && i <= 4) {
                if (ArrayUtils.contains(ingressTables, i)) {
                    table_entries.add(new TableEntry("MyIngress.t" + i, new Match(null, list, null, null), "MyIngress.get_cad", new ActionParams(null, null, entry.getValue(), null, null)));
                } else {
                    //if (i >= 5 && i <= 8) {
                    if (ArrayUtils.contains(egressTables, i)) {
                        table_entries.add(new TableEntry("MyEgress.t" + i, new Match(null, list, null, null), "MyEgress.get_cad", new ActionParams(null, null, entry.getValue(), null, null)));
                    }
                }
            }
        }

        for (int i = 0; i < cofib_tables.length; i++) {
            cofib_tables[i].clear();
        }

        //Entries for the HCT 
        HashMap<Integer, Byte> hct = dch.getHTConflicting();
        Iterator<Entry<Integer, Byte>> it2 = hct.entrySet().iterator();
        while (it2.hasNext()) {
            Entry<Integer, Byte> entry = it2.next();
            List<Long> list = new ArrayList<>();
            list.add((long) entry.getKey());
            table_entries.add(new TableEntry("MyIngress.tHCT", new Match(null, null, list, null), "MyIngress.hct_hit", new ActionParams(null, null, null, entry.getValue(), null)));
            table_entries.add(new TableEntry("MyEgress.tHCT", new Match(null, null, list, null), "MyEgress.hct_hit", new ActionParams(null, null, null, entry.getValue(), null)));
        }

        //Entries for the DPST
        long shapes = ShapeEngine.createShapeFile(inputDatasetFileName, "shapes.txt");
        FileReader shapeReader = new FileReader("shapes.txt");
        BufferedReader sReader = new BufferedReader(shapeReader);

        while ((line = sReader.readLine()) != null) {
            Triple<Long, Integer, Integer> t = ShapeEngine.generateP4RuntimeLpmEntry(line, 8);
            List<Long> list = new ArrayList<>();
            list.add(t.getLeft());
            list.add((long) t.getMiddle());
            table_entries.add(new TableEntry("MyIngress.tDPST", new Match(null, null, null, list), "MyIngress.shape_dp_hit", new ActionParams(null, null, null, null, t.getRight())));
        }
        shapeReader.close();
        sReader.close();

        //File f = new File("shapes.txt");
        //f.delete();
        P4Runtime runtime = new P4Runtime("bmv2", "build/basic.p4.p4info.txt", "build/basic.json", table_entries);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String json = gson.toJson(runtime);
        FileWriter fWriter = new FileWriter(outputEntriesFileName);
        fWriter.write(json);
        fWriter.flush();
        fWriter.close();
    }

    public static boolean validateTemplateForTrafficGeneration(String templateFile) throws FileNotFoundException {
        FileReader fReader = new FileReader(templateFile);
        BufferedReader bReader = new BufferedReader(fReader);
        String line = "";

        boolean valid = true;

        for (int i = 1; i <= 8; i++) {
            int power = (int) Math.pow(2, i);
            for (int j = 1; j <= power; j++) {
                try {
                    line = bReader.readLine();
                } catch (IOException e) {
                    valid = false;
                    break;
                }
                int c = line.split("/").length - 1;
                if (c != i) {
                    valid = false;
                    break;
                }
            }
        }

        try {
            line = bReader.readLine();
            if (line != null) {
                valid = false;
            }
            fReader.close();
            bReader.close();
        } catch (IOException e) {

        }
        return valid;
    }

    private static boolean componentExistInDifferentPosition(String component, int position, DualComponentHashmap h) {
        boolean r = false;
        HashMap<String, CPEntry> aux = h.getHT_C_CAD_HS();
        CPEntry cpe = aux.get("component");
        if (cpe != null) {
            if (position != cpe.getCad().getPosition()) {
                r = true;
            }
        }
        return r;
    }

    private static String generatePrefixFromTemplate(String template, Pair<Byte, Byte> table_id_ingress_interval, Pair<Byte, Byte> table_id_egress_interval, List<String> L[]) {
        Random r = new Random();
        String url = "";
        for (int i = 0; i < template.length(); i++) {
            if (template.charAt(i) == '0') {
                byte table_id_ingress = (byte) r.nextInt(table_id_ingress_interval.getFirst(), table_id_ingress_interval.getSecond() + 1);
                url = url + "/" + L[table_id_ingress].get(r.nextInt(L[table_id_ingress].size()));
            } else {
                byte table_id_egress = (byte) r.nextInt(table_id_egress_interval.getFirst(), table_id_egress_interval.getSecond() + 1);
                url = url + "/" + L[table_id_egress].get(r.nextInt(L[table_id_egress].size()));
            }
        }
        return url;
    }

    /*This method creates a random canonical dataset following a given template.
     */
    public static void createRandomDatasetTemplateFile(int x1, int x2, Pair<Byte, Byte> table_id_ingress_interval, Pair<Byte, Byte> table_id_egress_interval, String datasetPath) throws IOException {

        HashMap<String, Byte> H[] = new HashMap[9];
        for (int i = 0; i < H.length; i++) {
            H[i] = new HashMap<>();
        }

        for (int i = 1; i < H.length; i++) {
            if (i == 1) {
                while (H[i].size() < 52) {
                    String component = RandomStringUtils.random(i, true, false);
                    H[i].putIfAbsent(component, (byte) 0);
                }
            } else {
                while (H[i].size() < 256 * i) {
                    String component = RandomStringUtils.random(i, true, true);
                    H[i].putIfAbsent(component, (byte) 0);
                }
            }
        }

        List<String> L[] = new ArrayList[9];
        for (int i = 1; i < L.length; i++) {
            L[i] = new ArrayList<>(H[i].keySet());
        }

        //template[1] = all possible urls with 1 component
        //template[2] = all possible urls with 2 components
        //..
        //template[8] = all possible urls with 8 components
        List<String> templates[] = new ArrayList[9];
        for (int i = 0; i < templates.length; i++) {
            templates[i] = new ArrayList<>();
        }

        for (int i = x1; i <= x2; i++) {
            for (int j = 0; j < Math.pow(2, i); j++) {
                String s = StringConverter.convertIntegerToBitstring(j, i);
                templates[i].add(s);
            }
        }

        DualComponentHashmap dch = new DualComponentHashmap();

        FileWriter file = new FileWriter(datasetPath);
        Random r = new Random();
        for (int i = x1; i <= x2; i++) {
            for (String template : templates[i]) {
                String url = "";
                do {
                    url = generatePrefixFromTemplate(template, table_id_ingress_interval, table_id_egress_interval, L);
                } while (!dch.insertPrefix(url, Byte.MIN_VALUE));
                int ports[] = {2, 4};
                //stores the url into a file
                file.write(url + " " + ports[r.nextInt(2)] + "\n");
            }
        }
        file.flush();
        file.close();
    }

    /*This method creates a random canonical dataset following a given template.
     */
    public static void createRandomDatasetTemplateFile(int number_components, Pair<Byte, Byte> table_id_ingress_interval, Pair<Byte, Byte> table_id_egress_interval, String datasetPath) throws IOException {

        HashMap<String, Byte> H[] = new HashMap[9];
        for (int i = 0; i < H.length; i++) {
            H[i] = new HashMap<>();
        }

        for (int i = 1; i < H.length; i++) {
            if (i == 1) {
                while (H[i].size() < 52) {
                    String component = RandomStringUtils.random(i, true, false);
                    H[i].putIfAbsent(component, (byte) 0);
                }
            } else {
                while (H[i].size() < Math.pow(2, number_components)) {
                    String component = RandomStringUtils.random(i, true, true);
                    H[i].putIfAbsent(component, (byte) 0);
                }
            }
        }

        List<String> L[] = new ArrayList[9];
        for (int i = 1; i < L.length; i++) {
            L[i] = new ArrayList<>(H[i].keySet());
        }

        List<String> templates = new ArrayList<>();
        for (int i = 0; i < Math.pow(2, number_components); i++) {
            String s = StringConverter.convertIntegerToBitstring(i, number_components);
            templates.add(s);
        }

        DualComponentHashmap dch = new DualComponentHashmap();

        FileWriter file = new FileWriter(datasetPath);
        Random r = new Random();
        for (String template : templates) {
            String url = "";
            do {
                url = generatePrefixFromTemplate(template, table_id_ingress_interval, table_id_egress_interval, L);
            } while (!dch.insertPrefix(url, Byte.MIN_VALUE));
            int ports[] = {2, 4};
            //stores the url into a file
            file.write(url + " " + ports[r.nextInt(2)] + "\n");
        }
        file.flush();
        file.close();
    }

    /*This method creates a random canonical dataset following a given template.
     */
    public static void createRandomDatasetTemplateFileWithoutConflicting(int number_components, Pair<Byte, Byte> table_id_ingress_interval, Pair<Byte, Byte> table_id_egress_interval, String datasetPath) throws IOException {
        HashMap<String, Byte> H[] = new HashMap[9];   //stores random name components for each table
        for (int i = 0; i < H.length; i++) {
            H[i] = new HashMap<>();
        }

        List<String> templates = new ArrayList<>();
        for (int i = 0; i < Math.pow(2, number_components); i++) {
            String s = StringConverter.convertIntegerToBitstring(i, number_components);
            System.out.println(s);
            templates.add(s);
        }

        FileWriter file = new FileWriter(datasetPath);
        Random r = new Random();
        for (String template : templates) {

            String url = "";

            //iterate bit by bit over template
            //bit 0 means INGRESS
            //bit 1 means EGRESS
            // Traverse the string
            for (int i = 0; i < template.length(); i++) {
                if (template.charAt(i) == '0') {
                    //generating a random component for the ingress tables
                    while (true) {
                        byte table_id_ingress = (byte) r.nextInt(table_id_ingress_interval.getFirst(), table_id_ingress_interval.getSecond() + 1);
                        String component = RandomStringUtils.random(table_id_ingress, true, true);
                        if (H[table_id_ingress].putIfAbsent(component, (byte) 0) == null) {
                            url = url + "/" + component;
                            break;
                        }
                    }
                } else {
                    //generating a random component for the egress tables
                    while (true) {
                        byte table_id_egress = (byte) r.nextInt(table_id_egress_interval.getFirst(), table_id_egress_interval.getSecond() + 1);
                        String component = RandomStringUtils.random(table_id_egress, true, true);
                        if (H[table_id_egress].putIfAbsent(component, (byte) 0) == null) {
                            url = url + "/" + component;
                            break;
                        }
                    }
                }
            }
            int ports[] = {2, 4};
            //stores the url into a file
            file.write(url + " " + ports[r.nextInt(2)] + "\n");
        }
        file.flush();
        file.close();
    }

    public static void error() {
        System.out.println("Syntax error. Use the following parameters: ");
        System.out.println("fantnet -f <[-c] | [-l] | [-e] | [-t] | [-r] | [-p] [-P]> [--input <dataset_file>] [<--output> <file>] [-n] [--pps] [--duration]");
        System.out.println("");
        System.out.println("Options: ");
        System.out.println("\t-c: \t Verify if the prefix dataset is canonical.");
        System.out.println("\t-h: \t Verify if the prefix dataset has collisions.");
        System.out.println("\t-l: \t Load the prefixes from the dataset file and provides a simple and interactive CLI.");
        System.out.println("\t-e: \t Generate P4Runtime table entries for the DFIB, HCT, and DPST for CoFIB switch.");
        System.out.println("\t-t: \t Generate the binary traffic file to be injected into the CoFIB switch.");
        System.out.println("\t-r: \t Generate a random syntetic dataset containing all combinations of prefixes of a given size.");
        System.out.println("\t-n: \t Number of components for each prefix to generate the syntetic dataset if [-r] is used.");
        System.out.println("\t-p: \t Generate the packets file containing a given amount of packets for each possible number of components, sorted by the number of components.");
        System.out.println("\t-P: \t Generate the packets file containing a given amount of packets for each possible number of components, in a random order in the number of components.");
        System.out.println("\t--pps: \t\t Packets per second if [-p] is used.");
        System.out.println("\t--duration: \t Duration of the traffic in seconds if [-p] is used.");

        System.out.println("");
    }
    
    public static void tablePlacement(String datasetPath) throws FileNotFoundException, IOException {
        FileReader fReader = new FileReader(datasetPath);
        BufferedReader bReader = new BufferedReader(fReader);
        String line = "";

        long M[][] = new long[32][9];   //M[i][j] = stores the amount of components with 'i' characters at position 'j'.
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 9; j++) {
                M[i][j] = 0;
            }
        }

        //hash table to calculate the memory consumption at each pipeline
        HashMap<Long, Byte> H[] = new HashMap[32];
        for (int i = 0; i < H.length; i++) {
            H[i] = new HashMap<>();   //H[i] = store crc32 hashes of component with i characters
        }

        CRC32 crc = new CRC32();
        long n = 0;
        //creating the matrix
        while ((line = bReader.readLine()) != null) {
            n++;
            String parts[] = line.split(" ");
            String p[] = parts[0].split("/");

            for (int i = 1; i < p.length; i++) {
                int component_length = p[i].length();
                M[component_length][i]++;
                crc.update(p[i].getBytes());
                H[p[i].length()].putIfAbsent(crc.getValue(), Byte.MIN_VALUE);
                crc.reset();
            }
        }
        fReader.close();
        bReader.close();

        System.out.println("Prefixes: " + n);

        //serialize the matriz into an array of triples <frequency, position, size>
        TripleObject V[] = new TripleObject[32 * 9];   //serialized matrix M
        int index = 0;
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 9; j++) {
                V[index] = new TripleObject(M[i][j], (byte) j, (byte) i);
                index++;
            }
        }

        //sorting the array V        
        Arrays.sort(V);

        //hash table to store the tables in the ingress and egress
        HashMap<String, Byte> INGRESS = new HashMap<>();
        HashMap<String, Byte> EGRESS = new HashMap<>();

        int control = 1;
        for (int i = 0; i < V.length; i++) {

            if (V[i].getFrequency() == 0) {
                break;
            }

            String tableName = "T" + V[i].getSize();
            if (control % 2 == 1) {
                //ingress
                if (INGRESS.get(tableName) == null && EGRESS.get(tableName) == null) {
                    INGRESS.put(tableName, Byte.MIN_VALUE);
                }
            } else {
                //egress                
                if (INGRESS.get(tableName) == null && EGRESS.get(tableName) == null) {
                    EGRESS.put(tableName, Byte.MIN_VALUE);
                }
            }
            control++;
        }

        long ingress_mem = 0;
        long egress_mem = 0;

        System.out.println("Ingress Tables: ");
        Iterator<String> iterator1 = INGRESS.keySet().iterator();
        while (iterator1.hasNext()) {
            String table = iterator1.next();
            String p[] = table.split("T");
            byte table_id = Byte.parseByte(p[1]);
            if (table_id <= 4) {
                ingress_mem = ingress_mem + H[table_id].size() * (table_id + 3);  //table_id bytes for component and 3 bytes for CAD
            } else {
                ingress_mem = ingress_mem + H[table_id].size() * 7;  //4 byte for crc32 and 3 bytes for CAD
            }
            System.out.print(table + " ");
        }
        System.out.print("  Memory footprint (bytes): " + ingress_mem);

        System.out.println("\n");

        System.out.println("Egress Tables: ");
        Iterator<String> iterator2 = EGRESS.keySet().iterator();
        while (iterator2.hasNext()) {
            String table = iterator2.next();
            String p[] = table.split("T");
            byte table_id = Byte.parseByte(p[1]);
            if (table_id <= 4) {
                egress_mem = egress_mem + H[table_id].size() * (table_id + 3);  //table_id bytes for component and 3 bytes for CAD
            } else {
                egress_mem = egress_mem + H[table_id].size() * 7;  //4 byte for crc32 and 3 bytes for CAD
            }
            System.out.print(table + " ");
        }
        System.out.print("  Memory footprint (bytes): " + egress_mem);
    }

    private static boolean hasExcludedComponent(String prefix, ArrayList<String> list) {
        boolean has = false;
        String p[] = prefix.split("/");
        for (int i = 1; i < p.length; i++) {
            if (list.contains(p[i])) {
                has = true;
                break;
            }
        }
        return has;
    }
    
    public static void removeCollidesPrefixes(String datasetPath, String collidingComponentsPath) throws FileNotFoundException, IOException {
        FileReader fReader = new FileReader(collidingComponentsPath);
        BufferedReader bReader = new BufferedReader(fReader);
        String line = "";

        ArrayList<String> list = new ArrayList<>();
        while ((line = bReader.readLine()) != null) {
            list.add(line);
        }
        fReader.close();
        bReader.close();

        FileWriter fWriter = new FileWriter(Parameters.DATASET_URL + "//final_canonical_dataset_no_collisions.txt");
        fReader = new FileReader(datasetPath);
        bReader = new BufferedReader(fReader);
        long n = 0;
        while ((line = bReader.readLine()) != null) {
            String parts[] = line.split(" ");
            if (!hasExcludedComponent(parts[0], list)) {
                fWriter.write(line + "\n");
            } else {
                n++;
                System.out.println(n + ": Prefix '" + parts[0] + "' removed.");
            }
        }
        fReader.close();
        bReader.close();
        fWriter.close();
    }

    public static void tableSizes(String datasetPath) throws IOException {
        FileReader fReader = new FileReader(datasetPath);
        BufferedReader bReader = new BufferedReader(fReader);
        String line = "";

        HashMap<Long, Byte> H[] = new HashMap[32];
        for (int i = 0; i < H.length; i++) {
            H[i] = new HashMap<>();
        }

        CRC32 crc = new CRC32();
        while ((line = bReader.readLine()) != null) {
            String parts[] = line.split(" ");
            String url = parts[0];
            String p[] = url.split("/");
            for (int i = 1; i < p.length; i++) {
                crc.update(p[i].getBytes());
                H[p[i].length()].putIfAbsent(crc.getValue(), Byte.MIN_VALUE);
                crc.reset();
            }
        }
        fReader.close();
        bReader.close();

        for (int i = 1; i <= 31; i++) {
            System.out.println("T" + i + ": " + H[i].size());
        }
    }

    public static int log2(int N) {
        int result = (int) Math.ceil((Math.log(N) / Math.log(2)));
        return result;
    }

    public static boolean isActionProfileBetter(int i, int M, int N) {
        int mem_p4_table = 0;
        int mem_action_profile = 0;

        if (i <= 4) {
            mem_p4_table = (i * 8 + 24) * M;
            mem_action_profile = i * 8 * M + M * log2(N) + 24 * N;
        } else {
            mem_p4_table = 56 * M;
            mem_action_profile = 32 * M + M * log2(N) + 24 * N;
        }
        return (mem_action_profile < mem_p4_table);
    }
    
    public static void memoryConsumptionCoFIBActionProfileOptimization(String datasetPath, String dpstPath, String cpstPath) throws FileNotFoundException, IOException {
        FileReader fReader = new FileReader(datasetPath);
        BufferedReader bReader = new BufferedReader(fReader);
        String line = "";
        DualComponentHashmap dch = new DualComponentHashmap();
        while ((line = bReader.readLine()) != null) {
            String parts[] = line.split(" ");
            String url = parts[0];
            Byte port = Byte.parseByte(parts[1]);
            dch.insertPrefix(url, port);
        }
        fReader.close();
        bReader.close();

        HashMap<String, Byte> DFIB_M[] = new HashMap[32];
        HashMap<String, Byte> DFIB_N[] = new HashMap[32];
        for (int i = 0; i < DFIB_M.length; i++) {
            DFIB_M[i] = new HashMap<>();
            DFIB_N[i] = new HashMap<>();
        }

        Set<Entry<String, CPEntry>> h = dch.getHT_C_CAD_HS().entrySet();
        Iterator<Entry<String, CPEntry>> iterator = h.iterator();
        while (iterator.hasNext()) {
            Entry<String, CPEntry> entry = iterator.next();
            String key = entry.getKey();
            String value = entry.getValue().getCad().getCADBits();
            DFIB_M[key.length()].putIfAbsent(key, Byte.MIN_VALUE);
            DFIB_N[key.length()].putIfAbsent(value, Byte.MIN_VALUE);
        }

        long T_P4[] = new long[32];
        long T_ACTION_PROFILE[] = new long[32];

        long dfib_p4 = 0;
        long dfib_action_profile = 0;
        for (int i = 1; i <= 31; i++) {
            int M = DFIB_M[i].size();
            int N = DFIB_N[i].size();
            if (i <= 4) {
                T_P4[i] = (i * 8 + 24) * M;
                T_ACTION_PROFILE[i] = i * 8 * M + M * log2(N) + 24 * N;
            } else {
                T_P4[i] = 56 * M;
                T_ACTION_PROFILE[i] = 32 * M + M * log2(N) + 24 * N;
            }
            dfib_p4 = dfib_p4 + T_P4[i];
            dfib_action_profile = dfib_action_profile + T_ACTION_PROFILE[i];
            System.out.println("T=" + i + " (P4 table): " + T_P4[i] / 8 + "   ->   " + "T=" + i + " (action profile): " + T_ACTION_PROFILE[i] / 8);
        }
        dfib_p4 = dfib_p4 / 8;
        dfib_action_profile = dfib_action_profile / 8;
        
        System.out.println("Memory DFIB (P4 table): " + String.format("%.3f", (double)dfib_p4/1000000.0) + " MB");
        System.out.println("Memory DFIB (Action Profile): " + String.format("%.3f", (double)dfib_action_profile/1000000.0) + " MB");

        HashMap<Byte, Byte> HCT_N = new HashMap<>();
        Set<Entry<Integer, Byte>> h1 = dch.getHTConflicting().entrySet();
        Iterator<Entry<Integer, Byte>> iterator1 = h1.iterator();
        while (iterator1.hasNext()) {
            Entry<Integer, Byte> entry = iterator1.next();
            Integer key = entry.getKey();
            Byte value = entry.getValue();
            HCT_N.putIfAbsent(value, value);
        }
        int M = dch.getHTConflicting().size();
        int N = HCT_N.size();

        long hct_p4 = M * 40;  //in bits
        long hct_action_profile = 32 * M + M * log2(N) + 8 * N;
        hct_p4 = hct_p4 / 8;
        hct_action_profile = hct_action_profile / 8;

        hct_p4 = hct_p4*2;   //its because hct is stored in both ingress and egress
        hct_action_profile = hct_action_profile*2; //its because hct is stored in both ingress and egress
        
        System.out.println("Memory HCT (P4 table): " + String.format("%.3f", (double)hct_p4/1000000.0) + " MB");        
        System.out.println("Memory HCT (Action Profile): " + String.format("%.3f", (double)hct_action_profile/1000000.0) + " MB");

        //Memory consumption for CPST
        HashMap<String, Integer> CPST_M = new HashMap<>();
        HashMap<Integer, Integer> CPST_N = new HashMap<>();
        fReader = new FileReader(cpstPath);
        bReader = new BufferedReader(fReader);
        line = "";        
        while ((line = bReader.readLine()) != null) {
            String p[] = line.split("/");
            int value = p.length-1;
            CPST_M.putIfAbsent(line, value);
            CPST_N.putIfAbsent(value, value);
        }
        fReader.close();
        bReader.close();
        M = CPST_M.size();
        N = CPST_N.size();
        
        long cpst_p4 = M * 48;  //in bits
        long cpst_action_profile = 40 * M + M * log2(N) + 8 * N;
        cpst_p4 = cpst_p4 / 8;
        cpst_action_profile = cpst_action_profile / 8;

        System.out.println("Memory CPST (P4 table): " + String.format("%.3f", (double)cpst_p4/1000000.0) + " MB");
        System.out.println("Memory CPST (Action Profile): " + String.format("%.3f", (double)cpst_action_profile/1000000.0) + " MB");
        
        //Memory consumption for DPST
        HashMap<String, Integer> DPST_M = new HashMap<>();
        HashMap<Integer, Integer> DPST_N = new HashMap<>();
        fReader = new FileReader(dpstPath);
        bReader = new BufferedReader(fReader);
        line = "";        
        while ((line = bReader.readLine()) != null) {
            String p[] = line.split("/");
            int value = p.length-1;
            DPST_M.putIfAbsent(line, value);
            DPST_N.putIfAbsent(value, value);
        }
        fReader.close();
        bReader.close();
        M = DPST_M.size();
        N = DPST_N.size();
        
        long dpst_p4 = M * 48;  //in bits
        long dpst_action_profile = 40 * M + M * log2(N) + 8 * N;
        dpst_p4 = dpst_p4 / 8;
        dpst_action_profile = dpst_action_profile / 8;

        System.out.println("Memory DPST (P4 table): " + String.format("%.3f", (double)dpst_p4/1000000.0) + " MB");
        System.out.println("Memory DPST (Action Profile): " + String.format("%.3f", (double)dpst_action_profile/1000000.0) + " MB");
          
        System.out.println("Total memory (P4 table): "+ String.format("%.3f", (double)(dfib_p4+hct_p4+cpst_p4+dpst_p4)/1000000.0) + " MB");
        System.out.println("Total memory (Action Profile): "+String.format("%.3f", (double)((dfib_action_profile+hct_action_profile+cpst_action_profile+dpst_action_profile))/1000000.0) + " MB");        
        
    }

    public static void memoryConsumptionCoFIB(String datasetPath) throws IOException {
        FileReader fReader = new FileReader(datasetPath);
        BufferedReader bReader = new BufferedReader(fReader);
        String line = "";

        HashMap<Long, Byte> H[] = new HashMap[32];
        for (int i = 0; i < H.length; i++) {
            H[i] = new HashMap<>();
        }

        CRC32 crc = new CRC32();
        while ((line = bReader.readLine()) != null) {
            String parts[] = line.split(" ");
            String url = parts[0];
            String p[] = url.split("/");
            for (int i = 1; i < p.length; i++) {
                crc.update(p[i].getBytes());
                H[p[i].length()].putIfAbsent(crc.getValue(), Byte.MIN_VALUE);
                crc.reset();
            }
        }
        fReader.close();
        bReader.close();

        long cofib_memory_consumption = 0;

        for (int i = 1; i <= 31; i++) {
            if (i <= 4) {
                cofib_memory_consumption = cofib_memory_consumption + (i + 3) * H[i].size();
            } else {
                cofib_memory_consumption = cofib_memory_consumption + (4 + 3) * H[i].size();
            }
        }
        System.out.println("CoFIB memory consumption (bytes): " + cofib_memory_consumption);
    }
    
    public static void generateShapeFile(String datasetPath, String outputPath) throws FileNotFoundException, IOException {
        FileReader fReader = new FileReader(datasetPath);
        BufferedReader bReader = new BufferedReader(fReader);
        HashMap<String, Byte> h = new HashMap<>();

        String line = "";
        while ((line = bReader.readLine()) != null) {
            String parts[] = line.split(" ");
            String url = parts[0];
            h.putIfAbsent(ShapeEngine.generateShape(url), Byte.MIN_VALUE);
        }
        fReader.close();
        bReader.close();

        FileWriter fWriter = new FileWriter(outputPath);
        Iterator<String> iterator = h.keySet().iterator();
        while (iterator.hasNext()) {
            fWriter.write(iterator.next() + "\n");
        }
        fWriter.close();
    }

    public static void generateCDFPipelinePasses(String datasetPath, String cdfFilePath, int ingress[], int egress[]) throws IOException {

        List<Integer> ingress_list = Arrays.stream(ingress).boxed().toList();
        List<Integer> egress_list = Arrays.stream(egress).boxed().toList();

        FileReader fReader = new FileReader(datasetPath);
        BufferedReader bReader = new BufferedReader(fReader);
        String line = "";
        int i = 0;
        int passes = 0;

        List<Byte> list = new ArrayList<>();
        while ((line = bReader.readLine()) != null) {
            String parts[] = line.split(" ");
            passes = getPipelinePasses(parts[0], ingress_list, egress_list);
            i++;
            list.add((byte) passes);
        }
        fReader.close();
        bReader.close();

        Collections.sort(list);

        ArrayList<String> cdf = new ArrayList<>();

        double previous = 0;
        double value = 0;
        for (Byte p : list) {
            value = ((double) 1 / (double) list.size()) + previous;
            cdf.add(p + " " + value);
            previous = value;
        }

        FileWriter fWriter = new FileWriter(cdfFilePath);
        for (String str : cdf) {
            fWriter.write(str + "\n");
        }
        fWriter.close();
    }

    public static int getPipelinePasses(String prefix, List<Integer> ingress, List<Integer> egress) {

        String p[] = prefix.split("/");
        String bitstring = "";   //0: ingress; 1: egress
        for (int i = 1; i < p.length; i++) {
            if (ingress.contains(p[i].length())) {
                bitstring = bitstring + "0";
            } else {
                if (egress.contains(p[i].length())) {
                    bitstring = bitstring + "1";
                } else {
                    System.out.println("Error.");
                    System.exit(0);
                }
            }
        }

        int passes = 0;
        for (int i = 0; i < bitstring.length(); i++) {
            if (i == 0) {
                passes++;
            } else {
                if (bitstring.charAt(i) == '0') {
                    passes++;
                } else {
                    if (bitstring.charAt(i - 1) == '1') {
                        passes++;
                    }
                }
            }
        }
        return passes;
    }
    
    public static void main(String[] args) throws IOException, Exception {
                
        tablePlacement(Parameters.DATASET_URL + "//final_canonical_dataset.txt");
         
        Pair<Byte, Byte> table_id_ingress_interval = new Pair<>((byte) 1, (byte) 15);
        Pair<Byte, Byte> table_id_egress_interval = new Pair<>((byte) 16, (byte) 31);
        
        if (args.length >= 4) {
            if (args[0].equals("-f")) {
                switch (args[1]) {
                    case "-c":
                        switch (args[2]) {
                            case "--input" -> {
                                if (isCanonicalDataset(args[3], true)) {
                                    System.out.println("The input dataset is canonical.");
                                    System.exit(0);
                                } else {
                                    System.out.println("The input dataset is non-Canonical.");
                                    System.exit(0);
                                }
                            }
                            default -> {
                                error();
                                System.exit(0);
                            }
                        }
                    case "-h":
                        switch (args[2]) {
                            case "--input" -> {
                                if (hasCollision(args[3], true)) {
                                    System.out.println("The input dataset has collision.");
                                    System.exit(0);
                                } else {
                                    System.out.println("The input dataset has no collision.");
                                    System.exit(0);
                                }
                            }
                            default -> {
                                error();
                                System.exit(0);
                            }
                        }
                    case "-l":
                        if (args.length == 4) {
                            switch (args[2]) {
                                case "--input" -> {
                                    loadPrefixes(args[3]);
                                    System.exit(0);
                                }
                                default -> {
                                    error();
                                    System.exit(0);
                                }
                            }
                        } else {
                            error();
                            System.exit(0);
                        }
                    case "-e":
                        if (args.length == 6) {
                            switch (args[2]) {
                                case "--input" -> {
                                    switch (args[4]) {
                                        case "--output" -> {
                                            int ingressTables[] = {5, 8, 23, 11, 22, 13, 24, 16, 15, 29, 17, 2, 3};
                                            int egressTables[] = {4, 6, 7, 9, 30, 10, 21, 20, 31, 12, 14, 25, 27, 26, 18, 28, 19, 1};
                                            generateP4RuntimeEntries(args[3], args[5], ingressTables, egressTables);
                                            System.exit(0);
                                        }
                                        default -> {
                                            error();
                                            System.exit(0);
                                        }
                                    }
                                }
                                default -> {
                                    error();
                                    System.exit(0);
                                }
                            }
                        } else {
                            error();
                            System.exit(0);
                        }

                    case "-t":
                        if (args.length == 6) {
                            switch (args[2]) {
                                case "--input" -> {
                                    switch (args[4]) {
                                        case "--output" -> {
                                            AdaptorToBMv2.generateScapyPackets(args[3], args[5], true);
                                            System.exit(0);
                                        }
                                        default -> {
                                            error();
                                            System.exit(0);
                                        }
                                    }
                                }
                                default -> {
                                    error();
                                    System.exit(0);
                                }
                            }
                        } else {
                            error();
                            System.exit(0);
                        }
                    case "-r":
                        if (args.length == 6) {
                            switch (args[2]) {
                                case "--output" -> {
                                    switch (args[4]) {
                                        case "-n": {
                                            String s[] = args[5].split("-");
                                            if (s.length != 2) {
                                                int num_comps = 1;
                                                try {
                                                    num_comps = Integer.parseInt(args[5]);
                                                    if (num_comps < 1 || num_comps > 8) {
                                                        throw new Exception();
                                                    }
                                                } catch (Exception e) {
                                                    System.exit(0);
                                                }
                                                createRandomDatasetTemplateFile(num_comps, table_id_ingress_interval, table_id_egress_interval, args[3]);
                                                System.exit(0);
                                            } else {
                                                try {
                                                    int x1 = Integer.parseInt(s[0]);
                                                    int x2 = Integer.parseInt(s[1]);
                                                    if (x1 >= x2) {
                                                        throw new Exception();
                                                    } else {
                                                        if (x1 < 1 || x2 > 8) {
                                                            throw new Exception();
                                                        }
                                                    }
                                                    createRandomDatasetTemplateFile(x1, x2, table_id_ingress_interval, table_id_egress_interval, args[3]);
                                                    System.exit(0);
                                                } catch (Exception e) {
                                                    System.out.println("Error in parameters -n. " + e.getLocalizedMessage());
                                                    System.exit(0);
                                                }
                                            }
                                        }
                                        default: {
                                            error();
                                            System.exit(0);
                                        }
                                    }
                                }
                                default -> {
                                    error();
                                    System.exit(0);
                                }
                            }
                        } else {
                            error();
                            System.exit(0);
                        }
                    case "-p":
                        if (args.length == 10) {
                            String templateFile = "";
                            String packetsBinFile = "";
                            if (args[2].equals("--input")) {
                                templateFile = args[3];
                                if (args[4].equals("--output")) {
                                    packetsBinFile = args[5];
                                    if (args[6].equals("--pps")) {
                                        Integer pps = Integer.parseInt(args[7]);
                                        if (pps >= 1 && pps <= 5000) {
                                            if (args[8].equals("--duration")) {
                                                Integer duration = Integer.parseInt(args[9]);
                                                if (duration % 8 == 0) {
                                                    if (validateTemplateForTrafficGeneration(templateFile)) {
                                                        //generate the intermediate packets.txt
                                                        AdaptorToBMv2.generateFinalPacketsTxtSorted(templateFile, pps, duration);
                                                        //generate the final packets.bin 
                                                        AdaptorToBMv2.generateScapyPackets("packets.txt", packetsBinFile, true);
                                                        System.exit(0);
                                                    } else {
                                                        System.out.println("The template is not valid.");
                                                        System.exit(0);
                                                    }
                                                } else {
                                                    System.out.println("The duration value must be multiple of 8.");
                                                    System.exit(0);
                                                }
                                            } else {
                                                System.out.println("You must provide the '--duration' parameter.");
                                                System.exit(0);
                                            }
                                        } else {
                                            System.out.println("The '--pps' parameter need to be in the interval [1, 5000], inclusive.");
                                            System.exit(0);
                                        }
                                    } else {
                                        System.out.println("You must provide the '--pps' parameter.");
                                        System.exit(0);
                                    }
                                } else {
                                    System.out.println("You must provide the '--output' parameter.");
                                    System.exit(0);
                                }
                            } else {
                                System.out.println("You must provide the '--input' parameter.");
                                System.exit(0);
                            }
                        } else {
                            error();
                            System.exit(0);
                        }
                    case "-P":
                        if (args.length == 10) {
                            String templateFile = "";
                            String packetsBinFile = "";
                            if (args[2].equals("--input")) {
                                templateFile = args[3];
                                if (args[4].equals("--output")) {
                                    packetsBinFile = args[5];
                                    if (args[6].equals("--pps")) {
                                        Integer pps = Integer.parseInt(args[7]);
                                        if (pps >= 1 && pps <= 5000) {
                                            if (args[8].equals("--duration")) {
                                                Integer duration = Integer.parseInt(args[9]);
                                                if (duration % 8 == 0) {
                                                    if (validateTemplateForTrafficGeneration(templateFile)) {
                                                        //generate the intermediate packets.txt
                                                        AdaptorToBMv2.generateFinalPacketsTxtRandom(templateFile, pps, duration);
                                                        //generate the final packets.bin 
                                                        AdaptorToBMv2.generateScapyPackets("packets.txt", packetsBinFile, true);
                                                        System.exit(0);
                                                    } else {
                                                        System.out.println("The template is not valid.");
                                                        System.exit(0);
                                                    }
                                                } else {
                                                    System.out.println("The duration value must be multiple of 8.");
                                                    System.exit(0);
                                                }
                                            } else {
                                                System.out.println("You must provide the '--duration' parameter.");
                                                System.exit(0);
                                            }
                                        } else {
                                            System.out.println("The '--pps' parameter need to be in the interval [1, 5000], inclusive.");
                                            System.exit(0);
                                        }
                                    } else {
                                        System.out.println("You must provide the '--pps' parameter.");
                                        System.exit(0);
                                    }
                                } else {
                                    System.out.println("You must provide the '--output' parameter.");
                                    System.exit(0);
                                }
                            } else {
                                System.out.println("You must provide the '--input' parameter.");
                                System.exit(0);
                            }
                        } else {
                            error();
                            System.exit(0);
                        }
                    default: {
                        error();
                        System.exit(0);
                    }
                }
            } else {
                error();
                System.exit(0);
            }
        } else {
            error();
            System.exit(0);
        }
    }
}