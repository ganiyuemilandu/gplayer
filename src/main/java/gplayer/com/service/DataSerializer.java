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

import gplayer.com.service.enumconst.FileSortParameter;
import gplayer.com.service.enumconst.FileFilter;
import gplayer.com.util.Trio;
import static gplayer.com.util.Utility.ofEqualContent;
import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import static java.util.Objects.requireNonNull;
import java.util.TreeMap;
import javafx.util.Duration;

/**Facilitates info serialization
*for future recovery
*by encapsulating various serializable objects
*and implementing Serializable interface
*@author Ganiyu Emilandu
*/

public class DataSerializer implements Serializable {
    boolean isMarked;
    int filePosition;
    LinkedList<File> playListEntry;
    LinkedList<File> recentPlayList, mostRecentPlayList;
    LinkedList<File> playedPlayList;
    LinkedList<File> mostPlayedPlayList;
    LinkedList<File> defaultPlayingPlayList;
    LinkedList<File> favouritePlayList;
    ArrayList<Duration> durationTime;
    ArrayList<Integer> timesPlayed;
    LinkedHashMap<String, Trio<File, Duration, Boolean>> mediaDurationMap;
    LinkedHashMap<String, Trio<String, LinkedList<File>, String>> createdPlayListMap;
    Map<String, Trio<LinkedList<File>, FileFilter, FileSortParameter>> playListContents;
    File videoFileFolder;
    File audioFileFolder;
    File filesFileFolder;
    File currentFile;
    FileSortParameter PARAMETER;
    DataSerializer(boolean im, int fp, LinkedList<File> ple, LinkedList<File> rpl, LinkedList<File> mrpl, LinkedList<File> ppl, LinkedList<File> mppl, LinkedList<File> dppl, LinkedList<File> fpl, ArrayList<Duration> dt, ArrayList<Integer> tp, LinkedHashMap<String, Trio<File, Duration, Boolean>> mdm, LinkedHashMap<String, Trio<String, LinkedList<File>, String>> cplm, Map<String, Trio<LinkedList<File>, FileFilter, FileSortParameter>> plc, File vff, File aff, File fff, FileSortParameter PAR) {
        isMarked = im;
        filePosition = fp;
        playListEntry = requireNonNull(ple);
        recentPlayList = requireNonNull(rpl);
        mostRecentPlayList = requireNonNull(mrpl);
        playedPlayList = requireNonNull(ppl);
        mostPlayedPlayList = requireNonNull(mppl);
        defaultPlayingPlayList = requireNonNull(dppl);
        favouritePlayList = requireNonNull(fpl);
        durationTime = requireNonNull(dt);
        timesPlayed = requireNonNull(tp);
        mediaDurationMap = requireNonNull(mdm);
        createdPlayListMap = requireNonNull(cplm);
        playListContents = requireNonNull(plc);
        videoFileFolder = vff;
        audioFileFolder = aff;
        filesFileFolder = fff;
        PARAMETER = requireNonNull(PAR);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (object instanceof DataSerializer) {
            DataSerializer o = (DataSerializer) object;
            if (isMarked != o.isMarked)
                return false;
            else if (filePosition != o.filePosition)
                return false;
            else if (PARAMETER != o.PARAMETER)
                return false;
            else if (!(ofEqualContent(videoFileFolder, o.videoFileFolder) && ofEqualContent(audioFileFolder, o.audioFileFolder) && ofEqualContent(filesFileFolder, o.filesFileFolder)))
                return false;
            else if (!(ofEqualContent(playListEntry, o.playListEntry) && ofEqualContent(recentPlayList, o.recentPlayList) && ofEqualContent(mostRecentPlayList, o.mostRecentPlayList) && ofEqualContent(playedPlayList, o.playedPlayList) && ofEqualContent(mostPlayedPlayList, o.mostPlayedPlayList) && ofEqualContent(defaultPlayingPlayList, o.defaultPlayingPlayList) && ofEqualContent(favouritePlayList, o.favouritePlayList)))
                return false;
            else if (!(ofEqualContent(durationTime, o.durationTime) && ofEqualContent(timesPlayed, o.timesPlayed)))
                return false;
            else if (!(ofEqualContent(mediaDurationMap, o.mediaDurationMap) && ofEqualContent(createdPlayListMap, o.createdPlayListMap) && ofEqualContent(playListContents, o.playListContents)))
                return false;
            return true;
        }
        return false;
    }
}