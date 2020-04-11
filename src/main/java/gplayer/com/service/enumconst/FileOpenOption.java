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

package gplayer.com.service.enumconst;

/**Defines
*various open-file options
*@author Ganiyu Emilandu
*/

public enum FileOpenOption {
    /**Flags file open option for selecting single audio/video file.*/
    FILE,
    /**Flags file open option for selecting multi audio/video files.*/
    FILES,
    /**Flags directory open option for selecting multi audio/video files.*/
    FILEFOLDER,
    /**Flags file open option for selecting single audio file.*/
    AUDIOFILE,
    /**Flags file open option for selecting multi audio files.*/
    AUDIOFILES,
    /**Flags directory open option for selecting multi audio files.*/
    AUDIOFOLDER,
    /**Flags file open option for selecting single video file.*/
    VIDEOFILE,
    /**Flags file open option for selecting multi video files.*/
    VIDEOFILES,
    /**Flags directory open option for selecting multi video files.*/
    VIDEOFOLDER,
}