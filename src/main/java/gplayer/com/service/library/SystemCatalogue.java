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

import gplayer.com.service.enumconst.FileOpenOption;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;

/**Catalogs system files
*into items within a TreeView.
*@author Ganiyu Emilandu
*/

class SystemCatalogue extends CheckBoxTreeItem<Path> implements Catalogue {
    private boolean isLeaf;
    private boolean firstTimeLeaf = true;
    private boolean firstTimeChildren = true;
    private boolean firstTimeList = true;
    private LinkedList<File> playlist = new LinkedList<>();
    private LinkedList<File> allPlaylists = new LinkedList<>();

    /**Constructor
    *@param path
    *the path to the system file.
    */
    SystemCatalogue(Path path) {
        super(path);
    }

    @Override
    public Path getPath() {
        return getValue();
    }

    @Override
    public Path getPathChain() {
        if (!MediaLibrary.isValidPath(getPath()))
            return java.nio.file.Paths.get("");
        return getPath();
    }

    @Override
    public String getName() {
        if (!MediaLibrary.isValidPath(getPath()))
            return MediaLibrary.resolvePathName(getPath());
        return MediaLibrary.getFileName(getPath());
    }

    @Override
    public LinkedList<File> getPlaylist() {
        if (firstTimeChildren)
            getChildren();
        return playlist;
    }

    @Override
    public LinkedList<File> getAllPlaylists() {
        if (firstTimeList) {
            firstTimeList = false;
            if (!isLeaf()) {
                String name = MediaLibrary.libraryContent[2];
                if (name.equals(MediaLibrary.libraryContent[0]))
                    allPlaylists.addAll(fetchFiles(FileOpenOption.AUDIOFOLDER));
                else if (name.equals(MediaLibrary.libraryContent[1]))
                    allPlaylists.addAll(fetchFiles(FileOpenOption.VIDEOFOLDER));
                else
                    allPlaylists.addAll(fetchFiles(FileOpenOption.FILEFOLDER));
            }
        }
        return allPlaylists;
    }

    @Override
    public boolean hasCachedChildren() {
        return !isLeaf() && !firstTimeChildren;
    }

    @Override
    public void playPlaylist() {
        if (!isLeaf())
            gplayer.com.exec.GPlayer.getSceneRoot().transitionSystemPlayList(getAllPlaylists(), validatePath().toFile());
        else
            gplayer.com.exec.GPlayer.getSceneRoot().openFile(getPath().toFile(), getPath().toFile());
    }

    @Override
    public boolean isLeaf() {
        if (firstTimeLeaf) {
            firstTimeLeaf = false;
            Path path = getValue();
            isLeaf = Files.isRegularFile(path);
        }
        return isLeaf;
    }

    @Override
    public ObservableList<TreeItem<Path>> getChildren() {
        if (firstTimeChildren) {
            firstTimeChildren = false;
            super.getChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
    }

    @SuppressWarnings("unchecked")
    private ObservableList<TreeItem<Path>> buildChildren(CheckBoxTreeItem<Path> item) {
        if (!MediaLibrary.isValidPath(item.getValue()))
            return FXCollections.observableArrayList(new SystemCatalogue(MediaLibrary.baseRoot));
        Path path = (item != null)? item.getValue(): null;
        if (path != null && Files.isDirectory(path)) {
            try(Stream<Path> stream = Files.list(path).sorted((a, b) -> compare(a, b))) {
                return getSupportedFiles(stream).map(p -> new SystemCatalogue(p)).collect(Collectors.toCollection(() -> FXCollections.observableArrayList()));
            }
            catch (Exception ex) {}
        }
        return FXCollections.emptyObservableList();
    }

    private Stream<Path> getSupportedFiles(Stream<Path> stream) {
        String name = MediaLibrary.libraryContent[2];
        return stream.filter((path) -> {
            if (Files.isDirectory(path))
                return true;
            boolean supported = false;
            if (name != null) {
                if (name.equals(MediaLibrary.libraryContent[0]))
                    supported = gplayer.com.service.FileMedia.ofAudioExtensions(MediaLibrary.getFileName(path));
                else if (name.equals(MediaLibrary.libraryContent[1]))
                    supported = gplayer.com.service.FileMedia.ofVideoExtensions(MediaLibrary.getFileName(path));
                else
                    supported = gplayer.com.service.FileMedia.ofAllExtensions(MediaLibrary.getFileName(path));
            }
            if (supported)
                playlist.add(path.toFile());
            return supported;
        });
    }

    private int compare(Path a, Path b) {
        boolean ba = Files.isDirectory(a), bb = Files.isDirectory(b);
        if (ba || bb) {
            if (ba && bb)
                return a.compareTo(b);
            else
                return (ba)? -1: 1;
        }
        return a.compareTo(b);
    }

    private java.util.List<File> fetchFiles(FileOpenOption option) {
        return new gplayer.com.service.FolderChooser(validatePath().toFile(), option).fetchFolderItems();
    }

    private Path validatePath() {
        return (MediaLibrary.isValidPath(getValue()))? getPath(): MediaLibrary.baseRoot;
    }

    @Override
    public boolean equals(Object object) {
        return equals(this, object);
    }
}