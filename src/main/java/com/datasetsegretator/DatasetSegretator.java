package com.datasetsegretator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;
import org.apache.commons.math3.distribution.WeibullDistribution;
import com.p4.environment.adaptation.Port;

/**
 * I set the parameter -Xmx4000m to JVM in VM options.
 */
public class DatasetSegretator {

    public static HashMap<Integer, String> extractDomainWordsFrom10MDatasetAnd400KDataset(String datasetPath400K, String datasetPath10M) throws FileNotFoundException, IOException {

        File root = new File(datasetPath400K);
        File[] list = root.listFiles();

        if (list == null) {
            return null;
        }

        HashMap<String, Byte> hash = new HashMap<>();

        System.out.print("Extracting individual domain words from 400k dataset...");

        for (File f : list) {
            if (f.isFile()) {
                if (f.getAbsoluteFile().getPath().contains(".txt")) {
                    try {

                        FileReader file = new FileReader(f.getAbsoluteFile().getPath());
                        BufferedReader fileReader = new BufferedReader(file);

                        String line = "";

                        while ((line = fileReader.readLine()) != null) {

                            String url1 = line;
                            String parts[] = url1.split("//");

                            String url2 = parts[1];
                            parts = url2.split("/");

                            String url3 = parts[0];

                            String p[] = url3.split("\\.");

                            for (int i = 0; i < p.length; i++) {
                                if (!p[i].equals(" ")) {
                                    hash.putIfAbsent(p[i], (byte) p[i].length());
                                }
                            }

                        }

                    } catch (FileNotFoundException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
        System.out.println(" Domain words extracted: " + hash.size() + " OK");

        System.out.print("Extracting individual domain words from 10m dataset...");
        FileReader dataset_file = new FileReader(datasetPath10M);
        BufferedReader fileReader = new BufferedReader(dataset_file);
        String line = "";
        String url = "";
        boolean first = true;
        while ((line = fileReader.readLine()) != null) {

            if (!first) {
                String l[] = line.split(",");
                String p[] = l[1].split("\"");
                url = p[1];

                String parts[] = url.split("\\.");
                for (int i = 0; i < parts.length; i++) {
                    if (!parts[i].equals(" ")) {
                        hash.putIfAbsent(parts[i], (byte) 1);
                    }
                }
            } else {
                first = false;
            }
        }
        dataset_file.close();
        fileReader.close();

        System.out.println(" OK. Total individual domain words extracted from 400k and 10m dataset: " + hash.size());

        HashMap<Integer, String> domain_words = new HashMap<>();
        Iterator<String> iterator = hash.keySet().iterator();
        int n = 0;
        while (iterator.hasNext()) {
            domain_words.put(n, iterator.next());
            n++;
        }

        hash.clear();

        return domain_words;
    }

    public static void printArray(int v[]) {
        System.out.print("[");
        for (int i = 0; i < v.length; i++) {
            if (i < (v.length - 1)) {
                System.out.print(v[i] + " ");
            } else {
                System.out.println(v[i] + "]");
            }
        }
    }

    public static boolean isParsable(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    public static boolean containNumber(String url) {
        String p[] = url.split("/");
        for (int i = 1; i < p.length; i++) {
            if (isParsable(p[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUrlValid(String url, int max_name_components, int max_component_length) {

        if (!url.contains("/") || url.contains(" ") || url.contains("'") || url.contains(":")) {
            return false;
        }

        if (containNumber(url)) {
            return false;
        }

        String p[] = url.split("/");
        for (int i = 1; i < p.length; i++) {
            if (p[i].length() > max_component_length) {
                return false;
            }
        }

        int n_comp = p.length - 1;

        if (n_comp < 1 || n_comp > max_name_components) {
            return false;
        }

        if (hasDuplicatedWords(url.split("/"))) {
            return false;
        }

        return true;
    }

    public static void generatePopularityCDFFiles(String entireDataset, String canonicalDataset, String nonCanonicalDataset) throws FileNotFoundException, IOException {
        ArrayList<Double> list = new ArrayList<>();
        String line;
        
        //Calculating the popularity CDF for the entire dataset...
        FileReader fReader = new FileReader(entireDataset);
        BufferedReader bReader = new BufferedReader(fReader);
        while ((line = bReader.readLine()) != null) {
            String parts[] = line.split(" ");
            list.add(Double.parseDouble(parts[3]));
        }
        fReader.close();
        bReader.close();

        Collections.sort(list);
        
        ArrayList<String> cdf = new ArrayList<>();
        
        double previous = 0;
        double value = 0;
        for (Double d : list) {
            value = ((double) 1 / (double) list.size()) + previous;
            cdf.add(d + " " + value);
            previous = value;
        }

        FileWriter fWriter = new FileWriter(Parameters.DATASET_URL + "\\cdf_popularity\\full_dataset_cdf.txt");
        for (String str : cdf) {
            fWriter.write(str + "\n");
        }
        fWriter.close();
        list.clear();
        cdf.clear();

        //Calculating the popularity CDF for the canonical dataset...
        fReader = new FileReader(canonicalDataset);
        bReader = new BufferedReader(fReader);
        while ((line = bReader.readLine()) != null) {
            String parts[] = line.split(" ");
            list.add(Double.parseDouble(parts[3]));
        }
        fReader.close();
        bReader.close();

        Collections.sort(list);

        cdf = new ArrayList<>();

        previous = 0;
        value = 0;
        for (Double d : list) {
            value = ((double) 1 / (double) list.size()) + previous;
            cdf.add(d + " " + value);
            previous = value;
        }

        fWriter = new FileWriter(Parameters.DATASET_URL + "\\cdf_popularity\\canonical_dataset_cdf.txt");
        for (String str : cdf) {
            fWriter.write(str + "\n");
        }
        fWriter.close();
        list.clear();
        cdf.clear();

        //Calculating the popularity CDF for the non canonical dataset...
        fReader = new FileReader(nonCanonicalDataset);
        bReader = new BufferedReader(fReader);
        while ((line = bReader.readLine()) != null) {
            String parts[] = line.split(" ");
            list.add(Double.parseDouble(parts[3]));
        }
        fReader.close();
        bReader.close();

        Collections.sort(list);

        cdf = new ArrayList<>();

        previous = 0;
        value = 0;
        for (Double d : list) {
            value = ((double) 1 / (double) list.size()) + previous;
            cdf.add(d + " " + value);
            previous = value;
        }

        fWriter = new FileWriter(Parameters.DATASET_URL + "\\cdf_popularity\\non_canonical_dataset_cdf.txt");
        for (String str : cdf) {
            fWriter.write(str + "\n");
        }
        fWriter.close();
        list.clear();
        cdf.clear();
    }

    public static void performBasicStatistics(String filePath) throws FileNotFoundException, IOException {
        FileReader fReader = new FileReader(filePath);
        BufferedReader bReader = new BufferedReader(fReader);
        String line;
        long n = 0;
        long sum_comp_length = 0;
        long sum_number_comps = 0;
        long sum_url_size = 0;
        long max_number_comp = 0;

        long v[] = new long[9];
        Arrays.fill(v, 0);

        while ((line = bReader.readLine()) != null) {
            n++;
            String parts[] = line.split(" ");
            String url = parts[0];
            String p[] = url.split("/");
            sum_number_comps = sum_number_comps + (p.length - 1);

            if ((p.length - 1) > max_number_comp) {
                max_number_comp = p.length - 1;
            }

            v[p.length - 1]++;

            sum_url_size = sum_url_size + url.length();
            for (int i = 1; i < p.length; i++) {
                sum_comp_length = sum_comp_length + p[i].length();
            }
        }
        fReader.close();
        bReader.close();

        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++ STATISTICS ++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("Number canonical names: " + n);
        System.out.println("Sum number of components: " + sum_number_comps);
        System.out.println("Max number of components: " + max_number_comp);
        System.out.println("Number of components per name: " + String.format("%.2f", (double) (sum_number_comps) / (double) n));
        System.out.println("Average name length: " + String.format("%.2f", (double) sum_url_size / (double) n));
        System.out.println("Average component length: " + String.format("%.2f", (double) sum_comp_length / (double) sum_number_comps));

        System.out.print("Number of Components: ");
        long s = 0;
        for (int i = 1; i < v.length; i++) {
            s = s + v[i];
            System.out.print(v[i] + " ");
        }
        System.out.println();
        System.out.print("Percentage of prefixes / number of name components: ");
        for (int i = 1; i < v.length; i++) {
            System.out.print(String.format("%.2f", (double) (v[i]) / (double) s) + " ");
        }
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    public static void generateSynteticDataset(long n, int max_name_components, int max_component_length, double pr) throws IOException {

       
        HashMap<Integer, String> h = extractDomainWordsFrom10MDatasetAnd400KDataset("C:\\Users\\casti\\OneDrive\\Documentos\\Backup Old Laptop\\Arquivos Não Salvos\\dataset1", Parameters.DATASET_URL + "\\raw_dataset.txt");

        System.out.print("Generating the syntetic dataset with Pr=" + pr + " ..");

        FileWriter file = new FileWriter(Parameters.DATASET_URL + "\\syntetic_dataset.txt");
        //PsRandom ps = new PsRandom();
        WeibullDistribution wb = new WeibullDistribution(0.3, 2);
        Random r = new Random();

        //This hashmap is to avoid having an url containing a given word in two different positions.
        //E.g.: Avoid url like /com/google/com
        HashMap<String, Byte> aux = new HashMap<>();

        //This hashmap is to avoid dupplicated urls in the dataset..
        HashMap<String, Byte> hash = new HashMap<>();

        long sum_number_comps = 0;
        long max_number_comps = 0;
        long sum_prefix_length = 0;
        long sum_comp_length = 0;
        long num_comps = 0;

        Random random = new Random();
        
        for (int k = 1; k <= n; k++) {

            String url = "/";

            //Generate the number of components from the weibull distribuition with parameters 0.1, 0.3, 2;
            int number_of_components = 0;            
            do {
                number_of_components = (int) Math.round(wb.sample()*10);
                //number_of_components = (int) Math.round(ps.nextWeibull(0.1, 0.3, 2) * 10);
            } while (number_of_components <= 0 || number_of_components > max_name_components);

            
            
            //Create an url concatenating sequence of words from the hash table h
            for (int i = 0; i < number_of_components; i++) {

                String component = "";
                Byte b = 0;
                
                //component = RandomStringUtils.random(random.nextInt(1, 32), true, false);
                
                
                do {
                    component = h.get(r.nextInt(h.size()));
                    if (component.length() <= max_component_length) {
                        b = aux.putIfAbsent(component, (byte) component.length());
                    }
                } while ((component.length() > max_component_length) || (b != null));
                
                
                
                if (i < (number_of_components - 1)) {
                    url = url + component + "/";
                } else {
                    url = url + component;
                }
            }

            //For statistics purpose....................
            String p[] = url.split("/");
            for (int i = 1; i < p.length; i++) {
                sum_comp_length = sum_comp_length + p[i].length();
                num_comps++;
            }
            sum_number_comps = sum_number_comps + (p.length - 1);
            if ((p.length - 1) > max_number_comps) {
                max_number_comps = p.length - 1;
            }
            sum_prefix_length = sum_prefix_length + url.length();
            /////////////////////////////////////////////

            aux.clear();

            //At this point, a valid url is available and we insert it into the hashmap to avoid dupplication..
            if (hash.putIfAbsent(url, (byte) url.length()) == null) {

                //Save the url the file assuring that its unique
                file.write(url + " " + Port.generateRandomPort() + " 1 1\n");
                //-------------------------------------------------------------------------------

                //Adding subprefixes..                
                if (r.nextDouble() < pr) {
                    String parts_reverse[] = url.split("/");

                    // url           = /com/google/www
                    // parts_reverse = ["", "com", "google", "www"]
                    for (int i = parts_reverse.length - 1; i >= 2; i--) {

                        String subprefix = subprefix(url, parts_reverse[i]);
                        if (hash.putIfAbsent(subprefix, (byte) subprefix.length()) == null) {

                            //Save the url into the file assuring that its unique
                            file.write(subprefix + " " + Port.generateRandomPort() + " 1 1\n");
                            //-------------------------------------------------------------------------------

                        }
                    }
                }

            } else {
                //Whenever we generate an url that already exist, we need to increment n by 1
                n++;
            }
        }

        //For statistics purpose..
        System.out.println();
        System.out.println("Avg Prefix Levels: " + String.format("%.2f", (double) sum_number_comps / (double) n));
        System.out.println("Avg Prefix Length: " + String.format("%.2f", (double) sum_prefix_length / (double) n));
        System.out.println("Avg Comp. Length: " + String.format("%.2f", (double) sum_comp_length / (double) num_comps));
        System.out.println("Max Prefix Levels: " + max_number_comps);

        file.close();
        hash.clear();
        h.clear();
        System.out.println("OK");
    }

    public static void generateSynteticDatasetWithValidationInfo(DualComponentHashmap dataStructure, HashMap<String, Boolean>[] dpShapeHashTable, HashMap<String, Boolean>[] cpShapeHashTable, int n, int max_component_size, double pr) throws IOException {

        //int length = r.nextInt(31);            
        //String s = RandomStringUtils.random(length, true, true);            
                
        HashMap<Integer, String> h = extractDomainWordsFrom10MDatasetAnd400KDataset("E:\\Users\\IF Pesquisa\\Documents\\Arquivos Não Salvos\\dataset1", Parameters.DATASET_URL + "\\raw_dataset.txt");

        System.out.print("Generating the syntetic dataset with Pr=" + pr + " ..");

        FileWriter file = new FileWriter(Parameters.DATASET_URL + "\\syntetic_dataset.txt");
        
                                                        //mu, alpha, gama in previous PsRandom class
                                                        //0.1, 0.3, 2 
        WeibullDistribution wb = new WeibullDistribution(0.3, 2);
        
        Random r = new Random();

        //This hashmap is to avoid having an url containing a given word in two different positions.
        //E.g.: Avoid url like /com/google/com
        HashMap<String, Byte> aux = new HashMap<>();

        //This hashmap is to avoid dupplicated urls in the dataset..
        HashMap<String, Byte> hash = new HashMap<>();

        for (int k = 1; k <= n; k++) {

            String url = "/";

            //Generate the number of components from the weibull distribuition with parameters 0.1, 0.3, 2;
            int number_of_components = 0;
            do {
                number_of_components = (int) Math.round(wb.sample()*10);
                                                                   //mu, alpha, gama
                //number_of_components = (int) Math.round(ps.nextWeibull(0.1, 0.3, 2) * 10);
            } while (number_of_components <= 0 || number_of_components >= 9);

            //Create an url concatenating sequence of words from the hash table h
            for (int i = 0; i < number_of_components; i++) {

                String component = "";
                Byte b = 0;

                do {
                    component = h.get(r.nextInt(h.size()));
                    if (component.length() <= max_component_size) {
                        b = aux.putIfAbsent(component, (byte) component.length());
                    }
                } while ((component.length() > max_component_size) || (b != null));

                if (i < (number_of_components - 1)) {
                    url = url + component + "/";
                } else {
                    url = url + component;
                }
            }

            aux.clear();

            //At this point, a valid url is available and we insert it into the hashmap to avoid dupplication..
            if (hash.putIfAbsent(url, (byte) url.length()) == null) {

                //-------------------------------------------------------------------------------
                //Determining the expected output port. This piece of code 
                //reflects exacly the same samantic of the P4 code 
                Byte expected_output_port;
                if (!ShapeEngine.shapeMatch(url, dpShapeHashTable)) {
                    if (ShapeEngine.shapeMatch(url, cpShapeHashTable)) {
                        expected_output_port = Port.CPU;  //CPU port 
                    } else {
                        expected_output_port = Port.DEFAULT;  //DEFAULT port
                    }
                } else {
                    //checking if the first name component matches.. if it doesn't,
                    //the packet should be sent to CPU (see in P4 code)
                    String p1[] = url.split("/");
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
                            expected_output_port = dataStructure.performLPM(url);
                        }
                    }
                }
                //Save both url and the output port into the file assuring that its unique
                file.write(url + " " + expected_output_port + "\n");
                //-------------------------------------------------------------------------------

                //Adding subprefixes..                
                if (r.nextDouble() < pr) {
                    String parts_reverse[] = url.split("/");

                    // url           = /com/google/www
                    // parts_reverse = ["", "com", "google", "www"]
                    for (int i = parts_reverse.length - 1; i >= 2; i--) {

                        String subprefix = subprefix(url, parts_reverse[i]);
                        if (hash.putIfAbsent(subprefix, (byte) subprefix.length()) == null) {

                            //-------------------------------------------------------------------------------
                            //Determining the expected output port. This piece of code 
                            //reflects exacly the same samantic of the P4 code 
                            expected_output_port = 0;
                            if (!ShapeEngine.shapeMatch(subprefix, dpShapeHashTable)) {
                                if (ShapeEngine.shapeMatch(subprefix, cpShapeHashTable)) {
                                    expected_output_port = Port.CPU;  //CPU port 
                                } else {
                                    expected_output_port = Port.DEFAULT;  //DEFAULT port
                                }
                            } else {
                                //checking if the first name component matches.. if it doesn't,
                                //the packet should be sent to CPU (see in P4 code)
                                String p1[] = subprefix.split("/");
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
                                        expected_output_port = dataStructure.performLPM(subprefix);
                                    }
                                }
                            }
                            //Save both url and the output port into the file assuring that its unique
                            file.write(subprefix + " " + expected_output_port + "\n");
                            //-------------------------------------------------------------------------------

                        }
                    }
                }

            } else {
                //Whenever we generate an url that already exist, we need to increment n by 1
                n++;
            }

        }
        file.close();
        hash.clear();
        h.clear();
        System.out.println("OK");
    }

    public static void adapt400KDataset(String datasetPathDir, int max_name_components, int max_component_length, double pr) throws IOException {

        File root = new File(datasetPathDir);
        File[] list = root.listFiles();

        System.out.print("Adapting the raw dataset (400k websites)...");

        if (list == null) {
            return;
        }

        long nFiles = 0;
        long nUrls = 0;

        FileWriter new_dataset_file = new FileWriter(Parameters.DATASET_URL + "\\adjusted_dataset.txt");
        HashMap<String, Byte> hash = new HashMap<>();

        long sum_number_comps = 0;
        long max_number_comps = 0;
        long sum_prefix_length = 0;
        long sum_comp_length = 0;
        long num_comps = 0;

        for (File f : list) {
            if (f.isFile()) {
                if (f.getAbsoluteFile().getPath().contains(".txt")) {
                    nFiles++;
                    try {

                        FileReader file = new FileReader(f.getAbsoluteFile().getPath());
                        BufferedReader fileReader = new BufferedReader(file);

                        String line = "";

                        while ((line = fileReader.readLine()) != null) {

                            nUrls++;

                            String url1 = line;
                            String parts[] = url1.split("//");
                           
                            String url2 = parts[1];
                            parts = url2.split("/");

                            String url3 = parts[0];
                            String url4 = url3.replace(".", "/");

                            parts = url4.split("/");
                            if (parts[0].equals("www")) {
                                url4 = url4.replaceFirst("www", "");
                            } else {
                                url4 = "/" + url4;
                            }

                            String url5 = reverseUrl(url4);

                            if (isUrlValid(url5, max_name_components, max_component_length)) {
                                if (hash.putIfAbsent(url5, (byte) 1) == null) {

                                }
                            }
                             
                        }

                    } catch (FileNotFoundException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }

        Random r = new Random();
        HashMap<String, Byte> hash_copy = (HashMap<String, Byte>) hash.clone();

        int n_subprefixes = 0;

        String url = "";

        Iterator<String> iterator = hash_copy.keySet().iterator();
        while (iterator.hasNext()) {

            url = iterator.next();
            new_dataset_file.write(url + " " + Port.generateRandomPort() + " 1 1\n");

            //Adding subprefixes..
            if (r.nextDouble() < pr) {
                String parts_reverse[] = url.split("/");

                // url           = /com/google/www
                // parts_reverse = ["", "com", "google", "www"]
                for (int i = parts_reverse.length - 1; i >= 2; i--) {

                    String subprefix = subprefix(url, parts_reverse[i]);
                    if (hash.putIfAbsent(subprefix, (byte) subprefix.length()) == null) {
                        new_dataset_file.write(subprefix + " " + Port.generateRandomPort() + " 1 1\n");
                        n_subprefixes++;
                    }
                }
            }
        }
        System.out.println("#prefixes=" + nUrls + " #subprefixes=" + n_subprefixes + " #unique prefixes=" + hash.size() + " OK");
        new_dataset_file.close();
    }

    /**
     * Adapt the 10M raw dataset as follows: 1) Eliminate urls that contains
     * duplicated name components. I.e: /com/google/www/com 2) For urls with
     * more than max name componets, take just the 'max' first ones. Typically
     * we use max=8. 3) Eliminate urls that have name components greater than
     * 'max_comp_size' 4) Reverse the url 5) Check with probability 'pr' whether
     * or not the url will contain all its subprefixes 6) Generate the output
     * port for each url (for the time being we are using 1 Byte) 7) Store the
     * url and its rank and open page rank into the file adjusted_dataset.txt
     *
     * When finishing this method, each url is as follows: url output_port rank
     * open_page_rank
     *
     * @ Input: raw_dataset.txt, max name components, probability to have
     * subprefixes
     * @ Output: adjusted_dataset.txt
     */
    public static void adaptRawDataset(String rawDatasetPath, int max_comps, int max_comp_size, double pr) throws FileNotFoundException, IOException {

        System.out.print("Adapting the raw dataset (10 million most popular websites)...");

        FileReader dataset_file = new FileReader(rawDatasetPath);
        BufferedReader fileReader = new BufferedReader(dataset_file);
        String line = "";
        String url = "";

        FileWriter new_dataset_file = new FileWriter(Parameters.DATASET_URL + "\\adjusted_dataset.txt");

        long regularUrls = 0;
        long dupplicatedUrls = 0;
        long urlsIncorrectNameComponentSizes = 0;
        boolean firstLine = true;

        HashMap<String, Byte> hash = new HashMap<>();

        Random r = new Random();

        //For statistics purpuse...
        long sum_number_comps = 0;
        long max_number_comps = 0;
        long sum_prefix_length = 0;
        long sum_comp_length = 0;
        long num_comps = 0;
        long nUrls = 0;

        while ((line = fileReader.readLine()) != null) {

            //Not copy the first line from the file.. /Domain
            if (!firstLine) {

                String l[] = line.split(",");
                String rank = l[0].split("\"")[1];
                String p[] = l[1].split("\"");
                url = p[1];

                String openPageRankStr[] = l[2].split("\"");

                url = "/" + url.replace('.', '/');

                String reverseUrl = "";

                String parts[] = url.split("/");
                if (!hasDuplicatedWords(parts)) {

                    
                    //For statistics purpose....................
                    String p1[] = url.split("/");
                    nUrls++;
                    for (int i = 1; i < p.length; i++) {
                        sum_comp_length = sum_comp_length + p1[i].length();
                        num_comps++;
                    }
                    sum_number_comps = sum_number_comps + (p1.length - 1);
                    if ((p1.length - 1) > max_number_comps) {
                        max_number_comps = p1.length - 1;
                    }
                    sum_prefix_length = sum_prefix_length + url.length();
                    /////////////////////////////////////////////

                    
                    
                    //filtering urls with 8 or less name components
                    if (parts.length > 1 && ((parts.length - 1) <= max_comps)) {

                        //filtering urls with name component size up to max_comp_size
                        if (isComponentSizesValid(url, max_comp_size)) {

                            reverseUrl = reverseUrl(url);

                            if (hash.putIfAbsent(reverseUrl, (byte) reverseUrl.length()) == null) {

                                //byte output_port = (byte) (1 + (byte) r.nextInt(127));
                                byte output_port = Port.generateRandomPort();

                                new_dataset_file.write(reverseUrl + " " + output_port + " " + rank + " " + openPageRankStr[1] + "\n");
                                regularUrls++;
                            }

                            if (r.nextDouble() < pr) {

                                String parts_reverse[] = reverseUrl.split("/");

                                // url           = /com/google/www
                                // parts_reverse = ["", "com", "google", "www"]
                                for (int i = parts_reverse.length - 1; i >= 2; i--) {

                                    String subprefix = subprefix(reverseUrl, parts_reverse[i]);

                                    if (hash.putIfAbsent(subprefix, (byte) subprefix.length()) == null) {

                                        //byte output_port = (byte) (1 + (byte) r.nextInt(127));
                                        byte output_port = Port.generateRandomPort();

                                        new_dataset_file.write(subprefix + " " + output_port + " " + rank + " " + openPageRankStr[1] + "\n");
                                        regularUrls++;
                                    }
                                }
                            }
                        } else {
                            urlsIncorrectNameComponentSizes++;
                        }
                    } else {

                        //The URL has more than 8 name components.
                        //Checking if the component sizes is valid
                        if (isComponentSizesValid(url, max_comp_size)) {

                            //Take only the 8 first name components
                            String u = "";
                            for (int i = 1; i <= max_comps; i++) {
                                u = u + "/" + parts[i];
                            }

                            reverseUrl = reverseUrl(u);

                            if (hash.putIfAbsent(reverseUrl, (byte) reverseUrl.length()) == null) {

                                //byte output_port = (byte) (1 + (byte) r.nextInt(127));
                                byte output_port = Port.generateRandomPort();

                                new_dataset_file.write(reverseUrl + " " + output_port + " " + rank + " " + openPageRankStr[1] + "\n");
                                regularUrls++;
                            }

                            if (r.nextDouble() < pr) {

                                String parts_reverse[] = reverseUrl.split("/");

                                // url           = /com/google/www
                                // parts_reverse = ["", "com", "google", "www"]
                                for (int i = parts_reverse.length - 1; i >= 2; i--) {
                                    String subprefix = subprefix(reverseUrl, parts_reverse[i]);

                                    if (hash.putIfAbsent(subprefix, (byte) subprefix.length()) == null) {

                                        //byte output_port = (byte) (1 + (byte) r.nextInt(127));
                                        byte output_port = Port.generateRandomPort();

                                        new_dataset_file.write(subprefix + " " + output_port + " " + rank + " " + openPageRankStr[1] + "\n");
                                        regularUrls++;
                                    }
                                }
                            }
                        } else {
                            urlsIncorrectNameComponentSizes++;
                        }
                    }
                } else {
                    dupplicatedUrls++;
                }
            } else {
                firstLine = false;
            }
        }

        
        hash.clear();

        dataset_file.close();
        fileReader.close();
        new_dataset_file.close();

        System.out.println(" " + regularUrls + " adjusted urls and " + dupplicatedUrls + " dupplicated urls eliminated. Also " + urlsIncorrectNameComponentSizes + " urls were eliminated because of incorrect name component sizes..");
    }

    /**
     * Create a hash table with key=name component and value=position map
     * Position map is an array containing the frequency of each name component
     * in a given position. I.e: "com" -> [23 12 3 4 0 0 0 0] means that the
     * name component com appears 23 times in 1th position, 12 times in 2th
     * position, and so on
     *
     * @ Input: adjusted_dataset.txt
     * @ Output: hash table h<String,int[]>
     */
    public static HashMap<String, int[]> createHashTable(String adjustedDatasetPath, String typeDataset) throws IOException {

        System.out.print("Creating the hash table...");
        HashMap<String, int[]> h = new HashMap<>();
        FileReader dataset_file = new FileReader(adjustedDatasetPath);
        BufferedReader fileReader = new BufferedReader(dataset_file);

        String line = "";
        String url = "";

        while ((line = fileReader.readLine()) != null) {

            String l[] = null;
            String p[] = null;

            l = line.split(" ");
            url = l[0];
            p = url.split("/");

            for (int i = 1; i < p.length; i++) {

                int v[] = new int[8];
                Arrays.fill(v, 0);

                try {
                    v[i - 1] = 1;
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("URL=" + l[0]);
                    System.exit(0);
                }

                int value[] = h.putIfAbsent(p[i], v);

                if (value != null) {

                    if (value[i - 1] < Integer.MAX_VALUE) {
                        value[i - 1]++;
                        h.replace(p[i], value);
                    }
                }
            }
        }
        dataset_file.close();
        fileReader.close();
        System.out.println(" " + h.size() + " individual words extracted.. OK");
        return h;

    }

    /**
     * Calculate the weights to all URLs in the ajusted dataset.
     *
     * @ Input: adjusted_dataset.txt, hash table h<String,int[]>
     * @ Output: weighted_dataset.txt
     */
    public static void calculateWeights(String adjustedDatasetPath, HashMap<String, int[]> h, String typeDataset) throws FileNotFoundException, IOException {

        System.out.print("Calculating the weights for each adjusted url...");

        FileReader dataset_file = new FileReader(adjustedDatasetPath);
        BufferedReader fileReader = new BufferedReader(dataset_file);
        FileWriter fileWriter = new FileWriter(Parameters.DATASET_URL + "\\weighted_dataset.txt");

        String line = "";

        while ((line = fileReader.readLine()) != null) {

            String p[] = null;
            String url = "";
            String output_port = "";
            String rank = "";
            String openPageRank = "";

            p = line.split(" ");
            url = p[0];
            output_port = p[1];

            if (typeDataset.equals("dataset3")) {
                rank = p[2];
                openPageRank = p[3];
            }

            int weigth = calculateUrlWeight(url, h);

            if (typeDataset.equals("dataset3")) {
                fileWriter.write(url + " " + output_port + " " + weigth + " " + rank + " " + openPageRank + "\n");
            } else {
                fileWriter.write(url + " " + output_port + " " + weigth + "\n");
            }
        }
        dataset_file.close();
        fileReader.close();
        fileWriter.close();

        System.out.println("OK.");
    }

    /**
     * Insert all weighted URLs into a list and sort it into a sorted dataset
     * file.
     *
     * @ Input: weighted_dataset.txt, type of dataset (dataset1, dataset2,
     * dataset3)
     * @ Output: sorted_dataset.txt
     */
    public static void sortWeightedDataset(String weightedDatasetPath, String typeDataset) throws FileNotFoundException, IOException {

        System.out.println("Inserting all URLs into the array list...");

        FileReader dataset_file = new FileReader(weightedDatasetPath);
        BufferedReader fileReader = new BufferedReader(dataset_file);
        FileWriter fileWriter = new FileWriter(Parameters.DATASET_URL + "\\sorted_dataset.txt");
        String url = "";

        boolean exception = false;

        //For dataset3 (with rank information)
        ArrayList<WeightedURLWithRank> listRank = new ArrayList<>();
        WeightedURLWithRank uRank;

        //For dataset1 and dataset2 (with no ranks information)
        ArrayList<WeightedURL> list = new ArrayList<>();
        WeightedURL u;

        try {
            while ((url = fileReader.readLine()) != null) {
                String p[] = url.split(" ");

                if (typeDataset.equals("dataset3")) {
                    uRank = new WeightedURLWithRank(p[0], Byte.parseByte(p[1]), Integer.parseInt(p[2]), Long.parseLong(p[3]), Double.parseDouble(p[4]));
                    listRank.add(uRank);
                } else {
                    u = new WeightedURL(p[0], Byte.parseByte(p[1]), Integer.parseInt(p[2]));
                    list.add(u);
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println(" Fail");
            System.out.println("Java heap space exceed. List size=" + list.size());
            exception = true;
        } catch (NumberFormatException e) {
            System.out.println(url + "   " + e.getMessage());
            System.exit(0);
        } finally {
            Runtime.getRuntime().gc();
            dataset_file.close();
            fileReader.close();
        }

        if (!exception) {
            if (typeDataset.equals("dataset3")) {
                System.out.println("List rank size=" + listRank.size() + " OK");
            } else {
                System.out.println("List size=" + list.size() + " OK");
            }
        }

        System.out.print("Sorting the list...");
        if (typeDataset.equals("dataset3")) {
            Collections.sort(listRank, new SortByWeightRank());
        } else {
            Collections.sort(list, new SortByWeight());
        }
        System.out.println(" OK");

        System.out.print("Storing the urls from the sorted list to the file..");

        if (typeDataset.equals("dataset3")) {
            for (int i = 0; i < listRank.size(); i++) {
                fileWriter.write(listRank.get(i).getUrl() + " " + listRank.get(i).getOutputPort() + " " + listRank.get(i).getRank() + " " + listRank.get(i).getOpenPageRank() + "\n");
            }
        } else {
            for (int i = 0; i < list.size(); i++) {
                fileWriter.write(list.get(i).getUrl() + " " + list.get(i).getOutputPort() + "\n");
            }
        }

        listRank.clear();
        list.clear();

        System.out.println(" OK");
        fileWriter.close();

    }
            
            
    /**
     * Extract all canonical URLs from the sorted dataset
     *
     * @ Input: sorted_dataset.txt and the Hash Table h
     * @ Output: canonical_dataset.txt, non_canonical_dataset.txt
     * Tuple<(#urls, #canonical urls, #non canonical urls), Hash Table h>
     */
    public static Map.Entry<String, HashMap<String, int[]>> extractCanonicalURLS(String sortedDatasetPath, HashMap<String, int[]> h, String typeDataset) throws IOException {

        System.out.print("Segregating the FIB...");
        FileWriter fileWriterCanonical = new FileWriter(Parameters.DATASET_URL + "\\canonical_dataset.txt");
        FileWriter fileWriterNonCanonical = new FileWriter(Parameters.DATASET_URL + "\\non_canonical_dataset.txt");
        FileReader dataset_file = new FileReader(sortedDatasetPath);
        BufferedReader fileReader = new BufferedReader(dataset_file);

        String line = "";

        long canonicalUrls = 0;
        long nonCanonicalUrls = 0;
        long totalUrls = 0;

        long sumRankCanonicalUrls = 0;
        long sumRankNonCanonicalUrls = 0;
        double sumOpenPageRankCanonicalUrls = 0;
        double sumOpenPageRankNonCanonicalUrls = 0;

        while ((line = fileReader.readLine()) != null) {

            String parts[] = null;
            String url = "";
            byte output_port = 0;
            long rank = 0;
            double openPageRank = 0.0;

            parts = line.split(" ");
            url = parts[0];
            output_port = Byte.parseByte(parts[1]);

            if (typeDataset.equals("dataset3")) {
                rank = Long.parseLong(parts[2]);
                openPageRank = Double.parseDouble(parts[3]);
            }

            totalUrls++;

            if (!isCanonicalUrl(url, h)) {

                String p[] = url.split("/");

                //The url will be excluded and all its name components will be updated in hash table 'h'
                for (int i = 1; i < p.length; i++) {

                    int v_old[] = h.get(p[i]);

                    int v_new[] = new int[8];
                    System.arraycopy(v_old, 0, v_new, 0, v_old.length);

                    if (v_old[i - 1] > 0) {
                        v_new[i - 1]--;
                        h.replace(p[i], v_old, v_new);
                    }
                }

                nonCanonicalUrls++;

                if (typeDataset.equals("dataset3")) {
                    sumRankNonCanonicalUrls = sumRankNonCanonicalUrls + rank;
                    sumOpenPageRankNonCanonicalUrls = sumOpenPageRankNonCanonicalUrls + openPageRank;
                    fileWriterNonCanonical.write(url + " " + output_port + " " + rank + " " + openPageRank + "\n");
                } else {
                    fileWriterNonCanonical.write(url + " " + output_port + "\n");
                }

            } else {
                //Write the canonical url into the segregated file..
                canonicalUrls++;

                if (typeDataset.equals("dataset3")) {
                    sumRankCanonicalUrls = sumRankCanonicalUrls + rank;
                    sumOpenPageRankCanonicalUrls = sumOpenPageRankCanonicalUrls + openPageRank;
                    fileWriterCanonical.write(url + " " + output_port + " " + rank + " " + openPageRank + "\n");
                } else {
                    fileWriterCanonical.write(url + " " + output_port + "\n");
                }
            }
        }

        System.out.println("OK");

        System.out.println("Total Number of URLs: " + totalUrls);
        System.out.println("Number of Canonical URLs: " + canonicalUrls);
        System.out.println("Number of Non-Canonical URLs: " + nonCanonicalUrls);
        double d = ((double) ((double) canonicalUrls / (double) totalUrls) * 100.0);
        System.out.println("Offloaded URLs: " + String.format("%.3f", d) + "%");

        if (typeDataset.equals("dataset3")) {
            System.out.println("Average Rank (Canonical URLs): " + (double) ((double) sumRankCanonicalUrls / (double) canonicalUrls));
            System.out.println("Average Rank (Non-Canonical URLs): " + (double) ((double) sumRankNonCanonicalUrls / (double) nonCanonicalUrls));
            System.out.println("Average Open Page Rank (Canonical URLs): " + (double) ((double) sumOpenPageRankCanonicalUrls / (double) canonicalUrls));
            System.out.println("Average Open Page Rank (Non-Canonical URLs): " + (double) ((double) sumOpenPageRankNonCanonicalUrls / (double) nonCanonicalUrls));
        }

        fileReader.close();
        dataset_file.close();
        fileWriterCanonical.close();
        fileWriterNonCanonical.close();

        String r = totalUrls + " " + canonicalUrls + " " + nonCanonicalUrls;
        
        Map.Entry<String, HashMap<String, int[]>> result = new AbstractMap.SimpleEntry<>(r, h);
        
        return result;
    }

    
    
    
    
    /**
     * During the process of extracting canonical prefixes, some canonical
     * prefixes will end up being stored in the non-canonical file. To solve
     * this problem and to increase the number of canonical prefixes in the
     * canonical file, this method makes a repechage to move all prefixes that
     * is canonical from the non-canonical file to the canonical file.
     *
     */
    public static Map.Entry<Long, Long> repechage(String canonicalPath, String nonCanonicalPath, DualComponentHashmap dataStructure) throws FileNotFoundException, FileNotFoundException, IOException {

        System.out.print("Performing the repechage...");

        FileWriter fWriterNonCanonical = new FileWriter(Parameters.DATASET_URL + "\\non_canonical_dataset_repechage.txt");
        FileWriter fWriterCanonical = new FileWriter(Parameters.DATASET_URL + "\\canonical_dataset_repechage.txt");

        //Copying the canonical prefixes to canonical_dataset_repechage.txt
        FileReader fReader = new FileReader(canonicalPath);
        BufferedReader bReader = new BufferedReader(fReader);
        long n_canonical = 0;
        long n_non_canonical = 0;
        String line;
        while ((line = bReader.readLine()) != null) {
            n_canonical++;
            fWriterCanonical.write(line + "\n");
        }
        fReader.close();
        bReader.close();

        fReader = new FileReader(nonCanonicalPath);
        bReader = new BufferedReader(fReader);
        while ((line = bReader.readLine()) != null) {
            String p[] = line.split(" ");
            String c[] = p[0].split("/");
            boolean isCanonical = true;
            for (int i = 1; i < c.length; i++) {
                CPEntry cpe = dataStructure.get(c[i]);
                if (cpe != null) {
                    if (i != cpe.getCad().getPosition()) {
                        isCanonical = false;
                        break;
                    }
                }
            }
            if (isCanonical) {
                if (dataStructure.insertPrefix(p[0], Byte.parseByte(p[1]))) {
                    dataStructure.incrementNumberOfPrefixes();
                    n_canonical++;
                    fWriterCanonical.write(line + "\n");
                } else {
                    n_non_canonical++;
                    fWriterNonCanonical.write(line + "\n");
                }
            } else {
                n_non_canonical++;
                fWriterNonCanonical.write(line + "\n");
            }
        }
        fWriterCanonical.close();
        fWriterNonCanonical.close();
        fReader.close();
        bReader.close();
        System.out.println("OK");

        System.out.println("Number of canonical prefixes after the repechage: " + n_canonical);
        System.out.println("Number of non-canonical prefixes after the repechage: " + n_non_canonical);
        long total = n_canonical + n_non_canonical;
        double v = ((double) n_canonical / (double) total) * 100.0;
        System.out.println("Total urls: " + total);
        System.out.println("Offloaded URLs after repechage: " + String.format("%.3f", v) + "%");
        
        Map.Entry<Long, Long> result = new AbstractMap.SimpleEntry<>(n_canonical, n_non_canonical);
        
        return result;
    }

    public static void removeProblematicPrefixes(String canonicalDatasetPath, String nonCanonicalDatasetPath) throws FileNotFoundException, IOException {

        System.out.println("Removing problematic prefixes. First Phase..");
        HashMap<String, Byte> hash_table = new HashMap<>();

        FileWriter fWriterNonCanonical = new FileWriter(Parameters.DATASET_URL + "\\final_non_canonical_dataset.txt");
        FileReader dataset_file = new FileReader(nonCanonicalDatasetPath);
        BufferedReader fileReader = new BufferedReader(dataset_file);
        long n = 0;
        long n_canonical_prefixes = 0;
        long n_non_canonical_prefixes = 0;
        String line;
        while ((line = fileReader.readLine()) != null) {
            fWriterNonCanonical.write(line + "\n");
            n_non_canonical_prefixes++;
            String parts[] = line.split(" ");
            String url = parts[0];
            String p[] = url.split("/");
            if (p.length > 3) {
                String key = "/" + p[1] + "/" + p[2] + "/" + p[3];
                hash_table.putIfAbsent(key, (byte) key.length());
            }
        }
        dataset_file.close();
        fileReader.close();

        dataset_file = new FileReader(canonicalDatasetPath);
        fileReader = new BufferedReader(dataset_file);
        n = 0;
        ArrayList<Double> popularity = new ArrayList();
        FileWriter fWriterCanonical = new FileWriter(Parameters.DATASET_URL + "\\final_canonical_dataset.txt");
        while ((line = fileReader.readLine()) != null) {
            String parts[] = line.split(" ");
            String url = parts[0];
            String p[] = url.split("/");
            if (p.length > 3) {
                String key = "/" + p[1] + "/" + p[2] + "/" + p[3];
                Byte value = hash_table.get(key);
                if (value != null) {
                    fWriterNonCanonical.write(line + "\n");
                    n_non_canonical_prefixes++;
                    n++;
                    //double pop = Double.parseDouble(parts[3]);
                    //popularity.add(pop);
                    //System.out.println("Prefix: "+url);
                } else {
                    fWriterCanonical.write(line + "\n");
                    n_canonical_prefixes++;
                }
            } else {
                fWriterCanonical.write(line + "\n");
                n_canonical_prefixes++;
            }
        }
        dataset_file.close();
        fileReader.close();
        fWriterCanonical.close();
        fWriterNonCanonical.close();

        Collections.sort(popularity);
        for (Double d : popularity) {
            //System.out.println(d);
        }

        System.out.println("OK");
        System.out.println("Prefixes to be removed: " + n);
        System.out.println("Final Number of Canonical Prefixes After Removing the Problematics: " + n_canonical_prefixes);
        System.out.println("Final Number of Non-Canonical Prefixes After Removing the Problematics: " + n_non_canonical_prefixes);
    }

    /**
     * Create and returns a CPDataStructure object; The CPDataStructure contains
     * the following fields: - HashMap<String, CPEntry> HT_C_CAD_HS (Main Data
     * Structure) - HashMap<Integer,Byte> HT_CONFLICTING (Secondary Data
     * Structure)
     *
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
        CRC32 crc = new CRC32();
        String subprefix;

        CPEntry element;

        while ((line = canonicalFileReader.readLine()) != null) {

            String parts[] = line.split(" ");
            String url = parts[0];
            byte output_port = Byte.parseByte(parts[1]);

            if (HT_CP.insertPrefix(url, output_port)) {
                HT_CP.incrementNumberOfPrefixes();
            }
        }

        canonicalFileReader.close();
        final_canonical_dataset_file.close();

        System.out.println("OK");

        return HT_CP;
    }

    /*
        This method identify all confliting prefixes in the dataset file
        CONSIDERING ONLY URLs OF THE SAME SIZE.
        For example: /a/b/c and /x/z/c are confliting because the component
                     c is in the same position in both urls and c is final.
                     On the other hand /g/h/i and /o/p/i/y are not confliting because
                     the size of both is different. In other words, i is not final
                     in both prefixes.
        Definition:
            Let C be a canonical dataset and n a name prefix where n ∈ C, 
            with n = <c1, c2, ..., ck> and ci = name component at position i. 
            We call n and n' confliting name prefixes if and only if n' ∈ C
            and n' = <c'1, c'2, ..., c'k> such that ck=c'k and
            <c1,...,ck-1> ≠ <c'1,...,c'k-1>
        E.g.: Confliting prefixes: /b/a and /c/a
              The prefixes /b/a/x and /c/a/z/t are not confliting because despite the 
              name component 'a' in the second position are present in both prefixes with
              different subprefixes (/b and /c), the name component 'a' are not final. In other
              words, the name component 'a' does not end a prefix.
     */
    public static int identifyCanonicalConflitingFinalPrefixes(String canonicalDatasetPath) throws FileNotFoundException, IOException {

        HashMap<String, String> h = new HashMap<>();    //store key=word; value=subprefix
        HashMap<String, Integer> aux = new HashMap<>();  //store key=url; value=url length.

        System.out.print("Identifying all canonical confliting final prefixes..");

        //We read the file 7 times, each one looking into one position of the url.. 
        for (int i = 2; i <= 8; i++) {

            //System.out.print("Position " + i+": ");
            FileReader canonical_dataset_file = new FileReader(canonicalDatasetPath);
            BufferedReader canonicalFileReader = new BufferedReader(canonical_dataset_file);

            String line = "";

            //Looking into position i
            while ((line = canonicalFileReader.readLine()) != null) {

                String parts[] = line.split(" ");
                String url = parts[0];

                String p[] = url.split("/");

                //   index            0    1    2    3 
                //   /a/b/c   -> p = ["", "a", "b", "c"]
                int n_comp = p.length - 1;

                //Checking if p[i] is final
                if (n_comp == i) {
                    String value = h.get(p[i]);
                    String sub = subprefix(url, p[i]);
                    if (value != null) {
                        if (!value.equals(sub)) {
                            aux.putIfAbsent(url, url.length());
                            aux.putIfAbsent(value + "/" + p[i], (value + "/" + p[i]).length());
                        }
                    } else {
                        h.putIfAbsent(p[i], sub);
                    }
                }
            }
            canonical_dataset_file.close();
            canonicalFileReader.close();
            //System.out.println(n + " conflits has been detected.");            
        }
        System.out.println("OK");
        return aux.size();
    }

    /*
        This method identify all confliting prefixes in the dataset file
        CONSIDERING URLs WITH DIFFERENT SIZES.
        For example: /a/b/c and /x/z/c are confliting because the component
                     c is in the same position in both urls and the subprefixes of 
                     both urls are different.
                     Likewise, /g/h/i and /o/p/i/y are confliting prefixes because
                     i is in the same position and its subprefixes are different (/g/h != /o/p)
        Definition:
            Let C be a canonical dataset and n a name prefix where n ∈ C, 
            with n = <c1, c2, ..., ck, ..., cp> and ci = name component at position i. 
            We call n and n' confliting name prefixes if and only if n' ∈ C
            and n' = <c'1, c'2, ..., c'k, ..., c'q>, with c'i = name component at position i,
            such that ck=c'k and <c1,...,ck-1> ≠ <c'1,...,c'k-1>
        E.g.: Confliting prefixes: /b/a and /c/a;
                                   /b/a/x and /c/a/z/t
                                   /y/b and /i/b/z
                                   /p/q/w/u and /e/r/w
     */
    public static int identifyCanonicalConflitingPrefixes(String canonicalDatasetPath) throws FileNotFoundException, IOException {

        HashMap<String, String> h = new HashMap<>();    //store key=word; value=subprefix
        HashMap<String, Integer> aux = new HashMap<>();  //store key=url; value=url length.

        System.out.print("Identifying all canonical confliting prefixes..");

        //We read the file 7 times, each one looking into one position of the url.. 
        for (int i = 2; i <= 8; i++) {

            //System.out.print("Position " + i+": ");
            FileReader canonical_dataset_file = new FileReader(canonicalDatasetPath);
            BufferedReader canonicalFileReader = new BufferedReader(canonical_dataset_file);

            String line = "";

            //Looking into position i
            while ((line = canonicalFileReader.readLine()) != null) {

                String parts[] = line.split(" ");
                String url = parts[0];

                String p[] = url.split("/");

                //   index            0    1    2    3 
                //   /a/b/c   -> p = ["", "a", "b", "c"]
                if (i <= (p.length - 1)) {
                    String value = h.get(p[i]);
                    String sub = subprefix(url, p[i]);
                    if (value != null) {
                        if (!value.equals(sub)) {
                            aux.putIfAbsent(url, url.length());
                            aux.putIfAbsent(value + "/" + p[i], (value + "/" + p[i]).length());
                        }
                    } else {
                        h.putIfAbsent(p[i], sub);
                    }
                }
            }
            canonical_dataset_file.close();
            canonicalFileReader.close();
            //System.out.println(n + " conflits has been detected.");            
        }
        System.out.println("OK");
        return aux.size();
    }

    /*
        This methods identify and transfer to non-canonical urls dataset all canonical urls that are subprefixes 
        of at least one non-canonical url. It means that the non canonical file contains some canonical urls too.
        E.g.: canonical url /a/b/c and non-canonical url /a/b/c/d. The canonical url /a/b/c is transfered from the
        canonical set to the non-canonical one.
     */
    public static long identifyCanonicalCommonPrefixes(String canonicalDatasetPath, String nonCanonicalDatasetPath, String typeDataset) throws FileNotFoundException, IOException {

        FileReader non_canonical_dataset_file = new FileReader(nonCanonicalDatasetPath);
        BufferedReader nonCanonicalFileReader = new BufferedReader(non_canonical_dataset_file);

        FileWriter final_non_canonical_dataset_file = new FileWriter(Parameters.DATASET_URL + "\\final_non_canonical_dataset.txt");
        FileWriter final_canonical_dataset_file = new FileWriter(Parameters.DATASET_URL + "\\final_canonical_dataset.txt");

        // H[0] = {urls with 1 name component} 
        // H[1] = {urls with 2 name component} 
        // ... 
        // H[7] = {urls with 8 name component}        
        HashMap<String, Byte>[] H = (HashMap<String, Byte>[]) (Map<String, Byte>[]) new HashMap<?, ?>[8];
        for (int i = 0; i < H.length; i++) {
            H[i] = new HashMap<String, Byte>();
        }

        long n_canonical_urls = 0;
        String line = "";

        System.out.print("Storing the non-canonical urls into the hash tables H[i], with 1<=i<=8...");
        while ((line = nonCanonicalFileReader.readLine()) != null) {

            String parts[] = null;
            String url = "";
            String output_port = "";
            String rank = "";
            String openPageRank = "";

            parts = line.split(" ");
            url = parts[0];
            output_port = parts[1];

            //Copying urls from the non_canonical dataset to final_non_canonical_dataset
            if (typeDataset.equals("dataset3")) {
                rank = parts[2];
                openPageRank = parts[3];
                final_non_canonical_dataset_file.write(url + " " + output_port + " " + rank + " " + openPageRank + " " + "\n");
            } else {
                final_non_canonical_dataset_file.write(url + " " + output_port + "\n");
            }

            // url = /com/tamtokki/leblog/sporting/www
            //              0        1          2          3         4               
            //   p = ["", "com", "tamtokki", "leblog", "sporting", "www"]
            //        0     1        2          3          4         5
            n_canonical_urls++;

            String p[] = url.split("/");
            int url_length = p.length - 1;   // 6 - 1 = 5
            for (int i = url_length; i > 0; i--) {
                H[i - 1].putIfAbsent(url, (byte) url.length());
                url = subprefix(url, p[i]);
            }
        }

        System.out.println(" OK");

        nonCanonicalFileReader.close();
        non_canonical_dataset_file.close();

        FileReader canonical_dataset_file = new FileReader(canonicalDatasetPath);
        BufferedReader canonicalFileReader = new BufferedReader(canonical_dataset_file);

        System.out.println("Identifying all canonical url that are subprefixes in non-canonical urls..");
        long n_canonical_common_prefixes = 0;
        line = "";

        while ((line = canonicalFileReader.readLine()) != null) {

            String parts[] = line.split(" ");
            String url = parts[0];
            String output_port = parts[1];
            String rank = "";
            String openPageRank = "";

            if (typeDataset.equals("dataset3")) {
                rank = parts[2];
                openPageRank = parts[3];
            }

            String p[] = url.split("/");
            int n_comps = p.length - 1;

            if (H[n_comps - 1].containsKey(url)) {
                n_canonical_common_prefixes++;
                final_non_canonical_dataset_file.write(url + " " + output_port + " " + rank + " " + openPageRank + "\n");
            } else {
                final_canonical_dataset_file.write(url + " " + output_port + " " + rank + " " + openPageRank + "\n");
            }
        }

        canonicalFileReader.close();
        canonical_dataset_file.close();
        final_canonical_dataset_file.close();
        final_non_canonical_dataset_file.close();

        System.out.println("Number of canonical common prefixes: " + n_canonical_common_prefixes);
        return n_canonical_common_prefixes;
    }

    public static void calculateFinalAverageRanks(String finalCanonicalDatasetPath, String finalNonCanonicalDatasetPath, String typeDataset) throws FileNotFoundException, IOException {

        FileReader final_non_canonical_dataset_file = new FileReader(finalNonCanonicalDatasetPath);
        BufferedReader nonCanonicalFileReader = new BufferedReader(final_non_canonical_dataset_file);

        String line = "";
        long rank = 0;
        long sumRank = 0;
        double openPageRank = 0.0;
        double sumOpenPageRank = 0.0;
        long totalUrls = 0;

        while ((line = nonCanonicalFileReader.readLine()) != null) {

            String parts[] = line.split(" ");

            if (typeDataset.equals("dataset3")) {
                rank = Long.parseLong(parts[2]);
                openPageRank = Double.parseDouble(parts[3]);
                sumRank = sumRank + rank;
                sumOpenPageRank = sumOpenPageRank + openPageRank;
            }

            totalUrls++;
        }

        nonCanonicalFileReader.close();
        final_non_canonical_dataset_file.close();

        System.out.println("Final Number of Non-Canonical URLs: " + totalUrls);

        if (typeDataset.equals("dataset3")) {
            System.out.println("Final Average Rank (Non-Canonical URLs): " + (double) ((double) sumRank / (double) totalUrls));
            System.out.println("Final Average Open Page Rank (Non-Canonical URLs): " + (double) ((double) sumOpenPageRank / (double) totalUrls));
        }

        FileReader final_canonical_dataset_file = new FileReader(finalCanonicalDatasetPath);
        BufferedReader canonicalFileReader = new BufferedReader(final_canonical_dataset_file);

        line = "";
        sumRank = 0;
        rank = 0;
        openPageRank = 0.0;
        sumOpenPageRank = 0.0;

        totalUrls = 0;

        while ((line = canonicalFileReader.readLine()) != null) {

            String parts[] = line.split(" ");

            if (typeDataset.equals("dataset3")) {
                rank = Long.parseLong(parts[2]);
                openPageRank = Double.parseDouble(parts[3]);
                sumRank = sumRank + rank;
                sumOpenPageRank = sumOpenPageRank + openPageRank;
            }

            totalUrls++;
        }
        canonicalFileReader.close();
        final_canonical_dataset_file.close();

        System.out.println("Final Number of Canonical URLs: " + totalUrls);

        if (typeDataset.equals("dataset3")) {
            System.out.println("Final Average Rank (Canonical URLs): " + (double) ((double) sumRank / (double) totalUrls));
            System.out.println("Final Average Open Page Rank (Canonical URLs): " + (double) ((double) sumOpenPageRank / (double) totalUrls));
        }
    }

    /**
     * Return a subprefix from the prefix starting from the root up to
     * component, exclusive.
     *
     * @ Input: prefix, component (Example: Prefix: /a/b/c, Component: c)
     * @ Output: subprefix (Example: /a/b)
     */
    public static String subprefix(String prefix, String component) {
        String components[] = prefix.split("/");
        String subprefix = "";
        for (int i = 1; i < components.length; i++) {
            if (!components[i].equals(component)) {
                subprefix = subprefix + "/" + components[i];
            } else {
                break;
            }
        }
        return subprefix;
    }

    public static long countLinesFile(String filePath) throws FileNotFoundException, IOException {
        FileReader file = new FileReader(filePath);
        BufferedReader fReader = new BufferedReader(file);
        String line;
        long n = 0;
        while ((line = fReader.readLine()) != null) {
            n++;
        }
        file.close();
        fReader.close();
        return n;
    }

    public static void printTimeMinutesSeconds(long miliseconds) {

        long minutes = miliseconds / 60000;
        long seconds = (miliseconds % 60000) / 1000;

        String m = (minutes < 10) ? "0" + minutes : "" + minutes;
        String s = (seconds < 10) ? "0" + seconds : "" + seconds;

        System.out.print(m + ":" + s);
    }

    /**
     * This method calculates the the number of collisions for each table that
     * store the 32-bit hash (T5, T6, ..., T31), where Ti store the 32-bit hash
     * of a word with length i
     */
    public static void calculateCollisions(HashMap<String, int[]> h) throws NoSuchAlgorithmException {

        System.out.println("Calculating the number of collisions..");

        HashMap<Long, String>[] tables;
        tables = (HashMap<Long, String>[]) (Map<Long, String>[]) new HashMap<?, ?>[27];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = new HashMap<>();
        }

        /*
            tables[0] = hash(word with length 5)
            tables[1] = hash(word with length 6)
            ...
            tables[26] = hash(word with length 31)
        
         */
        //stores the number of collisions for each table
        int collisions[] = new int[27];
        CRC32 crc = new CRC32();
        Iterator<String> iterator = h.keySet().iterator();
        while (iterator.hasNext()) {
            String word = iterator.next();
            if (word.length() >= 5 && word.length() <= 31) {
                crc.update(word.getBytes());
                int index = word.length() - 5;
                if (tables[index].putIfAbsent(crc.getValue(), word) != null) {
                    collisions[index]++;
                    String value = tables[index].get(crc.getValue());
                    System.out.println("Collision at table with size " + word.length() + ": [" + value + ", " + word + "]");
                }
                crc.reset();
            }
        }
        System.out.println("Printing the number of collisions: ");
        for (int i = 0; i < collisions.length; i++) {
            System.out.println("Word with size " + (i + 5) + ": " + collisions[i]);
        }
    }

    public static void main(String[] args) throws IOException, FileNotFoundException, NoSuchAlgorithmException {
                
    }

    /**
     * Check if a given url is canonical. To be canonical, each name component
     * ni in the url, with 1 <= i <= k, must appear in only one position in all
     * URLs in the dataset.
     */
    public static boolean isCanonicalUrl(String url, HashMap<String, int[]> h) {
        boolean isCanonical = true;
        String p[] = url.split("/");
        for (int i = 1; i < p.length; i++) {
            int v[] = h.get(p[i]);
            if (!isCanonicalArray(v)) {
                isCanonical = false;
                break;
            }
        }
        return isCanonical;
    }

    /**
     * Check if the int array is canonical. To be canonical, an array must have
     * one, and only one, element greater than 0 and all the remaining must be
     * 0.
     *
     * @ Input: int array
     * @ Output: boolean (yes or no)
     */
    public static boolean isCanonicalArray(int v[]) {
        String value = convertToString(v);
        return (value.equals("10000000")
                || value.equals("01000000")
                || value.equals("00100000")
                || value.equals("00010000")
                || value.equals("00001000")
                || value.equals("00000100")
                || value.equals("00000010")
                || value.equals("00000001"));
    }

    /**
     * Calculate the word weigth as being the v[position]
     *
     * @ Input: integer array v, position
     * @ Output: v[position]
     */
    public static int calculateWordWeight(int v[], int position) {
        return v[position];
    }

    /**
     * Calculate the url weigth by summing up all the words weight in the url.
     *
     * @ Input: url and the hash table
     * @ Output: weight as a sum of all word weight
     */
    public static int calculateUrlWeight(String url, HashMap<String, int[]> h) {
                
        String p[] = url.split("/");
        int w = 0;
        int v[];
        for (int i = 1; i < p.length; i++) {
            v = h.get(p[i]);
            w = w + calculateWordWeight(v, i - 1);
        }
        return w;
    }

    /**
     * Convert a given array into a bit string. Each element v[i] in the array
     * is mapped to 1, if v[i]>0 or to 0 if v[i]=0
     *
     * @ Input: int array
     * @ Output: bit string
     */
    public static String convertToString(int v[]) {
        String bitstring = "";
        for (int i = 0; i < v.length; i++) {
            if (v[i] == 0) {
                bitstring = bitstring + "0";
            } else {
                bitstring = bitstring + "1";
            }
        }
        return bitstring;
    }

    public static short getFrequency(int cpm[]) {
        short frequency = 0;
        if (isCanonicalArray(cpm)) {
            for (int i = 0; i < cpm.length; i++) {
                if (cpm[i] != 0) {
                    frequency = (short) cpm[i];
                    break;
                }
            }
        }
        return frequency;
    }

    /**
     * Reverse the url.
     *
     * @ Input: Original URL (i.e /www/google/com)
     * @ Output: Reversed URL (i.e /com/google/www)
     */
    public static String reverseUrl(String url) {
        String s[] = url.split("/");
        String r = "";
        for (int i = s.length - 1; i > 0; i--) {
            r = r + "/" + s[i];
        }
        return r;
    }

    /**
     * This method takes an url and returns true if all name components has size
     * >= 1 and <= max_comp_size @param u
     *
     * rl @param max_comp_size @retur
     *
     * n
     *
     */
    public static boolean isComponentSizesValid(String url, int max_comp_size) {
        boolean isValid = true;
        String parts[] = url.split("/");
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].length() < 1 || parts[i].length() > max_comp_size) {
                isValid = false;
                break;
            }
        }
        return isValid;
    }

    public static boolean hasDuplicatedWords(String p[]) {
        HashMap<String, Integer> h = new HashMap<>();
        boolean duplicated = false;
        for (int i = 1; i < p.length; i++) {
            if (h.containsKey(p[i])) {
                duplicated = true;
                break;
            } else {
                h.put(p[i], p[i].length());
            }
        }
        h.clear();
        return duplicated;
    }
}
