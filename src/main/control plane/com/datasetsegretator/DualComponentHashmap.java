/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datasetsegretator;

import com.datasetsegretator.CAD;
import static com.datasetsegretator.TemporaryTestClass.printTables;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.CRC32;
import com.p4.environment.adaptation.Port;
import com.p4.environment.adaptation.StringConverter;

/**
 * The CPDataStructure object stores in control plane the prefixes that are
 * stored in data plane. Any change in this data structure triggers changes in
 * the P4 tables. Despite the name (CPDataStructure - Control Plane Data
 * Structure), it has nothing to to with the non-canonical prefixes stored in
 * the file final_non_canonical_dataset.txt.
 *
 * A better name for this class is: DualComponentHashmap: DCH
 *
 */
public class DualComponentHashmap {

    //Main Hash Table: <word>, <cp, <cad>, cc, ce, cf, hs>
    private HashMap<String, CPEntry> HT_C_CAD_HS;
    //Secondary Hash Table: <hash prefix>, <output_port>
    private HashMap<Integer, Byte> HT_CONFLICTING;

    private long numberOfPrefixes;

    public DualComponentHashmap() {
        HT_C_CAD_HS = new HashMap<>();
        HT_CONFLICTING = new HashMap<>();
        numberOfPrefixes = 0;
    }

    public HashMap<Integer, Byte> getHTConflicting() {
        return HT_CONFLICTING;
    }

    public HashMap<String, CPEntry> getHT_C_CAD_HS() {
        return HT_C_CAD_HS;
    }

    public void setHTConflicting(HashMap<Integer, Byte> HT_CONFLICTING) {
        this.HT_CONFLICTING = HT_CONFLICTING;
    }

    public void incrementNumberOfPrefixes() {
        numberOfPrefixes++;
    }

    public long getNumberOfPrefixes() {
        return numberOfPrefixes;
    }

    public CPEntry get(String key) {
        return HT_C_CAD_HS.get(key);
    }

    public CPEntry put(String key, CPEntry value) {
        return HT_C_CAD_HS.put(key, value);
    }

    public CPEntry putIfAbsense(String key, CPEntry value) {
        return HT_C_CAD_HS.putIfAbsent(key, value);
    }

    public CPEntry replace(String key, CPEntry value) {
        return HT_C_CAD_HS.replace(key, value);
    }

    public Set<String> keySet() {
        return HT_C_CAD_HS.keySet();
    }

    public int size() {
        return HT_C_CAD_HS.size();
    }

    /**
     * Clear all entries in both HT_C_CAD_HS and HT_CONFLICTING tables.
     */
    public void clear() {
        HT_C_CAD_HS.clear();
        HT_CONFLICTING.clear();
    }

