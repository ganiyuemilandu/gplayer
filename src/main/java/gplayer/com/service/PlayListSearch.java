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

import gplayer.com.exec.GPlayer;
import gplayer.com.service.enumconst.FileFilter;
import gplayer.com.util.Utility;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**Initiates search process
*to find a match or matches
*among an array of items
*under specified parameters.
*@author Ganiyu Emilandu
*/

public class PlayListSearch {
    private static Logger LOGGER;
    static Thread thread;  //Conducts the search process in the background
    static Semaphore sem1 = new Semaphore(1, true), sem2 = new Semaphore(1, true);
    String[] stringArray;  //Search array
    String searchString = "";  //Holds the string to be sought for, empty on initialization
    static String searchStringFilter = "";
    static String lastSearchedString = "";
    boolean[] boolArray;  //Saves the match state of each item in the search array
    static int[] num;
    boolean doesMatch = false;  //A boolean vairable to verify a match
    static boolean runValue = false;
    static int filePosition = 0;
    static TreeMap<Integer, File> treeMap = new TreeMap<>();
    static LinkedList<File> linkedList =  new LinkedList<>();
    static LinkedList<File> recentFiles =  new LinkedList<>();
    static ArrayList<Integer> fileIndices = new ArrayList<>();
    static TreeMap<Integer, File> fileMap = new TreeMap<>();
    static String searchStringHolder = "";
    static int filePositionHolder = 0, linkedListSize = 0, nextFileIndex = 0;
    static boolean foundMatch = false, isModified = false;

