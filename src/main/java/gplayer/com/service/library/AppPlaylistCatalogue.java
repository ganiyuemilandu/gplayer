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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.CheckBoxTreeItem;

/**Catalogs app-configured playlists,
*e.g, recent, most played,favourite, etc.
*into items within a tree view.
*@author Ganiyu Emilandu
*/

class AppPlaylistCatalogue extends PlaylistCatalogue {

    /**Constructor
    *@param path
    *the unique identifier of this object
    */
    AppPlaylistCatalogue(Path path) {
        super(path);
    }

    /**Constructor
    *@param path
    *the unique identifier of this object.
    *@param index
    *the level of this object within a TreeView
    */
    AppPlaylistCatalogue(Path path, int index) {
        super(path, index);
    }

    @Override
    public void playPlaylist() {
        if (!isLeaf()) {
            String name = null;
            Path path = getPathChain();
            if (path.getNameCount() > 1)
                name = MediaLibrary.resolvePathName(path.getName(1));
            playPlaylist(getPlaylist(), name, index < 2);
        }
        else
            playFile(getPath().toFile());
    }

    @Override
    ObservableList<TreeItem<Path>> playlistContents(TreeItem<Path> child, TreeItem<Path> parent) {
        int index = getMap().get(0).indexOf(parent.getValue().toString());
        String childName = MediaLibrary.resolvePathName(child.getValue());
        String parentName = gplayer.com.service.FileMedia.DEFAULT_PLAYLIST_NAMES.get(index) + gplayer.com.service.FileMedia.DEFAULT_PLAYLIST_EXTENSION;
        java.util.List<File> list = gplayer.com.exec.GPlayer.getSceneRoot().getPlayList(parentName);
        Stream<File> stream = list.stream();
        stream = getFilteredStream(stream, childName);
        return stream.map((file) -> new AppPlaylistCatalogue(file.toPath(), getMap().size() + 1)).collect(Collectors.toCollection(() -> FXCollections.observableArrayList()));
    }

    /**Returns the base map for the app-configured playlist catalog.*/
    @Override
    java.util.Map<Integer, java.util.List<String>> getMap() {
        return MediaLibrary.appPlaylist;
    }

    /**Creates and returns a new instance of this object.
    *@param path
    *the unique identifier of the prospective object.
    *@param index
    *the level of the prospective object within a TreeView
    */
    @Override
    PlaylistCatalogue newInstance(Path path, int index) {
        return new AppPlaylistCatalogue(path, index);
    }
}