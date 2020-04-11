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

package gplayer.com.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.RecursiveTask;

/**Concurrently searches through an array of strings to find the most suitable match.
*@author Ganiyu Emilandu
*/

class DataSearchTask extends RecursiveTask<Duo<Integer, Matcher>> {
    private String[] data;
    private Pattern pattern;
    private int focusedIndex, start, end, threshold;

    /**Creates a new instance of this class.
    *@param data
    *an array of strings to search through
    *@param pattern
    *the pattern of a search string employed in the search process
    *@param focusedIndex
    *currently active index
    *@param start
    *the start index of the search range
    *@param end
    *one more index than the end index of the search range
    *@param threshold
    *the average workload of each concurrent search task.
    */
    DataSearchTask(String[] data, Pattern pattern, int focusedIndex, int start, int end, int threshold) {
        this.data = data;
        this.pattern = pattern;
        this.focusedIndex = focusedIndex;
        this.start = start;
        this.end = end;
        this.threshold = threshold;
    }

    private int findBestMatchIndex(int bestMatchIndex, int nextBestMatchIndex) {
        if (focusedIndex == bestMatchIndex || focusedIndex == nextBestMatchIndex)
            return (focusedIndex == bestMatchIndex)? nextBestMatchIndex: bestMatchIndex;
        //At this point,
        //we are certain that focusedIndex is a unique number
        int max = gplayer.com.util.Utility.maxValue(focusedIndex, bestMatchIndex, nextBestMatchIndex), min = gplayer.com.util.Utility.minValue(focusedIndex, bestMatchIndex, nextBestMatchIndex);
        //If focusedIndex were at one of the extremes (min or max)
        if (max == focusedIndex || min == focusedIndex)
            //return the smaller of bestMatchIndex and nextBestMatchIndex
            return Math.min(bestMatchIndex, nextBestMatchIndex);
        //If focusedIndex were at the middle however,
        //return the greater of bestMatchIndex and nextBestMatchIndex
        return Math.max(bestMatchIndex, nextBestMatchIndex);
    }

    private Duo<Integer, Matcher> findBestMatch(Duo<Integer, Matcher> bestMatch, Duo<Integer, Matcher> nextBestMatch, boolean compareIndices) {
        if (nextBestMatch == null)
            return bestMatch;
        if (bestMatch == null)
            return nextBestMatch;
        //At this point,
        //we are certain that none of the matches is null
        //So, let's get the start indices of the matches
        int bestMatchStart = bestMatch.getValue().start();  //Match location in bestMatch
        int nextBestMatchStart = nextBestMatch.getValue().start();  //Match location in nextBestMatch
        //Let's ascertain if the match location in nextBestMatch is less than that of bestMatch.
        //If it is, return the value of nextBestMatch
        if (nextBestMatchStart < bestMatchStart)
            return nextBestMatch;
        if (compareIndices && nextBestMatchStart == bestMatchStart)
            return (findBestMatchIndex(bestMatch.getKey(), nextBestMatch.getKey()) == nextBestMatch.getKey())? nextBestMatch: bestMatch;
        return bestMatch;
    }

    @Override
    protected Duo<Integer, Matcher> compute() {
        Duo<Integer, Matcher> bestMatch = null;
        try {
            if (!isCancelled()) {
                //if the range between start and end indices is less or equal to the threshold,
                //process sequentially.
                if ((end - start) <= threshold) {
                    int length = data.length, index = 0;
                    for (int i = start; i < end && !isCancelled(); i++) {
                        Matcher matcher = pattern.matcher(data[(index = (i < length)? i: i - length)].toLowerCase());
                        if (matcher.find())
                            bestMatch = findBestMatch(bestMatch, new Duo<Integer, Matcher>(index, matcher), false);
                        if (bestMatch != null && bestMatch.getValue().start() == 0)
                            //Stop searching
                            break;
                    }  //End of for-loop
                }
                else {  //Further divide the work
                    //Let's get the mid point between the start and end indices
                    int middle = (start + end) / 2;
                    //Instantiate 2 new processes
                    DataSearchTask task1 = new DataSearchTask(data, pattern, focusedIndex, start, middle, threshold);
                    DataSearchTask task2 = new DataSearchTask(data, pattern, focusedIndex, middle, end, threshold);
                    //Execute the subtasks asynchronously
                    task1.fork();
                    task2.fork();
                    //Compare results of both subtasks upon return
                    bestMatch = findBestMatch(task1.join(), task2.join(), true);
                }
            }
        }
        catch (Exception ex) {}
        return bestMatch;
    }

}