    public void iterativeTesting() {
        System.out.println("Testing the insertion, removal, search, and update operations..");
        Scanner scanner = new Scanner(System.in);
        String l;
        do {
            System.out.println("=============================================================================================================================");
            System.out.println("I - Insert \nU - Update \nS - Search \nR - Remove \nP - Print \nL - Longest Prefix Matching\nE - End");
            System.out.println("Option: ");
            l = scanner.nextLine();
            if (l.equals("E")) {
                break;
            } else {
                if (l.equals("S")) {
                    System.out.println("Enter the prefix to search: ");
                    String prefix = scanner.nextLine();
                    if (this.hasPrefix(prefix)) {
                        System.out.println("Prefix " + prefix + " ====>  FOUND");
                    } else {
                        System.out.println("Prefix " + prefix + " ====>  NOT FOUND");
                    }
                } else {
                    if (l.equals("R")) {
                        System.out.println("Enter the prefix to remove: ");
                        String prefix = scanner.nextLine();
                        if (this.removePrefix(prefix)) {
                            System.out.println("Prefix " + prefix + " ====> REMOVED");
                        } else {
                            System.out.println("Prefix " + prefix + " ====> Does not exist OR we can't remove because at least one name component in " + prefix + " is conflicting..");
                        }
                    } else {
                        if (l.equals("P")) {
                            printTables(this);
                        } else {
                            if (l.equals("U")) {
                                System.out.println("Enter the prefix to update: ");
                                String prefix = scanner.nextLine();
                                System.out.println("Enter the new output port: ");
                                Byte output_port = scanner.nextByte();
                                if (this.updatePrefix(prefix, output_port)) {
                                    System.out.println("Prefix " + prefix + " was updated..");
                                } else {
                                    System.out.println("Prefix can't be updated..");
                                }
                            } else {
                                if (l.equals("I")) {
                                    System.out.println("Enter the prefix to insert: ");
                                    String prefix = scanner.nextLine();
                                    System.out.println("Enter the output port: ");
                                    Byte output_port = scanner.nextByte();
                                    if (this.insertPrefix(prefix, output_port)) {
                                        System.out.println("Prefix " + prefix + " was inserted..");
                                    } else {
                                        System.out.println("Prefix can't be inserted..");
                                    }
                                } else {
                                    if (l.equals("L")) {
                                        System.out.println("Enter the url to perform the lpm: ");
                                        String url = scanner.nextLine();
                                        System.out.println("Port: " + this.performLPM(url));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } while (true);
    }

    /**
     * For a dataset containing only non-conflicting prefixes, this method
     * checks if a given 'url' is cross conflicting with the prefixes stored in
     * this data structure. Definition: A given url /c1/c2/.../cn is cross
     * conflicting with the prefixes in the data structure when all of its name
     * components exists in the data structure in the correct position and at
     * least one of its name component ci, with i>1, have a subprefix hash
     * h(/c1/c2/.../c(i-1)) that differs from the hs of ci in the data
     * structure.
     *
     * Example: Let's suppose we have the following non-conflicting prefixes in
     * the data structure:
     *
     * /a/b/c /a/b /a /x /x/e /x/e/f
     *
     * As we can see, none of these prefixes is conflicting.
     *
     * However, what if a prefix /x/b arrives at the switch?
     *
     * We know, of course, that although the name components 'x' and 'b' do
     * exist in the data structure, such prefix does not exist in the list and
     * should be sent it to the DEFAULT PORT. However, if we do not consider the
     * hs value, we are not able to say that /x/b is not present in the prefixes
     * list. Therefore, the prefix /x/b will end up be sending to the same
     * output port as /a/b.
     */
    public boolean isCrossConflicting(String url) {

        /* This code is incomplete for now*/
        boolean isCrossConflicting = false;

        String p[] = url.split("/");
        //   url = /a/b/c
        //     p = "", "a", "b", "c"

        String subprefix = "";
        CRC32 crc = new CRC32();
        for (int i = 1; i < p.length; i++) {
            subprefix = subprefix + "/" + p[i];
            CPEntry cpe = HT_C_CAD_HS.get(p[i]);
            if (cpe != null) {
                if (i != cpe.getCad().getPosition()) {
                    return false;
                }
                if (i > 1) {
                    crc.update(subprefix.getBytes());
                    int value = (int) crc.getValue();
                }
            } else {
                break;
            }
        }
        return isCrossConflicting;
    }

    public boolean hasDuplicateComponents(String prefix) {
        boolean has = false;
        HashMap<String, Byte> h = new HashMap<>();
        String p[] = prefix.split("/");
        for (int i = 1; i < p.length; i++) {
            if (h.putIfAbsent(p[i], (byte) 0) != null) {
                has = true;
                break;
            }
        }
        h.clear();
        return has;
    }

    /**
     * This method indicate whether or not a given prefix 'prefix' can be
     * inserted. The insertion criteria is based on the existence of such prefix
     * in the data structure, the position of each name component and if there
     * are duplicated name componentes in the prefix. If the prefix already
     * exist, if there are duplicated componentes or the position of any of its
     * name component differs from the position of the name component in the
     * data structure, returns false. Otherwise, returns true.
     */
    public boolean canPrefixBeInserted(String prefix) {
        boolean can = true;
        if (!hasDuplicateComponents(prefix)) {
            if (!hasPrefix(prefix)) {
                String p[] = prefix.split("/");
                for (int i = 1; i < p.length; i++) {
                    CPEntry element = HT_C_CAD_HS.get(p[i]);
                    if (element != null) {
                        if (i != element.getCad().getPosition()) {
                            //The position of the prefix to be inserted differs from 
                            //its position in the data strucute.. We can not insert
                            //such prefix because we have to mantain the canonical property..
                            can = false;
                            break;
                        }
                    } else {
                        //If the element p[i] does not exist, we can insert it into
                        //the data struture..
                    }
                }
            } else {
                //The prefix already exist. It doesn't make any sense adding it, right? :)
                can = false;
            }
        } else {
            //the prefix contains duplicate components
            can = false;
        }
        return can;
    }

    /**
     * This method indicate whether or not an existing prefix 'prefix' can be
     * removed. The criteria to decide if a given prefix could be removed is
     * based on the conflicting bit in all name components. The prefix 'prefix'
     * can be remove if: 1) All name components has conflicting bit equals to 0,
     * OR 2) All name components with conflicting bit equals to 1 must follow
     * the rule: count_position(name
     * component)=count_conflicting(name_component) AND h(subprefix from
     * position 1 up to current position)!=hs(p[i])
     */
    public boolean canExistingPrefixBeRemoved(String prefix) {
        boolean can = true;
        String subprefix = "";
        CRC32 crc = new CRC32();
        String p[] = prefix.split("/");
        for (int i = 1; i < p.length; i++) {

            subprefix = subprefix + "/" + p[i];

            //if i==1 and the 'prefix' has only one name component
            // (i == (p.length-1)), we do nothing because we can 
            // remove such prefix and we just let the function to return
            // the default value can = true
            if (i > 1) {
                CPEntry cpe = HT_C_CAD_HS.get(p[i]);
                if (cpe.getCad().isConfliting()) {
                    if (cpe.getCountPosition() != cpe.getCountConflicting()) {
                        can = false;
                        break;
                    } else {
                        crc.update(subprefix.getBytes());
                        int v = (int) crc.getValue();
                        if (v == cpe.getHashSubprefix()) {
                            //I can not remove the prefix if it contain a name 
                            //component 'nc' which was the first one to
                            //be added in the data structure.
                            can = false;
                            break;
                        }
                    }
                }
            }
            crc.reset();
        }
        return can;
    }

    /**
     * IMPORTANT NOTE: For the dataset 3 (10M domains), when we insert all final
     * prefixes (4345749) we got three misforwarded packets probably because of
     * hash collisions: /com/alltheragefaces/clean Hash: 1335393430 => Expected
     * output port: 59 Obtained output port: 27 /com/capitalxtra/assets5 Hash:
     * 589727024 => Expected output port: 55 Obtained output port: 73
     * /com/list-manage/us4/apan Hash: -1088942593 => Expected output port: 23
     * Obtained output port: 43 This probably explain why we get 4345746
     * prefixes, instead of 4345749 (-3 prefixes), when we use the following sql
     * commands to calculate the total number of prefixes in CPDataStructure
     * (considering both HT_C_CAD_HS and HT_CONFLICTING tables): select count(*)
     * from ht_conflicting where (port != -1) select count(*) from word_map
     * where (conflicting=false and end_prefix=true and output_ports != -1)
     * =========================================================================
     * Before calling this method, its important to make sure that the prefix to
     * be inserted is not a subprefix in non-canonical dataset. If it is, we can
     * not inserted.
     * =========================================================================
     *
     * This method inserts a given prefix along with its associated output port
     * into the control plane data structure, that represents the prefixes
     * stored in data plane (P4 tables). Before inserting it, we first check
     * whether or not the prefix meets the requirements to be inserted. The
     * requirements are: 1) The prefix must not exist already in the
     * CPDataStructure; 2) Each name component in the prefix must be at the
     * rigth position
     *
     * @ Input: Prefix to be inserted and the output port(s)
     * @ Output: Boolean value indicating if the prefix was inserted or not
     *
     */
    public boolean insertPrefix(String prefix, Byte new_output_port) {
        boolean insert = canPrefixBeInserted(prefix);
        if (insert) {
            CRC32 crc = new CRC32();
            String subprefix = "";
            CPEntry element;
            String p[] = prefix.split("/");
            for (int i = 1; i < p.length; i++) {
                crc.reset();
                //subprefix = subprefix + p[i];
                subprefix = subprefix + "/" + p[i];
                element = HT_C_CAD_HS.get(p[i]);
                if (element == null) {
                    //The element p[i] does not exist in the HT_CMP_CAD_HS table. 
                    //We have to create and insert it.

                    //1 - We first set the count_position to 1 
                    short count_position = 1;

                    //We set the next three variables to 0 because element is null, ie., it does not exist 
                    //in the HT_CMP_CAD_HS data structure..
                    short count_continue = 0;
                    short count_end_prefix = 0;
                    short count_conflicting = 0;

                    //2 - Then we create the cad                    
                    CAD cad = new CAD();
                    cad.setPosition((byte) i);
                    //check if it is the end of a prefix..
                    if (i == (p.length - 1)) {
                        cad.setEndPrefix(true);
                        cad.setContinue(false);
                        cad.setOutputPorts(new_output_port);   //take the port in the url line..
                        count_end_prefix = 1;
                    } else {
                        cad.setEndPrefix(false);
                        cad.setContinue(true);
                        count_continue = 1;
                    }

                    //3 - And the hs
                    int hs = Port.DEFAULT;
                    if (i > 1) {
                        String key = subprefix;
                        crc.update(key.getBytes());
                        hs = (int) crc.getValue();
                        String bitstring = StringConverter.convertIntegerToBitstring(hs);
                        cad.setHs(bitstring.substring(22, 32));
                    }

                    //4 - Now we create the element with the information count_position, cad, count_continue, count_end_prefix, count_conflicting and hs.
                    element = new CPEntry(count_position, cad, count_continue, count_end_prefix, count_conflicting, hs);

                    //5 - We insert the element into the HT_C_CAD_HS data structure
                    HT_C_CAD_HS.put(p[i], element);
                } else {

                    //The element p[i] is already in the HT_CPM_CAD_HS data structure
                    //Incrementing the count_position by 1..
                    element.incrementCountPosition();

                    //take the existing cad
                    CAD cad = element.getCad();

                    //update the cad..
                    //check if it is the end of a prefix..
                    if (i == (p.length - 1)) {

                        //At this point we can securely set the end prefix bit to 1 
                        cad.setEndPrefix(true);

                        //We can let the continue bit unchanged because it can be either 0 or 1
                        //We don't know yet whether or not the p[i] is conflicting.
                        //Therefore, we can not change the output port for now. 
                        //Increment the count_end_prefix parameter by 1.
                        element.incrementCountEndPrefix();
                    } else {
                        //I let the end_prefix bit unchanged because it can be either 0 or 1
                        cad.setContinue(true);
                        //increment the count_continue parameter by 1.
                        element.incrementCountContinue();
                    }

                    //As the element p[i] exist in HT_CPM_CAD_HS and we are dealing with canonical
                    //prefixes only, it is not necessary to change the position of p[i] because
                    //it's position is already correct
                    //Also, it is not necessary to change the hash subprefix value of p[i] (hs)
                    //because for each word p[i] in the HT_CPM_CAD_HS table its hs value represents 
                    //only the first occurency..
                    //For example, if we have two prefixes:
                    // 1) /a/b/c
                    // 2) /x/b
                    // The word 'b' in the HT_CPM_CAD_HS table is associated with the hs = h(ab) and not h(xb)
                    if (cad.isConfliting()) {
                        //It means that the first and the second occurence of p[i] 
                        //(/<subprefix1>/p[i] and /<subprefix2>/p[i]) are already in the HT_CONFLICTING table.
                        //This is because the first time we detected a conflict, we inserted into the HT_CONFLICTING 
                        //table the two conflicting prefixes

                        //Let's check it just to be on the safe side.
                        //int first_pi_subprefix = HT_CMP_CAD_HS.get(p[i]).getHashSubprefix();
                        int first_pi_subprefix = HT_C_CAD_HS.get(p[i]).getHashSubprefix();
                        if (HT_CONFLICTING.get(first_pi_subprefix) == null) {
                            System.out.println("ERROR TO INSERT: HT_CONFLICTING table is supposed to contain the entry: /<subprefix>/" + p[i]);
                            System.exit(0);
                        }

                        //This is the 3th or more occurence of p[i]. All I need to do is add it into the 
                        //HT_CONFLICTING table..
                        String key = subprefix;
                        crc.update(key.getBytes());
                        int hs = (int) crc.getValue();

                        //update the output port for p[i] if it is necessary...
                        if (i == (p.length - 1)) {
                            //trying to insert the hs into the HT_CONFLICTING table taking the output port value 
                            if (HT_CONFLICTING.putIfAbsent(hs, new_output_port) == null) {

                                //it means that p[i] is conflicting because we were able to add it into 
                                //the HT_CONFLICTING table..
                                //we have to increment the count_conflicting parameter in the element
                                element.incrementCountConflicting();
                            } else {

                                //Since p[i] is the last name component and hs 
                                //already exist in HT_CONFLICTING table, we must
                                //update the hs to reflect the new_output_port
                                HT_CONFLICTING.replace(hs, new_output_port);
                            }
                        } else {
                            //trying to insert the hs into the HT_CONFLICTING table using Port.DEFAULT as output port since p[i] does
                            //not end a prefix..
                            if (HT_CONFLICTING.putIfAbsent(hs, Port.DEFAULT) == null) {
                                //it means that p[i] is conflicting because we were able to add it into the
                                //HT_CONFLICTING table

                                //we have to increment the count_conflicting parameter in the element
                                element.incrementCountConflicting();
                            } else {
                                //in this case I do nothing because the value hs is already in the HT_CONFLICTING table
                                //In other words, the current value h(/<subprefix>/p[i]) is in the HT_CONFLICTING table
                            }
                        }
                    } else {

                        //p[i] is NON CONFLICTING
                        //I have to check if p[i] is in the 2 position or more because if p[i] is 
                        //in the first position it does not make any sense to consider conflicting..
                        if (i > 1) {

                            crc.update(subprefix.getBytes());
                            int key = (int) crc.getValue();

                            //Let's suppose we have the prefixes /a/b and /a/b/c
                            //To avoid setting b as conflicting, we
                            //only consider b as conflicting if h(current subprefix) != h(element.getHashSubprefix()).
                            //if h(current subprefix) == h(element.getHashSubprefix()) we do not have conflicting at all
                            if (key != element.getHashSubprefix()) {

                                // A conflict for p[i] is definetly detected for the first time between 
                                // /<some previous subprefix>/p[i] and /<current subprefix>/p[i] and both are not in the
                                // HT_CONFLICTING table because the bit cad.isConflicting is 0 (false)
                                //Change the conflicting bit to 1 (true). 
                                cad.setConfliting(true);

                                //Calculating the 'hs' for the current prefix..
                                int hs = key;

                                //Setting the output ports for HT_CONFLICTING
                                byte output_ports_conflicting_table = Port.DEFAULT;
                                if (i == (p.length - 1)) {
                                    output_ports_conflicting_table = new_output_port;   //take from the url...
                                }

                                //I have to store in HT_CONFLICTING table both /<some previous subprefix>/p[i] and /<current subprefix>/p[i]
                                if (HT_CONFLICTING.putIfAbsent(element.getHashSubprefix(), element.getCad().getOutputPorts()) == null) {
                                    element.incrementCountConflicting();
                                } else {
                                    //I do not increment the count_conflicting parameter 
                                }
                                if (HT_CONFLICTING.putIfAbsent(hs, output_ports_conflicting_table) == null) {
                                    element.incrementCountConflicting();
                                } else {
                                    //I do not increment the count_conflicting parameter 
                                }
                            } else {
                                if (i == (p.length - 1)) {
                                    //I change the output port here because p[i] already exist
                                    //in the hash table, p[i] is final, and its not conflicting
                                    cad.setOutputPorts(new_output_port);
                                }
                            }
                        } else {
                            if (i == (p.length - 1)) {
                                //I change the output port here because p[i] already exist
                                //in the hash table, p[i] is final, and its not conflicting
                                cad.setOutputPorts(new_output_port);
                            }
                        }
                    }

                    //We update the cad in the element..                  
                    element.setCad(cad);

                    //The hs is unchanged. Therefore, we dont need to call element.setHashSubprefix()..
                    //Its necessary to replace the element p[i] into HT_CPM_CAD_HS table..
                    HT_C_CAD_HS.replace(p[i], element);
                }
            }
            incrementNumberOfPrefixes();
        }
        return insert;
    }

    /**
     * This method updates the output port(s) of an existing prefix in the
     * CPDataStructure.
     *
     * @ Input: Prefix to be updated and the new output port(s)
     * @ Output: Boolean value indicating whether or not the prefix was updated
     *
     */
    public boolean updatePrefix(String prefix, Byte new_output_port) {
        boolean update = false;
        String p[] = prefix.split("/");

        //Take the last name component..
        String last_name_component = p[p.length - 1];

        //Take the element from the data structure
        CPEntry cpe = HT_C_CAD_HS.get(last_name_component);
        if (cpe != null) {

            //Remove all '/' from the prefix to calculate the hash..
            String key = prefix.replaceAll("/", "");

            //Calculate the hash for the current prefix..
            CRC32 crc = new CRC32();
            crc.update(key.getBytes());
            int current_prefix_hash = (int) crc.getValue();

            if (cpe.getCad().isConfliting()) {

                if (current_prefix_hash == cpe.getHashSubprefix()) {
                    //We can update the output port in cpe..
                    cpe.getCad().setOutputPorts(new_output_port);
                    //Update the cpe in HT
                    if (HT_C_CAD_HS.replace(last_name_component, cpe) != null) {
                        update = true;
                    }
                }

                //We have to seach the current_prefix_hash in the HT_CONFLICTING table..
                if (HT_CONFLICTING.get(current_prefix_hash) != null) {
                    //The current_prefix_hash exist in the HT_CONFLICTING table and 
                    //we can update its output_port                    
                    if (HT_CONFLICTING.replace(current_prefix_hash, new_output_port) != null) {
                        update = true;
                    }
                } else {
                    //returns the default value of update (false)..
                }
            } else {
                //Compare the current prefix hash with the hs of the last name component (p[p.length-1])
                if (current_prefix_hash == cpe.getHashSubprefix()) {
                    //We can update the output port in cpe..
                    cpe.getCad().setOutputPorts(new_output_port);
                    //Update the cpe in HT
                    if (replace(last_name_component, cpe) != null) {
                        update = true;
                    }
                } else {
                    //Since current_prefix_hash is different from cpe.getHashSubprefix, the
                    //prefix does not exist in HT_C_CAD_HS..
                    //returns the default value of update (false)                    
                }
            }

        } else {
            //The last name component p[p.length-1] does not exist..
            update = false;
        }
        return update;
    }

    /**
     * This method removes an existing prefix 'prefix' from the CPDataStructure.
     * Before being removed, a given prefix must follow two conditions: - The
     * prefix must exist (canExistingPrefixBeRemoved(prefix) should return true)
     * - The criteria to remove the prefix must be hold
     * (canExistingPrefixBeRemoved(prefix) should return true) Since in some
     * cases an existing prefix can not be removed even if it exist, depending
     * on the circunstances, we will have a group of prefixes in the data
     * structure forever.
     *
     */
    public boolean removePrefix(String prefix) {

        boolean has = hasPrefix(prefix);
        boolean remove;

        if (has) {
            remove = canExistingPrefixBeRemoved(prefix);
            if (remove) {
                CRC32 crc = new CRC32();
                String subprefix = "";
                String p[] = prefix.split("/");
                for (int i = 1; i < p.length; i++) {

                    subprefix = subprefix + "/" + p[i];

                    CPEntry cpe = HT_C_CAD_HS.get(p[i]);
                    //As 'remove' is true at this point, it means that the prefix exists in HT_C_CAD_HS. 
                    //Therefore, we would not need to check whether cpe is null or not because it is obviously not null
                    //However, to be on the safe side, we check it anyway
                    if (cpe != null) {
                        //First I need to check the count_position..
                        if (cpe.getCountPosition() > 1) {
                            cpe.decrementCountPosition();

                            //Checking if the current p[i] continues..
                            if (i < (p.length - 1)) {
                                //p[i] continues (it is not final)
                                //I have to decrement the count_continue..
                                if (cpe.decrementCountContinue() == 0) {
                                    //If after decrementing the count_continue we get 0, its necessary to change
                                    //the continue bit to false
                                    cpe.getCad().setContinue(false);
                                }
                            } else {
                                //p[i] is final (is the end of prefix)
                                //I have to decrement the count_end_prefix..
                                if (cpe.decrementCountEndPrefix() == 0) {
                                    //If after decrementing the count_end_prefix we get 0, its necessary to change
                                    //the end_prefix bit to false
                                    cpe.getCad().setEndPrefix(false);

                                    //Since the word p[i] is no longer a final word, its output port need to be set 
                                    //to Port.DEFAULT...
                                    cpe.getCad().setOutputPorts(Port.DEFAULT);
                                }
                            }

                            //Checking if it is the second position or more..
                            if (i > 1) {
                                //Checking if p[i] is conflicting..
                                if (cpe.getCad().isConfliting()) {

                                    //As the method canExistingPrefixBeRemoved(prefix) returned true,
                                    //it means that I can securely remove the h(subprefix) from the HT_CONFLICTING table..
                                    crc.update(subprefix.getBytes());
                                    //Removing the h(subprefix)..
                                    if (HT_CONFLICTING.remove((int) crc.getValue()) != null) {
                                        //Now I need to update the count_conflicting and the conflicting bit..                                        
                                        //I have to decrement the count_continue anyway..
                                        cpe.decrementCountConflicting();
                                        if (cpe.getCountConflicting() == 1) {
                                            //It means that the count_conflicting was 2 before removing the h(subprefix) 
                                            //from HT_CONFLICTING. Therefore, we decrement it once again to make
                                            //count_conflicting be 0
                                            cpe.decrementCountConflicting();

                                            //At this point, both current h(subprefix) and hs of p[i] must be removed from HT_CONFLICTING and the
                                            //conflicting bit of p[i] must be set to false.
                                            //Since the current h(subprefix) was removed at this point (HT_CONFLICTING.remove() call above)
                                            //we remove now the hs of p[i] from HT_CONFLICTING table..
                                            if (HT_CONFLICTING.remove(cpe.getHashSubprefix()) != null) {
                                                //We removed the hs of p[i] from HT_CONFLICTING succefully..
                                                //Since the count_conflicting is 0, we have to set the conflicting bit to false
                                                cpe.getCad().setConfliting(false);
                                            } else {
                                                //ERROR: Since the count_conflicting is 2, HT_CONFLICTING table was suppose to store the hs of p[i]..
                                                System.out.println("ERROR: HT_CONFLICTING table was suppose to store the hs=" + cpe.getHashSubprefix() + " of " + p[i] + " but it doesn't..");
                                                System.exit(0);
                                            }

                                        } else {
                                            //It means that cpe.getCountConflicting()>1 even after removing the h(subprefix) 
                                            //from HT_CONFLICTING. Thus, the conflicting bit continues to be true
                                        }
                                    }
                                }
                            }

                        } else {
                            //cpe.getCountPosition() is equals to 1.
                            //It means that I can remove p[i] from the HT_C_CAD_HS table..

                            //Since the count_position is 1, it is not necessary to change the count_continue,
                            //count_end_prefix and count_conflicting as well as the continue, end prefix and conflicting bits
                            //No matter what position p[i] is, we can just remove it..
                            if (HT_C_CAD_HS.remove(p[i]) == null) {
                                //ERROR: cpe.getCountPosition() is equals to 1 but we could not remove the p[i] because it does not exist in HT_C_CAD_HS table..
                                System.out.println("ERROR: cpe.getCountPosition() is equals to 1 but we could not remove the p[i] because it does not exist in HT_C_CAD_HS table..");
                                System.exit(0);
                            }
                        }
                    } else {
                        //It is not suppose to occur this error.. I have to check the hasPrefix method to see whats
                        //going on..
                        System.out.println("ERROR: The method hasPrefix(" + prefix + ", HT_CONFLICTING); returns true but the element cpe is null..");
                        System.exit(0);
                    }
                    crc.reset();
                }
            }
        } else {
            remove = false;
        }
        return remove;
    }

    //Return true if this data structure has the prefix or false otherwise
    public boolean hasPrefix(String prefix) {
        boolean has = false;
        CRC32 crc = new CRC32();
        String subprefix = "";
        String p[] = prefix.split("/");

        for (int i = 1; i < p.length; i++) {

            has = true;

            //subprefix = subprefix + p[i];
            subprefix = subprefix + "/" + p[i];

            CPEntry cpe = HT_C_CAD_HS.get(p[i]);

            //The first thing to do is to check whether or not p[i] exists in the data structure
            if (cpe == null) {
                //p[i] does not exist.. just break to return false
                has = false;
                break;
            } else {
                //Since p[i] exists in the data structure, the second thing to do is to verify if
                //the current position in the URL and the same as p[i] position in the data structure (CP_HT)
                if (cpe.getCad().getPosition() != i) {
                    //The current position differs from the position of p[i]...
                    //just break to return false..
                    has = false;
                    break;
                } else {
                    //Checking if the current position is not final... 
                    if (i < (p.length - 1)) {
                        //The current position is not final. That means the continue bit in cad need to be 1
                        if (!cpe.getCad().isContinue()) {
                            //The continue bit in cad is 0. 
                            //just break to return false
                            has = false;
                            break;
                        } else {
                            //The continue bit in cad is 1.
                            //It is necessary to verify if the conflicting bit in cad is 1 
                            if (cpe.getCad().isConfliting()) {
                                //Since the conflicting bit in cad is 1, we check if the current prefix hash
                                //is in HT_CONFLICTING table..
                                crc.update(subprefix.getBytes());
                                int key = (int) crc.getValue();
                                if (HT_CONFLICTING.get(key) == null) {
                                    //The current prefix hash does not exist in HT_CONFLICTING table..
                                    //just break and return false
                                    has = false;
                                    break;
                                } else {
                                    //no matter what output port the method HT_CONFLICTING.get(key) returned
                                    // (Port.DEFAULT or valid port), we are sure at this point that until now the current sub-prefix 
                                    //exist but we have to look for the next p[i]                                    
                                }
                            } else {
                                //We check if the current position is greater than 1 because if its 1 it does
                                //not make sense to check the hash values...
                                if (i > 1) {
                                    //The conflicting bit in cad is 0 but p[i] is already in the CP_HT.
                                    //We have to make sure that current prefix hash is equal to hs..
                                    //If so, we look for the next p[i]. If its not, we just breaks and return false
                                    crc.update(subprefix.getBytes());
                                    int key = (int) crc.getValue();
                                    if (key != cpe.getHashSubprefix()) {
                                        //For some reason, even the conflicting bit is equals to 0, the 
                                        //current prefix hash differs from the hs... 
                                        //just breaks and return false..
                                        has = false;
                                        break;
                                    } else {
                                        //So far so good. looking to the next p[i]..
                                    }
                                }
                            }
                        }
                    } else {
                        // i==(p.length-1)  meaning that we are at the end of a prefix..
                        if (!cpe.getCad().isEndPrefix()) {
                            //we are at the end of this prefix but the end prefix bit in cad is 0
                            //just break and return false
                            has = false;
                            break;
                        } else {
                            //end prefix bit in cad is 1
                            //checking if the conflict bit in cad is 1
                            if (cpe.getCad().isConfliting()) {
                                //the conflict bit in cad is 1..                                
                                crc.update(subprefix.getBytes());
                                int key = (int) crc.getValue();
                                Byte output_port = HT_CONFLICTING.get(key);
                                if (output_port != null) {
                                    if (output_port == Port.DEFAULT) {
                                        //if the output port is Port.DEFAULT it means that /<some subprefix>/p[i] does not
                                        //exist because otherwise, HT_CONFLICTING table would contain h(/<some subprefix>/p[i])
                                        has = false;
                                        break;
                                    } else {
                                        //At this point, we are sure that such a prefix exist.. 
                                        break;
                                    }
                                } else {
                                    has = false;
                                    break;
                                }
                            } else {
                                //the conflict bit in cad is 0..
                                if (i > 1) {
                                    crc.update(subprefix.getBytes());
                                    int key = (int) crc.getValue();
                                    if (key != cpe.getHashSubprefix()) {
                                        has = false;
                                        break;
                                    } else {
                                        //So far so good. looking to the next p[i]..
                                    }
                                }
                            }
                        }
                    }
                }
            }
            crc.reset();
        }
        return has;
    }

    /**
     * This method takes an url and performs the LNPM on it. It returns the
     * corresponding output port if a match occurs or the DEFAULT port
     * otherwise.
     *
     * @param url
     * @return the output port or the default port
     */
    public byte performLPM(String url) {
        byte port = Port.DEFAULT;  //DEFAULT PORT
        String subprefix = "";
        CRC32 crc = new CRC32();
        String p[] = url.split("/");
        for (int i = 1; i < p.length; i++) {
            //subprefix=subprefix+p[i];
            subprefix = subprefix + "/" + p[i];
            CPEntry c = get(p[i]);
            if (c != null) {
                if (i == c.getCad().getPosition()) {
                    if (i > 1) {
                        crc.update(subprefix.getBytes());
                        int current_hash = (int) crc.getValue();
                        if (c.getCad().isConfliting()) {
                            Byte value = HT_CONFLICTING.get(current_hash);
                            if (value != null) {
                                //the port will be updated only if value != 0
                                if (value != 0) {
                                    port = value;
                                }
                            } else {
                                //This condition should not occur because if
                                //a given word 'w1' is conflicting, it means
                                //that the HT_CONFLICTING table must contain
                                //hashes of all subprefixes that contains 'w1'
                                //For example: h(/<subprefix1>/w1), h(/<subprefix2>/w1), ...
                                System.out.println("ERROR: in performLPM(" + url + "). The h(" + subprefix + ") value should be in HT_CONFLICTING table but is does not..");
                                System.exit(0);
                            }
                            if (!c.getCad().isContinue()) {
                                //if it does not continue, we just break and returns
                                //the latest port setted    
                                break;
                            }
                        } else {
                            if (current_hash == c.getHashSubprefix()) {
                                if (c.getCad().isEndPrefix()) {
                                    port = c.getCad().getOutputPorts();
                                } else {
                                    //the port keeps the same value as it has
                                }
                                if (!c.getCad().isContinue()) {
                                    //if it does not continue, we just break and returns
                                    //the latest port set    
                                    break;
                                }
                            } else {
                                //this condition should not happen.. 
                                //(a cross conflicting problem was detected)
                                //the output port is set to default even if it had a previous value <> than Port.DEFAULT                                
                                port = Port.DEFAULT;
                                break;
                            }
                        }
                    } else {
                        if (c.getCad().isEndPrefix()) {
                            port = c.getCad().getOutputPorts();
                        }
                        if (!c.getCad().isContinue()) {
                            //if it does not continue, we just break and returns
                            //the latest port set
                            break;
                        }
                    }
                } else {
                    if (i > 1) {
                        //Since its not responsability of this method to send packets, 
                        //we just break it and return the current port.

                        //Of course, before calling this method, regardless of the 
                        //results of the lookup on CP Shape table, the packet
                        //will not be sent to CPU because the current position is greater than 1 and
                        //the non-canonical dataset does not contain URLs that 
                        //have common subprefixes with the prefixes in the data plane. 
                        //E.g: we do not have a prefix like /a/b in data plane and 
                        ///a/b/c in control plane. 
                        break;
                    } else {
                        //Its not responsability of this method to send the packet
                        //to CPU. Therefore, we just break and return the DEFAULT port.

                        //But, of course, before calling this method, if the 
                        //CP shape table hits, the packet will be sent to CPU..
                        break;
                    }
                }
            } else {
                break;
            }
            crc.reset();
        }
        return port;
    }

    /**
     * This method takes a given url and returns true if any of its name
     * components has the conflicting bit set to true in this data structure. If
     * all name components in the url are not conflicting, returns false
     */
    public boolean isPrefixConflicting(String url) {
        boolean has = false;
        if (hasPrefix(url)) {
            String p[] = url.split("/");
            for (int i = 1; i < p.length; i++) {
                CPEntry c = this.get(p[i]);
                if (c != null) {
                    if (c.getCad().isConfliting()) {
                        has = true;
                        break;
                    }
                }
            }
        }
        return has;
    }
}
