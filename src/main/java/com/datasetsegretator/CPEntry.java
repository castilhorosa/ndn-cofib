/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datasetsegretator;

import com.datasetsegretator.CAD;

/**
 *
 * @author user
 */
public class CPEntry {

    private int count_position;  //represents the frequency of a word in a position=cad.position 
    //in all urls in canonical dataset..

    private CAD cad;     //This class will be used as action data for each component stored in the P4 tables

    private int count_continue;   //represents the amount of not final words in a given position in all urls
    //For example, for the word 'b' in the following dataset:
    // /a/b/c
    // /x/b/t/n
    // /y/b
    // P = {/a/b/c, /x/b/t/n, /y/b}
    //we have count_continue=2 because there are two prefixes in all dataset
    //that 'b' is not final (it continues)

    private int count_end_prefix;  //represents the amount of final words in a given position in all urls
    //For example, for the word 'b' in the following dataset:
    // /a/b/c
    // /x/b/t/n
    // /y/b
    //we have count_end_prefix=1 because there is one prefix in all dataset
    //that 'b' is final (not continues)

    private short count_conflicting; //represents the amount of conflicting prefixes for a given word in all urls
    //For example, for the word 'b' in the following dataset:
    // /a/b/c
    // /x/b/t/n
    // /y/b
    // /a/b/t
    //we have count_conflicting=3 because there are three prefixes in all dataset
    //that is conflicting considering the word 'b'

    private int hash_subprefix;    //hash of the first subprefix of a given word.
    //For instance, for prefix /com/google/www we have:
    //com    -> hash_subprefix=Port.DEFAULT;
    //google -> hash_subprefix=hash of 'com'
    //www    -> hash_subprefix=hash of 'com' and 'google'

    public CPEntry(int count_position, CAD cad, int count_continue, int count_end_prefix, short count_conflicting, int hash_subprefix) {
        this.count_position = count_position;
        this.cad = cad;
        this.count_continue = count_continue;
        this.count_end_prefix = count_end_prefix;
        this.count_conflicting = count_conflicting;
        this.hash_subprefix = hash_subprefix;
    }

    public CAD getCad() {
        return cad;
    }

    public int getHashSubprefix() {
        return hash_subprefix;
    }

    public void setCad(CAD cad) {
        this.cad = cad;
    }

    public void setHashSubprefix(int hash_subprefix) {
        this.hash_subprefix = hash_subprefix;
    }

    public int getCountPosition() {
        return count_position;
    }

    public void decrementCountPosition() {
        //To be consistent, I only decrement if the value is greater than 1..
        if (this.count_position > 1) {
            this.count_position--;
        }
    }

    public void incrementCountPosition() {
        this.count_position++;
    }

    public int getCountContinue() {
        return count_continue;
    }

    public int decrementCountContinue() {
        //To be consistent, I only decrement if the value is greater than 0..
        if (this.count_continue > 0) {
            this.count_continue--;
        }
        //I return the current count_continue because depending on its value (if count_continue=0, for example)
        //its necessary to change the continue bit to false
        return this.count_continue;
    }

    public void incrementCountContinue() {
        this.count_continue++;
    }

    public int getCountEndPrefix() {
        return count_end_prefix;
    }

    public int decrementCountEndPrefix() {
        //To be consistent, I only decrement if the value is greater than 0..
        if (this.count_end_prefix > 0) {
            this.count_end_prefix--;
        }
        //I return the current count_end_prefix because depending on its value (if count_end_prefix=0, for example)
        //its necessary to change the end_prefix bit to false
        return this.count_end_prefix;
    }

    public void incrementCountEndPrefix() {
        this.count_end_prefix++;
    }

    public short getCountConflicting() {
        return count_conflicting;
    }

    public short decrementCountConflicting() {
        //To be consistent, I only decrement if the value is greater than 0..
        if (this.count_conflicting > 0) {
            this.count_conflicting--;
        }
        //I return the current count_conflicting because depending on its value (if count_conflicting=0, for example)
        //its necessary to change the conflicting bit to false and remove the entries in HT_CONFLICTING table 
        return this.count_conflicting;
    }

    public void incrementCountConflicting() {
        this.count_conflicting++;
    }
   

    public String toString() {

        String countPosition = "count_position=" + this.count_position + ", ";
        String cadStr = "cad=[continue=" + this.cad.isContinue() + ", end_prefix=" + this.cad.isEndPrefix() + ", conflicting=" + this.cad.isConfliting() + ", ";
        cadStr = cadStr + "position=" + this.cad.getPosition() + ", output_ports=" + this.cad.getOutputPorts() + "], ";
        String countContinue = "count_continue=" + this.count_continue + ", ";
        String countEndPrefix = "count_end_prefix=" + this.count_end_prefix + ", ";
        String countConflicting = "count_conflicting=" + this.count_conflicting + ", ";
        String hs = "hs=[" + this.hash_subprefix + "]";

        return countPosition + cadStr + countContinue + countEndPrefix + countConflicting + hs;
    }
}
