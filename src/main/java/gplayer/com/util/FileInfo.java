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
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**Retrieves and encapsulates passed file info
*@author Ganiyu Emilandu
*/
public class FileInfo {
    private File file;
    private String fileName;
    private BasicFileAttributes attribute;
    private String name, type, location;

    public FileInfo(File file) {  //Constructor
        this.file = file;
        try {
            instantiateStrings();
            attribute = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        }
        catch (Exception ex) {
            fileName = name = type = location = "...";
        }
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fileName;
    }

    public String getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public int getPlayFrequency() {
        return (file == null || gplayer.com.exec.GPlayer.getSceneRoot() == null)? 0: gplayer.com.exec.GPlayer.getSceneRoot().getPlayFrequency(file);
    }

    public long getSize() {
        return (attribute == null)? 0: attribute.size();
    }

    public FileTime getLastModifiedTime() {
        return (attribute == null)? null: attribute.lastModifiedTime();
    }

    public FileTime getLastAccessTime() {
        return (attribute == null)? null: attribute.lastAccessTime();
    }

    public FileTime getCreationTime() {
        return (attribute == null)? null: attribute.creationTime();
    }

    private void instantiateStrings() {
        fileName = gplayer.com.service.FileMedia.fileToString(file);
        name = Utility.postCharacterString(fileName, '.', false);
        type = Utility.postCharacterString(fileName, '.', true);
        location = file.getParent();
    }
}