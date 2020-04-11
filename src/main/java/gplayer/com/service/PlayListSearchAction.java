/**
*Copyright (C) 2019 Ganiyu Emilandu
*
*This program is free software: you can redistribute it and/or modify
*it under the terms of the GNU General Public License as published by
*the Free Software Foundation, either version 3 of the License, or
*(at your option) any later version.
*
*THIS PROGRAM IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
*BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
*MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
*SEE THE GNU GENERAL PUBLIC LICENSE for MORE DETAILS.
*
*You should have received a copy of the GNU General Public License along with this program. If not, see
*<https://www.gnu.org/licenses/>.
*
*/

package gplayer.com.service;

import gplayer.com.service.enumconst.FileFilter;
import gplayer.com.util.Utility;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**Facilitates concurrent execution of a task
*on multi-processor devices
*by extending java.util.concurrent.RecursiveAction class
*and overriding its compute() method.
*@author Ganiyu Emilandu
*/

class PlayListSearchAction extends RecursiveAction {
    //A run time instance
    private Runtime r = Runtime.getRuntime();
    private Logger logger = PlayListSearch.createLogger();
    private LinkedList<File> linkedList;
    private final int THRESHOLD_MAX = 2000;  //The highest value of any set threshold
    private final int THRESHOLD_MIN = 1000;  //The lowest value of any set threshold
    static int threshold;  //Holds the average work load
    private int start, end;
    private boolean[] boolArray;
    private String[] stringArray;
    private String searchString;
    private static FileFilter FILTER = FileFilter.CONTAINS;  //The search parameter
    static ConcurrentLinkedQueue<Integer> clq;
    static ArrayList<Integer> sortedIndices;
    static LinkedList<File> sortedFiles;
    static AtomicInteger ai = new AtomicInteger(0);
    private IntUnaryOperator operator = ((a) -> {
        //Sorts the contents of clq in ascending order
        //and returns a value to which AtomicInteger ai is set
        final Integer[] array = clq.toArray(new Integer[clq.size()]);  //An array of the contents of clq
        final int size = array.length;
        ArrayList<Integer> indices = null;
        if (FileMedia.runValue) {
            indices = new ArrayList<>();
            indices.ensureCapacity(size);
        }
        final LinkedList<File> files = new LinkedList<>();
        boolean got = false;
        int updateInt = 0;
        //Loop through clq contents
        for (int i = 0; i < size; i++) {
            int lessNum =array[i];
            //An inner loop
            for (int j = i+1; j < size; j++) {
                int highNum = array[j];
                //Compare lessNum and highNum
                if (lessNum > highNum) {
                    //Swap positions
                    array[j] = lessNum;
                    lessNum = highNum;
                }
            }
            files.add(linkedList.get(lessNum));
            if (indices != null)
                indices.add(lessNum);
            if (!got && lessNum > FileMedia.getFilePosition()) {
                updateInt = lessNum;
                got = true;
            }
        }
        sortedIndices = indices;
        sortedFiles = files;
        return (got)? updateInt: ai.intValue();
    });

    PlayListSearchAction(LinkedList<File> linkedList, boolean[] boolArray, String[] stringArray, String searchString, int start, int end) {
        this.linkedList = linkedList;
        this.boolArray = boolArray;  //Holds boolean values of string comparison outcome
        this.stringArray = stringArray;  //Holds the strings to be compared
        this.searchString = searchString;  //The string to be sort for
        this.start = start;  //The entry point of each recursive action
        this.end = end;  //The exit point of each revursive action
        if (threshold == -1) {
            //Set the threshold value
            //Firstly,
            //Ascertain the number of processors available
            int processorNum = r.availableProcessors();
            threshold = Math.max(Math.min(THRESHOLD_MAX, boolArray.length/processorNum), THRESHOLD_MIN);
            clq = new ConcurrentLinkedQueue<>();
            FILTER = FileFilter.getDefaultFilter();
            logger.info("Search info:\n" + String.join(": ", "threshold value", ""+threshold) + String.join(": ", ", available processors", ""+processorNum));
        }
    }  //End of constructor

    public static boolean isFilterEqual(FileFilter F) {
        return (F == FILTER)? true: false;
    }

    public static void freeMemory() {
        clq = null;
        sortedIndices = null;
        sortedFiles = null;
    }

