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

import java.io.File;
import java.util.Collection;
import java.util.function.Function;
import java.util.regex.Matcher;
import static java.util.regex.Pattern.compile;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**Conducts concurrent search process on an array of strings.
*@param <T>
*the type of objects from which the search array of strings will be constructed.
*@author Ganiyu Emilandu
*/

public class DataSearch<T> extends Service<Integer> {
    private static Runtime runtime = Runtime.getRuntime();  //References the Java runtime
    private DataSearchTask searchTask;  //Initialized to execute search proceedings
    private IntegerProperty matchIndex = new SimpleIntegerProperty(this, "matchIndex");  //Updated to reflect the match discovered on every execution
    private String[] data;  //References the array on which searches can be carried out
    private ObservableList<T> sourceData = javafx.collections.FXCollections.observableArrayList();  //Updates/refreshes the search array relative to its content
    private final StringBuffer SEARCH_BUFFER = new StringBuffer();  //Holds the strings to search for in the data array
    private int averageWorkLoad = 0;  //The average workload for each concurrent task
    private int start, end;  //Lower and upper boundaries of data search
    private static long permissibleTimeDifference = 1500;  //Time interval to precipitate reset of SEARCH_BUFFER content, after it must have been exhausted
    private volatile long lastSearchTime = 0;  //Records every search process execution
    private boolean allowLeadingSpaces;  //Flags if spaces can be allowed at the start of a search string
    private volatile boolean runOnce = false;  //Indicates if a task has been executed at least once

    /**Creates a new instance of this class.
    *@param data
    *the collection of elements cached in the data deployed during search proceedings.
    */
    public DataSearch(Collection<? extends T> data) {
        this(data, null, permissibleTimeDifference);
    }

    /**Creates a new instance of this class.
    *@param data
    *the collection of elements cached in the data deployed during search proceedings.
    *@param converter
    *String translation  of collection elements.
    */
    public DataSearch(Collection<? extends T> data, Function<T, String> converter) {
        this(data, converter, permissibleTimeDifference);
    }

    /**Creates a new instance of this class.
    *@param data
    *the collection of elements cached in the data deployed during search proceedings.
    *@param converter
    *String translation  of collection elements.
    *@param timeout
    *permissible millisecond interval between a search procedure and a subsequent one before resetting/clearing the search buffer content.
    */
    @SuppressWarnings("unchecked")
    public DataSearch(Collection<? extends T> data, Function<T, String> converter, long timeout) {
        if (data instanceof ObservableList)
            sourceData = (ObservableList) data;
        initialize(collateData(data, converter), timeout, converter);
    }

    /**Creates a new instance of this class.
    *@param data
    *the vararg array cached in the data deployed during search proceedings.
    */
    @SafeVarargs
    public DataSearch(T... data) {
        this(data, null, permissibleTimeDifference);
    }

    /**Creates a new instance of this class.
    *@param data
    *the array cached in the data deployed during search proceedings.
    *@param converter
    *String translation  of array elements.
    */
    public DataSearch(T[] data, Function<T, String> converter) {
        this(data, converter, permissibleTimeDifference);
    }

    /**Creates a new instance of this class.
    *@param data
    *the array cached in the data deployed during search proceedings.
    *@param converter
    *String translation  of array elements.
    *@param timeout
    *permissible millisecond interval between a search procedure and a subsequent one before resetting/clearing the search buffer content.
    */
    public DataSearch(T[] data, Function<T, String> converter, long timeout) {
        initialize(collateData(data, converter), timeout, converter);
    }

    /**Constructs and fills a string array
    *with the String conversion of each element in the passed array.
    *@param array
    *the array whose content names are used to fill the prospective array
    *@param converter
    *String translation  of array elements.
    *@return a string array of equal length as the passed array.
    */
    private String[] collateData(T[] array, Function<T, String> converter) {
        Function<T, String> nonNullConverter = (converter != null)? converter: ((object) -> object.toString());
        if (converter == null && array instanceof String[])
            return (String[]) array;
        int length = array.length;
        String[] data = new String[length];
        for (int index = 0; index < length; index++)
            data[index] = nonNullConverter.apply(array[index]);
        return data;
    }

    /**Constructs and fills a string array
    *with the String conversion of each element in the passed collection.
    *@param collection
    *the collection whose content names are used to fill the prospective array
    *@param converter
    *String translation  of collection elements.
    *@return a string array of equal length as the passed array.
    */
    @SuppressWarnings("unchecked")
    private String[] collateData(Collection<? extends T> collection, Function<T, String> converter) {
        return collateData(((T[]) collection.toArray()), converter);
    }

    private void initialize(String[] data, long searchTimeout, Function<T, String> converter) {
        initialize(data);
        permissibleTimeDifference = searchTimeout;
        sourceData.addListener((ListChangeListener.Change<? extends T> c) -> initialize(collateData(sourceData, converter)));
    }

    private void initialize(String[] data) {
        this.data = data;
        averageWorkLoad = averageWorkLoad(data.length);
    }

    public final ObservableList<T> fetchData() {
        return sourceData;
    }

