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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javafx.application.Platform.runLater;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.CheckBoxTreeItem;

/**Skeletal framework for cataloging app and user-configured playlists
*into TreeItems within a TreeView.
*@author Ganiyu Emilandu
*/

abstract class PlaylistCatalogue extends CheckBoxTreeItem<Path> implements Catalogue {
    private boolean isLeaf;
    private boolean firstTimeLeaf = true;
    private boolean firstTimeChildren = true;
    private boolean firstTimeList = true;
    private static char separator = File.separatorChar;
    private Path pathChain;
    int index;
    private LinkedList<File> playlist = new LinkedList<>();
    private LinkedList<File> allPlaylists = new LinkedList<>();

    /**Constructor
    *@param path
    *the unique identifier of this object
    */
    PlaylistCatalogue(Path path) {
        super(path);
    }

    /**Constructor
    *@param path
    *the unique identifier of this object.
    *@param index
    *the level of this object within a TreeView
    */
    PlaylistCatalogue(Path path, int index) {
        this(path);
        this.index = index;
    }

    /**Returns the unique identifier of this object.*/
    @Override
    public Path getPath() {
        return getValue();
    }

    @Override
    public Path getPathChain() {
        return constructPathChain();
    }

    @Override
    public String getName() {
        return MediaLibrary.resolvePathName(getPath());
    }

    @Override
    public LinkedList<File> getPlaylist() {
        if (firstTimeChildren) {
            java.util.List<File> list = gplayer.com.service.FileMedia.enlist(getChildren(), ((child) -> MediaLibrary.isValidPath(child.getValue())), ((child) -> child.getValue().toFile()));
            playlist.addAll(list);
        }
        return playlist;
    }

    @Override
    public LinkedList<File> getAllPlaylists() {
        if (firstTimeList) {
            firstTimeList = false;
            allPlaylists.addAll(getPlaylist());
            getChildren().forEach((item) -> {
                PlaylistCatalogue pc = (PlaylistCatalogue) item;
                if (!pc.isLeaf() && pc.traverseChildContents(index)) {
                    if (pc.index != pc.getMap().size() || !(pc.getName().equals(MediaLibrary.libraryContent[0]) || pc.getName().equals(MediaLibrary.libraryContent[1]))) {
                        LinkedList<File> ll = pc.getAllPlaylists();
                        allPlaylists.addAll(gplayer.com.service.FileMedia.uniqueElements(allPlaylists, ll));
                    }
                }
            });
        }
        return allPlaylists;
    }

    @Override
    public boolean hasCachedChildren() {
        return !isLeaf() && !firstTimeChildren;
    }

    @Override
    public boolean isLeaf() {
        if (firstTimeLeaf) {
            firstTimeLeaf = false;
            isLeaf = index > getMap().size();
        }
        return isLeaf;
    }

    @Override
    public ObservableList<TreeItem<Path>> getChildren() {
        if (firstTimeChildren) {
            firstTimeChildren = false;
            super.getChildren().setAll(buildChildren(this, index));
        }
        return super.getChildren();
    }

    ObservableList<TreeItem<Path>> buildChildren(TreeItem<Path> item, int index) {
        int size = getMap().size();
        if (index < size)
            return getMap().get(index).stream().map((str) -> newInstance(Paths.get(str), index+1)).collect(Collectors.toCollection(() -> FXCollections.observableArrayList()));
        else if (index == size)
            return playlistContents(item, item.getParent());
        else
            return FXCollections.emptyObservableList();
    }

    Stream<java.io.File> getFilteredStream(Stream<java.io.File> stream, String name) {
        if (name.equals(MediaLibrary.libraryContent[2]))
            return stream;
        boolean audio = name.equals(MediaLibrary.libraryContent[0]);
        return stream.filter((file) -> {
            if (audio)
                return gplayer.com.service.FileMedia.ofAudioExtensions(file.getPath());
            else
                return gplayer.com.service.FileMedia.ofVideoExtensions(file.getPath());
        });
    }

    void playPlaylist(LinkedList<File> ll, String name, boolean allowEmptyPlaylist) {
        runLater(() -> gplayer.com.exec.GPlayer.getSceneRoot().transitionAppPlayList(ll, name, allowEmptyPlaylist));
    }

    void playPlaylist(LinkedList<File> ll, String name, int index, boolean allowEmptyPlaylist) {
        runLater(() -> gplayer.com.exec.GPlayer.getSceneRoot().transitionUserPlayList(ll, name, index, allowEmptyPlaylist));
    }

    void playFile(File file) {
        runLater(() -> gplayer.com.exec.GPlayer.getSceneRoot().openFile(null, file));
    }

    private Path constructPathChain() {
        if (pathChain != null)
            return pathChain;
        String name = getName();
        pathChain = Paths.get(name);
        if (getParent() != null)
            pathChain = MediaLibrary.getPathChain(getParent()).resolve(name);
        return pathChain;
    }

    private boolean traverseChildContents(int index) {
        if (this instanceof AppPlaylistCatalogue && index == 0)
            return getName().equals(MediaLibrary.resource.getString("appPlaylistContent.array", 0)) || getName().equals(MediaLibrary.resource.getString("appPlaylistContent.array", 3));
        return true;
    }

    abstract ObservableList<TreeItem<Path>> playlistContents(TreeItem<Path> child, TreeItem<Path> parent);

    abstract java.util.Map<Integer, java.util.List<String>> getMap();

    abstract PlaylistCatalogue newInstance(Path path, int index);

    @Override
    public boolean equals(Object object) {
        return equals(this, object);
    }
}