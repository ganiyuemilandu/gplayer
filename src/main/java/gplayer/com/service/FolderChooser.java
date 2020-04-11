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

import gplayer.com.exec.GPlayer;
import gplayer.com.service.enumconst.FileOpenOption;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**Facilitates directory traversal
*by opening up a DirectoryChooser dialog
*for a selection to be made by the user,
*After which all supported files are retrieved from the selected directory
*and added to user playlist
*@author Ganiyu Emilandu
*/

public class FolderChooser {
    //A Logger instance
    private Logger logger = GPlayer.LOGGER;
    //A directory chooser object
    private DirectoryChooser directoryChooser;
    //Owner window
    private Stage stage;
//For storing retrieved files
private java.util.List<File> folderItems;
    //Directory path
    public Path path;
    //File open option
    FileOpenOption OPENOPTION;

    /**constructor
    *@param file
    *an existing directory
    *@param OPTION
    *outlines the group of files to retrieve
    *@throws NullPointerException
    *if file is null
    *@throws IllegalArgumentException
    *if the directory cannot be found
    *or if file isn't a directory
    */
    public FolderChooser(File file, FileOpenOption OPTION) {
        validateDirectory(file);
        //Encapsulate file into a path object
        path = file.toPath();
        OPENOPTION = OPTION;
    }

    /**Overoaded constructor.
    *@param file
    *an existing directory
    *@throws NullPointerException
    *if file is null
    *@throws IllegalArgumentException
    *if the directory cannot be found
    *or if file isn't a directory
    */
    public FolderChooser(File file) {
        this(file, FileOpenOption.FILEFOLDER);
    }

    /**Overloaded constructor
    *@param FILEOPENOPTION
    *yardstick for which files to retrieve from selected directory
    *@param stage
    *the window on which the DirectoryChooser dialog would be displayed
    *@param file
    *a directory to open to
    *@throws NullPointerException
    *if stage is null
    */
    public FolderChooser(FileOpenOption FILEOPENOPTION, Stage stage, File file) {
        if (stage == null)
            throw new NullPointerException("Stage cannot be null");
        directoryChooser = new DirectoryChooser();
        OPENOPTION = FILEOPENOPTION;
        java.util.function.Consumer<String> initiator = ((title) -> {
            directoryChooser.setTitle(title);
            if (file != null)
                directoryChooser.setInitialDirectory(file);
        });
        switch(FILEOPENOPTION) {
                //Ascertain the invoker of this constructor
            case FILEFOLDER:
                initiator.accept("Select a folder to play media files");  //All supported media files are regarded
                break;
            case AUDIOFOLDER:
                initiator.accept("Select a folder to play audio files");  //Only supported audio files are regarded
                break;
            case VIDEOFOLDER:
                initiator.accept("Select a folder to play video files");  //Only supported video files are regarded
                break;
            default:  //Unreachable line of code!
                throw new IllegalArgumentException();
        }
        //Save the selected folder in path
        path = directoryChooser.showDialog(stage).toPath();
    }

    /**Traverses selected directory
    *so as to retrieve all supported/required files
    *@param FILEOPENOPTION
    *yardstick for which files to retrieve from selected directory
    */
    private ArrayList<File> getSupportedFiles(FileOpenOption FILEOPENOPTION) {
        logger.info("About to retrieve supported files in the selected folder");
        ArrayList<File> array = new ArrayList<>();
        try {  //If the folder can be traversed
            //Begin file retrieval
            long startTime = System.nanoTime();
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {  //An anonymous inner class
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attribute) {
                    //Get a string representation of each file extension
                    String extension = FileMedia.fileToString(path.toFile(), '.');
                    switch (FILEOPENOPTION) {
                        case FILEFOLDER:
                            if (FileMedia.ofAllExtensions(extension))
                                //Add to array collection
                                array.add(path.toFile());
                            break;
                        case AUDIOFOLDER:
                            if (FileMedia.ofAudioExtensions(extension))
                                //Add to array collection
                                array.add(path.toFile());
                            break;
                        case VIDEOFOLDER:
                            if (FileMedia.ofVideoExtensions(extension))
                                //Add to array collection
                                array.add(path.toFile());
                            break;
                        default:
                            //Add to array collection
                            array.add(path.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException io) {
                    if (io instanceof java.nio.file.AccessDeniedException)
                        return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }
                //}
            });
            logger.info("Retrieved " + array.size() + " files in: " + (System.nanoTime() - startTime));
        }
        //If the folder is not traversable
        catch (IOException ex) {
            logger.log(Level.INFO, "Couldn't traverse selected directory", ex);
        }
        return array;
    }

    /**Gets and returns all files retrieved from selected directory
    *@return a list of all the files
    */
    public java.util.List<File> fetchFolderItems() {
        folderItems = (folderItems == null)? getSupportedFiles(OPENOPTION): folderItems;
        return folderItems;
    }

    private void validateDirectory(File file) {
        if (file == null)
            throw new NullPointerException("Directory cannot be null");
        if (!file.exists())
            throw new IllegalArgumentException("Directory does not exist");
        if (!file.isDirectory())
            throw new IllegalArgumentException("Directory required; file passed");
    }
}