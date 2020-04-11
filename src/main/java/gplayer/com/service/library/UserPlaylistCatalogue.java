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

/**Catalogs user-created playlists
*into items within a tree view.
*@author Ganiyu Emilandu
*/

class UserPlaylistCatalogue extends PlaylistCatalogue {
    /**Constructor
    *@param path
    *the unique identifier of this object.
    */
    UserPlaylistCatalogue(Path path) {
        super(path);
    }

    /**Constructor
    *@param path
    *the unique identifier of this object.
    *@param index
    *the level of this object within a TreeView
    */
    UserPlaylistCatalogue(Path path, int index) {
        super(path, index);
    }

    @Override
    public void playPlaylist() {
        if (!isLeaf()) {
            int playIndex = 0;
            Path path = getPathChain();
            int nameCount = path.getNameCount();
            String name = null;
            java.util.List<String> list = null;
            if (nameCount > 1) {
                name = path.getName(1).toString().concat(MediaLibrary.PATH_EXTENSION);
                list = getMap().get(0);
                playIndex = list.indexOf(name);
            }
            list = getMap().get(playIndex + 1);
            if (!list.isEmpty()) {
                if (name != null && nameCount > 2)
                    name = path.getName(2).toString();
                else
                    name = list.get(list.size()-1).replace(MediaLibrary.PATH_EXTENSION, "");
            }
            playPlaylist(getPlaylist(), name, playIndex, index <= 3);
        }
        else
            playFile(getPath().toFile());
    }

    @Override
    ObservableList<TreeItem<Path>> buildChildren(TreeItem<Path> item, int index) {
        if (index == 1) {
            java.util.List<String> list = getMap().get(0);
            String pathName = MediaLibrary.getFileName(item.getValue());
            int i = list.indexOf(pathName), size = list.size();
            return getMap().get(i + 1).stream().map((path) -> new UserPlaylistCatalogue(Paths.get(path), size + 1)).collect(Collectors.toCollection(() -> FXCollections.observableArrayList()));
        }
        else
            return super.buildChildren(item, index);
    }

    @Override
    ObservableList<TreeItem<Path>> playlistContents(TreeItem<Path> child, TreeItem<Path> parent) {
        String childName = MediaLibrary.resolvePathName(child.getValue());
        String parentName = MediaLibrary.resolvePathName(parent.getValue());
        String grandParentName = MediaLibrary.getFileName(parent.getParent().getValue());
        java.util.List<String> list = getMap().get(0);
        int index = list.indexOf(grandParentName);
        Stream<File> stream = gplayer.com.exec.GPlayer.getSceneRoot().getMapPlayList(parentName, index).stream();
        stream = getFilteredStream(stream, childName);
        return stream.map((file) -> new UserPlaylistCatalogue(file.toPath(), getMap().size() + 1)).collect(Collectors.toCollection(() -> FXCollections.observableArrayList()));
    }

    /**Returns the base map for the user-configured playlist catalog.*/
    @Override
    java.util.Map<Integer, java.util.List<String>> getMap() {
        return MediaLibrary.userPlaylist;
    }

    /**Creates and returns a new instance of this object.
    *@param path
    *the unique identifier of the prospective object.
    *@param index
    *the level of the prospective object within a TreeView
    */
    @Override
    PlaylistCatalogue newInstance(Path path, int index) {
        return new UserPlaylistCatalogue(path, index);
    }
}