    /**Calculates the average workload for each task executed during the concurrent search process,
    *with the lower boundary at 1000 and the upper boundary at 2000.
    *@param totalWorkLoad
    *the total number of data to be processed
    *@return the average workload relative to the toatal workload.
    */
    private int averageWorkLoad(int totalWorkLoad) {
        int min = 1000, max = 2000;  //lower and upper boundaries respectively
        int average = totalWorkLoad / runtime.availableProcessors();
        //if the value of average is greater than that of max, return max.
        //If the value of average is less than that of min, return min.
        //else, return average
        return Math.max(Math.min(max, average), min);
    }

    /**Updates the value of matchIndex to a specified value.
    *@param index
    *the new value to set matchIndex value to.
    */
    private void setMatchIndex(int index) {
        matchIndex.set(index);
    }

    /**Gets the value held by matchIndex.
    *@return the current value of matchIndex.
    */
    public final int getMatchIndex() {
        return matchIndexProperty().get();
    }

    /**Gets and returns the reference to matchIndex property.*/
    public final javafx.beans.property.ReadOnlyIntegerProperty matchIndexProperty() {
        return matchIndex;
    }

    /**Returns the difference between the current time
    *and the last time a search process was invoked.
    */
    public final long getTimeInterval() {
        return System.currentTimeMillis() - lastSearchTime;
    }

    /**Indicates if leading spaces are to be allowed in the search string.*/
    public final void allowLeadingSpaces(boolean allow) {
        allowLeadingSpaces = allow;
    }

    private void setSearchParameters(int start, int end, int searchBufferLength) {
        this.start = Math.min(start, end);
        this.end = Math.max(start, end);
        if (searchBufferLength > 0)
            SEARCH_BUFFER.delete(0, searchBufferLength);
    }

    private void initializeSearch(CharSequence searchBuffer, int start, int end) {
        //The string in the searchBuffer
        String string = (searchBuffer == null)? "": searchBuffer.toString();
        //Only process if string is not empty
        if (!string.isEmpty()) {
            long priorSearchTime = lastSearchTime, currentSearchTime = System.currentTimeMillis();
            //Update lastSearchTime
            lastSearchTime = currentSearchTime;
            //If the difference between current time and the last time this process was invoked,
            //is greater than the value of permissibleTimeDifference,
            //reset various search parameters such as the start index, end index and the search buffer
            if ((currentSearchTime - priorSearchTime) > permissibleTimeDifference)
                setSearchParameters(start, end, SEARCH_BUFFER.length());
            boolean empty = SEARCH_BUFFER.length() == 0;
            if (empty && !allowLeadingSpaces && (string = string.trim()).isEmpty())  //Spaces at the start of the string
                return;
            //Append the new entry
            SEARCH_BUFFER.append(string);
            if (!runOnce) {
                runOnce = true;
                start();
            }
            else {
                if (!searchTask.isDone())
                    cancel();
                restart();
            }
        }
    }

    /**Initiates concurrent search process on cached data;
    *beginning at a specified index,
    *and concluding at another.
    *@param searchBuffer
    *the buffer from which the search string would be formulated
    *@param start
    *the index from which the search process will commence
    *@param end
    *1 index past the actual index on which the search process will terminate
    */
    public void search(CharSequence searchBuffer, int start, int end) {
        initializeSearch(searchBuffer, start, end);
    }

    /**Initiates concurrent search process on cached data;
    *beginning at a specified index,
    *through the sum value of the start index and the length of the search data.
    *@param searchBuffer
    *the buffer from which the search string would be formulated
    *@param start
    *the index from which the search process will commence
    */
    public void search(CharSequence searchBuffer, int start) {
        initializeSearch(searchBuffer, start, start + data.length);
    }

    /**Initiates concurrent search process on cached data;
    *beginning at index 0,
    *through the length of the search data.
    *@param searchBuffer
    *the buffer from which the search string would be formulated
    */
    public void search(CharSequence searchBuffer) {
        initializeSearch(searchBuffer, 0, data.length);
    }

    /**Executes a search process to find a match among the cached data, using the content in the search buffer;
    *beginning at the value of start,
    *through the value of end (exclusive).
    *@return a number between 0 and length of data if a match is found,
    *and -1 otherwise.
    */
    private int findMatch() {
        int matchIndex = -1;
        String searchString = SEARCH_BUFFER.toString().toLowerCase();
        if (data == null || data.length == 0 || searchString.isEmpty())
            return matchIndex;
        try {
            searchTask = new DataSearchTask(data, compile(searchString), start-1, start, end, averageWorkLoad);
            gplayer.com.util.Duo<Integer, Matcher> d = searchTask.invoke();
            matchIndex = d.getKey();
        }
        catch (Exception ex) {}
        return matchIndex;
    }

    @Override
    protected Task<Integer> createTask() {
        return new Task<Integer>() {
            @Override
            protected Integer call() {
                int index = findMatch();
                if (index != -1)
                    setMatchIndex(index);
                return index;
            }
        };
    }

    @Override
    protected void cancelled() {
        if (searchTask != null)
            searchTask.cancel(true);
        super.cancelled();
    }

}