    private void findMatch() {
        logger.info("Beginning search process between " + start + " " + end);
        long time = System.currentTimeMillis();
        int iLoop = start;
        while (iLoop < end) {
            String previousString = null;
            String string = stringArray[iLoop];
            String delimiters = Utility.eliminateCharacters(string, ((i) -> i == '\'' || Character.isLetterOrDigit(i)));
            //Get a string token of the retrieved stringArray
            StringTokenizer stringToken = new StringTokenizer(string, delimiters);
            int k = 0, l = 0;
            boolean pass = false;
            //An inner while loop
            //to cycle through the various tokens
            while (stringToken.hasMoreTokens()) {
                string = stringToken.nextToken();
                if (string.contains("\'"))  //If the string contains apostrophe (')
                    //Obtain only the letters and numbers
                    string = Utility.retainLettersAndDigits(string);
                //Ascertain the appropriate length for both strings (searchString and string)
                int length = (string.length() <= searchString.substring(k).length())? string.length(): searchString.substring(k).length();
                l = k;
                if (string.substring(0,length).equalsIgnoreCase(searchString.substring(k, k+length))) {  //If the respective length-range characters in both strings are equal
                    boolArray[iLoop] = true;
                    k += length;  //The point at which subsequent comparison is to begin
                    //If all searchString characters are already exhausted, however
                    if (k >= searchString.length() && FILTER != FileFilter.ENDSWITH)
                        //Terminate the loop
                        break;
                    if (k >= searchString.length() && FILTER == FileFilter.ENDSWITH) {  //If matches are to be at the rear
                        k = 0;  //Continue search
                        pass = true;  //with pass reflecting the current state
                    }
                }
                else {  //If respective characters are not equal
                    boolArray[iLoop] = false;
                    if (FILTER == FileFilter.BEGINSWITH)
                        //Do nothing else
                        break;  //Out of the inner while loop
                    if (FILTER == FileFilter.ENDSWITH && !stringToken.hasMoreTokens())
                        boolArray[iLoop] = pass;
                    //Else
                    //Revert the value of k to 0
                    k = 0;
                    pass = false;
                }
            }  //End of inner while loop
            if (boolArray[iLoop] && linkedList.get(iLoop).exists())
                clq.add(iLoop);
            iLoop++;  //Iterate the outer while loop
        }  //End of outer while loop
        //Update the atomic integer
        ai.getAndSet(operator.applyAsInt(0));
        logger.info("Ended search process between " + start + " " + end + " in " + (System.currentTimeMillis()-time) + " milliseconds");
    }

    private void constructSearchArray() {
        logger.info("About to construct the search parameters with " + FileFilter.getDefaultFilter().toString() + " filter");
        long startTime = System.nanoTime();
        //Loop through the items in ll
        for (int j = start; j < end; j++) {
            String fileName = "";
            try {
                fileName = FileMedia.fileToString(linkedList.get(j));
                if (FileFilter.getDefaultFilter() == FileFilter.ENDSWITH) {
                    String endString = "", extension = "";
                    boolean discontinue = false;
                    //We are going to retrieve the string at the end of each file name
                    //by looping through fileName characters backward
                    for (int i = fileName.length()-1; i >= 0; i--) {
                        //Retrieve the character at index i
                        char ch = fileName.charAt(i);
                        if (!discontinue)
                            //Append extension to the end of ch to maintain fileName character natural order
                            extension = ch + extension;
                        else
                            //Append endString to the end of ch to maintain fileName character natural order
                            endString = ch + endString;
                        if (ch != '\'' && !Character.isLetterOrDigit(ch)) {
                            if (discontinue && endString.length() >= searchString.length())
                                break;
                            discontinue = true;
                        }
                    }  //End of inner for-loop
                    //Concactinate endString and extension, and assign to fileName
                    fileName = endString + extension;
                }
            }
            catch (NullPointerException ex) {}
            stringArray[j] = fileName;
            boolArray[j] = false;
        }
        logger.info("Completed search parameter construction in: " + (System.nanoTime() - startTime));
    }

    @Override
    protected void compute() {  //Process begins here
        //But first of,
        if (end-start <= threshold) {  //If the work range less than or equal to the threshold
            //Begin process
            constructSearchArray();
            findMatch();
        }  //If-block closes
        else {  //If the threshold isn't reached yet
            //Further divide the work
            //Get the midpoint
            int middle = (start+end)/2;
            invokeAll(new PlayListSearchAction(linkedList, boolArray, stringArray, searchString, start, middle), new PlayListSearchAction(linkedList, boolArray, stringArray, searchString, middle, end));
        }  //Else-block closes
    }  //Method ends
}  //End of class