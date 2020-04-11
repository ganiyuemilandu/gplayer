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

package gplayer.com.service.library;

import static gplayer.com.util.Utility.ofEqualContent;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;

/**Outlines a framework for cataloging a TreeItem
*within a TreeView of TreeItem<Path> objects.
*@author Ganiyu Emilandu
*/

interface Catalogue {
    /**Returns the path to this tree item.
    *Should return the same result as calling getValue() on the tree item.
    */
    public Path getPath();
    /**Returns a hierarchy of paths, encapsulating a chain of paths from the root parent of this tree item to the item itself.*/
    public Path getPathChain();
    /**Returns a String encapsulating the name of the farthest path from the root of this tree item path.*/
    public String getName();
    /**Returns a list of all leaf TreeItem paths of this tree item direct/immediate descent.*/
    public LinkedList<File> getPlaylist();
    /**Returns a list of all leaf TreeItem paths of this tree item descent.*/
    public LinkedList<File> getAllPlaylists();
    /**Initiates playlist playback.*/
    public void playPlaylist();
    /**Indicates if the children of this tree item (if not a leaf) has been called at least once.*/
    public boolean hasCachedChildren();

    /**Compares 2 items for equality.
    *@param first
    *first of the objects, exclusively of type Catalogue
    *@param second
    *last of the objects, of any type
    *@return true if the passed objects are of the same type (Catalogue), contain the same value, and are of equal descent;
    *return false otherwise.
    */
    default boolean equals(Catalogue first, Object second) {
        if (first == second)
            return true;
        if (second instanceof Catalogue) {
            Path p1 = first.getPathChain();
            Path p2 = ((Catalogue)second).getPathChain();
            return p1.equals(p2);
        }
        return false;
    }
}