    /**constructor
    *@param s
    *the search string
    *@param ll
    *the list of files on which the search would occur
    *@param num
    *holds an array of integer values, usually one or none
    */
    public PlayListSearch(String s, LinkedList<File> ll, int... num) {
        if (LOGGER == null)
            createLogger();
        String myString = (lastSearchedString.equals(searchStringHolder))? searchStringHolder: searchStringFilter;
        boolean suspendFlag = (num.length > 0)? isContentSimilar(s, myString): false;
        searchString = (num.length > 0)? searchString: s;
        if (!suspendFlag || ll != linkedList) {
            if (!searchString.isEmpty()) {
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //Try to acquire access
                            if (FileMedia.runValue)
                                sem1.acquire();
                            else
                                sem2.acquire();
                        }
                        catch (InterruptedException ex) {
                            LOGGER.log(Level.WARNING, "Interrupted search semaphore", ex);
                            return;
                        }
                        //If access is granted
                        GPlayer.getSceneRoot().disablePlayLists(true);  //Have the play lists disabled
                        //Ascertain if play is to occur after the search is performed
                        boolean play = (num.length > 0)? true: false;
                        foundMatch = false;  //Flags true if a search or searches are found
                        doesMatch = false;  //Flags true if an object satisfys the parameter
                        //Create a copy of the passed num
                        PlayListSearch.num = num.clone();
                        final int position = FileMedia.getFilePosition();
                        linkedList = ll;
                        linkedListSize = linkedList.size();
                        //Sort for matches
                        boolean[] matchResult = findMatch();
                        if (FileMedia.runValue) {
                            nextFileIndex = -1;
                            createGoToPlayList(linkedList, matchResult, position);
                            if (foundMatch) {
                                filePosition = 0;
                                if (num.length > 0)
                                    lastSearchedString = searchString;
                            }
                            if (!foundMatch || !play)
                                //Relinquish access
                                sem1.release();
                            GPlayer.getSceneRoot().playMedia(play);
                        }
                        else {
                            LinkedList<File> matchFiles = PlayListSearchAction.sortedFiles;
                            if (!matchFiles.isEmpty()) {
                                searchStringFilter = searchString;
                                lastSearchedString = searchString;
                            }
                            if (num[0] == -1)
                                GPlayer.getSceneRoot().conductFileMatch(matchFiles, s, FileMedia.newSearch, play, 0);
                            else
                                GPlayer.getSceneRoot().conductFileMatch(matchFiles, s, FileMedia.newSearch, play, 0, num[0]);
                            sem2.release();
                        }
                        //Enable the disabled play lists
                        GPlayer.getSceneRoot().disablePlayLists(false);
                        PlayListSearchAction.freeMemory();
                        //Release used log resources
                        GPlayer.releaseLogResourceFor(LOGGER);
                    }
                });
                //To ensure thread is terminated
                //when the main thread exits
                thread.setDaemon(true);
                thread.start();  //Begin thread execution
            }
        }
    }  //Constructor closes

    /**Overloaded constructor*/
    public PlayListSearch() {}

    /**Attempts to instantiate the logger object for this class
    *@return the logger object, if already instantiated
    *or create/instantiate it and return the result
    */
    public static Logger createLogger() {
        return (LOGGER = (LOGGER == null)? GPlayer.createLogger(PlayListSearch.class): LOGGER);
    }

    /**Confirms the equality of 2 search processes
    *by examining filters used, runValue states, and search words
    *@param s
    *one of the search words
    *@param t
    *second of the search words
    *@return true if the parameters outlined above are equal,
    *and false otherwise
    */
    private boolean isContentSimilar(String s, String t) {
        boolean isEqual = PlayListSearchAction.isFilterEqual(FileFilter.getDefaultFilter()) && (runValue == FileMedia.runValue);
        runValue = FileMedia.runValue;
        searchString = Utility.retainLettersAndDigits(s);
        return (searchString.equalsIgnoreCase(t) && isEqual)? true: false;
    }

    /**Compiles search result
    *and transitions them to the play stage
    *@param ll
    *the list of files the search was performed on
    *@param matchResult
    *boolean array indexing each file match state
    *@param position
    *the current playing media position
    */
    private void createGoToPlayList(LinkedList<File> ll, boolean[] matchResult, int position) {
        ArrayList<Integer> fileIndices = new ArrayList<>();
        LinkedList<File> recentFiles = new LinkedList<>();
        if (!PlayListSearchAction.clq.isEmpty()) {
            //Great, we have fresh search result
            //Update the older one then
            this.recentFiles = PlayListSearchAction.sortedFiles;
            this.fileIndices = PlayListSearchAction.sortedIndices;
            searchStringHolder = searchString;
            foundMatch = true;
            nextFileIndex = (FileMedia.goToNext)? PlayListSearchAction.ai.intValue()-1: FileMedia.getFilePosition();
        }
        int numValue = -1;
        if (num.length > 0) {  //If the num varargs is passed 1 or more arguments
            int x = num[0];  //We are only interested in the first
            if (x > -1) {  //It's a valid number
                //play has to begin from that number
                numValue = x;
                nextFileIndex = (FileMedia.goToNext)? numValue-1: numValue+1;
                updateMatchIndex(ll, ll.get(x), x);  //Infuse the number into the mix of existing numbers, if any
            }
        }
        else {  //If num isn't passed any value
            if (!foundMatch) {  //If there were no matches
                this.recentFiles = recentFiles;
                this.fileIndices = fileIndices;
            }
        }
        if (!fileMap.isEmpty() && numValue == -1) {
            //Obtain a set of fileMap keys
            Set<Integer> set = fileMap.keySet();
            for (int key: set) {
                updateMatchIndex(linkedList, fileMap.get(key), key);  //Update the various indices
                //Only interested in the first, so
                break; //out of the loop
            }
        }
        if (!foundMatch && !this.fileIndices.isEmpty())
            searchStringHolder = searchString;
    }

    /**Commences the search process
    *@return the boolean array indexing the match state of each string
    *true at indices that match, and false at other indices
    */
    public boolean[] findMatch() {
        LOGGER.info("Starting search process on an array of size: " + linkedList.size());
        stringArray = new String[linkedList.size()];  //Create an array with the size of the past linked list, so to receive array of strings
        boolArray = new boolean[linkedList.size()];  //Another array of boolean literals of equal size
        searchString = Utility.retainLettersAndDigits(searchString);
        PlayListSearchAction.threshold = -1;
        PlayListSearchAction.ai.getAndSet(FileMedia.getFilePosition());
        long startTime = System.nanoTime();
        //Invoke the task to begin the search
        new PlayListSearchAction(linkedList, boolArray, stringArray, searchString, 0, stringArray.length).invoke();
        LOGGER.info("Ended the search process in: " + (System.nanoTime() - startTime));
        return boolArray;
    }

    /**Obtains the immediate higher or lower number
    *from existing numbers in fileIndices
    *relative to a value passed as argument.
    *@param f
    *the value being used as the parameter to get the immediate higher or lower number
    *@param next
    *tells which number to return to the caller.
    *@return the higher number if next is true,
    *or the lower number if next is false
    */
    public static int findMatchIndex(int f, boolean next) {
        File file = recentFiles.get(filePosition);
        int position = fileIndices.get(filePosition);
        boolean searchOn = false, indexChange = false;
        int startUpPosition = filePosition, endPosition = 0, index = filePosition;
        if (next) {  //If the next parameter is set to true
            searchOn = (position <= f);  //If the file position is less than the current playing file position, searchOn is set to true
            endPosition = filePosition-1;
            if (endPosition < 0)
                endPosition = recentFiles.size()-1;
        }
        else { //If the next parameter is set to false
            searchOn = (position >= f);  //searchOn would be set to true, if the value of position is greater or equal to the current playing media index
            endPosition = filePosition+1;
            if (endPosition == recentFiles.size())
                endPosition = 0;
        }
        if (searchOn) {  //If searchOn is set to true
            for (int i = startUpPosition; ;) {  //iterate through the others to obtain the immediate higher or lower file position
                index = i;
                file = recentFiles.get(i);  //Retrieve the file at index i in recentFiles
                int thisPosition = fileIndices.get(i);
                if (next && thisPosition > f && thisPosition > -1) {
                    filePosition = i;
                    break;  //Terminate the loop
                }
                if (!next && thisPosition < f && thisPosition > -1) {
                    filePosition = i;
                    break;  //Terminate
                }
                if (i == endPosition) {
                    break;  //Forcefully terminate the loop
                }
                if (next) {
                    if (i == recentFiles.size()-1)
                        i = -1;
                    i++;  //Increase i by 1
                }
                else {
                    if (i == 0)
                        i = recentFiles.size();
                    i--;  //Decrease i by 1
                }
            }  //End of loop
        }  //End of the conditional expression
        if (searchOn)  //If the for-loop block was entered
            position = fileIndices.get(filePosition);  //Obtain the new value of position
        if (position != -1) {  //If a valid file location is discovered
            filePositionHolder = position;
            if (foundMatch) {
                //Relinquish access
                sem1.release();
                //Negate foundMatch
                foundMatch = false;
            }
        }
        return position;
    }

    /**Checks to see if a file whose index in a playlist must have been passed to the constructor at one time,
    *still maintains the same index in another playlist.
    *In situations where the recorded index does not reflect the same file in the new playlist,
    *an update of index is attempted.
    *If the file is found in the new playlist, the index is updated,
    *otherwise, it is discarded.
    *@param ll
    *the new playlist
    *@param file
    *the file to update its index
    *@param number
    *the recorded/older index
    */
    private static void updateMatchIndex(LinkedList<File> ll, File file, int number) {
        linkedList = ll;
        linkedListSize = linkedList.size();
        int fileIndex = number;
        treeMap = new TreeMap<>();
        TreeMap<Integer, File> fileMap2 = new TreeMap<>();
        if (number < ll.size() && number > -1 && !ll.get(number).equals(file))
            fileIndex = (ll.indexOf(file) == -1)? number: ll.indexOf(file);
        fileMap.put(fileIndex, file);
        Set<Map.Entry<Integer, File>> set = fileMap.entrySet();
        for (Map.Entry<Integer, File> map: set) {  //A for-each loop style
            int num = map.getKey();
            File f = map.getValue();
            if (num < ll.size() && f.exists()) {  //If the retrieved number is within range, and the file exists
                if (!ll.get(num).equals(f))  //If the item at num index doesn't equal the corresponding file value
                    num = ll.indexOf(f);  //Retrieve the new index value of the file
            }
            else
                num = ll.indexOf(f);  //Retrieve the new index value of the file
            if (num == -1 || !f.exists())  //If the linked list doesn't contain the file, or the file location is unavailable
                continue;
            //Else
            treeMap.put(num, f);  //Add the index of the retrieved file, and the file itself to tree map
            fileMap2.put(num, f);
        }
        if (!fileMap2.isEmpty())
            fileMap = fileMap2;
        if (!recentFiles.isEmpty()) {
            int i = 0;
            for (File files: recentFiles) {
                int index = fileIndices.get(i++);
                if (index < ll.size() && files.exists()) {
                    if (!ll.get(index).equals(files))
                        index = ll.indexOf(files);  //Get the index of the retrieved file
                }
                else
                    index = ll.indexOf(files);  //Get the index of the retrieved file
                if (index == -1 || !files.exists())  //If file does not exist in the linkedList, or file location is unavailable
                    continue;
                //otherwise
                treeMap.put(index, files);  //Store its value in tree map
            }
            recentFiles = new LinkedList<>();
            fileIndices = new ArrayList<>();
        }
        //Iterate through the tree map items
        set = treeMap.entrySet();  //Retrieve a map entry of the items
        for (Map.Entry<Integer, File> map: set) {
            fileIndices.add(map.getKey());  //The unique integer values are saved in fileIndices
            recentFiles.add(map.getValue());  //Retrieve the corresponding files, and save them in recentFiles
        }
    }
}