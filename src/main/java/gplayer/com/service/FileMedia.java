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
import gplayer.com.lang.BaseResourcePacket;
import gplayer.com.prefs.*;
import gplayer.com.service.enumconst.*;
import gplayer.com.service.library.FileManager;
import gplayer.com.service.library.MediaLibrary;
import gplayer.com.util.*;

import java.io.File;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.UUID;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import javafx.application.Platform;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;

import javafx.scene.chart.NumberAxis;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.PopupControl;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.scene.media.Media;
import javafx.scene.media.MediaErrorEvent;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Popup;
import javafx.stage.Stage;

import javafx.util.Duration;

/**The scene root of GPlayer app window.
*Defines most of the control functionalities for the player,
*and populates the scene graph with the various controls
*necessary for media playback.
*@author Ganiyu Emilandu
*/

public class FileMedia extends BorderPane {
    public static final BaseResourcePacket resource = BaseResourcePacket.getPacket();
    private PlayListSearch playListSearch = null;  //For initiating search among files
    private final GPlayerPreferences preferences = GPlayer.PREFERENCES;  //Media playback registry node
    private Logger logger = GPlayer.LOGGER;  //records app exceptions and media info
    private Property<Duration> durationProperty = new Property<>();
    private Property<File> fileProperty = new Property<>();  //Keeps tabs on files being played
    private Property<List<File>> playListProperty = new Property<>();  //Keeps tabs on playlist transitions
    private FileSortParameter PARAMETER;
    private Alert mediaErrorAlert;  //To alert the user to a media error
    private Alert deleteFileAlert;  //Prompts user to confirm file deletion
    private Alert fileNotFoundAlert;  //Prompts user to an unavailable file
    //Declare button references
    private Button play_pause, stop, previous, next, textButton;  //for controlling media playback
    private Button previous_next;  //Reflects which of either previous or next button was last pressed
    private CheckBox repeat;  //For toggling repeat on or off
    private Duration duration;  //Required to hold the time duration of a playing media
    private Duration mediaPosition = Duration.millis(0.0), markedPlace = Duration.millis(0.0), lastMarkedTime = Duration.millis(0.0);
    private VBox controlNode;  //A layout container for the various buttons, labels and sliders
    private HBox libraryPane;
    private Node[] traversableNodes;
    private Image playImage = iconify(resource.getString("play.icon"));  //Display icon for the play button
    private Image pauseImage = iconify(resource.getString("pause.icon"));  //Display icon for the pause button
    private Image image;
    private Label playTime, spacer, sliderLabel, timeLabel, volumeLabel, label;  //Declare label references for displaying respective texts
    private final ObservableList<File> playListView = FXCollections.observableArrayList();  //For storing files in the playlist currently being played
    private ListView<File> listView;  //For displaying the files in playListView
    private DataSearch<File> listViewSearch = new DataSearch<>(playListView, ((file) -> file.getName()));  //For searching for a match among listView contents in relation to the key-typed characters, while listView is in focus
    private Media media;  //Declares a Media reference for encapsulating files for playback
    private MediaPlayer mediaPlayer = null;  //Declares a media player reference for playing media objects
    private final MediaView mediaView = configureMediaView();  //for displaying media visuals
    private final Group mediaViewHost = new Group(mediaView);
    private MenuBar menuBar;  //A container to hold and display the various menus and menu items
    //Declare MenuItem references
    private MenuItem exitApplication, quitApplication, quitFile, rewind, jumpForward, jumpForwardByTime, jumpBackward, jumpBackwardByTime, jumpToEnd, jumpToTime, speedUp, speedDown, normalise, fullScreen, previousItem, nextItem, play_pauseItem, stopItem, mediaPositionMarker, mediaMarkerSearch, mediaMarkerEdit, nextMediaMarker, previousMediaMarker;  //For controlling media playback
    private CheckMenuItem quitApplicationOnMediaEnd, quitApplicationOnPlayListEnd, recallMediaStopPosition, backwardPlay;  //Dictate action to take at the end of media or playlist
    private MenuItem openFile, openFiles, openFileFolder, openAudioFile, openAudioFiles, openAudioFolder, openVideoFile, openVideoFiles, openVideoFolder, openPlayList, openLastSession, openLastPlayedFile, openRecentPlayList, openMostPlayedPlayList, openUnfilteredPlayList, openFavouritePlayList, openPlayListSearch, openPreviousPlayListSearch, openNextPlayListSearch, openGoToPlayList, openPreviousGoToPlayList, previousGoToPlayList, nextGoToPlayList, favouriteMenuItem, newPlayList, deleteCurrentFile;  //For opening files for play
    private Menu myPlayList;  //Holds playlists created by user
    private Menu availableMediaMarks;  //Displays available mediamarks for playing file as menu items
    private Menu playListOptions;
    //Declare some RadioMenuItem references
    private RadioMenuItem sortDefault, sortAscending, sortDescending, sortShuffle;  //For determining play list ordering
    private RadioMenuItem containsSearchString, beginsSearchString, endsSearchString;  //For determining the parameter with which a search is to be conducted
    private RadioMenuItem defaultAudioBalance, leftAudioBalance, rightAudioBalance, audioMute;  //For determining sound outlets/channels
    private Slider timeSlider, volumeSlider;  //Sliders for controlling media seek time, and volume respectively
    private ToggleButton libraryToggle = createToggle();  //For showing and hiding playlist catalog
    private String currentFileInfo;  //Holds focused file info
    //Boolean variables employed to determine various courses of actions during media playback
    private boolean muteAudio = false, isMarked = false, isValueChanging = false, isPlaying = true, isPaused = false, isDisabled = false, wasPaused = false, endOfFile = false, isAudio = true;
    private boolean checkedUpdate, sortUnfilteredPlayList, isMediaReady, isDialogShowing, isPopupShowing, isPlayedPlayListOn, autoPlay, multipleSelection, markMediaStopPosition, encounteredMediaError, hasValidArgument;
    static boolean isInitialized = false, goToNext = true, isGoTo = false, runValue = false, newSearch = false;
    private volatile boolean flagOff = false, playListViewSelection = false;
    private final java.awt.Robot ROBOT = createRobot();
    private static int jumpPosition = -1;
    private final ObservableList<String> exitTimeOptions = FXCollections.observableArrayList();  //Available options displayed to user on exit dialog
    public static final ObservableList<String> DEFAULT_PLAYLIST_NAMES = FXCollections.observableArrayList(resource.getStringArray("DEFAULT_PLAYLIST_NAMES.content.array"));  //Names of app constructed playlist
    private String exitTime = "...";
    private ContextMenu contextMenu = new ContextMenu();  //Container for menus and menu items displayed to user on right-click
    private File currentFile = null;  //Points to the current playing media
    private File lastPlayedFile = currentFile;  //Points to previously played file
    private File videoFileFolder = null, audioFileFolder = null, filesFileFolder = null;  //Save last user opened directory
    private Thread quitApplicationThread;  //Quits the application after a specified time
    private Thread inputThread;  //Retrieves file arguments sent by other instances of this app
    private String fileName;  //Name of the current Playing file
    private String markedText = null;
    private boolean repeatIsSelected = preferences.getUserPreferences().getBoolean("repeatOn", false);  //True if repeat is on, and false otherwise
    private String stageTitle = GPlayer.stageTitle;
    private String playListID;  //Holds the name of current playing playlist
    private Stage stage;  //References the main window
    private MediaLibrary mediaLibrary;
    private FileManager fileManager;
    private FileChooser fileChooser;  //Facilitates selection of file/files by user
    private LinkedList<File> filteredPlayList = new LinkedList<>(), unfilteredPlayList = new LinkedList<>();
    private LinkedList<File> playingPlayList = new LinkedList<>();  //References focused play list
    private LinkedList<File> recentPlayList;  //Uniquely holds an array of recently played files
    private LinkedList<File> playedPlayList;  //References previously focused play list
    private LinkedList<File> defaultPlayedPlayList = new LinkedList<>();  //References the route source of previously focused play list
    private LinkedList<File> playListEntry;  //Stores every file opened by the user
    private LinkedList<File> mostPlayedPlayList;  //Stores unique files in order of highest played to least played
    private LinkedList<File> defaultPlayingPlayList;  //References the route source of a playing play list
    private LinkedList<File> mostRecentPlayList = new LinkedList<File>();  //References either of the route source of recentPlayList and mostPlayedPlayList
    private LinkedList<File> favouritePlayList;  //Uniquely stores files favourited by user
    private LinkedList<File> nextPlayListSearch = new LinkedList<>();  //References search-result playlist
    private LinkedList<File> previousPlayListSearch = new LinkedList<>();  //References search-result playlist
    private LinkedList<File> durationMatch = new LinkedList<>();  //Stores mediamark-search result
    private LinkedList<File> lastDurationMatch = new LinkedList<>();  //Stores previous mark-search result
    private LinkedHashMap<String, Duo<LinkedList<File>, FileFilter>> searchArrayMap = new LinkedHashMap<>();
    private ArrayList<String> searchArrayString = new ArrayList<>();  //Stores search strings that produce at least a match
    private ArrayList<Integer> timesPlayed;  //Stores the number of times a file has been played
    private ArrayList<Duration> defaultDurationTime = new ArrayList<>(), durationTime = new ArrayList<>(), lastDefaultDurationTime = new ArrayList<>(), lastDurationTime = new ArrayList<>();  //Store marked positions for mediamark-searched files
    private final ArrayList<String> repeatOptions = new ArrayList<>(Arrays.asList(resource.getStringArray("repeat.text.array")));  //Available repeat options
    private TreeMap<Duration, String> durationSet = new TreeMap<>();  //Stores the available marked positions for the current playing media
    private LinkedHashMap<String, Trio<File, Duration, Boolean>> mediaDurationMap;  //A map that stores file paths with their associated mediamarks
    private final TreeMap<String, LinkedList<File>> miscellaneousPlayListMap = new TreeMap<>();
    private Set<Map.Entry<String, Trio<File, Duration, Boolean>>> mediaDurationSet;  //A medium through which the keys and values of  mediaDurationMap can be retrieved
    private LinkedHashMap<String, Trio<String, LinkedList<File>, String>> createdPlayListMap;  //Holds playlists created by user
    private Map<String, LinkedList<File>> allPlayLists = new HashMap<>();
    private Map<String, Trio<LinkedList<File>, FileFilter, FileSortParameter>> playListContents;
    private Map<Menu, Duo<Supplier<List<File>>, java.util.function.BiConsumer<List<File>, Runnable>>> playListOperationsMap = new LinkedHashMap<>();
    private final List<File> errorFiles = new ArrayList<>(), unplayableFiles = new ArrayList<>();
    private int unfilteredPlayListPosition;
    private int searchList = 0, lastSearchList = 0, searchListIntermediate = 0;  //Denote various searchArrayString indexes
    private int lastPlayedFilePosition, lastFilePosition, playedPlayListFilePosition;  //Mark various file positions
    private int intRepeat;  //Marks the number of times the repeat button is pressed
    private static int filePosition;
    private static char separator = File.separatorChar;
    private double speedRate = GPlayerSettings.playBackPrefs().getDouble("speedRate", 1.0);
    private double volumeLevel = preferences.getUserPreferences().getDouble("volumeLevel", 70.0);  //The level to which the volume slider is set at start up
    private String repeatText = preferences.getUserPreferences().get("repeatText", repeatOptions.get(0));
    private double audioBalanceLevel = 0.0;
    private double seekTime = 0.0;
    private double lastMouseMoveTime = 0;
    private Scene scene;
    private java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1), latch2 = new java.util.concurrent.CountDownLatch(1);
    public static final String[] AUDIO_EXTENSIONS = {"*.mp3", "*.wav"};  //Supported audio files
    public static final String[] VIDEO_EXTENSIONS = {"*.mp4", "*.flv"};  //Supported video files
    public static final String[] ALL_EXTENSIONS = Utility.concatArrays(AUDIO_EXTENSIONS, VIDEO_EXTENSIONS);
    public static final String SERIALIZATION_PATH = GPlayer.getResourcePath("file");
    public static final String DEFAULT_PLAYLIST_EXTENSION = UUID.randomUUID().toString(), CREATED_PLAYLIST_EXTENSION = UUID.randomUUID().toString(), SEARCH_PLAYLIST_EXTENSION = UUID.randomUUID().toString(), MISCELLANEOUS_PLAYLIST_EXTENSION = UUID.randomUUID().toString();
    private final static Map<javafx.stage.Window, javafx.stage.Window> displayedPopups = new HashMap<javafx.stage.Window, javafx.stage.Window>() {
        @Override
        public javafx.stage.Window put(javafx.stage.Window key, javafx.stage.Window value) {
            if (key == null || value == null)
                throw new NullPointerException("Null elements are not allowed in this map.");
            return super.put(key, value);
        }
    };


    /**Constructor.*/
    public FileMedia() {
        super();
    }

    /**Constructor
    *@param stage
    *window root and scene container
    *@param scene
    *playback controls container
    */
    public FileMedia(Stage stage, Scene scene) {
        this();
        initialize(stage, scene);
    }


    /**Sets up the scene graph.
    *@param stage
    *window root and scene container
    *@param scene
    *playback controls container
    */
    public void initialize(Stage stage, Scene scene) {
        logger.info("Initializing scene-root controls and affiliates");
        //Retrieve the current system time
        long startTime = System.nanoTime();
        Thread thread = new Thread(() -> {
            configurePlayLists();
            latch2.countDown();
            configureExitProperties();
            awaitLatchCountDown(latch);
            completePlayListsConfiguration();
            java.nio.file.Path watchDirectory = java.nio.file.Paths.get(GPlayer.RESOURCE_PATH);
            FileManager.watch(watchDirectory, ((event) -> {
                if (event.context().toString().equals("input file"))
                    retrieveInput();
            }), FileManager.copyOfWatchEventKinds(2, 3));
        });
        thread.setDaemon(true);
        thread.start();

        this.stage = stage;
        stage.setOnHidden((we) -> GPlayer.fireHideItem(false, false));
        stage.setOnShown((we) -> {
            Platform.setImplicitExit(!GPlayerSettings.programPrefs().getBoolean("implicitExit", true));
            GPlayer.fireHideItem(true, false);
            //Let's check for updates
            if (!checkedUpdate && GPlayerSettings.generalPrefs().getBoolean("autoUpdateCheck", true))
                checkedUpdate = GPlayer.checkUpdates();
        });

        stage.addEventFilter(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, (we) -> {
            if (!Platform.isImplicitExit())
                GPlayer.exitProgram();
        });

        stage.focusedProperty().addListener((listener) -> {
            if (currentFile != null && !isAudioFile(currentFile)) {
                if (!isDialogShowing && !stage.isFocused()) {
                    if (GPlayerSettings.viewPrefs().getBoolean("windowViewOnly", false) && !isAudio && mediaPlayer != null && mediaPlayer.getStatus() == Status.PLAYING) {
                        mediaPlayer.pause();
                        GPlayer.notify(videoPlaybackNotification());
                        wasPaused = true;
                    }
                    else
                        wasPaused = false;
                }
                else if (stage.isFocused()) {
                    if (!isDialogShowing) {
                        verifyPlayStatus(mediaPlayer, currentFile, fileName, (wasPaused && !isMediaReady), Duration.seconds(2));
                        if (GPlayer.platformExiting && wasPaused)
                            seekToMarkedPosition(currentFile);
                        resumePlay(wasPaused);
                    }
                    isDialogShowing = false;
                }
            }
            if (stage.isFocused() && GPlayer.platformExiting)
                GPlayer.platformExiting = false;
        });

        stage.fullScreenProperty().addListener((listener) -> fullScreen.setDisable(isAudio || stage.isFullScreen()));

        this.scene = scene;
        scene.setOnKeyPressed((ke) -> {
            if (isInitialized) {
                Slider slider = setSeekSlider(ke);
                if (playTime != null && new KeyCodeCombination(KeyCode.T).match(ke))
                    requestMomentaryFocus(playTime, Duration.millis(100));
                else if (slider != null)
                    requestMomentaryFocus(slider, Duration.millis((slider == volumeSlider)? 100: 500));
            }
        });

        widthProperty().addListener((listener) -> setMediaViewSize("width"));

        heightProperty().addListener((listener) -> setMediaViewSize("height"));

        playListView.addListener((javafx.collections.ListChangeListener.Change<? extends File> c) -> waitThenRun(Duration.seconds(3), (() -> playListProperty.setValue(new ArrayList<File>(playListView)))));

        fileProperty.valueProperty().addListener((value, oldValue, newValue) -> {
            markMediaStopPosition = markMediaPosition(newValue);
            recallMediaStopPosition.setSelected(markMediaStopPosition);
            configureMediaMarkers(newValue);
            discard(unplayableFiles);
            unplayableFiles.clear();
            if (oldValue != null) {
                lastPlayedFile = oldValue;
                lastPlayedFilePosition = lastFilePosition;
                if (openLastPlayedFile.isDisable())
                    openLastPlayedFile.setDisable(false);
            }
        });

        durationProperty.valueProperty().addListener((value, oldValue, newValue) -> updateMediaMarkEditor(newValue));

        setTop(configureMenuBar());
        logger.info("Completed set-up in: " + (System.nanoTime() - startTime));
    }


    private void configurePlayLists() {
        logger.info("About to deserialize media info");
        try {
            DataSerializer deserialization = GPlayer.deserializeMediaInfo(String.join("/", GPlayer.RESOURCE_PATH, preferences.getUserPreferences().get("serializedFile", "file")));
            audioFileFolder = deserialization.audioFileFolder;
            createdPlayListMap = requireNonNull(deserialization.createdPlayListMap);
            defaultPlayingPlayList = requireNonNull(deserialization.defaultPlayingPlayList);
            durationTime = requireNonNull(deserialization.durationTime);
            favouritePlayList = requireNonNull(deserialization.favouritePlayList);
            filePosition = deserialization.filePosition;
            filesFileFolder = deserialization.filesFileFolder;
            isMarked = deserialization.isMarked;
            mediaDurationMap = requireNonNull(deserialization.mediaDurationMap);
            mostPlayedPlayList = requireNonNull(deserialization.mostPlayedPlayList);
            mostRecentPlayList = requireNonNull(deserialization.mostRecentPlayList);
            PARAMETER = requireNonNull(deserialization.PARAMETER);
            playedPlayList = requireNonNull(deserialization.playedPlayList);
            playListContents = requireNonNull(deserialization.playListContents);
            playListEntry = requireNonNull(deserialization.playListEntry);
            recentPlayList = requireNonNull(deserialization.recentPlayList);
            timesPlayed = requireNonNull(deserialization.timesPlayed);
            videoFileFolder = deserialization.videoFileFolder;
            preferences.getUserPreferences().putBoolean("deserializationSuccess", true);
            logger.info("Successfully deserialized media info");
        }
        //If the required file cannot be found,
        //Or cannot be opened for access
        catch(Exception ex) {
            //Resort to the default
            audioFileFolder = filesFileFolder = videoFileFolder = null;
            createdPlayListMap = new LinkedHashMap<>();
            defaultPlayingPlayList = new LinkedList<>();
            favouritePlayList = new LinkedList<>();
            mediaDurationMap = new LinkedHashMap<>();
            mostPlayedPlayList = new LinkedList<>();
            PARAMETER = FileSortParameter.DEFAULT;
            playedPlayList = new LinkedList<>();
            playListContents = new HashMap<>();
            playListEntry = new LinkedList<File>();
            recentPlayList = new LinkedList<>();
            timesPlayed = new ArrayList<>();
            preferences.getUserPreferences().putBoolean("deserializationSuccess", false);
            logger.log(Level.INFO, "Media info deserialization was unsuccessful.", ex);
        }  //End of the default block
        FileSortParameter.setSortParameter(PARAMETER);
        String lpl = DEFAULT_PLAYLIST_NAMES.get(4);
        //Retrieve the name of the playlist to commence play on
        String string = GPlayerSettings.playListPrefs().get("playList", lpl);
        playListID = (string.equals(lpl))? string: null;
        LinkedList<File> ll = getPlayList(string + DEFAULT_PLAYLIST_EXTENSION, true, false);  //Get the playlist that corresponds with the name in string
        if (ll == null && (ll = getPlayList(string + CREATED_PLAYLIST_EXTENSION, true, false)) == null) {
            ll = playedPlayList;
            playListID = lpl;
        }
        if (!ll.isEmpty() || !playedPlayList.isEmpty()) {
            playingPlayList = (!ll.isEmpty())? ll: playedPlayList;
            if (playingPlayList != playedPlayList) {  //If incoming playlist isn't the same as outgoing playlist
                if (defaultPlayingPlayList == defaultPlayedPlayList) {  //If incoming playlist root source is the same as outgoing playlist root source, however
                    //Ok to assign incoming playlist to outgoing one
                    playingPlayList = playedPlayList;
                    //Then, assign outgoing playlist and affiliates to empty lists
                    playedPlayList = new LinkedList<>();
                    defaultPlayedPlayList = new LinkedList<>();
                    //Revert the positions of filePosition and playedPlayListFilePosition
                    filePosition = playedPlayListFilePosition;
                    playedPlayListFilePosition = 0;
                }
                else
                    PARAMETER = FileSortParameter.DEFAULT;
            }
            else {  //If playedPlayList is the same object with playingPlayList
                //So, it's ok to assign previously played playlist and affiliates to empty lists
                playedPlayList = new LinkedList<>();
                defaultPlayedPlayList = new LinkedList<>();
            }
        }
        if (isMarked) {
            if (!playedPlayList.isEmpty()) {  //Last played playlist isn't the one to be played
                //We need to assign durationMatch to defaultPlayedPlayList
                durationMatch = defaultPlayedPlayList;
                //and set isMarked to false
                isMarked = false;
            }
            else
                durationMatch = defaultPlayingPlayList;
            //Now, we need to get the midpoint of durationTime
            //so we can split its content between defaultDurationTime and durationTime as well.
            //This is necessary because the contents of defaultDurationTime were added to durationTime at the point of saving/serialization
            int midPoint = durationTime.size()/2;
            //The last half is assigned to defaultDurationTime
            defaultDurationTime = new ArrayList<>(durationTime.subList(midPoint, durationTime.size()));
            //and first half to durationTime
            durationTime = new ArrayList<>(durationTime.subList(0, midPoint));
            miscellaneousPlayListMap.put("Media-marked playlist", durationMatch);
        }
        mediaDurationSet = mediaDurationMap.entrySet();
    }


    private void completePlayListsConfiguration() {
        boolean empty = playingPlayList.isEmpty();
        MenuItem[] items = {mediaMarkerSearch, openPlayList, openRecentPlayList, openMostPlayedPlayList, openFavouritePlayList, openPlayListSearch, openGoToPlayList, openPreviousGoToPlayList, favouriteMenuItem};
        boolean[] b = {isEmptyOfUserEntry(), playListEntry.isEmpty(), recentPlayList.isEmpty(), mostPlayedPlayList.isEmpty(), favouritePlayList.isEmpty(), playListEntry.isEmpty(), empty, empty, empty};
        Platform.runLater(() -> {
            configureLibrary();
            for (int i = 0; i < items.length; i++)
                items[i].setDisable(b[i]);
            if (!createdPlayListMap.isEmpty()) {
                Set<String> keys = createdPlayListMap.keySet();
                for (String k: keys)
                    createMyPlayList(k, createdPlayListMap.get(k), playListOptions);
            }
            disableControls(1, true);  //Disables the momentarily irrelevant items
            setSortParameter(PARAMETER, false, false);
        });
    }


    private void awaitLatchCountDown(java.util.concurrent.CountDownLatch latch) {
        try {
            latch.await();
        }
        catch (InterruptedException ex) {}
    }


    private boolean readyPlayResources() {
        awaitLatchCountDown(latch2);
        makeAdjustments(GPlayer.LAST_PROGRAM_EXECUTION_PHASE);
        return true;
    }


    /**Called after stage and scene set-up is complete
    *@param callArgument
    *the argument passed in main()
    */
    public void initializePlay(String... callArgument) {
        boolean playing = false, ready = false;
        if (callArgument != null && callArgument.length > 0) {
            logger.info("Called with arguments");
            ready = readyPlayResources();
            playing = hasValidArgument = playPlayListEntry(true, currentFile, callArgument);
        }
        if (!playing) {
            if (!ready)
                readyPlayResources();
            if (!playingPlayList.isEmpty()) {
                logger.info("Initializing playback");
                isPlaying = false;
                filePosition = (filePosition < 0 || filePosition >= playingPlayList.size())? 0: filePosition;
                transitionPlayList(playedPlayListFilePosition, filePosition, playedPlayList, defaultPlayedPlayList, playingPlayList, defaultPlayingPlayList, FileSortParameter.getSortParameter(), PARAMETER, false);
            }
        }
    }


    /**Gets and returns a list of files
    *in relation to specified path/paths.
    *@param args
    *the path from which the file, (a file), or files, (a directory), is to be retrieved
    *@return a list of files retrieved
    *or an empty list, if no files were retrieved
    */
    private LinkedList<File> retrieveFiles(String... args) {
        File file = null;
        final LinkedList<File> myPlayList = new LinkedList<>();
        //Cycle through the number of arguments
        for (String path: args) {
            file = new File(path);  //Convert the retrieved string to a file
            if (file.exists()) {  //If the converted file location is available
                filesFileFolder = file;
                if (file.isDirectory()) {  //If the file refers to a directory
                    FolderChooser fc = new FolderChooser(file);  //The applicable folderChooser constructor is called to traverse this directory
                    List<File> files = fc.fetchFolderItems();  //The items in the folder are retrieved
                    if (!files.isEmpty())  //If supported files were retrieved successfully
                        myPlayList.addAll(files);
                }  //Closes the file directory expression block
                else {  //If the converted file refers to a file
                    if (ofAllExtensions(fileToString(file, '.')))
                        myPlayList.add(file);
                }
            }  //End of successful file retrieval block
        }  //End of loop
        return myPlayList;
    }


    private void add(List<File> list, List<File> ppl, List<File> dppl, int addIndex) {
        if (list == null || list.isEmpty())
            return;
        List<Integer> indices = fileIndices(ppl, list);
        deleteFiles(ppl, dppl, filePosition, indices);
        //How many indices are less than the insertion point?
        int count = (int) indices.stream().filter((index) -> index < addIndex).count();
        int insertionIndex = addIndex - count;
        ppl.addAll((insertionIndex > ppl.size())? ppl.size(): insertionIndex, list);
        if (ppl != dppl)
            dppl.addAll((insertionIndex > dppl.size())? dppl.size(): insertionIndex, list);
        Platform.runLater(() -> {
            if (ppl == playingPlayList) {
                setSortParameter(FileSortParameter.DEFAULT, false, true);
                displayPlayList(ppl, filePosition);
            }
            else {
                if (ppl == playedPlayList)
                    FileSortParameter.setPriorSortParameter(FileSortParameter.DEFAULT);
                updateMediaInfo(false);
                disablePlayLists(false);
                playListProperty.setValue(new ArrayList<File>(ppl));
            }
        });
    }


    private void add(List<File> list, List<File> ppl, List<File> dppl) {
        add(list, ppl, dppl, ppl.size());
    }


    private void discard(List<File> list, List<File> ppl, List<File> dppl) {
        if (list == null || list.isEmpty() || ppl == null || ppl.isEmpty())
            return;
        int fp = filePosition;
        boolean deleted = deleteFiles(ppl, dppl, fp, fileIndices(ppl, list));
        Platform.runLater(() -> {
            if (!playNextIf(deleted, fp)) {
                disablePlayLists(false);
                playListProperty.setValue(new ArrayList<File>(ppl));
            }
        });
    }


    private void discard(List<File> list) {
        discard(list, playingPlayList, defaultPlayingPlayList);
    }


    private void discard(File... files) {
        discard(Arrays.asList(files), playingPlayList, defaultPlayingPlayList);
    }


    private boolean playPlayListEntry(boolean play, File currentFile, String... args) {
        LinkedList<File> ll = retrieveFiles(args);
        if (isInitialized)
            Platform.runLater(() -> GPlayer.focusStage(stage));
        if (ll.isEmpty())
            return false;
        playListEntry.addAll(uniqueElements(playListEntry, ll));
        if (play) {
            previous_next = next;
            openFiles(filesFileFolder, ll.get(0), 0, filePosition);
        }
        else {
            if (defaultPlayingPlayList == playListEntry) {
                if (playingPlayList != playListEntry)
                    playingPlayList.addAll(uniqueElements(playingPlayList, ll));
                int position = (currentFile == null)? -1: playingPlayList.indexOf(currentFile);
                if (position != -1)
                    filePosition = position;
                displayPlayList(playingPlayList, filePosition);
            }
        }
        return play;
    }


    private void playSubmittedArguments(boolean play) {
        String[] entries = GPlayer.getSubmittedArguments();
        if (entries.length > 0) {
            //Clear input file
            GPlayer.submitArguments(false, null);
            playPlayListEntry(play, currentFile, entries);
        }
    }


    private void configureExitProperties() {
        exitTimeOptions.addAll(GPlayerSettings.exitTimeOptions.subList(1, GPlayerSettings.exitTimeOptions.size()));  
        exitTimeOptions.addAll(Utility.generateHoursStringTimeFormat(0, 1, 1, 2, 3, 5, 10, 15, 20, 25, 30, 35, 40, 45));
        exitTimeOptions.setAll(Utility.sortStringTimeFormat(exitTimeOptions));
        exitTimeOptions.add(0, "...");
    }


    /**Creates and returns an image object from the name specified,
    *with resources/icons as parent directory
    *@param imageName
    *the name of the image file to be sought for in resources/icons directory
    *@return an image object if the file was found, and no exceptions during image construction
    *and null otherwise.
    */
    public static Image iconify(String imageName) {
        try {
            return new Image(FileMedia.class.getResourceAsStream("/resources/icons/" + imageName));
        }
        catch (Exception ex) {
            return null;
        }
    }


    /**Creates and returns a java.io.Inpustream from a specified string,
    *with resources/properties as the parent directory and properties as the type of the string passed.
    *If the string passed does not end with '.properties' extension,
    *a '.properties' string is automatically apended to the end of the string passed.
    *@param name
    *the name of the file to be sought for in resources/properties directory
    *@return an input stream if the file is found,
    *and null otherwise.
    */
    public static java.io.InputStream inputStream(String name) {
        return FileMedia.class.getResourceAsStream("/resources/properties/".concat((Utility.postCharacterString(name, '.').equals("properties"))? name: name + ".properties"));
    }


    private void makeAdjustments(int i) {
        if (i == 1) {
            String path = preferences.getUserPreferences().get("currentFile", null);
            if (path != null && preferences.getUserPreferences().getBoolean("deserializationSuccess", false)) {
                File currentFile = new File(path);
                double d = preferences.getUserPreferences().getDouble("mediaStopPosition", 0.0);
                if (preferences.getUserPreferences().getBoolean("markMediaStopPosition", false)) {
                    long time = Utility.convertTimeToMillis(GPlayerSettings.generalPrefs().get("mediaMarkTime", Utility.formatTime(1, 1)));
                    String key = " " + path;
                    if (d - time >= 0)
                        mediaDurationMap.put(key, new Trio<File, Duration, Boolean>(currentFile, Duration.millis(d), !markMediaPosition(currentFile)));
                    else
                        mediaDurationMap.remove(key);
                }
            }
        }
        preferences.getUserPreferences().putBoolean("markMediaStopPosition", false);
    }


    private Button createButton(String key) {
        String prefix = key + '.';
        String text = resource.getString(prefix + "text");  //The text to be displayed on the button
        String tooltip = resource.getString(prefix + "tooltip");  //The text shown when the mouse is hovered over the button
        Image icon = iconify(resource.getString(prefix + "icon"));  //The image shown on the button
        Button button = new Button(text, new ImageView(icon));
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }


    /**Sets playback controls to an VBox node
    *@return the modified node
    */
    private VBox setAndGetControls() {
        logger.config("Configuring playback controls");
        long startTime = System.nanoTime();
        //Create the VBox node
        //To host the various controls
        controlNode = new VBox(10);
        controlNode.setAlignment(Pos.BOTTOM_CENTER);
        controlNode.setPadding(new Insets(5, 10, 5, 10));
        //Instantiate an HBox layout for the playback buttons
        HBox hb1 = new HBox(8);
        hb1.setAlignment(Pos.CENTER);
        previous = createButton("previous");
        previous.setOnAction((ae) -> {  //Action definition
            if (GPlayerSettings.generalPrefs().getBoolean("wrapPrevious", true))  //If true
                //And current position is at the start (0)
                //we need to wrap around by playing from the rear of the playlist
                filePosition = (filePosition == 0)? playingPlayList.size(): filePosition;
            //Now, decrease by 1 to play previous item
            filePosition = filePosition > 0? --filePosition: 0;
            previous_next = previous;
            playMedia(filePosition, true);  //Sends the item for play
        });  //Case close for the previous button

        next = createButton("next");  //Plays the next media file in a playlist
        previous_next = next;
        next.setOnAction((ae) -> {  //Action definition
            if (!GPlayerSettings.generalPrefs().getBoolean("wrapNext", true))  //If false
                //And current position is at the end (playlist size -1)
                //we need not wrap around by repeatedly playing the same item
                filePosition = (filePosition == playingPlayList.size()-1)? --filePosition: filePosition;
            //Now, increase by 1 to play next item
            filePosition = filePosition == playingPlayList.size()-1? 0: ++filePosition;
            previous_next = next;
            if (filePosition == 0 && sortShuffle.isSelected())
                sortPlayList(playingPlayList, 0);
            else
                playMedia(filePosition, true);  //Sends the item for play
        });  //Block ends

        play_pause = createButton("play");  //Plays/pauses focused media file in a playlist
        //Set its default feature to true
        //so it can be activated on enter-key press, regardless of its focus
        play_pause.setDefaultButton(true);
        play_pause.setOnAction((ae) -> {  //Action definition
            //On entering the block,
            //Get the status of the player
            Status status = mediaPlayer.getStatus();
            if (status == Status.UNKNOWN || status == Status.HALTED)
                //Do nothing in these states
                return;
            if (status == Status.PLAYING) {
                //Pause the media
                mediaPlayer.pause();
            }
            else {  //If media isn't playing
                if (status == Status.STOPPED)  //Media has been stopped
                    playMedia(filePosition, true, currentFile);  //Initiate replay from start
                else
                    mediaPlayer.play();  //Initiate replay from paused position
                if (status == Status.READY)
                    verifyPlayStatus(mediaPlayer, currentFile, fileName, true, Duration.seconds(2));
            }
        });

        hb1.getChildren().addAll(play_pause, previous);

        stop = createButton("stop");  //Stops media playback
        stop.setOnAction((ae) -> finalizeStoppage());

        repeat = new CheckBox();  //Toggles repeat options on and off
        repeat.setGraphic(new ImageView(iconify(resource.getString("repeat.icon"))));
        repeat.setAllowIndeterminate(true);  //Allows 3 toggle states for this checkbox
        repeat.setTooltip(new Tooltip(resource.getString("repeat.tooltip")));
        setRepeatProperties(repeat, repeatText);
        repeat.setOnAction((ae) -> {  //Action definition
            int i = (playingPlayList.size() == 1)? 2: 3;
            //If the size of playing playlist is one
            //2 toggle states are made available (partially-selected and deselected)
            //else, all 3 toggle states are made available (selected, partially-selected, and deselected)
            repeatText = Utility.nextItem(repeatText, repeatOptions.subList(0, i));
            setRepeatProperties(repeat, repeatText);
            preferences.getUserPreferences().put("repeatText", repeatText);
            preferences.getUserPreferences().putBoolean("repeatOn", repeatIsSelected);
        });

        hb1.getChildren().addAll(next, stop, repeat);

        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
        separator.setPrefWidth(180);
        hb1.getChildren().addAll(separator, libraryToggle);
        if (GPlayerSettings.viewPrefs().getBoolean("displayLibrary", false))  //If true
            //Show library on start-up
            Platform.runLater(() -> libraryToggle.setSelected(true));

        HBox hb2 = new HBox(5);
        hb2.setAlignment(Pos.CENTER);
        timeSlider = new Slider();  //For displaying playback level, or adjusting playback time
        sliderLabel = new Label(resource.getString("sliderLabel.text"));  //Time slider descriptor
        sliderLabel.setLabelFor(timeSlider);
        timeSlider.setMinWidth(200);
        timeSlider.setPrefWidth(500);
        timeSlider.setFocusTraversable(false);
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        //Action definition
        armSlider(timeSlider, resource.getString("timeSlider.tooltip"), resource.getString("timeSlider.popup"), GPlayerSettings.playBackPrefs().getFloat("timeStepLevel", 5.0f), 1);

        playTime = new Label();  //Displays media play time
        playTime.setPrefWidth(130);  //The preferred width size
        playTime.setMinWidth(50);  //The minimum width size
        timeLabel = new Label(resource.getString("timeLabel.text"));  //play time label descriptor
        timeLabel.setLabelFor(playTime);
        //A spacer label
        spacer = new Label();

        hb2.getChildren().addAll(sliderLabel, timeSlider, timeLabel, playTime, spacer);

        volumeSlider = new Slider(0.0, 100.0, volumeLevel);  //For adjusting playback volume level
        volumeSlider.setMinWidth(100);
        volumeSlider.setPrefWidth(250);
        volumeSlider.setFocusTraversable(false);
        HBox.setHgrow(volumeSlider, Priority.ALWAYS);
        volumeLabel = new Label(resource.getString("volumeLabel.text"));  //Volume slider descriptor
        volumeLabel.setLabelFor(volumeSlider);
        //Action definition
        armSlider(volumeSlider, resource.getString("volumeSlider.tooltip"), resource.getString("volumeSlider.popup"), GPlayerSettings.playBackPrefs().getFloat("volumeStepLevel", 3.0f), 2);
        hb2.getChildren().addAll(volumeLabel, volumeSlider);

        //Add the now defined controls
        //to the node
        controlNode.getChildren().addAll(hb1, hb2);
        logger.info("Completed control set-up in: " + (System.nanoTime() - startTime));
        return controlNode;  //The methods returns with the
    }  //added controls


    private void setRepeatProperties(CheckBox repeat, String repeatText) {
        if (repeatText.equals(repeatOptions.get(0))) {
            //Turn repeat off
            //by deselecting the repeat control
            repeatIsSelected = false;
            repeat.setSelected(false);
        }
        else {
            //Turn repeat on
            //by partially-selecting or selecting the repeat control
            repeatIsSelected = true;
            if (repeatText.equals(repeatOptions.get(1)))
                //Partially-select
                repeat.setIndeterminate(true);
            else
                //Select
                repeat.setSelected(true);
        }
        repeat.setText(repeatText);
        int index = repeatOptions.indexOf(repeatText);
        Platform.runLater(() -> notify(resource.getStringFamily("repeat.notification")[index]));
    }


    private void initiateStoppage(MediaPlayer mediaPlayer, File file, boolean endOfFile) {
        Duration position = mediaPlayer.getCurrentTime();  //Current playback position
        mediaPlayer.stop();// Terminates the active file
        String key = " " + file.getPath();
        if (key != null || recallMediaStopPosition.isSelected()) {
            //We need to save this position for future recovery
            long time = Utility.convertTimeToMillis(GPlayerSettings.generalPrefs().get("mediaMarkTime", Utility.formatTime(1, 1)));  //Time that must have been expended before this info can be saved
            if (!recallMediaStopPosition.isSelected() || endOfFile || position.lessThan(Duration.millis(time)))
                //Remove the associated info, if any
                mediaDurationMap.remove(key);
            else
                //Save the media info
                mediaDurationMap.put(key, new Trio<File, Duration, Boolean>(file, position, !markMediaPosition(file)));
            //Update media info
            updateMediaInfo(false);
        }
        if (!endOfFile && multipleSelection)
            multipleSelection = false;
        lastMarkedTime = Duration.millis(0);
        reflectPlayState(Status.STOPPED);
    }


    private void finalizeStoppage() {
        initiateStoppage(mediaPlayer, currentFile, endOfFile);
        setStageTitle(GPlayer.stageTitle);
    }


    private ToggleButton createToggle() {
        String[] toggleTextOptions = resource.getStringArray("libraryToggle.text.array"), toggleTooltipOptions = resource.getStringFamily("libraryToggle.tooltip");
        ToggleButton toggleButton = new ToggleButton(toggleTextOptions[0]);
        toggleButton.setTooltip(new Tooltip(toggleTooltipOptions[0]));
        toggleButton.selectedProperty().addListener((listener) -> {
            if (toggleButton.isSelected()) {
                toggleButton.setText(toggleTextOptions[1]);
                toggleButton.getTooltip().setText(toggleTooltipOptions[1]);
                playListViewSelection = false;
                if (stage.isShowing())
                    mediaLibrary.showPopupStage();
            }
            else {
                toggleButton.setText(toggleTextOptions[0]);
                toggleButton.getTooltip().setText(toggleTooltipOptions[0]);
                mediaLibrary.hidePopupStage();
            }
        });
        toggleButton.setOnKeyPressed((ke) -> {
            setSeekSlider(ke);
            if (new KeyCodeCombination(KeyCode.ENTER).match(ke) || new KeyCodeCombination(KeyCode.SPACE).match(ke)) {
                toggleButton.fire();
                ke.consume();
            }
            else {
                if (mediaPlayer != null && mediaPlayer.getStatus() == Status.PLAYING) {
                    if (triggeredSliderAction(ke, true, KeyCode.LEFT, KeyCode.RIGHT))
                        requestMomentaryFocus(timeSlider, Duration.millis(500));
                    else if (triggeredSliderAction(ke, true, KeyCode.UP, KeyCode.DOWN))
                        requestMomentaryFocus(volumeSlider, Duration.millis(100));
                }
                else {
                    if (keyMatch(ke, null, KeyCode.LEFT))
                        Utility.previousItem(toggleButton, traversableNodes).requestFocus();
                }
            }
        });
        return toggleButton;
    }


    private void configureLibrary() {
        //Initialize library content
        mediaLibrary = new MediaLibrary(stage);
        mediaLibrary.initialize(configureListView());
        mediaLibrary.popupShowingProperty().addListener((val, oldVal, newVal) -> Platform.runLater(() -> libraryToggle.setSelected(newVal)));
    }


    private ListView<File> createListView() {
        //Instantiate a list view object
        //with the contents of playListView
        ListView<File> listView = new ListView<>(playListView);
        listView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        //Set the size
        listView.setPrefSize(80, 80);
        listView.setTooltip(new Tooltip(resource.getString("listView.tooltip")));
        listView.setOnKeyReleased((ke) -> {
            if (playListViewSelection)
                playListViewSelection(new KeyCodeCombination(KeyCode.ENTER).match(ke) && !playingPlayList.isEmpty());
            else
                playListViewSelection = true;
        });
        listView.setOnKeyTyped((ke) -> listViewSearch.search(keyTyped(ke), listView.getSelectionModel().getSelectedIndex() + 1));
        listView.setOnMouseClicked((me) -> playListViewSelection(me.getClickCount() == 2 && !playingPlayList.isEmpty()));
        listView.setContextMenu(listViewContextMenu(listView));
        listView.setCellFactory((ListView<File> l) -> {
            javafx.scene.control.ListCell<File> cell = new javafx.scene.control.ListCell<File>() {
                @Override
                public void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty)
                        setText(null);
                    else
                        setText(getFileInfo(item));
                }
            };
            cell.focusedProperty().addListener((listener) -> {
                if (!playingPlayList.isEmpty() && cell.isFocused())
                    cell.setText(getFileInfo(playingPlayList.get(cell.getIndex())));
            });
            return cell;
        });
        return listView;
    }


    private void makeListViewSelection(int filePosition) {
        if (listView == null)
            return;
        if (filePosition >= 0 && filePosition < playListView.size()) {
            if (!playListView.isEmpty() && mediaPlayer != null && (mediaPlayer.getStatus() == Status.PLAYING || mediaPlayer.getStatus() == Status.PAUSED)) {
                listView.getSelectionModel().clearAndSelect(filePosition);
                return;
            }
            listView.getSelectionModel().clearSelection();
            if (!playListView.isEmpty())
                listView.getFocusModel().focus(filePosition);
        }
    }


    private void playListViewSelection(boolean play) {
        if (play) {
            int position = listView.getSelectionModel().getSelectedIndex();
            if (position > -1)
                playMedia(position, true);
        }
    }


    private VBox configureListView() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(5, 5, 5, 5));
        box.setAlignment(Pos.CENTER);
        listView = createListView();
        VBox.setVgrow(listView, Priority.ALWAYS);
        listViewSearch.setOnSucceeded((wse) -> makeListViewSelection(listViewSearch.getMatchIndex()));
        Label label = new Label(resource.getString("listView.label.text"));
        label.setLabelFor(listView);
        box.getChildren().addAll(label, listView);
        return box;
    }


    private ContextMenu listViewContextMenu(ListView<File> listView) {
        EventHandler<? super KeyEvent> keyReleasedHandler = listView.getOnKeyReleased();
        ContextMenu cm = new ContextMenu();
        Runnable runnable = (() -> playListViewSelection(true));
        Supplier<List<File>> supplier = (() -> listView.getSelectionModel().getSelectedItems());
        javafx.util.Callback<File, File> callback = ((file) -> file);
        cm.getItems().addAll(playListOperations(resource, listView.getSelectionModel(), runnable, supplier, callback));
        cm.getItems().addAll(MediaLibrary.availableFileActions("", cm.getItems().get(0), this::performFileAction));
        mediaLibrary.popupOpenAction(cm, (() -> listView.setOnKeyReleased(null)));
        mediaLibrary.popupCloseAction(cm, (() -> {
            makeListViewSelection(filePosition);
            waitThenRun(Duration.millis(200), (() -> listView.setOnKeyReleased(keyReleasedHandler)));
        }));
        return cm;
    }


    private void performFileAction(FileManager.FileAction ACTION) {
        List<File> files = new ArrayList<>(listView.getSelectionModel().getSelectedItems());
        VBox layout = (VBox) listView.getParent();
        fileManager = mediaLibrary.invokeFileAction(fileManager, layout, ACTION, resource.getString("fileOperation.empty_list.message"), files, ((collection) -> collection.stream().map((file) -> file.toPath()).collect(Collectors.toList())));
    }


    private List<File> substitute(List<File> defaultList) {
        if (defaultList == defaultPlayingPlayList)
            return playingPlayList;
        return (defaultList == defaultPlayedPlayList)? playedPlayList: defaultList;
    }


    public <T> MenuItem[] playListOperations(BaseResourcePacket resource, MultipleSelectionModel<T> selectionModel, Runnable runnable, Supplier<List<File>> supplier, javafx.util.Callback<T, File> callback) {
        String prefix = "playListOperations.", suffix = ".text";
        SerialTaskExecutor playListModifier = new SerialTaskExecutor();
        java.util.function.BiConsumer<List<File>, Runnable> consumer = ((list, executable) -> {
            if (list.isEmpty())
                mediaLibrary.showTempPopup(resource.getString("fileOperation.empty_list.message"));
            else
                playListModifier.run(executable);
        });
        Duo<Supplier<List<File>>, java.util.function.BiConsumer<List<File>, Runnable>> playListProvisions = new Duo<>(supplier, consumer);
        //To play most recent selected item
        MenuItem playItem = playListOperator(resource, new MenuItem(), "SHORTCUT+P", "play", "...");
        playItem.setDisable(true);
        String playString = playItem.getText();
        playItem.setOnAction((ae) -> runnable.run());
        //To add selected items to playing queue
        MenuItem queueItem = playListOperator(resource, new MenuItem(), "SHORTCUT+Q", "queue");
        queueItem.setOnAction((ae) -> {
            List<File> list = new ArrayList<>(supplier.get());
            list.remove(currentFile);
            consumer.accept(list, (() -> add(list, playingPlayList, playingPlayList, filePosition+1)));
        });
        //To select all items in the selection model
        MenuItem selectItem = playListOperator(resource, new MenuItem(), "SHORTCUT+A", "select");
        selectItem.setOnAction((ae) -> waitThenRun(Duration.millis(200), selectionModel::selectAll));
        //To unselect all selected items in the selection model
        MenuItem unselectItem = playListOperator(resource, new MenuItem(), "SHORTCUT+Z", "unselect");
        unselectItem.setOnAction((ae) -> waitThenRun(Duration.millis(200), selectionModel::clearSelection));
        //To add to various playlists
        Menu addMenu = playListOperator(resource, new Menu(), null, "add");
        addMenu.getItems().addAll(playListAddOperations(supplier, consumer));
        playListOperationsMap.put(addMenu, playListProvisions);
        //To delete selected files from various playlists
        Menu discardMenu = playListOperator(resource, new Menu(), null, "discard");
        discardMenu.getItems().addAll(playListRemoveOperations(supplier, consumer));
        playListOperationsMap.put(discardMenu, playListProvisions);
        selectionModel.selectedItemProperty().addListener((listener) -> {
            T selectedItem = selectionModel.getSelectedItem();
            File selectedFile = (selectedItem == null)? null: callback.call(selectedItem);
            boolean nullSelection = selectedItem == null;
            String string = (nullSelection)? null: String.join((java.nio.file.Files.isRegularFile(selectedFile.toPath()))? "play": "alt_play", prefix, suffix);
            playItem.setText((nullSelection)? playString: FileMedia.resource.getAndFormatMessage(string, selectedFile.getName()));
            playItem.setDisable(nullSelection);
        });
        MenuItem[] items = {playItem, queueItem, selectItem, unselectItem, addMenu, discardMenu};
        bindProperties(Arrays.stream(items).filter((item) -> item != playItem && item != selectItem).map((item) -> item.disableProperty()), playItem.disableProperty());
        return items;
    }


    private <T extends MenuItem> T playListOperator(BaseResourcePacket resource, T operator, String accelerator, String string, Object... parameters) {
        String prefix = "playListOperations.", suffix = ".text";
        String joinedString = String.join(string, prefix, suffix);
        BaseResourcePacket brp = (resource.handleGetObject(joinedString) == null)? FileMedia.resource: resource;
        String text = (parameters.length > 0)? brp.getAndFormatMessage(joinedString, parameters): brp.getString(joinedString);
        operator.setText(text);
        if (accelerator != null)
            operator.setAccelerator(KeyCombination.keyCombination(accelerator));
        return operator;
    }


    private MenuItem[] playListAddOperations(Supplier<List<File>> supplier, java.util.function.BiConsumer<List<File>, Runnable> consumer) {
        MenuItem currentPlayListItem = new MenuItem(resource.getString("playListOperations.currentPlayList.text"));
        currentPlayListItem.setOnAction((ae) -> consumer.accept(supplier.get(), (() -> add(supplier.get(), playingPlayList, playingPlayList))));
        MenuItem[] items = {currentPlayListItem, null, null, null};
        int[] indices = {0, 3, 4};
        for (int i = 1; i < items.length; i++) {
            String text = DEFAULT_PLAYLIST_NAMES.get(indices[i-1]);
            items[i] = new MenuItem(text);
            if (i < items.length -1) {
                List<File> playList = getPlayList(text.concat(DEFAULT_PLAYLIST_EXTENSION), false, true);
                items[i].setOnAction((ae) -> consumer.accept(supplier.get(), (() -> add(supplier.get(), substitute(playList), playList))));
            }
            else
                items[i].setOnAction((ae) -> consumer.accept(supplier.get(), (() -> add(supplier.get(), playedPlayList, playedPlayList))));
        }
        return items;
    }


    private MenuItem[] playListRemoveOperations(Supplier<List<File>> supplier, java.util.function.BiConsumer<List<File>, Runnable> consumer) {
        MenuItem currentPlayListItem = new MenuItem(resource.getString("playListOperations.currentPlayList.text"));
        currentPlayListItem.setOnAction((ae) -> consumer.accept(supplier.get(), (() -> discard(supplier.get(), playingPlayList, playingPlayList))));
        MenuItem[] items = new MenuItem[DEFAULT_PLAYLIST_NAMES.size()+1];
        items[0] = currentPlayListItem;
        for (int i = 1; i < items.length; i++) {
            //Name of playlist at this index
            String text = DEFAULT_PLAYLIST_NAMES.get(i-1);
            items[i] = new MenuItem(text);
            if (i < items.length -1) {
                List<File> playList = getPlayList(text.concat(DEFAULT_PLAYLIST_EXTENSION), false, true);
                items[i].setOnAction((ae) -> consumer.accept(supplier.get(), (() -> discard(supplier.get(), substitute(playList), playList))));
            }
            else
                items[i].setOnAction((ae) -> consumer.accept(supplier.get(), (() -> discard(supplier.get(), playedPlayList, playedPlayList))));
        }
        return items;
    }


    private void updatePlayListOperations(String playListName, boolean add, String... formerPlayListName) {
        int iterationIndex = -1;
        for (Menu key: playListOperationsMap.keySet()) {
            boolean noEntry = formerPlayListName.length == 0;
            MenuItem item = newCreatedPlayListItem((noEntry)? playListName: formerPlayListName[0]);
            if (add) {
                int index = (noEntry)? createdPlayListIndex(playListName): key.getItems().indexOf(item);
                if (noEntry) {
                    Duo<Supplier<List<File>>, java.util.function.BiConsumer<List<File>, Runnable>> value = playListOperationsMap.get(key);
                    Supplier<List<File>> supplier = value.getKey();
                    java.util.function.BiConsumer<List<File>, Runnable> consumer = value.getValue();
                    boolean addTo = ((++iterationIndex % 2) == 0);
                    item.setOnAction((ae) -> {
                        List<File> list = createdPlayListMap.get(createdPlayListName(item.getText())).getValue();
                        if (addTo)
                            consumer.accept( supplier.get(), (() -> add(supplier.get(), substitute(list), list)));
                        else
                            consumer.accept( supplier.get(), (() -> discard(supplier.get(), substitute(list), list)));
                    });
                    key.getItems().add(item);
                }
                else
                    key.getItems().get(index).setText(playListName);
            }
            else
                key.getItems().remove(item);
        }
        if ((myPlayList.getItems().size() / 3) == createdPlayListMap.size())
            playListProperty.setValue(new ArrayList<File>(playListView));
    }


    private void showLibraryPopup(boolean show) {
        if (mediaLibrary != null) {
            if ((!show && !mediaLibrary.isPopupShowing()) || (show && isDialogShowing))
                return;
            boolean state = (show)? isPopupShowing: show;
            if (!show)
                isPopupShowing = mediaLibrary.isPopupShowing();
            else
                isPopupShowing = false;
            libraryToggle.setSelected(state);
        }
    }


    private void displayRetrievedFilePopup(int size) {
        String word = "file.single.word", message = "fileRetrieve.fail.message";
        final String str = resource.getString((size == 1)? word: word.replace("single", "multiple"));
        Platform.runLater(() -> notify((size == 0)? resource.getString(message): resource.getAndFormatMessage(message.replace("fail", "success"), new Integer(size), str)));
    }


    private HBox setAndGetControls(String[] keyNames1, String[] keyNames2, String... texts) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(5);
        hBox.setPadding(new Insets(5, 12, 5, 12));
        String[] prompts = resource.getStringFamily("setAndGetControls.prompt");
        int length = texts.length;
        TextField tf1 = new TextField();
        tf1.setPrefColumnCount(17);
        tf1.setPromptText(prompts[0]);
        if (length > 0)
            tf1.setText(texts[0]);
        final TextField tf2 = new TextField();
        tf2.setPromptText(prompts[1]);
        if (length > 1)
            tf2.setText(texts[1]);
        addListenersTo(tf2, com.sun.javafx.tk.Toolkit.getToolkit().getPlatformShortcutKey().getName() + "+ALT+", keyNames1);
        final TextField tf3 = new TextField();
        tf3.setPromptText(prompts[2]);
        if (length > 2)
            tf3.setText(texts[2]);
        addListenersTo(tf3, "SHIFT+ALT+", keyNames2);
        hBox.getChildren().addAll(tf1, tf2, tf3);
        return hBox;
    }


    private void addListenersTo(TextField tf, String textFill, String[] names) {
        tf.addEventFilter(KeyEvent.KEY_TYPED, (ke) -> {
            if (!Utility.hasUniqueName(ke.getCharacter(), names))
                ke.consume();
        });
        tf.addEventHandler(KeyEvent.KEY_PRESSED, (ke) -> tf.selectAll());
        tf.textProperty().addListener((value, oldValue, newValue) -> {
            if (newValue.length() == 1)
                tf.setText(textFill + newValue);
        });
    }


    @SuppressWarnings("unchecked")
    private HBox setAndGetControls(TreeMap<Duration, String> durationSet, File file, Duration currentDuration, Duration duration, javafx.scene.control.Dialog dialog) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(5);
        hBox.setPadding(new Insets(5, 12, 5, 12));
        String[] strings = resource.getStringFamily("setAndGetControls.string");
        final Label durationLabel = new Label();
        final Status status = (mediaPlayer == null)? null: mediaPlayer.getStatus();
        //Obtain and store all the keys in durationSet
        List<Duration> keys = new ArrayList<>();
        durationSet.keySet().forEach((key) -> keys.add(key));
        final javafx.scene.control.ComboBox<Double> comboBox = new javafx.scene.control.ComboBox<>(FXCollections.observableArrayList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0));  //Scale options for spinner values
        comboBox.setCellFactory((ListView<Double> l) -> new GPlayerSettings.NumberLocalizer());
        comboBox.setValue(1.0);  //The default value
        final Label label2 = new Label(strings[0]);
        label2.setLabelFor(comboBox);
        final String name = Utility.postCharacterString(durationSet.get(currentDuration), separator, true);
        TextField textField = new TextField();  //for editing mediamark name
        textField.setPrefColumnCount(17);
        textField.setPromptText(strings[1]);
        textField.setText(name);
        textField.focusedProperty().addListener((value, oldValue, newValue) -> makeInvisible(newValue, comboBox, label2));
        final Spinner<Double> spinner = new Spinner<>(); //For adjusting mediamark values
        Label label = new Label(strings[2]);
        label.setLabelFor(spinner);
        //Initial value of the spinner control
        double initialValue = currentDuration.toSeconds();
        //Fractional portion of initialValue
        double fraction = initialValue - Math.floor(initialValue);
        //Define both lower and upper bound values
        Duration lowerDuration = Utility.previousItem(currentDuration, keys), upperDuration = Utility.nextItem(currentDuration, keys);
        double lowerValue = (lowerDuration == currentDuration || lowerDuration.greaterThan(currentDuration))? 0.0: lowerDuration.toSeconds()+1;
        double upperValue = (upperDuration == currentDuration || upperDuration.lessThan(currentDuration))? duration.toSeconds(): upperDuration.toSeconds()-1;
        //spinner value factory
        final SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(Math.floor(lowerValue) + fraction, Math.floor(upperValue) + fraction, initialValue);
        valueFactory.setWrapAround(true);
        spinner.setValueFactory(valueFactory);
        comboBox.valueProperty().addListener((value, oldValue, newValue) -> valueFactory.setAmountToStepBy(newValue));
        spinner.valueProperty().addListener((value, oldValue, newValue) -> {
            boolean b = file.equals(currentFile);
            if (b && durationLabel.getText().isEmpty()) {
                durationLabel.setText("" + mediaPlayer.getCurrentTime().toMillis());
                if (wasPaused || status != Status.PLAYING)
                    play_pause.fire();
            }
            if (!comboBox.isVisible()) {
                label2.setVisible(true);
                comboBox.setVisible(true);
            }
            if (b)
                mediaPlayer.seek(Duration.millis(newValue * 1000));
        });
        Button button = new Button(strings[3]);
        button.focusedProperty().addListener((value, oldValue, newValue) -> makeInvisible(newValue, comboBox, label2));
        button.setOnAction((ae) -> {
            FileManager.createAlert(strings[4], AlertType.CONFIRMATION).showAndWait().filter((response) -> response == ButtonType.OK).ifPresent((response) -> {
                configureMediaMarkers(name, file, currentDuration, false);
                mediaMarkerSearch.setDisable(isEmptyOfUserEntry());
                dialog.hide();
            });
        });
        dialog.setOnHiding((de) -> {
            if (file.equals(currentFile) && !durationLabel.getText().isEmpty())
                mediaPlayer.seek(Duration.millis(Double.parseDouble(durationLabel.getText())));
        });
        hBox.getChildren().addAll(textField, label, spinner, label2, comboBox, button);
        return hBox;
    }


    private void makeInvisible(boolean state, Node... nodes) {
        if (state && nodes.length > 0 && nodes[0].isVisible())
            for (Node node: nodes)
                node.setVisible(false);
    }


    /**Transitions from one playlist to another
    *@param fp
    *the file position of the outgoing playlist
    *@param nfp
    *the file position of the incoming playlist
    *@param ppl
    *the outgoing playlist reference
    *@param nppl,
    *the incoming playlist reference
    *@param dppl
    *the outgoing playlist route source reference
    *@param nddpl
    *the incoming playlist route source reference
    *@param P
    *the outgoing playlist sort parameter
    *@param NP
    *the incoming playlist sort parameter
    *@param sort
    *flags if sorting can be performed on the incoming playlist
    *@return true if the transition was successful
    *and false otherwise
    */
    private boolean transitionPlayList(int fp, int nfp, LinkedList<File> ppl, LinkedList<File> dppl, LinkedList<File> nppl, LinkedList<File> ndppl, FileSortParameter P, FileSortParameter NP, boolean sort) {
        if (nppl.isEmpty()) {
            playListID = null;
            disablePlayLists(false);
            notify(resource.getString("playList.empty.message"));
            return false;
        }
        logger.info("Transitioning playlist");
        isPlayedPlayListOn = playListID != null;
        if (dppl != ndppl) {
            playedPlayListFilePosition = fp;
            playedPlayList = ppl;
            defaultPlayedPlayList = dppl;
            FileSortParameter.setPriorSortParameter(P);
        }
        logger.info("Outgoing playlist information:\n" + String.join(": ", "Playlist name", playListIdentity(defaultPlayedPlayList)) + String.join(": ", ", filePosition index", "" + playedPlayListFilePosition) + String.join(": ", ", sort parameter", P.toString()));
        filePosition = nfp;
        playingPlayList = nppl;
        defaultPlayingPlayList = ndppl;
        String playListID = playListIdentity(defaultPlayingPlayList);
        logger.info("Incoming playlist information:\n" + String.join(": ", "Playlist name", playListID) + String.join(": ", ", filePosition index", "" + filePosition) + String.join(": ", ", sort parameter", NP.toString()));
        if (menuBar != null) {
            unfilteredPlayList = new LinkedList<>();
            sortUnfilteredPlayList = false;
            if (!filteredPlayList.isEmpty() && !playingPlayList.equals(filteredPlayList)) {
                File file = playingPlayList.get(filePosition);
                unfilteredPlayList = playingPlayList;
                playingPlayList = filteredPlayList;
                filteredPlayList = new LinkedList<>();
                if (filePosition != 0) {
                    int fileIndex = playingPlayList.indexOf(file);
                    filePosition = (fileIndex < 0)? 0: fileIndex;
                }
                unfilteredPlayListPosition = filePosition;
            }
            if (!isCurrentParameter(NP))
                setSortParameter(NP, false, false);
            if (defaultPlayingPlayList != previousPlayListSearch && defaultPlayingPlayList != nextPlayListSearch) {
                if (!isCurrentParameter(FileFilter.getDefaultFilter()))
                    setSearchParameter(FileFilter.getDefaultFilter(), false, false);
            }
            if (sort && !sortDefault.isSelected() && playingPlayList.size() > 1) {
                sortUnfilteredPlayList = true;
                sortPlayList(playingPlayList, filePosition);
            }
            else
                organizeControls(filePosition);
            logger.info("organized controls");
            if (stage.isShowing()) {
                notify(resource.getAndFormatMessage("playing.message", playListID));
            }
        }
        this.playListID = null;
        return true;
    }


    /**Transitions either of recentPlayList or mostPlayedPlayList to the play stage
    *@param ll
    *recentPlayList or mostPlayedPlayList instance
    *@param name
    *name of the playlist
    */
    private void transitionMostRecentPlayList(LinkedList<File> ll, String name) {
        Task<LinkedList<File>> task = new Task<LinkedList<File>>() {
            @Override
            protected LinkedList<File> call() {
                return getMostRecentPlayList(ll, name);
            }
        };
        task.setOnSucceeded((wse) -> transitionPlayList(filePosition, 0, playingPlayList, defaultPlayingPlayList, task.getValue(), ll, FileSortParameter.getSortParameter(), PARAMETER, true));
        initiateBackgroundTask(task);
    }


    public void transitionAppPlayList(LinkedList<File> playList, String name, boolean allowEmptyPlayList) {
        filteredPlayList.addAll(playList);
        if (name != null) {
            if (playList.isEmpty() && !allowEmptyPlayList) {
                notify(resource.getString("playList.empty.message"));
                return;
            }
            MenuItem[] items = {openPlayList, openRecentPlayList, openMostPlayedPlayList, openFavouritePlayList, openLastSession};
            int length = items.length;
            for (int i = 0; i < length; i++) {
                if (DEFAULT_PLAYLIST_NAMES.get(i).startsWith(name)) {
                    fire(items[i]);
                    break;
                }
            }
        }
        else
            fire(openPlayList);
    }


    public void transitionUserPlayList(LinkedList<File> playList, String name, int index, boolean allowEmptyPlayList) {
        if (name == null || availableMaps()[index].isEmpty() || (!allowEmptyPlayList && playList.isEmpty())) {
            notify(resource.getString("playList.empty.message"));
            return;
        }
        filteredPlayList.addAll(playList);
        switch (index) {
            case 0:
                List<File> list = createdPlayListMap.get(name).getValue();
                if (!list.isEmpty()) {
                    int playIndex = myPlayList.getItems().indexOf(newCreatedPlayListItem(resource.getAndFormatMessage("createdPlayList.text", name)));
                    MenuItem playItem = myPlayList.getItems().get(playIndex);
                    fire(playItem);
                }
                else
                    notify(resource.getString("playList.empty.message"));
                break;
            case 1:
                if (searchArrayMap.get(name).getKey() != defaultPlayingPlayList)
                    searchListIntermediate = searchList;
                conductFileMatch((searchList = searchArrayString.indexOf(name)));
                break;
            default:
                throw new ArrayIndexOutOfBoundsException("The index passed surpasses currently available map provisions.");
        }
    }


    public void transitionSystemPlayList(LinkedList<File> playList, File directory) {
        if (!playList.isEmpty()) {
            filteredPlayList.addAll(playList);
            int size = playList.size();
            File file = playList.get(0);
            playListEntry.addAll(uniqueElements(playListEntry, playList));
            openFiles(directory, file, 0, filePosition);
        }
        displayRetrievedFilePopup(playList.size());
    }


    /**Returns the current focused node among the available traversable nodes in the scene graph.*/
    private Node getFocusedNode() {
        return getFocusedNode(traversableNodes);
    }


    /**Focuses a node for a specified time
    *On elapse, focus is returned to prior focused node
    *@param node
    *the node to focus
    *@param duration
    *the duration to wait for before stealing focus back to prior focused node
    */
    private void requestMomentaryFocus(Node node, Duration duration) {
        Node focusedNode = getFocusedNode();
        node.requestFocus();
        Timeline t = new Timeline();
        t.getKeyFrames().add(new KeyFrame(duration, event -> focusedNode.requestFocus()));
        t.play();
    }


    public static List<Node> getTraversableChildren(ObservableList<Node> parent) {
        List<Node> list = new ArrayList<>();
        for (Node child: parent) {
            if (child instanceof javafx.scene.Group)
                list.addAll(getTraversableChildren(((javafx.scene.Group)child).getChildren()));
            else if (child instanceof javafx.scene.layout.Pane)
                list.addAll(getTraversableChildren(((javafx.scene.layout.Pane)child).getChildren()));
            else if (child.isFocusTraversable())
                list.add(child);
        }
        return list;
    }


    /**Obtains the node whose focused property is set to true.
    *@param traversableNodes
    *the array of nodes to search through.
    *@return null if the array is empty,
    *or the first node if there is no node with focused property set to true.
    */
    public static Node getFocusedNode(Node... nodes) {
        if (nodes.length == 0)
            return null;
        for (Node node: nodes)
            if (node.isFocused())
                return node;
        return nodes[0];
    }


    /**Organizes play back controls
    *@param filePosition
    *the position from which play back is going to begin
    */
    private void organizeControls(int filePosition) {
        logger.info("Preparing to play playlist");
        if (playingPlayList.isEmpty()) {
            if (isInitialized)
                displayPlayList(playingPlayList, 0);
            logger.info("Playlist is empty");
            return;
        }
        if (!isInitialized) {  //On the first innvokation
            logger.info("First invokation since application launch");
            if (isPlaying)
                autoPlay = true;
            setBottom(setAndGetControls());  //The controls are added to the scene
            List<Node> children = getTraversableChildren(controlNode.getChildren());
            traversableNodes = children.toArray(new Node[children.size()]);
            bindDisableProperties();
            GPlayer.addToTrayIcon(configureTrayPlaybackOptions(), 0);
            isInitialized = true;
        }
        else {  //On a second or a later invokation
            logger.info("Later invokation since application launch");
            autoPlay = true;
        }
        multipleSelection = (playingPlayList.size() > 1)? true: false;
        displayPlayList(playingPlayList, filePosition);
        playMedia(filePosition, autoPlay);  //The file is passed to the playMedia method for consequent actions
    }


    public static <K, V> List<K> getMapKeys(Map<K, V> map) {
        return new ArrayList<K>(map.keySet());
    }


    @SuppressWarnings("unchecked")
    public <K> List<K> getMapKeys(int index) {
        try {
            return getMapKeys(availableMaps()[index]);
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            return null;
        }
    }


    public <K> LinkedList<File> getMapPlayList(K key, int index) {
        try {
            switch (index) {
                case 0:
                    return createdPlayListMap.get(key).getValue();
                case 1:
                    return searchArrayMap.get(key).getKey();
                case 2:
                    return miscellaneousPlayListMap.get(key);
                default:
                    return null;
            }
        }
        catch (Exception ex) {
            return null;
        }
    }


    public <K, V> LinkedList<File> getMapPlayList(K key, Map<K, V> map) {
        return getMapPlayList(key, Arrays.asList(availableMaps()).indexOf(map));
    }


    private Map[] availableMaps() {
        Map[] maps = {createdPlayListMap, searchArrayMap, miscellaneousPlayListMap};
        return maps;
    }


    private void adjustPlayList(LinkedList<File> ll, boolean adjust) {
        try {
            requireNonNull(ll);
            if (adjust) {
                defaultPlayedPlayList = defaultPlayingPlayList;
                defaultPlayingPlayList = ll;
            }
        }
        catch (Exception ex) {}
    }


    /**Returns property reference to playlist transitions.*/
    public final javafx.beans.property.ReadOnlyObjectProperty<List<File>> playListProperty() {
        return playListProperty.valueProperty();
    }


    private LinkedList<File> getDefaultPlayList(LinkedList<File> defaultPlayingPlayList) {
        if (defaultPlayingPlayList == recentPlayList || defaultPlayingPlayList == mostPlayedPlayList)
            return mostRecentPlayList;
        return defaultPlayingPlayList;
    }


    /**Gets and returns a copy of the playlist corresponding with the passed identity/name
    *@param name
    *identity of the required playlist
    */
    public final LinkedList<File> getPlayList(String name) {
        LinkedList<File> ll = getPlayList(name, false, true);
        return (ll == null)? ll: new LinkedList<File>(ll);
    }


    /**Gets and returns a playlist corresponding with the passed identity/name
    *@param name
    *identity of the required playlist
    *@param adjustValues
    *asserts if the value of filePosition is to be modified
    *@param returnDefaultValues
    *asserts the return of a copy of the recentPlayList/mostPlayedPlayList, if false, or the mentioned playlists as is, if true
    *@return a LinkedList object
    */
    private LinkedList<File> getPlayList(String name, boolean adjustValues, boolean returnDefault) {
        LinkedList<File> ll = null;
        boolean setValue = true;
        if (name.endsWith(DEFAULT_PLAYLIST_EXTENSION)) {
            String[] playListNames = resource.getStringArray("DEFAULT_PLAYLIST_NAMES.content.array");
            switch (DEFAULT_PLAYLIST_NAMES.indexOf(name.substring(0, name.indexOf(DEFAULT_PLAYLIST_EXTENSION)))) {
                case 4:
                    ll = playedPlayList;
                    setValue = false;
                    break;
                case 0:
                    adjustPlayList((ll = playListEntry), adjustValues);
                    break;
                case 1:
                    adjustPlayList(recentPlayList, adjustValues);
                    ll = (returnDefault)? recentPlayList: getMostRecentPlayList(recentPlayList, "recentPlayList");
                    break;
                case 2:
                    adjustPlayList(mostPlayedPlayList, adjustValues);
                    ll = (returnDefault)? mostPlayedPlayList: getMostRecentPlayList(mostPlayedPlayList, "mostPlayedPlayList");
                    break;
                case 3:
                    adjustPlayList((ll = favouritePlayList), adjustValues);
            }
        }
        else
            ll = getPlayList(name, adjustValues);
        if (ll != null && adjustValues && setValue && (ll != playedPlayList || !playedPlayList.equals(ll)) && !ll.isEmpty()) {
            playedPlayListFilePosition = filePosition;
            filePosition = 0;
        }
        return ll;
    }


    private LinkedList<File> getPlayList(String name, boolean adjustValues) {
        LinkedList<File> ll = null;
        if (name.endsWith(CREATED_PLAYLIST_EXTENSION) && !createdPlayListMap.isEmpty())
            adjustPlayList((ll = createdPlayListMap.get(name.substring(0, name.indexOf(CREATED_PLAYLIST_EXTENSION))).getValue()), adjustValues);
        else if (name.endsWith(SEARCH_PLAYLIST_EXTENSION) && !searchArrayMap.isEmpty())
            adjustPlayList((ll = searchArrayMap.get(name.substring(0, name.indexOf(SEARCH_PLAYLIST_EXTENSION))).getKey()), adjustValues);
        else if (name.endsWith(MISCELLANEOUS_PLAYLIST_EXTENSION) && !miscellaneousPlayListMap.isEmpty())
            adjustPlayList((ll = miscellaneousPlayListMap.get(name.substring(0, name.indexOf(MISCELLANEOUS_PLAYLIST_EXTENSION)))), adjustValues);
        return ll;
    }


    public ObservableList<String> getDefaultPlayListNames() {
        return DEFAULT_PLAYLIST_NAMES;
    }


    public ObservableList<String> getCreatedPlayListNames() {
        return FXCollections.observableArrayList(createdPlayListMap.keySet());
    }


    public ObservableList<String> getSearchPlayListNames() {
        return FXCollections.observableArrayList(searchArrayMap.keySet());
    }


    public ObservableList<String> getMiscellaneousPlayListNames() {
        return FXCollections.observableArrayList(miscellaneousPlayListMap.keySet());
    }


    /**Cumulates names of DEFAULT_PLAYLIST_NAMES
    *and all playlists in createdPlayListMap
    *@return a list of the names
    */
    public ObservableList<String> getPlayListNames() {
        ObservableList<String> names = FXCollections.observableArrayList(getDefaultPlayListNames());
        names.addAll(getCreatedPlayListNames());
        return names;
    }


    /**Cumulates the names of all available playlists
    *@return a list of the names
    */
    public ObservableList<String> getAllPlayListNames() {
        ObservableList<String> names = getPlayListNames();
        names.addAll(getSearchPlayListNames());
        names.addAll(getMiscellaneousPlayListNames());
        return names;
    }

    private String playListIdentity(LinkedList<File> ll, List<String> names, String nameExtension, String playListExtension) {
        for (String name: names)
            if (getPlayList(name + playListExtension, false, true) == ll)
                return name + nameExtension;
        return null;
    }


    /**Gets and returns the identity of a playlist
    *@param ll
    *the playlist whose identity is to be sought for
    *@return null if the playlist does not match any of the available playlists
    *and a name string, otherwise.
    */
    public String playListIdentity(LinkedList<File> ll) {
        if (isPlayedPlayListOn) {
            return DEFAULT_PLAYLIST_NAMES.get(4);
        }
        else {
            String name = null;
            if ((name = playListIdentity(ll, getDefaultPlayListNames(), "", DEFAULT_PLAYLIST_EXTENSION)) != null)
                return name;
            if ((name = playListIdentity(ll, getCreatedPlayListNames(), "", CREATED_PLAYLIST_EXTENSION)) != null)
                return name;
            if ((name = playListIdentity(ll, getSearchPlayListNames(), " " + resource.getString("searchPlayList.suffix"), SEARCH_PLAYLIST_EXTENSION)) != null)
                return name;
            if ((name = playListIdentity(ll, getMiscellaneousPlayListNames(), "", MISCELLANEOUS_PLAYLIST_EXTENSION)) != null)
                return name;
            return name;
        }
    }


    /**Gets and returns the group to which a playlist belongs
    *@param name
    *the identity of the passed playlist
    *@param ll
    *the playlist parameter
    *@return any of
    *Searched playlist, My playlist, miscellaneous playlist, playlist.
    */
    public String playListFamily(String name, LinkedList<File> ll) {
        String[] names = resource.getStringArray("playListFamily.name.array");
        if (!createdPlayListMap.isEmpty() && createdPlayListMap.containsKey(name) && createdPlayListMap.get(name).getValue() == ll)
            return names[0];
        else if (!searchArrayMap.isEmpty() && searchArrayMap.containsKey(name) && searchArrayMap.get(name).getKey() == ll)
            return names[1];
        else if (!miscellaneousPlayListMap.isEmpty() && miscellaneousPlayListMap.containsKey(name) && miscellaneousPlayListMap.get(name) == ll)
            return names[2];
        else
            return resource.getString("playList.single.word");
    }


    /**Sets the parameter with which sorting is done on play list files
    *@param PARAMETER,
    *the enum value that specifies the sort type
    *@param sort
    *determines if sorting is to be performed on the playing playlist
    *@param update
    *Asserts the adjustment of the default parameter value, (PARAMETER)
    *@return void
    */
    private void setSortParameter(FileSortParameter PARAMETER, boolean sort, boolean update) {
        //obtain the outgoing sort parameter
        FileSortParameter OLDPARAMETER = FileSortParameter.getSortParameter();
        //Set the sort parameter
        FileSortParameter.setSortParameter(PARAMETER);
        if (update)
            this.PARAMETER = PARAMETER;
        operateOnSortItems(getSortItem(OLDPARAMETER), getSortItem(PARAMETER));
        if (sort) {
            if (!sortDefault.isSelected() && playingPlayList.size() > 1) {
                sortUnfilteredPlayList = true;
                sortPlayList(playingPlayList, 0);
            }
            else
                organizeControls(0);
        }
    }


    /**Gets and returns the item associated with a specified sort parameter.
    *@param PARAMETER
    *the specified sort parameter
    *@return the radio menu item associated with the sort parameter.
    */
    private RadioMenuItem getSortItem(FileSortParameter PARAMETER) {
        switch (PARAMETER) {
            case ASCENDING:
                return sortAscending;
            case DESCENDING:
                return sortDescending;
            case SHUFFLE:
                return sortShuffle;
            default:
                return sortDefault;
        }
    }


    private void operateOnSortItems(RadioMenuItem oldItem, RadioMenuItem newItem) {
        RadioMenuItem[] items = {sortAscending, sortDescending, sortShuffle, sortDefault};
        for (RadioMenuItem item: items) {
            if (item != oldItem && item != newItem)
                continue;
            boolean select = (item == newItem);
            boolean disable = (item != sortShuffle && select);
            if (item == sortShuffle) {
                String[] shuffleTexts = resource.getStringArray("sortShuffle.text.array");
                sortShuffle.setText((select)? shuffleTexts[1]: shuffleTexts[0]);
            }
            item.setDisable((newItem == sortDefault && select)? playingPlayList.equals(getDefaultPlayList(defaultPlayingPlayList)): disable);
            item.setSelected(select);
        }
    }


    /**Asserts if a given parameter equals the default
    *@return true
    *if the compared values are equal
    *and false otherwise
    */
    private boolean isCurrentParameter(FileSortParameter p) {
        return getSortItem(p).isSelected();
    }


    /**Sets the parameter with which filtering is done on play list files
    *@param f
    *FileFilter enum value that specifies the filtering type
    *@param search
    *determines if search operation is to be performed on the playing playlist
    *@param update
    *Asserts the adjustment of the default FileFilter value
    *@return void
    */
    private void setSearchParameter(FileFilter f, boolean search, boolean update) {
        if (update)
            //Sets the enum default filter value
            FileFilter.setDefaultFilter(f);
        getFilterItem(f).setSelected(true);
        if (search ) {
            if (!PlayListSearch.lastSearchedString.isEmpty()) {  //If the string isn't empty
                if (playingPlayList == previousPlayListSearch || playingPlayList == nextPlayListSearch) {
                    String searchString = searchArrayString.get(searchList);
                    runValue = false;
                    newSearch = true;
                    new PlayListSearch(searchString, playListEntry, searchList);
                }
                else if (!PlayListSearch.searchStringHolder.isEmpty()) {
                    String searchString = PlayListSearch.searchStringHolder;
                    runValue = true;
                    playListSearch = new PlayListSearch(searchString, playingPlayList, -1);
                }
            }
        }
    }


    /**Gets the item associated with a specified filter parameter.
    *@param FILTER
    *the specified filter value.
    *@return the radio menu item associated with the search parameter.
    */
    private RadioMenuItem getFilterItem(FileFilter FILTER) {
        switch (FILTER) {
            case CONTAINS:
                return containsSearchString;
            case BEGINSWITH:
                return beginsSearchString;
            default:
                return endsSearchString;
        }
    }


    /**Asserts if a given FileFilter value has its associated item selected.
    *@pparam f
    *the specified filter parameter.
    *@return true
    *if the associated item is selected,
    *and false otherwise
    */
    private boolean isCurrentParameter(FileFilter f) {
        return getFilterItem(f).isSelected();
    }


    /**Returns the user selected FileFilter option.*/
    private FileFilter getSearchParameter() {
        List<String> list = Arrays.asList(GPlayerSettings.searchParameters);
        switch (list.indexOf(GPlayerSettings.playListPrefs().get("searchParameter", list.get(0)))) {
            case 0:
                return FileFilter.CONTAINS;
            case 1:
                return FileFilter.BEGINSWITH;
            default:
                return FileFilter.ENDSWITH;
        }
    }


    private boolean deleteFiles(List<File> ppl, List<File> dppl, int filePosition, int... filePositions) {
        if (ppl.isEmpty() || filePositions.length == 0)
            return false;
        boolean deletedCurrentFile = false;
        //Sort the contents of filePositions into ascendin order
        Arrays.sort(filePositions);
        //Given that the removal of an element inadvertently shifts up the indices of subsequent elements in a list,
        //We need to do a backward iteration on filePositions contents,
        //so as not to have other elements shifted into the indices listed for removal.
        for (int i = filePositions.length-1; i > -1; i--) {
            int fp = filePositions[i];
            makeDeletions(ppl, dppl, fp);
            if (fp == filePosition)
                deletedCurrentFile = true;
        }
        updateMediaInfo(false);
        return (ppl == playingPlayList && deletedCurrentFile);
    }


    private boolean deleteFiles(List<File> ppl, List<File> dppl, int filePosition, List<Integer> indices) {
        int[] array = indices.stream().mapToInt((index) -> (index == null)? 0: index).toArray();
        return deleteFiles(ppl, dppl, filePosition, array);
    }


    private boolean deleteUnavailableMediaMarks() {
        boolean deleted = false;
        Map<String, Trio<File, Duration, Boolean>> map = new TreeMap<>(mediaDurationMap);
        Set<String> keys = map.keySet();
        for (String key: keys) {
            Trio<File, Duration, Boolean> keyValue = map.get(key);
            File file = keyValue.getKey();
            if (!file.exists()) {
                configureMediaMarkers(Utility.postCharacterString(key, separator, true), file, keyValue.getValue(), false);
                if (!deleted)
                    deleted = true;
            }
        }
        mediaMarkerSearch.setDisable(isEmptyOfUserEntry());
        return deleted;
    }


    private boolean deleteMediaMarkedFiles(List<File> list, Duration duration, int position) {
        LinkedList<File> ppl = playedPlayList, dppl = defaultPlayedPlayList;
        int i = indexOf(duration, durationTime);
        boolean b = i == position;
        if (i != -1)
            makeDeletions(list, durationMatch, i);
        if (dppl == lastDurationMatch) {
            i = indexOf(duration, lastDurationTime);
            if (i != -1)
                makeDeletions(ppl, lastDurationMatch, i);
        }
        return b;
    }


    private File makeDeletions(List<File> playingPlayList, List<File> defaultPlayingPlayList, int fileIndex) {
        if (playingPlayList.isEmpty())
            return null;
        File removedFile = playingPlayList.remove(fileIndex);
        if (defaultPlayingPlayList == mostPlayedPlayList || defaultPlayingPlayList == recentPlayList) {
            if (playingPlayList != mostRecentPlayList)
                mostRecentPlayList.remove(removedFile);
            if (defaultPlayingPlayList == mostPlayedPlayList && !defaultPlayingPlayList.isEmpty()) {
                //Find the index of the removed file in mostPlayedPlayList
                int j = mostPlayedPlayList.indexOf(removedFile);
                if (j != -1)
                    timesPlayed.remove(j);
            }
        }
        else {
            if ((defaultPlayingPlayList == durationMatch || defaultPlayingPlayList == lastDurationMatch) && !defaultPlayingPlayList.isEmpty()) {
                if (defaultPlayingPlayList == lastDurationMatch) {
                    Duration d = lastDurationTime.remove(fileIndex);
                    if (lastDurationTime != lastDefaultDurationTime)
                        lastDefaultDurationTime.remove(indexOf(d, lastDefaultDurationTime));
                }
                else {  //If defaultPlayingPlayList equals durationMatch
                    Duration d = durationTime.remove(fileIndex);
                    if (durationTime != defaultDurationTime)
                        defaultDurationTime.remove(indexOf(d, defaultDurationTime));
                }
            }
        }
        if (playingPlayList != defaultPlayingPlayList)
            defaultPlayingPlayList.remove(removedFile);
        //Adjust the value of filePosition
        if (playingPlayList == this.playingPlayList && fileIndex <= filePosition) {
            //Decrese the value of filePosition by 1
            filePosition = (filePosition > 0)? --filePosition: 0;  //since indices of subsequent files will shrink by 1 after file removal
            preferences.getUserPreferences().putInt("filePosition", filePosition);
            if (!playingPlayList.isEmpty())
                preferences.getUserPreferences().put("currentFile", playingPlayList.get(filePosition).getPath());
        }
        else if (playingPlayList == playedPlayList && fileIndex <= playedPlayListFilePosition)
            //Decrese the value of playedPlayListFilePosition by 1
            playedPlayListFilePosition = (playedPlayListFilePosition > 0)? --playedPlayListFilePosition: 0;  //since indices of subsequent files will shrink by 1 after file removal
        return removedFile;
    }


    private void modifyPlayList(List<File> ppl, List<File> dppl, File unavailableFile) {
        if (!ppl.isEmpty()) {
            boolean forwardProgression = previous_next == next;
            Predicate<File> predicate = ((file) -> file.exists());
            File availableFile = (forwardProgression)? Utility.nextItem(unavailableFile, predicate, ppl): Utility.previousItem(unavailableFile, predicate, ppl);
            boolean found = availableFile != null && availableFile != unavailableFile;
            Platform.runLater(() -> {
                if (GPlayerSettings.playListPrefs().getBoolean("unavailableFiles", true)) {
                    List<File> files = enlist(ppl, ((file) -> !file.exists()));
                    discard(files, ppl, dppl);
                }
                if (GPlayerSettings.playListPrefs().getBoolean("unavailableFiles", true))
                    deleteUnavailableMediaMarks();
            });
            if (!unavailableFile.exists()) {
                if (fileNotFoundAlert == null || !fileNotFoundAlert.isShowing()) {
                    fileNotFoundAlert = FileManager.createAlert(AlertType.INFORMATION);
                    fileNotFoundAlert.setContentText(resource.getAndFormatMessage("fileNotFoundAlert.message", fileToString(unavailableFile)));  //inform the user that the file location cannot be found
                    fileNotFoundAlert.setOnHidden((de) -> resumePlay(wasPaused));
                    if (stage.isShowing()) {
                        fileNotFoundAlert.show();
                        dismissNotification(fileNotFoundAlert);
                    }
                }
            }
            if (found)
                //Begin play
                playMedia(ppl.indexOf(availableFile), autoPlay);
        }
    }


    private <T extends List<File>> T sortPlayList(T ppl, T dppl, FileSortParameter PARAMETER, T returnList) {
        if (PARAMETER == FileSortParameter.DEFAULT || returnList.size() < 2)
            return returnList;
        if (dppl != durationMatch && dppl != lastDurationMatch) {
            if (PARAMETER != FileSortParameter.SHUFFLE) {
                boolean ascending = PARAMETER == FileSortParameter.ASCENDING;
                returnList.sort((a, b) -> {
                    String aString = Utility.retainLettersAndDigits(fileToString(a)), bString = Utility.retainLettersAndDigits(fileToString(b));
                    return (ascending)? aString.compareToIgnoreCase(bString): bString.compareToIgnoreCase(aString);
                });
            }
            else
                java.util.Collections.shuffle(returnList);  //Have returnList shuffled
        }
        else {  //defaultPlayingPlayList equals either of durationMatch or lastDurationMatch
            durationTime = new ArrayList<>(durationTime);
            if (PARAMETER == FileSortParameter.SHUFFLE) {
                //Random number generator
                java.util.Random random = new java.util.Random();
                for (int i = returnList.size()-1; i > 0; i--) {
                    //Generate a random number between 0 (inclusive), and the value of i + 1 (exclusive)
                    int randomInt = random.nextInt(i+1);
                    //So as not to have the item at index randomInt generated again,
                    //we need to swap its position with that at index i.
                    swap(returnList, randomInt, i);
                    //Do likewise for durationTime
                    swap(durationTime, randomInt, i);
                }
            }
            else { //If either of sortAscending or sortDescending is selected
                boolean ascending = PARAMETER == FileSortParameter.ASCENDING;
                for (int i = 0; i < returnList.size()-1; i++) {
                    //An inner loop
                    for (int j = i+1; j < returnList.size(); j++) {
                        String firstString = Utility.retainLettersAndDigits(fileToString(returnList.get(i)));
                        String secondString = Utility.retainLettersAndDigits(fileToString(returnList.get(j)));
                        int comparisonNum = firstString.compareToIgnoreCase(secondString);
                        if ((ascending && comparisonNum > 0) || (!ascending && comparisonNum <= 0)) {
                            swap(returnList, i, j);
                            swap(durationTime, i, j);
                        }
                    }  //Closes inner loop
                }  //Closes outer loop
            }
        }
        return returnList;
    }


    private void sortPlayList(LinkedList<File> ppl, int position) {
        logger.info("Preparing to sort playlist contents of size: " + ppl.size());
        //Run in the backround on another thread
        initiateBackgroundTask((() -> {
            long startTime = System.nanoTime();
            filePosition = position;
            if (sortAscending.isSelected() || sortDescending.isSelected())
                logger.info((sortAscending.isSelected())? "Ascending playlist sorting": "Descending playlist sorting");
            else
                logger.info("Random playlist sorting");
            playingPlayList = sortPlayList(ppl, defaultPlayingPlayList, FileSortParameter.getSortParameter(), new LinkedList<File>(ppl));
            logger.info("Completed sorting in: " + (System.nanoTime() - startTime));
            //Pass the batton back to the main thread
            Platform.runLater(() -> organizeControls(filePosition));
        }));
    }


    final private void saveSelectedFile(File selectedFile, int fileNumber) {
        switch (fileNumber) {
            case 0:
                filesFileFolder = (selectedFile == null)? null: new File(selectedFile.getAbsolutePath());
                break;
            case 1:
                audioFileFolder = (selectedFile == null)? null: new File(selectedFile.getAbsolutePath());
                break;
            default:
                videoFileFolder = (selectedFile == null)? null: new File(selectedFile.getAbsolutePath());
        }
    }


    private File resolveSelectedFile(int i, File f) {
        if (i > 2) {
            File file = f;
            if (file != null && !file.exists())  //If the location is unavailable
                file = null;
            if (file != null)
                file = (file.isFile())? new File(new File(file.getParent()).getParent()): new File(file.getParent());
            return file;
        }
        else
            return (f != null && f.isFile())? new File(f.getParent()): f;
    }


    private File openSelectedFile(int fileNumber) {
        switch (fileNumber) {
            case 0:
                return (filesFileFolder = resolveSelectedFile(fileNumber, filesFileFolder));
            case 1:
                return (audioFileFolder = resolveSelectedFile(fileNumber, audioFileFolder));
            case 2:
                return (videoFileFolder = resolveSelectedFile(fileNumber, videoFileFolder));
            case 3:
                return (filesFileFolder = resolveSelectedFile(fileNumber, filesFileFolder));
            case 4:
                return (audioFileFolder = resolveSelectedFile(fileNumber, audioFileFolder));
            default:
                return (videoFileFolder = resolveSelectedFile(fileNumber, videoFileFolder));
        }
    }


    private void setSpeedRate(MenuItem item, double rate) {
        if (item.getText().equals(resource.getString("speedUp.text")))
            speedRate = (rate > GPlayerSettings.playBackPrefs().getDouble("speedHighestValue", 1.5))? GPlayerSettings.playBackPrefs().getDouble("speedHighestValue", 1.5): rate;
        else if (item.getText().equals(resource.getString("speedDown.text")))
            speedRate = (rate < GPlayerSettings.playBackPrefs().getDouble("speedLowestValue", 0.8))? GPlayerSettings.playBackPrefs().getDouble("speedLowestValue", 0.8): rate;
        mediaPlayer.setRate(speedRate);
        normalise.setDisable(speedRate == 1.0);
    }


    private void setAudioBalance(double audioBalanceLevel) {
        this.audioBalanceLevel = audioBalanceLevel;
        mediaPlayer.setBalance(audioBalanceLevel);
        mediaPlayer.setMute(false);
    }


    private void showFileOpenDialog(int openOption, String title, File folder, ExtensionFilter extensionFilter, boolean multipleSelection) {
        wasPaused = pauseMedia();
        fileChooser = new FileChooser();  //A FileChooser object
        fileChooser.setTitle(title);  //The title of the dialog box
        if (folder != null && folder.exists()) {  //If a selection has previously been made,
            //and its location is still available
            fileChooser.setInitialFileName(folder.getPath());
            fileChooser.setInitialDirectory(openSelectedFile(openOption));  //open the dialog to that same directory on invokation
        }
        fileChooser.getExtensionFilters().add(extensionFilter);
        try {
            final ObservableList<File> files = FXCollections.observableArrayList();
            files.addAll((multipleSelection)? fileChooser.showOpenMultipleDialog(stage): Arrays.<File>asList(fileChooser.showOpenDialog(stage)));
            if (files.get(0) != null) {
                initiateBackgroundTask((() -> {
                    final File selectedFile = files.get(0);
                    logger.info("About to retrieve unique contents of user file selection of size: " + files.size());
                    long startTime = System.nanoTime();
                    playListEntry.addAll(uniqueElements(playListEntry, files));  //the array of files is stored in playListEntry
                    logger.info("Completed retrieval of unique contents of user file selection in: " + (System.nanoTime() - startTime));
                    displayRetrievedFilePopup(files.size());
                    openFiles(selectedFile, selectedFile, openOption, filePosition);
                }));
            }
        }
        catch (Exception ex) {
            if (folder != null)
                saveSelectedFile(folder, openOption);
        }
        resumePlay(wasPaused);  //Resume playback
    }


    private void showDirectoryOpenDialog(int openOption, FileOpenOption fileOpenOption, File directory) {
        wasPaused = pauseMedia();
        try {
            final FolderChooser chooser = new FolderChooser(fileOpenOption, stage, openSelectedFile(openOption+3));
            initiateBackgroundTask((() -> {
                List<File> files = chooser.fetchFolderItems();
                if (!files.isEmpty()) {
                    logger.info("About to retrieve unique contents of user file selection of size: " + files.size());
                    long startTime = System.nanoTime();
                    playListEntry.addAll(uniqueElements(playListEntry, files));
                    logger.info("Completed retrieval of unique contents of user file selection in: " + (System.nanoTime() - startTime));
                    File selectedFile = chooser.path.toFile();
                    openFiles(selectedFile, files.get(0), openOption, filePosition);
                }
                else
                    if (directory != null)
                        saveSelectedFile(directory, openOption);
                displayRetrievedFilePopup(files.size());
            }));
        }
        catch (Exception ex) {
            if (directory != null)
                saveSelectedFile(directory, openOption);
        }
        resumePlay(wasPaused);
    }


    public void openFile(File selectedFile, File nextFile) {
        playListEntry.remove(nextFile);
        playListEntry.add(nextFile);
        openFiles(selectedFile, nextFile, 0, filePosition);
    }


    private void openFiles(File selectedFile, File nextFile, int fileNumber, int fp) {
        if (selectedFile != null)  //If selectedFile refers a valid File
            saveSelectedFile(selectedFile, fileNumber);
        //        playedPlayListFilePosition = (playListEntry == defaultPlayingPlayList)? playedPlayListFilePosition: fp;
        LinkedList<File> ppl = playingPlayList;
        LinkedList<File> dppl = defaultPlayingPlayList;
        if (playingPlayList != playListEntry) {  //If playingPlayList does not refer to play list
            if (defaultPlayingPlayList == playListEntry) {
                int filePosition = playListEntry.indexOf(nextFile);
                List<File> list = playListEntry.subList(filePosition, playListEntry.size());
                //Add the recently selected files to playingPlayList
                playingPlayList.addAll(uniqueElements(playingPlayList, list));
            }
            else  //If defaultPlayingPlayList does not equals playListEntry
                playingPlayList = playListEntry;
        }
        LinkedList<File> nppl = playingPlayList;
        Platform.runLater(() -> {
            if (nppl == playingPlayList) {
                int nextFilePosition = playingPlayList.indexOf(nextFile);
                nextFilePosition = (nextFilePosition == -1)? 0: nextFilePosition;
                transitionPlayList(filePosition, nextFilePosition, ppl, dppl, nppl, playListEntry, FileSortParameter.getSortParameter(), FileSortParameter.DEFAULT, false);
            }
        });
    }


    private boolean pauseMedia() {
        showLibraryPopup(false);
        isDialogShowing = true;
        Status status = (mediaPlayer == null)? null: mediaPlayer.getStatus();
        boolean paused = false;
        if (status != null) {
            if (!isAudio && GPlayerSettings.viewPrefs().getBoolean("stageViewOnly", true) && status == Status.PLAYING) {
                mediaPlayer.pause();  //Pause the media
                GPlayer.notify(videoPlaybackNotification());
                paused = true;
            }
        }
        return paused;
    }


    private void fire(MenuItem item) {
        if (item != null) {
            if (item.isDisable())
                item.setDisable(false);
            item.fire();
        }
    }


    private boolean playNextIf(boolean condition, int fp) {
        boolean autoPlay = (mediaPlayer != null && mediaPlayer.getStatus() == Status.PLAYING);
        boolean played = (condition)? playMedia((filePosition = (fp >= playingPlayList.size())? 0: fp), autoPlay): false;
        displayPlayList(playingPlayList, filePosition);
        return played;
    }


    public void resumePlay(boolean wasPaused) {
        if (wasPaused && mediaPlayer != null && mediaPlayer.getStatus() != Status.PLAYING)
            play_pause.fire();
        isDialogShowing = false;
        showLibraryPopup(true);
    }


    private void reflectPlayState(Status PLAYSTATE) {
        boolean adjust = PLAYSTATE != Status.PAUSED;
        switch (PLAYSTATE) {
            case PLAYING:
                play_pause.setGraphic(new ImageView(pauseImage));
                play_pause.setText(resource.getString("pause.text"));
                play_pause.getTooltip().setText(resource.getString("pause.tooltip"));
                break;
            default:
                play_pause.setGraphic(new ImageView(playImage));
                play_pause.setText(resource.getString("play.text"));
                play_pause.getTooltip().setText(resource.getString("play.tooltip"));
        }
        if (adjust) {
            boolean disable = PLAYSTATE != Status.PLAYING;
            stop.setDisable(disable);
            disableControls(1, disable);
        }
    }


    private String videoPlaybackNotification() {
        return String.join("\n", resource.getStringFamily("videoPlaybackNotification.message"));
    }


    private void group(ToggleGroup group, RadioMenuItem... members) {
        for (RadioMenuItem member: members)
            member.setToggleGroup(group);
    }


    private <T extends javafx.scene.control.Dialog> T inlayDialog(T dialog, String key) {
        dialog.setHeaderText(resource.getString("dialog.headerText"));
        String[] keys = {key + ".title", key + ".contentText"};
        int length = keys.length;
        for (int i = 0; i < length; i++) {
            Object object = resource.handleGetObject(keys[i]);
            if (object != null) {
                switch (i) {
                    case 0:
                        dialog.setTitle(object.toString());
                        break;
                    case 1:
                        dialog.setContentText(object.toString());
                        break;
                    default:
                        return dialog;
                }
            }
        }
        return dialog;
    }


    private java.awt.MenuItem getPlaybackOptions(String label, Runnable runnable) {
        java.awt.MenuItem item = new java.awt.MenuItem(label);
        item.addActionListener((event) -> Platform.runLater(runnable));
        return item;
    }


    private java.awt.MenuItem getPlaybackOptions(MenuItem item) {
        final java.awt.MenuItem subItem = getPlaybackOptions(item.getText(), item::fire);
        subItem.setEnabled(!item.isDisable());
        item.textProperty().addListener((listener) -> subItem.setLabel(item.getText()));
        item.disableProperty().addListener((listener) -> subItem.setEnabled(!item.isDisable()));
        return subItem;
    }


    /**Configures an awt menu to display on OS system tray.
    *@return java.awt.Menu instance with all its components
    */
    @SuppressWarnings("unchecked")
    private java.awt.Menu configureTrayPlaybackOptions() {
        String[] texts = resource.getStringFamily("configureTrayPlaybackOptions.text");
        java.awt.Menu menu = new java.awt.Menu(texts[0]);
        MenuItem[] items = {play_pauseItem, nextItem, previousItem, stopItem};
        for (MenuItem item: items)
            menu.add(getPlaybackOptions(item));
        Duo[] maps = {new Duo(texts[1], KeyCode.UP), new Duo(texts[2], KeyCode.DOWN), new Duo(texts[3], KeyCode.RIGHT), new Duo(texts[4], KeyCode.LEFT)};
        for (Duo<String, KeyCode> map: maps) {
            java.awt.MenuItem item = getPlaybackOptions(map.getKey(), () -> setSeekSlider(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", map.getValue(), false, true, false, false)));
            item.setEnabled(!stop.isDisable());
            stop.disableProperty().addListener((listener) -> item.setEnabled(!stop.isDisable()));
            menu.add(item);
        }
        return menu;
    }


    private MenuItem createMenuItem(String key, boolean state, Object... objects) {
        String prefix = key + ".";
        String accelerator = prefix.concat("accelerator"), icon = prefix.concat("icon");
        String text = (objects.length == 0)? resource.getString(prefix + "text"): resource.getAndFormatMessage(prefix + "text", objects);  //The text to be displayed on the menu item
        MenuItem item = new MenuItem(text);
        if (resource.handleGetObject(accelerator) != null)
            item.setAccelerator(KeyCombination.keyCombination(resource.getString(accelerator)));  //Key combination that activates the menu item
        if (resource.handleGetObject(icon) != null)
            item.setGraphic(new ImageView(iconify(resource.getString(icon))));  //The image shown on the button
        item.setDisable(state);
        return item;
    }


    private MenuItem createMenuItem(String key) {
        return createMenuItem(key, false);
    }


    private CheckMenuItem createCheckMenuItem(String key, boolean state) {
        String text = resource.getString(key + ".text");
        CheckMenuItem item = new CheckMenuItem(text);
        item.setDisable(state);
        return item;
    }


    private RadioMenuItem createRadioMenuItem(String key, boolean state) {
        String text = resource.getString(key + ".text");
        RadioMenuItem item = new RadioMenuItem(text);
        item.setSelected(state);
        return item;
    }


    private MenuBar configureMenuBar() {
        //Create an empty menu bar object
        menuBar = new MenuBar();

        //Create a file menu
        Menu fileMenu = new Menu(resource.getString("fileMenu.text"));

        //fileMenu children
        openFile = createMenuItem("openFile");  //for selecting a single file at a time
        openFile.setOnAction((ae) -> showFileOpenDialog(0, "Select a file to be played", filesFileFolder, new ExtensionFilter("Media files", ALL_EXTENSIONS), false));

        //for selecting multiple files at a time
        openFiles = createMenuItem("openFiles");
        openFiles.setOnAction((ae) -> showFileOpenDialog(0, "Select multiple files to be played", filesFileFolder, new ExtensionFilter("Media files", ALL_EXTENSIONS), true));

        //item for opening folders
        openFileFolder = createMenuItem("openFileFolder");
        openFileFolder.setOnAction((ae) -> showDirectoryOpenDialog(0, FileOpenOption.FILEFOLDER, filesFileFolder));

        //Create a menu to hold
        //both open file open options,
        //and the openFileFolder item
        Menu open = new Menu(resource.getString("open.text"));
        //Add openFile and openFiles
        //to the open menu
        open.getItems().addAll(openFile, openFiles, openFileFolder);

        quitFile = createMenuItem("quitFile", true);  //Quits current media
        quitFile.setOnAction((ae) -> {
            stop.fire();  //Terminate the active file
        });

        recallMediaStopPosition = createCheckMenuItem("recallMediaStopPosition", true);
        recallMediaStopPosition.selectedProperty().addListener((listener) -> {
            markMediaStopPosition = recallMediaStopPosition.isSelected();
            preferences.getUserPreferences().putBoolean("markMediaStopPosition", markMediaStopPosition);
            Map<String, Trio<File, Duration, Boolean>> map = new HashMap<>(mediaDurationMap);
            Set<String> set = map.keySet();
            for (String key: set) {
                if (key.charAt(0) != ' ')
                    continue;
                if (!markMediaPosition(map.get(key).getKey()))
                    if (!map.get(key).getExtension())
                        mediaDurationMap.remove(key);
            }
        });

        backwardPlay = createCheckMenuItem("backwardPlay", false);

        contextMenu.getItems().addAll(recallMediaStopPosition, backwardPlay);

        quitApplicationOnMediaEnd = createCheckMenuItem("quitApplicationOnMediaEnd", true);
        quitApplicationOnMediaEnd.setOnAction((ae) -> {
            if (quitApplicationOnMediaEnd.isSelected())
                quitApplicationOnPlayListEnd.setSelected(false);
        });

        quitApplicationOnPlayListEnd = new CheckMenuItem("Quit application at the end of playlist");
        quitApplicationOnPlayListEnd.setOnAction((ae) -> {
            if (quitApplicationOnPlayListEnd.isSelected())
                quitApplicationOnMediaEnd.setSelected(false);
        });

        //Create an exit menu item
        exitApplication = createMenuItem("exitApplication");
        exitApplication.setOnAction((ae) -> {
            Event.fireEvent(stage, new javafx.stage.WindowEvent(null, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
        });

        quitApplication = createMenuItem("quitApplication", false, exitTime);
        quitApplication.setOnAction((ae) -> {
            wasPaused = pauseMedia();
            int index = exitTimeOptions.indexOf(exitTime);
            index = (index == -1)? 0: index;
            ChoiceDialog<String> choiceDialog = inlayDialog(new ChoiceDialog<>(exitTimeOptions.get(index), exitTimeOptions), "quitApplicationDialog");
            Optional<String> selectedTime = choiceDialog.showAndWait();
            if (selectedTime.isPresent())
                quitApplicationIn(selectedTime.get());
            resumePlay(wasPaused);
        });

        //Add the menu items
        //to the file menu
        fileMenu.getItems().addAll(open, recallMediaStopPosition, backwardPlay, quitFile, quitApplicationOnMediaEnd, quitApplicationOnPlayListEnd, quitApplication, new SeparatorMenuItem(), exitApplication);

        //Create an audio menu
        Menu audioMenu = new Menu(resource.getString("audioMenu.text"));
        //Assign it some menu items
        openAudioFile = createMenuItem("openAudioFile");  //Only permits a single selection of audio files
        openAudioFile.setOnAction((ae) -> showFileOpenDialog(1, "Select an audio file to be played", audioFileFolder, new ExtensionFilter("Audio files", AUDIO_EXTENSIONS), false));

        //Create another menu item for the audio menu
        openAudioFiles = createMenuItem("openAudioFiles");  //Permits a multiple selection of audio files
        openAudioFiles.setOnAction((ae) -> showFileOpenDialog(1, "Select Audio files to be played", audioFileFolder, new ExtensionFilter("Audio files", AUDIO_EXTENSIONS), true));

        openAudioFolder = createMenuItem("openAudioFolder");  //Accesses the audio files only
        openAudioFolder.setOnAction((ae) -> showDirectoryOpenDialog(1, FileOpenOption.AUDIOFOLDER, audioFileFolder));

        //Create a menu to hold both the
        //openAudioFile and openAudioFiles
        Menu openAudio = new Menu(resource.getString("openAudio.text"));
        //Add the items to the menu
        openAudio.getItems().addAll(openAudioFile, openAudioFiles, openAudioFolder);

        //Radio menu items for controlling audio effects
        defaultAudioBalance = createRadioMenuItem("defaultAudioBalance", true);
        defaultAudioBalance.setOnAction((ae) -> setAudioBalance(0.0));

        leftAudioBalance = createRadioMenuItem("leftAudioBalance", false);
        leftAudioBalance.setOnAction((ae) -> setAudioBalance(-1.0));

        rightAudioBalance = createRadioMenuItem("rightAudioBalance", false);
        rightAudioBalance.setOnAction((ae) -> setAudioBalance(1.0));

        audioMute = createRadioMenuItem("audioMute", false);
        audioMute.setOnAction((ae) -> mediaPlayer.muteProperty().setValue(!muteAudio));

        //Anchor the audio effects items to a toggle group
        group(new ToggleGroup(), defaultAudioBalance, leftAudioBalance, rightAudioBalance, audioMute);

        Menu audioEffects = new Menu(resource.getString("audioEffects.text"));
        audioEffects.getItems().addAll(defaultAudioBalance, leftAudioBalance, rightAudioBalance, audioMute);
        contextMenu.getItems().add(audioEffects);
        audioMenu.getItems().addAll(openAudio, audioEffects);

        Menu videoMenu = new Menu(resource.getString("videoMenu.text"));

        openVideoFile = createMenuItem("openVideoFile");  //Single video file selector
        openVideoFile.setOnAction((ae) -> showFileOpenDialog(2, "Select a Video file to be played", videoFileFolder, new ExtensionFilter("Video files", VIDEO_EXTENSIONS), false));

        openVideoFiles = createMenuItem("openVideoFiles");  //Multiple video file selector
        openVideoFiles.setOnAction((ae) -> showFileOpenDialog(2, "Select Video files to be played", videoFileFolder, new ExtensionFilter("Video files", VIDEO_EXTENSIONS), true));

        openVideoFolder = createMenuItem("openVideoFolder");
        openVideoFolder.setOnAction((ae) -> showDirectoryOpenDialog(2, FileOpenOption.VIDEOFOLDER, videoFileFolder));

        //A menu to hold the video file items
        Menu openVideo = new Menu(resource.getString("openVideo.text"));
        //Add the items to the menu
        openVideo.getItems().addAll(openVideoFile, openVideoFiles, openVideoFolder);

        fullScreen = createMenuItem("fullScreen", true);  //responsible for maximizing the arial view of a video
        fullScreen.setOnAction((ae) -> stage.setFullScreen(true));

        //Add the full screen and videoStopMark menus to the context menu
        contextMenu.getItems().add(fullScreen);

        //Add the menus to the video
        videoMenu.getItems().addAll(openVideo, new SeparatorMenuItem(), fullScreen);

        Menu playbackMenu = new Menu(resource.getString("playbackMenu.text"));

        rewind = createMenuItem("rewind", true);
        rewind.setOnAction((ae) -> mediaPlayer.seek(mediaPlayer.getStartTime())); //Takes the media back to the start point

        jumpForward = createMenuItem("jumpForward", true);
        jumpForward.setOnAction((ae) -> {
            long time = Utility.convertTimeToMillis(GPlayerSettings.playBackPrefs().get("forwardJumpTime", Utility.formatTime(20, 2)));
            Duration jumpTime = mediaPlayer.getCurrentTime().add(Duration.millis(time));
            mediaPlayer.seek(jumpTime);
        });

        jumpForwardByTime = createMenuItem("jumpForwardByTime", true);  //Jumps forward by a time specified by the user
        jumpForwardByTime.setOnAction((ae) -> {
            wasPaused = pauseMedia();
            //load from resource
            TextInputDialog dialog = inlayDialog(new TextInputDialog(Utility.formatTime(Duration.millis(Utility.convertTimeToMillis(GPlayerSettings.playBackPrefs().get("forwardJumpTime", Utility.formatTime(20, 2)))), Duration.ZERO)), "jumpForwardByTimeDialog");
            Duration currentTime = mediaPlayer.getCurrentTime();
            Optional<String> dialogResult = dialog.showAndWait();  //Awaits user input
            if (dialogResult.isPresent()) {  //If result has been entered,
                double skipTime = timeToSkipTo(dialogResult.get(), 1);
                currentTime = mediaPlayer.getCurrentTime();
                if (skipTime > 0.0)
                    mediaPlayer.seek(currentTime.add(Duration.millis(skipTime)));
            }
            resumePlay(wasPaused);
        });

        jumpBackward = createMenuItem("jumpBackward", true);
        jumpBackward.setOnAction((ae) -> {
            long time = Utility.convertTimeToMillis(GPlayerSettings.playBackPrefs().get("backwardJumpTime", Utility.formatTime(10, 2)));  //load from resource
            Duration jumpTime = mediaPlayer.getCurrentTime().subtract(Duration.millis(time));
            mediaPlayer.seek(jumpTime);
        });

        jumpBackwardByTime = createMenuItem("jumpBackwardByTime", true);  //Jumps backward by a time specified by the user
        jumpBackwardByTime.setOnAction((ae) -> {
            wasPaused = pauseMedia();
            TextInputDialog dialog = inlayDialog(new TextInputDialog(Utility.formatTime(Duration.millis(Utility.convertTimeToMillis(GPlayerSettings.playBackPrefs().get("backwardJumpTime", Utility.formatTime(10, 2)))), Duration.ZERO)), "jumpBackwardByTimeDialog");
            Duration currentTime = mediaPlayer.getCurrentTime();
            Optional<String> dialogResult = dialog.showAndWait();  //Awaits user input
            if (dialogResult.isPresent()) {  //If result has been entered,
                double skipTime = timeToSkipTo(dialogResult.get(), 0);
                currentTime = mediaPlayer.getCurrentTime();
                if (skipTime > 0.0)
                    mediaPlayer.seek(currentTime.subtract(Duration.millis(skipTime)));
            }
            resumePlay(wasPaused);
        });

        jumpToEnd = createMenuItem("jumpToEnd", true);
        jumpToEnd.setOnAction((ae) -> {
            isValueChanging = true;
            timeSlider.setValue(100.0);
        });

        //Jump to specific time
        jumpToTime = createMenuItem("jumpToTime", true);
        jumpToTime.setOnAction((ae) -> {
            wasPaused = pauseMedia();
            TextInputDialog dialog = inlayDialog(new TextInputDialog(), "jumpToTimeDialog");
            Duration currentTime = mediaPlayer.getCurrentTime();
            dialog.setContentText(resource.getAndFormatMessage("jumpToTimeDialog.contentText", Utility.formatTime(currentTime, Duration.ZERO)));   
            Optional<String> dialogResult = dialog.showAndWait();  //Awaits user input
            if (dialogResult.isPresent()) {  //If result has been entered,
                double skipTime = timeToSkipTo(dialogResult.get(), 2);
                if (skipTime >= 0.0)
                    mediaPlayer.seek(Duration.millis(skipTime));
            }
            resumePlay(wasPaused);
        });

        speedUp = createMenuItem("speedUp", true);  //For increasing play speed
        speedUp.setOnAction((ae) -> setSpeedRate(speedUp, (speedRate = speedRate == GPlayerSettings.playBackPrefs().getDouble("speedHighestValue", 1.5)? 1.0: speedRate+GPlayerSettings.playBackPrefs().getDouble("speedStepLevel", 0.05))));

        speedDown = createMenuItem("speedDown", true);
        speedDown.setOnAction((ae) -> setSpeedRate(speedDown, (speedRate = speedRate == GPlayerSettings.playBackPrefs().getDouble("speedLowestValue", 0.8)? 1.0: speedRate-GPlayerSettings.playBackPrefs().getDouble("speedStepLevel", 0.05))));

        //A menu item to normalize speed rate
        normalise = createMenuItem("normalise", true);
        normalise.setOnAction((ae) -> setSpeedRate(normalise, (speedRate = 1.0)));

        previousItem = createMenuItem("previous", true);
        previousItem.setOnAction((ae) -> {
            previous.requestFocus();  //Put focus on the previous button
            previous.fire();  //Invokes the previous button
        });

        nextItem = createMenuItem("next", true);
        nextItem.setOnAction((ae) -> {
            next.requestFocus();  //Focus the next button
            next.fire();  //Invokes the next button
        });

        play_pauseItem = createMenuItem("play", true);
        play_pauseItem.setOnAction((ae) -> {
            play_pause.requestFocus();
            play_pause.fire();  //Invokes the play/pause button
        });

        stopItem = createMenuItem("stop", true);
        stopItem.setOnAction((ae) -> stop.fire());

        mediaPositionMarker = createMenuItem("mediaPositionMarker", true);  //marks media position
        mediaPositionMarker.setOnAction((ae) -> {
            //Obtain media current time
            Duration currentTime = mediaPlayer.getCurrentTime();
            wasPaused = pauseMedia();
            File file = currentFile;  //currentFile reflects the current playing media
            List<String> list = new ArrayList<>();
            Set<String> set = mediaDurationMap.keySet();
            //Save  file related existing media-marks
            for (String s: set) {
                if (!s.startsWith(" ") && mediaDurationMap.get(s).getKey().equals(file))
                    list.add(Utility.postCharacterString(s, separator, true));
            }
            String uniqueName = (!list.isEmpty())? list.get(list.size()-1): resource.getString("mediaMark.single.word");
            String resultText = Utility.generateUniqueName(uniqueName, list);
            boolean showDialog = GPlayerSettings.generalPrefs().getBoolean("showMediaMarkDialog", true);
            if (showDialog) {
                TextInputDialog dialog = inlayDialog(new TextInputDialog(Utility.generateUniqueName(uniqueName, list)), "mediaPositionMarkerDialog");
                Optional<String> result = dialog.showAndWait();  //The input is saved in 'result'
                if (result.isPresent()) {
                    resultText = result.get().trim().toLowerCase();  //The string representation of the entered value is set to lower case, trimmed, and saved in resultText
                    showDialog = false;
                }
            }
            if (!showDialog) {
                list.remove(resultText);
                resultText = assertUniqueName(list.toArray(new String[list.size()]), uniqueName, resultText);
                if (resultText != null) {
                    if (mediaMarkerSearch.isDisable())
                        mediaMarkerSearch.setDisable(false);
                    configureMediaMarkers(resultText, file, currentTime, true);
                    notify(resource.getString("mediaPositionMarker.message"));
                }
            }
            resumePlay(wasPaused);
        });

        //To search for a specified marked position
        mediaMarkerSearch = createMenuItem("mediaMarkerSearch");
        mediaMarkerSearch.setOnAction((ae) -> {
            wasPaused = pauseMedia();
            TextInputDialog dialog = inlayDialog(new TextInputDialog(), "mediaMarkerSearchDialog");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String text = result.get();
                retrieveMarkedPositions(text, 0);
            }
            resumePlay(wasPaused);
        });

        //To edit focused media-marked position
        mediaMarkerEdit = createMenuItem("mediaMarkerEdit", true);
        mediaMarkerEdit.setOnAction((ae) -> {
            final Duration currentDuration = durationProperty.getValue();
            final File file = currentFile;
            final TreeMap<Duration, String> durationSet = this.durationSet;
            final String name = Utility.postCharacterString(durationSet.get(currentDuration), separator, true);
            //Save  file related existing media-marks
            final List<String> list = new ArrayList<>();
            durationSet.keySet().forEach((duration) -> {
                if (currentDuration != duration)
                    list.add(Utility.postCharacterString(durationSet.get(duration), separator, true));
            });
            wasPaused = pauseMedia();
            TextInputDialog dialog = inlayDialog(new TextInputDialog(), "mediaMarkerEditDialog");
            dialog.setContentText(resource.getAndFormatMessage("mediaMarkerEditDialog.contentText", name));
            final HBox content = setAndGetControls(durationSet, file, currentDuration, duration, dialog);
            dialog.getDialogPane().setContent(content);
            dialog.resultProperty().addListener((listener) -> {
                String text = ((TextField)content.getChildren().get(0)).getText();
                text = assertUniqueName(list.toArray(new String[list.size()]), name, text);
                if (text != null) {
                    double value = (double) ((Spinner)content.getChildren().get(2)).getValue();
                    if (!text.equals(name) || value != currentDuration.toSeconds()) {
                        String str = String.join(File.separator, file.getPath(), name);
                        //Obtain key name value
                        Trio<File, Duration, Boolean> t = mediaDurationMap.get(str);
                        //Convert value to millisecond equivalent in Duration
                        Duration d = Duration.millis(value * 1000);
                        t.setValue(d);
                        if (file.equals(currentFile)) {
                            durationSet.remove(currentDuration);
                            durationSet.put(d, String.join(File.separator, file.getPath(), text));
                            durationProperty.setValue(d);
                        }
                        if (!name.equals(text)) {
                            if (file.equals(currentFile))
                                mediaMarkerEdit.setText(resource.getAndFormatMessage("mediaMarkerEditDialog.contentText", text));
                            Utility.updateMap(mediaDurationMap, String.join(File.separator, file.getPath(), name), String.join(File.separator, file.getPath(), text), t);
                        }
                        updateMediaInfo(false);
                    }
                }
            });
            dialog.show();
            ((TextField)content.getChildren().get(0)).requestFocus();
        });

        nextMediaMarker = createMenuItem("nextMediaMarker");  //To move to the next marked position
        nextMediaMarker.setOnAction((ae) -> {
            //Obtain the duration entry in durationSet
            java.util.Iterator<Duration> iterator = durationSet.keySet().iterator();
            Duration duration = iterator.next(), currentTime = mediaPlayer.getCurrentTime();
            if (duration.lessThan(currentTime) || duration.equals(currentTime)) {
                while (iterator.hasNext()) {  //A while loop to cycle through the items in durationSet
                    Duration thisDuration = iterator.next();
                    if (thisDuration.greaterThan(currentTime)) {  //If the retrieved time is greater than the current media player time
                        duration = thisDuration;
                        break;
                    }
                }
            }
            lastMarkedTime = duration;
            mediaPlayer.seek(duration);  //Skips to the next marked position in the array
            //Update mediaMarkerEdit position focus
            durationProperty.setValue(duration);
        });

        //A previous media marker
        previousMediaMarker = createMenuItem("previousMediaMarker");
        previousMediaMarker.setOnAction((ae) -> {
            java.util.Iterator<Duration> iterator = durationSet.descendingKeySet().iterator();
            Duration currentTime = mediaPlayer.getCurrentTime(), duration = iterator.next();
            Duration timeDifference = currentTime.subtract(lastMarkedTime);
            if (lastMarkedTime.greaterThan(Duration.ZERO) && timeDifference.greaterThan(Duration.millis(-1)) && timeDifference.lessThan(Duration.millis(Utility.convertTimeToMillis(GPlayerSettings.playBackPrefs().get("previousMarkedTime", Utility.formatTime(3, 2))))))
                currentTime = lastMarkedTime;
            if (duration.greaterThan(currentTime) || duration.equals(currentTime)) {
                while (iterator.hasNext()) {
                    Duration thisDuration = iterator.next();
                    if (thisDuration.lessThan(currentTime)) {
                        duration = thisDuration;
                        break;
                    }
                }
            }
            lastMarkedTime = duration;
            mediaPlayer.seek(duration);
            //Update mediaMarkerEdit position focus
            durationProperty.setValue(duration);
        });

        availableMediaMarks = new Menu(resource.getString("availableMediaMarks.text"));
        availableMediaMarks.setDisable(true);

        Menu mediaMarkMenu = new Menu(resource.getString("mediaMarkMenu.text"));
        mediaMarkMenu.getItems().addAll(availableMediaMarks, mediaPositionMarker, mediaMarkerSearch, mediaMarkerEdit);
        //Add the media marker items to the context menu
        contextMenu.getItems().addAll(mediaMarkMenu, previousMediaMarker, nextMediaMarker);

        playbackMenu.getItems().addAll(play_pauseItem, stopItem, previousItem, nextItem, rewind, jumpForward, jumpForwardByTime, jumpBackward, jumpBackwardByTime, jumpToEnd, jumpToTime, speedUp, speedDown, normalise, new SeparatorMenuItem(), mediaMarkMenu, nextMediaMarker, previousMediaMarker);  //The various items are added to the
        //playback menu

        //Create a play list menu
        Menu playListMenu = new Menu(resource.getString("playListMenu.text")), playMenu = new Menu(resource.getString("playMenu.text"));
        openPlayList = createMenuItem("openPlayList");  //For playing all media files
        openPlayList.setOnAction((ae) -> transitionPlayList(filePosition, 0, playingPlayList, defaultPlayingPlayList, playListEntry, playListEntry, FileSortParameter.getSortParameter(), PARAMETER, true));

        openRecentPlayList = createMenuItem("openRecentPlayList");
        openRecentPlayList.setOnAction((ae) -> transitionMostRecentPlayList(recentPlayList, "recentPlayList"));

        openMostPlayedPlayList = createMenuItem("openMostPlayedPlayList");
        openMostPlayedPlayList.setOnAction((ae) -> transitionMostRecentPlayList(mostPlayedPlayList, "mostPlayedPlayList"));

        openUnfilteredPlayList = createMenuItem("openUnfilteredPlayList", true);
        openUnfilteredPlayList.setOnAction((ae) -> {
            playingPlayList = unfilteredPlayList;
            if (sortUnfilteredPlayList && unfilteredPlayList.size() > 1)
                sortPlayList(playingPlayList, unfilteredPlayListPosition);
            else
                organizeControls(unfilteredPlayListPosition);
            unfilteredPlayList = new LinkedList<>();
            sortUnfilteredPlayList = false;
            openUnfilteredPlayList.setDisable(true);
        });

        openLastSession = createMenuItem("openLastSession", true);
        openLastSession.setOnAction((ae) -> {
            int position = filePosition;  //Holds the value of the current file position
            int filePosition = playedPlayListFilePosition;  //The next file position for the incoming play list
            int playedPlayListFilePosition = position;  //filePosition value for the outgoing play list
            FileFilter f = FileFilter.getDefaultFilter();
            if (defaultPlayedPlayList == nextPlayListSearch || defaultPlayedPlayList == previousPlayListSearch) {
                //Ascertain if the current playing play list
                //equals previous or next play list search
                if (defaultPlayingPlayList == previousPlayListSearch || defaultPlayingPlayList == nextPlayListSearch) {
                    searchListIntermediate = searchList;
                    searchList = lastSearchList;
                    lastSearchList = searchListIntermediate;
                }
                f = searchArrayMap.get(searchArrayString.get(searchList)).getValue();
            }
            if (!isCurrentParameter(f))
                setSearchParameter(f, false, false);
            playListID = DEFAULT_PLAYLIST_NAMES.get(4);
            transitionPlayList(playedPlayListFilePosition, filePosition, playingPlayList, defaultPlayingPlayList, playedPlayList, defaultPlayedPlayList, FileSortParameter.getSortParameter(), FileSortParameter.getPriorSortParameter(), false);
        });

        openLastPlayedFile = createMenuItem("openLastPlayedFile", true);  //Plays immediate past played file
        openLastPlayedFile.setOnAction((ae) -> {
            lastPlayedFilePosition = (lastPlayedFilePosition < playingPlayList.size() && playingPlayList.get(lastPlayedFilePosition).equals(lastPlayedFile))? lastPlayedFilePosition: playingPlayList.indexOf(lastPlayedFile);
            filePosition = (lastPlayedFilePosition == -1)? filePosition: lastPlayedFilePosition;
            playMedia(filePosition, true, lastPlayedFile);  //Begin playback
        });

        openFavouritePlayList = createMenuItem("openFavouritePlayList");
        openFavouritePlayList.setOnAction((ae) -> transitionPlayList(filePosition, 0, playingPlayList, defaultPlayingPlayList, favouritePlayList, favouritePlayList, FileSortParameter.getSortParameter(), PARAMETER, true));

        //Add the above defined playlist items to playMenu
        playMenu.getItems().addAll(openPlayList, openRecentPlayList, openMostPlayedPlayList, openFavouritePlayList, openLastSession, openLastPlayedFile, openUnfilteredPlayList);
        //Create a Menu item for
        //searching the play list for a match
        openPlayListSearch = createMenuItem("openPlayListSearch");
        openPlayListSearch.setOnAction((ae) -> {
            wasPaused = pauseMedia();
            TextInputDialog dialog = inlayDialog(new TextInputDialog(), "openPlayListSearchDialog");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String text = result.get();
                runValue = false;
                newSearch = true;
                searchListIntermediate = searchList;
                new PlayListSearch(text, playListEntry, -1);
            }
            resumePlay(wasPaused);
        });

        //A menu item
        openPreviousPlayListSearch = createMenuItem("openPreviousPlayListSearch", true);
        openPreviousPlayListSearch.setOnAction((ae) -> {
            searchListIntermediate = searchList;
            if (defaultPlayingPlayList != previousPlayListSearch && defaultPlayingPlayList != nextPlayListSearch)  //If there's been a switch from the search play list
                //Increase the value of searchList by 1
                //so as to play the immediate search play list before switch
                searchList++;
            searchList = searchList > 0? --searchList: 0;
            conductFileMatch(searchList);
        });

        //A menu item for opening next play list search
        openNextPlayListSearch = createMenuItem("openNextPlayListSearch", true);
        openNextPlayListSearch.setOnAction((ae) -> {
            searchListIntermediate = searchList;
            searchList = searchList < searchArrayString.size()-1? ++searchList: searchArrayString.size()-1;
            conductFileMatch(searchList);
        });

        //A goTo menu item
        openGoToPlayList = createMenuItem("openGoToPlayList");
        openGoToPlayList.setOnAction((ae) -> {
            wasPaused = pauseMedia();
            String numberText = "";  //To retrieve only number input
            String stringText = "";  //To retrieve all forms of input
            int num = 0;
            //Create the string prompt for the dialog
            String prompt1 = resource.getAndFormatMessage("openGoToPlayList.prompt", new Integer(1), new Integer(playingPlayList.size()));
            String prompt2 = resource.getAndFormatMessage("openGoToPlayList.prompt2", new Integer(filePosition+1));
            TextInputDialog dialog = inlayDialog(new TextInputDialog(), "openGoToPlayListDialog");
            dialog.setContentText(String.join(" ", prompt1, prompt2));
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String resultString = result.get();
                try {
                    //Assign the text entered to stringText
                    stringText = resultString;
                    num = Integer.parseInt(stringText);  //If the string were a number, assign it to num
                    //Assign num to numberText
                    numberText += num;
                }
                //else
                catch (NumberFormatException ex) {}  //Do nothing
                int position = -1;
                //Check to see if the entry was numeric
                if (numberText.length() > 0) {
                    if (num <= playingPlayList.size() && num > 0) {  //If the number is valid, that is, within range
                        position = --num;
                    }
                }
                runValue = true;
                //Find match among the files
                playListSearch = new PlayListSearch(stringText, playingPlayList, position);
                jumpPosition = position;
            }
            else
                goToNext = true;
            resumePlay(wasPaused);  //Resume playback
        });

        openPreviousGoToPlayList = createMenuItem("openPreviousGoToPlayList");
        openPreviousGoToPlayList.setOnAction((ae) -> {
            goToNext = false;
            openGoToPlayList.fire();
        });

        nextGoToPlayList = createMenuItem("nextGoToPlayList", true);  //Obtains the next match in playingPlayList
        nextGoToPlayList.setOnAction((ae) -> {
            previous_next = next;
            if (PlayListSearch.isModified) {
                PlayListSearch.filePosition = 0;
            }
            else {
                if (playListSearch.filePosition == playListSearch.recentFiles.size()-1)
                    playListSearch.filePosition = 0;
                else
                    playListSearch.filePosition++;
            }
            playListSearch.isModified = false;
            int position = (isGoTo)? playListSearch.nextFileIndex: filePosition;
            isGoTo = false;
            goTo(position, true);  //Retrieve the next match, if any
        });

        previousGoToPlayList = createMenuItem("previousGoToPlayList", true);  //Obtains the previous match in playingPlayList
        previousGoToPlayList.setOnAction((ae) -> {
            previous_next = previous;
            if (playListSearch.isModified) {
                playListSearch.filePosition = playListSearch.recentFiles.size()-1;
            }
            else {
                if (playListSearch.filePosition == 0)
                    playListSearch.filePosition = playListSearch.recentFiles.size()-1;
                else
                    playListSearch.filePosition--;
            }
            PlayListSearch.isModified = false;
            int position = (isGoTo)? playListSearch.nextFileIndex: filePosition;
            isGoTo = false;
            goTo(position, false);  //Retrieve the previous match, if any
        });

        Menu playListOptions = new Menu(resource.getString("playListOptions.text"));
        this.playListOptions = playListOptions;

        newPlayList = createMenuItem("newPlayList");
        newPlayList.setOnAction((ae) -> createNewPlayListDialog(playListOptions));

        favouriteMenuItem = createMenuItem("favouriteMenuItem");
        favouriteMenuItem.setOnAction((ae) -> {
            File file = currentFile;
            if (file != null)
                add(Arrays.asList(file), substitute(favouritePlayList), favouritePlayList);
            openFavouritePlayList.setDisable(favouritePlayList.isEmpty());
        });

        contextMenu.getItems().addAll(newPlayList, favouriteMenuItem);
        playListOptions.getItems().addAll(newPlayList, favouriteMenuItem);
        //Release latch
        latch.countDown();

        deleteCurrentFile = createMenuItem("deleteCurrentFile", true);  //Deletes current media
        deleteCurrentFile.setOnAction((ae) -> {
            wasPaused = pauseMedia();
            deleteFileAlert = inlayDialog(FileManager.createAlert(AlertType.CONFIRMATION, ButtonType.YES, ButtonType.NO), "deleteCurrentFileDialog");
            Optional<ButtonType> result = deleteFileAlert.showAndWait();  //Waits for user input
            if (result.isPresent() && result.get() == deleteFileAlert.getDialogPane().getButtonTypes().get(0))  //If the yes button is pressed
                discard(currentFile);
            resumePlay(wasPaused);
        });

        contextMenu.getItems().add(deleteCurrentFile);
        playListOptions.getItems().add(deleteCurrentFile);

        //Create some radio menu items
        containsSearchString = new RadioMenuItem(resource.getString("containsSearchString.text"));  //Filters files that contain the search string
        containsSearchString.setOnAction((ae) -> setSearchParameter(FileFilter.CONTAINS, isInitialized, true));

        beginsSearchString = new RadioMenuItem(resource.getString("beginsSearchString.text"));  //Filters files that begin with the search string
        beginsSearchString.setOnAction((ae) -> setSearchParameter(FileFilter.BEGINSWITH, isInitialized, true));

        endsSearchString = new RadioMenuItem(resource.getString("endsSearchString.text"));  //Filters files that ends with the search string
        endsSearchString.setOnAction((ae) -> setSearchParameter(FileFilter.ENDSWITH, isInitialized, true));

        //Group radio items
        group(new ToggleGroup(), containsSearchString, beginsSearchString, endsSearchString);
        setSearchParameter(getSearchParameter(), false, true);

        //Add the radio menu items to a filter menu
        Menu filter = new Menu(resource.getString("filter.text"));
        filter.getItems().addAll(containsSearchString, beginsSearchString, endsSearchString);
        contextMenu.getItems().add(filter);  //Adds the filter menu to the context menu
        playListOptions.getItems().add(filter);  //Adds the filter menu to the play list options

        //Another set of radio menu items
        sortShuffle = new RadioMenuItem(resource.getString("sortShuffle.text.array", 0));  //to control play list shuffling
        sortShuffle.setOnAction((ae) -> setSortParameter(FileSortParameter.SHUFFLE, isInitialized, true));

        sortDefault = new RadioMenuItem(resource.getString("sortDefault.text"));  //Played in the default order of the active play list.
        sortDefault.setOnAction((ae) -> {
            if (isInitialized)
                playingPlayList = getDefaultPlayList(defaultPlayingPlayList);
            setSortParameter(FileSortParameter.DEFAULT, isInitialized, true);
        });

        sortAscending = new RadioMenuItem(resource.getString("sortAscending.text"));  //Alphabetical ordering from A to Z
        sortAscending.setOnAction((ae) -> setSortParameter(FileSortParameter.ASCENDING, isInitialized, true));

        sortDescending = new RadioMenuItem(resource.getString("sortDescending.text"));  //Alphabetical ordering from Z to A
        sortDescending.setOnAction((ae) -> setSortParameter(FileSortParameter.DESCENDING, isInitialized, true));

        group(new ToggleGroup(), sortDefault, sortAscending, sortDescending, sortShuffle);

        //Create a menu to host the radio items
        Menu sort = new Menu(resource.getString("sort.text"));
        sort.getItems().addAll(sortDefault, sortAscending, sortDescending, sortShuffle);
        playListOptions.getItems().add(sort);  //Adds the sort menu to the play list options menu
        //Add the sort menu to the context menu
        contextMenu.getItems().add(sort);

        //Set the context menu to the scene
        this.setOnContextMenuRequested((ae) -> {
            contextMenu.show(this, ae.getScreenX(), ae.getScreenY());
            ae.consume();
        });

        //Add all play list items to the play list menu
        playListMenu.getItems().addAll(playMenu, openPlayListSearch, openPreviousPlayListSearch, openNextPlayListSearch, openGoToPlayList, openPreviousGoToPlayList, nextGoToPlayList, previousGoToPlayList, new SeparatorMenuItem(), playListOptions);
        Menu moreMenu = new Menu("_More");
        MenuItem userPreferences = createMenuItem("userPreferences");
        userPreferences.setOnAction((ae) -> new GPlayerSettings(stage, pauseMedia()));
        MenuItem updateCheck = createMenuItem("updateCheck");
        updateCheck.setOnAction((ae) -> GPlayer.checkUpdates());
        MenuItem aboutItem = createMenuItem("aboutItem");
        MenuItem libraryItem = new MenuItem("");
        libraryItem.textProperty().bind(libraryToggle.textProperty());
        libraryItem.setOnAction((ae) -> libraryToggle.fire());
        moreMenu.getItems().addAll(userPreferences, updateCheck, libraryItem);
        menuBar.getMenus().addAll(fileMenu, playbackMenu, audioMenu, videoMenu, playListMenu, moreMenu);  //Adds all the menus and menu items to the menu bar
        return menuBar;  //The method returns
    }  //with the configured menu bar


    private List<Integer> fileIndices(List<File> filesList, List<File> files) {
        return enlist(files, ((file) -> filesList.contains(file)), ((file) -> filesList.indexOf(file)));
    }


    private int createdPlayListIndex(String playListName) {
        try {
            ObservableList<MenuItem> items = myPlayList.getItems();
            int size = items.size();
            for (int i = 0; i < size; i+=3) {
                //Obtain the menu item at index i
                MenuItem item = items.get(i);
                String itemText = item.getText();
                if (createdPlayListName(itemText).equals(playListName))  //We've found the index of the sought playlist
                    return i;
            }
        }
        catch (Exception ex) {}
        return -1;
    }


    private String createdPlayListName(String name) {
        return Utility.postCharacterString(name, ' ');
    }


    private MenuItem newCreatedPlayListItem(String text) {
        return new MenuItem(text) {
            @Override
            public boolean equals(Object obj) {
                if (obj instanceof MenuItem) {
                    MenuItem item = (MenuItem) obj;
                    return getText().equals(item.getText());
                }
                return false;
            }
        };
    }


    private MenuItem[] createdPlayLists(Predicate<MenuItem> predicate) {
        try {
            ObservableList<MenuItem> items = myPlayList.getItems();
            return items.stream().filter(predicate).toArray(MenuItem[]::new);
        }
        catch (Exception ex) {
            return new MenuItem[0];
        }
    }


    private MenuItem[] createdAddPlayLists() {
        return createdPlayLists(((item) -> (myPlayList.getItems().indexOf(item) % 3) == 0));
    }


    private MenuItem[] createdPlayPlayLists() {
        return createdPlayLists(((item) -> ((myPlayList.getItems().indexOf(item) - 1) % 3) == 0));
    }


    private void disableCreatedPlayLists(boolean stateDisable) {
        MenuItem[][] items = {createdAddPlayLists(), createdPlayPlayLists()};
        for (int i = 0; i < items.length; i++) {
            for (MenuItem item: items[i]) {
                String text = createdPlayListName(item.getText());
                List<File> list = createdPlayListMap.get(text).getValue();
                //Disable playlist if current playlist equals the the default for this item
                boolean currentDisable = list == defaultPlayingPlayList;
                //Disable the addition of files to item default playlist if current playlist is empty,
                //as well as disable playing item default playlist if the default playlist is empty
                boolean emptyDisable = ((i == 0 && playingPlayList.isEmpty()) || (i == 1 && list.isEmpty()));
                item.setDisable(stateDisable || emptyDisable || currentDisable);
            }
        }  //Closes the outer loop
    }


    private void createMyPlayList(String s, Trio<String, LinkedList<File>, String> t, Menu menu) {
        String[] texts = resource.getStringFamily("createdPlayList.text");
        java.util.function.Function<Trio<String, String, Boolean>, MenuItem> itemConstructor = ((constructives) -> {
            MenuItem item = newCreatedPlayListItem(resource.formatMessage(constructives.getKey(), s));
            if (constructives.getValue() != null && !constructives.getValue().isEmpty())
                item.setAccelerator(KeyCombination.keyCombination(constructives.getValue()));
            item.setDisable(constructives.getExtension());
            return item;
        });
        final MenuItem playItem = itemConstructor.apply(new Trio<String, String, Boolean>(texts[0], t.getExtension(), t.getValue().isEmpty()));
        playItem.setOnAction((ae) -> {
            if (!t.getValue().isEmpty())
                transitionPlayList(filePosition, 0, playingPlayList, defaultPlayingPlayList, t.getValue(), t.getValue(), FileSortParameter.getSortParameter(), PARAMETER, true);
        });
        final MenuItem addItem = itemConstructor.apply(new Trio<String, String, Boolean>(texts[1], t.getKey(), playingPlayList.isEmpty()));
        addItem.setOnAction((ae) -> {
            LinkedList<File> ll = t.getValue();
            File file = currentFile;
            if (file != null)
                add(Arrays.asList(file), substitute(ll), ll);
        });
        final  MenuItem editItem = newCreatedPlayListItem(resource.formatMessage(texts[2], s));
        editItem.setOnAction((ae) -> {
            String string1 = createdPlayListName(addItem.getText());
            String string2 = (addItem.getAccelerator() != null)? addItem.getAccelerator().toString(): null;
            String string3 = (playItem.getAccelerator() != null)? playItem.getAccelerator().toString(): null;
            createNewPlayListDialog(menu, string1, string2, string3);
        });
        if (myPlayList == null) {
            myPlayList = new Menu(resource.getString("myPlayList.text"));
            menu.getItems().add(myPlayList);
            contextMenu.getItems().add(myPlayList);
        }
        myPlayList.getItems().addAll(addItem, playItem, editItem);
        updatePlayListOperations(s, true);
    }


    private void updateMyPlayList(String s, Trio<String, LinkedList<File>, String> t, MenuItem... items) {
        String formerText = createdPlayListName(items[0].getText());
        String[] texts = resource.getStringFamily("createdPlayList.text");
        items[0].setText(resource.formatMessage(texts[1], s));
        if (t.getKey() != null && !t.getKey().isEmpty()) {
            if (items[0].getAccelerator() == null) {
                items[0].setAccelerator(KeyCombination.keyCombination(t.getKey()));
                scene.getAccelerators().put(KeyCombination.keyCombination(t.getKey()), (() -> items[0].fire()));
            }
            else
                items[0].setAccelerator(KeyCombination.keyCombination(t.getKey()));
        }
        items[1].setText(resource.formatMessage(texts[0], s));
        if (t.getExtension() != null && !t.getExtension().isEmpty()) {
            if (items[1].getAccelerator() == null) {
                items[1].setAccelerator(KeyCombination.keyCombination(t.getExtension()));
                scene.getAccelerators().put(KeyCombination.keyCombination(t.getExtension()), (() -> items[1].fire()));
            }
            else
                items[1].setAccelerator(KeyCombination.keyCombination(t.getExtension()));
        }
        items[2].setText(resource.formatMessage(texts[2], s));
        updatePlayListOperations(s, true, formerText);
    }


    private void updateMyPlayList(String text, Menu menu) {
        //Remove the playlist contents from createdPlayListMap
        createdPlayListMap.remove(text);
        //Then, Eliminate its menu options from myPlayList
        int i = createdPlayListIndex(text);
        for (int j = i; j < i+3; j++)
            myPlayList.getItems().remove(i);
        if (myPlayList.getItems().isEmpty()) {
            //Remove it from the passed menu
            menu.getItems().remove(myPlayList);
            //Remove it from contextMenu as well
            contextMenu.getItems().remove(myPlayList);
            //Nullify myPlayList
            myPlayList = null;
        }
        updatePlayListOperations(text, false);
    }


    private void createNewPlayListDialog(Menu playListOptions, String... texts) {
        wasPaused = pauseMedia();
        String[] strings = resource.getStringFamily("createNewPlayListDialog.string");
        String name = "";
        //Obtain a set of the keys of createdPlayListMap
        Set<String> keys = createdPlayListMap.keySet();
        //Create a list of the retrieved keys
        List<String> list = new ArrayList<>(keys);
        //Create a string array of list contents
        //Infer name from last user entry, if present
        if (texts.length == 0) {
            name = (list.isEmpty())? resource.getString("playList.single.word"): list.get(list.size()-1);
        }
        else {
            name = texts[0];
            list.remove(name);
        }
        //Obtain a unique name with name as the origin
        final String uniqueName = Utility.generateUniqueName(name, list);
        ArrayList<String> al1 = new ArrayList<>(), al2 = new ArrayList<>();
        //Fill both collections
        for (String s: keys) {
            Trio<String, LinkedList<File>, String> t = createdPlayListMap.get(s);
            String k = t.getKey(), e = t.getExtension();
            if (k != null && !k.isEmpty()) {
                k = k.substring(k.length()-1);
                if (texts.length > 0 && texts[1] != null && !texts[1].isEmpty()) {
                    if (!k.equalsIgnoreCase(texts[1].substring(texts[1].length()-1)))
                        al1.add(k);
                }
                else
                    al1.add(k);
            }
            if (e != null && !e.isEmpty()) {
                e = e.substring(e.length()-1);
                if (texts.length > 0 && texts[2] != null && !texts[2].isEmpty()) {
                    if (!e.equalsIgnoreCase(texts[2].substring(texts[2].length()-1)))
                        al2.add(e);
                }
                else
                    al2.add(e);
            }
        }
        ArrayList<String> al3 = new ArrayList<>();
        al3.add(uniqueName);
        final TextInputDialog dialog = new TextInputDialog();
        if (texts.length == 0) {
            dialog.setTitle(resource.getString("newPlayListDialog.title"));
            dialog.setContentText(resource.getString("newPlayListDialog.contentText"));
        }
        else {
            dialog.setTitle(resource.getString("newPlayListDialog.title2"));
            dialog.setContentText(resource.getString("newPlayListDialog.contentText2"));
            al3.add(texts[1]);
            al3.add(texts[2]);
        }
        //Construct the node to display to user
        final HBox content = setAndGetControls(al1.toArray(new String[al1.size()]), al2.toArray(new String[al2.size()]), al3.toArray(new String[al3.size()]));
        ObservableList<Node> obs = content.getChildren();
        final TextField tf1 = (TextField) obs.get(0), tf2 = (TextField) obs.get(1), tf3 = (TextField) obs.get(2);
        if (texts.length > 0) {
            Button button = new Button(strings[0]);
            button.setOnAction((ae) -> {
                FileManager.createAlert(strings[1], AlertType.CONFIRMATION).showAndWait().filter((response) -> response == ButtonType.OK).ifPresent((response) -> {
                    updateMyPlayList(texts[0], playListOptions);
                    updateMediaInfo(false);
                    dialog.hide();
                });
            });
            content.getChildren().add(button);
        }
        dialog.getDialogPane().setContent(content);
        dialog.resultProperty().addListener((listener) -> {
            String text1 = tf1.getText(), text2 = tf2.getText(), text3 = tf3.getText();
            if (!text1.isEmpty() || !text2.isEmpty() || !text3.isEmpty()) {
                //Validate the entry made
                text1 = assertUniqueName(list.toArray(new String[list.size()]), uniqueName, text1);
                if (text1 != null) {
                    //Retrieve only the numbers and letters present in the text
                    text1 = Utility.retainLettersAndDigits(text1).toLowerCase();
                    if (texts.length == 0) {
                        //Insert the new playlist into createdPlayListMap
                        createdPlayListMap.put(text1, new Trio<>(text2, new LinkedList<>(), text3));
                        //Create the necessary submenu to insert the playlist options
                        createMyPlayList(text1, createdPlayListMap.get(text1), playListOptions);
                    }
                    else {
                        //Retrieve the contents of myPlayList menu
                        ObservableList<MenuItem> ob = myPlayList.getItems();
                        for (int i = 0; i < ob.size(); i+=3) {
                            //Obtain the menu item at index i
                            MenuItem item = ob.get(i);
                            String itemText = item.getText();
                            if (itemText.substring(itemText.lastIndexOf(' ')+1).equals(texts[0])) {  //We've found the edited playlist
                                //Retrieve the contents of the playlist from createdPlayListMap
                                Trio<String, LinkedList<File>, String> t = createdPlayListMap.get(texts[0]);
                                //Make the necessary modifications
                                t.setKey(text2);
                                t.setExtension(text3);
                                Utility.updateMap(createdPlayListMap, texts[0], text1, t);
                                updateMyPlayList(text1, t, item, ob.get(++i), ob.get(++i));
                                break;
                            }
                        }  //Closes the for-loop
                    }  //Closes the else-block
                    updateMediaInfo(false);
                }
            }
        });
        dialog.setOnHiding((de) -> resumePlay(wasPaused));
        dialog.show();
        tf1.requestFocus();
    }


    private String assertUniqueName(String[] array, String name, String text) {
        String txt = text;
        boolean assertName = true;
        while ((txt = Utility.retainLettersAndDigits(txt).toLowerCase()).isEmpty() || !Utility.hasUniqueName(txt, array)) {
            String str = (txt.isEmpty())? name: txt;
            TextInputDialog dialog = new TextInputDialog(Utility.generateUniqueName(str, array));
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            dialog.setHeaderText(resource.getString("dialog.headerText"));
            String prefix = "assertUniqueNameDialog.";
            dialog.setTitle(resource.getString(prefix + "title"));
            String[] prompts = resource.getStringFamily(prefix + "contentText");
            String promptText = (txt.isEmpty())? prompts[0]: resource.formatMessage(prompts[1], txt);
            String prompt = promptText.concat(" " + prompts[2]);
            dialog.setContentText(prompt);
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                txt = result.get();
                assertName = true;
            }
            else {
                assertName = false;
                break;
            }
        }
        return assertName? txt: null;
    }


    private void retrieveMarkedPositions(String result, int f) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final LinkedList<File> dm = new LinkedList<>();
                final ArrayList<Duration> dt = new ArrayList<>();
                String text = result.toLowerCase().trim();
                for (Map.Entry<String, Trio<File, Duration, Boolean>> map: mediaDurationSet) {  //A for-each loop
                    if (map.getKey().substring(0, 1).equals(" "))  //If the first character in the retrieved string is a space ' ',
                        continue;  //go to the next level
                    //That is, do not execute the following lines.
                    //Just iterate through the loop, and retrieve the next key in the map, if any
                    //If the first character isn't a space ' ', do the following
                    String str = map.getKey();
                    str = Utility.postCharacterString(str, separator, true);
                    boolean found = false;
                    if (str.equals(text))  //If the acquired string equals that being sought for
                        found = true;
                    else if (str.contains(text) || text.contains(str))  //In cases where characters entered are present in the retrieved mapped key
                        found = true;
                    if (found) {
                        File file = map.getValue().getKey();
                        if (file.exists()) {  //If the file location is available
                            dm.add(file);  //The matching result is added to durationMatch
                            markedPlace = map.getValue().getValue();  //The associated mapped value is retrieved
                            dt.add(markedPlace);  //And saved in durationTime
                        }
                    }
                }  //End of loop block
                if (!dm.isEmpty()) {  //If at least a result was found
                    markedText = text;
                    lastDurationMatch = durationMatch;
                    lastDurationTime = durationTime;
                    durationMatch = dm;
                    durationTime = dt;
                    lastDefaultDurationTime = defaultDurationTime;
                    defaultDurationTime = durationTime;
                    miscellaneousPlayListMap.put("Media-marked playlist", durationMatch);  //load from resource
                    miscellaneousPlayListMap.put("Last media-marked playlist", lastDurationMatch);  //load from resource
                    Platform.runLater(() -> transitionPlayList(filePosition, f, playingPlayList, defaultPlayingPlayList, durationMatch, durationMatch, FileSortParameter.getSortParameter(), PARAMETER, true));
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }


    /**Converts the string result
    *to hours/minutes/seconds time units
    *@param result,
    *a string of characters from which time unit numbers are retrieved
    *@return the time measurement in millisecond time unit
    */
    private double timeToSkipTo(String result, int parameter) {
        double skipTime = -1.0;  //To hold the value of the time to skip to
        boolean skipTimeSet = false;
        int jumpTime = 0;
        //Retain only the digits in result
        String text = Utility.retainDigits(result);
        int length = text.length();  //Gets the length of the digits entered
        double seconds = 0, minutes = 0, hours = 0;
        if (length > 0) {  //If text contains values
            if (length <= 2) {  //Pass the values to the seconds time unit
                hours = 0.0;
                minutes = 0.0;
                seconds = Double.parseDouble(text.substring(0, length));
            }
            else if(length <= 4) {  //Pass values to the minutes and seconds time units
                hours = 0.0;
                minutes = Double.parseDouble(text.substring(0, 2));  //Minutes gets the first 2 values
                seconds = Double.parseDouble(text.substring(2, length));  //And seconds gets the rest
            }
            else {  //If there are 6 or more values
                int l = (length >= 6)? 6: 5;
                hours = Double.parseDouble(text.substring(0, 2));  //Hours is accorded the first 2 values
                minutes = Double.parseDouble(text.substring(2, 4));  //minutes assumes the next 2 values
                seconds = Double.parseDouble(text.substring(4, l));  //And seconds the rest
                //If there are more values
                //Discard the rest
            }
            double durationBoundary = (parameter != 0)? duration.toMillis(): 0.0;
            double addOn = (parameter == 2)? 0.0: mediaPlayer.getCurrentTime().toMillis();
            //Now, Convert the available time units
            //into milliseconds
            seconds *= 1000.0;
            //Check to see if the specified seconds,
            //if applied,
            //would be within range
            boolean pass = (parameter == 0)? (addOn - seconds) >= durationBoundary: (addOn + seconds) <= durationBoundary;
            if (pass) {
                skipTime = seconds;
                //Convert minutes to milliseconds
                minutes *= 60.0 * 1000.0;
                //Get the addition of both the minutes and seconds value
                minutes += seconds;
                //Now, compare the sum
                pass = (parameter == 0)? (addOn - minutes) >= durationBoundary: (addOn + minutes) <= durationBoundary;
                if (pass) {
                    skipTime = minutes;
                    //Now,
                    //Get the hours millis conversion
                    hours *= 60.0 * 60.0 * 1000.0;
                    //Obtain the addition of all conversions
                    hours += minutes;
                    pass = (parameter == 0)? (addOn - hours) >= durationBoundary: (addOn + hours) <= durationBoundary;
                    if (pass) {
                        //Assign skipTime the value of hours
                        skipTime = hours;
                    }  //Closes the hours if-block
                }  //Closes the minutes if-block
            }  //Closes the seconds if-block
        }  //Closes the length if-block
        return skipTime;
    }


    private void conductFileMatch(int searchList) {
        String text = searchArrayString.get(searchList);
        Duo<LinkedList<File>, FileFilter> d = searchArrayMap.get(text);
        LinkedList<File> ll = d.getKey();
        if (!isCurrentParameter(d.getValue()))
            setSearchParameter(d.getValue(), false, false);
        newSearch = false;
        conductFileMatch(ll, text, newSearch, true, 0);
    }


    void conductFileMatch(LinkedList<File> searchResult, String text, boolean newSearch, boolean play, int position, int... intSearchList) {
        if (!searchResult.isEmpty()) {
            boolean isOfFamily = (defaultPlayedPlayList == previousPlayListSearch || defaultPlayedPlayList == nextPlayListSearch);
            FileSortParameter p = FileSortParameter.getSortParameter();
            LinkedList<File> mostRecentPlayList = playedPlayList;  //Temporary playedPlayList holder
            LinkedList<File> recentPlayList = defaultPlayedPlayList;
            int recentPosition = playedPlayListFilePosition;  //Temporary playedPlayListFilePosition holder
            int recentSearchList = lastSearchList;  //Temporary lastSearchList holder
            if (intSearchList.length == 0) {  //If the search is conducted by invoking the search control
                playedPlayListFilePosition = filePosition;
                playedPlayList = playingPlayList;
                defaultPlayedPlayList = defaultPlayingPlayList;
            }
            filePosition = position;
            playingPlayList = searchResult;
            defaultPlayingPlayList = playingPlayList;
            if (nextPlayListSearch == defaultPlayedPlayList)
                previousPlayListSearch = defaultPlayingPlayList;
            else if (previousPlayListSearch == defaultPlayedPlayList)
                nextPlayListSearch = defaultPlayingPlayList;
            else
                nextPlayListSearch = defaultPlayingPlayList;
            lastSearchList = searchListIntermediate;
            if (newSearch) {
                if (intSearchList.length == 1) {  //If this method is invoked by the filter control
                    for (int s: intSearchList)
                        searchList = s;
                }
                else {  //If it is invoked by the search control
                    //Cycle through the items in searchArrayString
                    for (int i = 0; i < searchArrayString.size(); i++) {
                        //Check to see if the search string already exists
                        if (haveSameCharacters(searchArrayString.get(i), text)) {  //If it does exist
                            //Obtain the focused search string
                            String proString = searchArrayString.get(lastSearchList);
                            //Obtain the previously focused string
                            String preString = searchArrayString.get(recentSearchList);
                            //Then, remove the string that is being compared
                            searchArrayMap.remove(searchArrayString.remove(i));
                            if (haveSameCharacters(proString, text)) {  //If the focused string equals that to be played
                                //Make the previously focused string the focused one
                                proString = preString;
                                //Try to revert defaultPlayedPlayList
                                //if it equals the previously focused play list
                                if (defaultPlayedPlayList == previousPlayListSearch || defaultPlayedPlayList == nextPlayListSearch) {
                                    if (isOfFamily) {
                                        if (nextPlayListSearch == defaultPlayedPlayList)
                                            nextPlayListSearch = recentPlayList;
                                        else if (previousPlayListSearch == defaultPlayedPlayList)
                                            previousPlayListSearch = recentPlayList;
                                    }
                                    //Revert playedPlayList to the prior as well
                                    playedPlayList = mostRecentPlayList;
                                    defaultPlayedPlayList = recentPlayList;
                                    //Do the same for playedPlayListFilePosition
                                    playedPlayListFilePosition = recentPosition;
                                    p = FileSortParameter.getPriorSortParameter();
                                }
                            }
                            lastSearchList = searchArrayString.indexOf(proString);
                            break;
                        }
                    }
                    searchArrayString.add(text);
                    searchList = searchArrayString.size()-1;
                }
                searchArrayMap.put(text, new Duo<LinkedList<File>, FileFilter>(playingPlayList, FileFilter.getDefaultFilter()));
                if (!isCurrentParameter(FileFilter.getDefaultFilter()))
                    setSearchParameter(FileFilter.getDefaultFilter(), false, false);
            }
            lastSearchList = (searchList == lastSearchList)? recentSearchList: lastSearchList;
            final FileSortParameter pp = p;
            if (play) 
                Platform.runLater(() -> transitionPlayList(playedPlayListFilePosition, filePosition, playedPlayList, defaultPlayedPlayList, playingPlayList, defaultPlayingPlayList, pp, PARAMETER, true));
        }
    }  //Method ends


    private boolean playMedia(int filePosition, boolean autoPlay, File... files) {
        if (mediaPlayer != null) {
            Status status = mediaPlayer.getStatus();
            if (status == Status.PLAYING || status == Status.PAUSED || endOfFile)
                initiateStoppage(mediaPlayer, currentFile, endOfFile);
            mediaPlayer.dispose();
        }
        boolean play = false;
        logger.info("Preparing to play media");
        this.filePosition = filePosition;
        if (files.length == 0) {
            if (!playingPlayList.isEmpty()) {
                boolean videoFile = !isAudioFile(playingPlayList.get(filePosition));
                int f = filePosition;
                if (autoPlay && videoFile && !stage.isFocused() && !isDialogShowing && GPlayerSettings.viewPrefs().getBoolean("windowViewOnly", false))
                    f = getNextAudioFileIndex((currentFile == null)? f: playingPlayList.indexOf(currentFile), (previous_next == next));
                if (f != -1)
                    this.filePosition = f;
                this.autoPlay = autoPlay;
                play = true;
                playMedia(playingPlayList.get(this.filePosition));  //Pass the file for subsequent actions to be performed
            }
            else
                setStageTitle(GPlayer.stageTitle);
        }
        else {
            this.autoPlay = autoPlay;
            play = true;
            playMedia(files[0]);  //Pass the file for subsequent actions to be performed
        }
        endOfFile = false;
        return play;
    }


    private void playMedia(File file) {
        try {
            if (currentFile == null)
                Platform.runLater(() -> playSubmittedArguments(!hasValidArgument));
            if (!file.exists())  //Given that the file path is unavailable
                throw new java.io.FileNotFoundException();
            //else
            final boolean bp = autoPlay;
            final boolean sameFile = currentFile != null && currentFile.equals(file);
            isMediaReady=encounteredMediaError=false;
            currentFile = file;
            currentFileInfo = getFileInfo(file);
            speedRate = (GPlayerSettings.playBackPrefs().getBoolean("revertSpeedRate", false))? GPlayerSettings.playBackPrefs().getDouble("speedRate", 1.0): speedRate;
            if (!sameFile) {
                isAudio = isAudioFile(file);
                fileName = new FileInfo(file).getName();
            }
            try {
                //Create a media object with the passed file
                media = new Media(file.toURI().toString());
                if (media.getError() == null) {  //If no error is generated in media creation
                    //instantiate a Runnable to catch asynchronous error, to begin with
                    media.setOnError(() -> initializeMediaErrorAlert(file, String.join(" ", resource.getAndFormatMessage("mediaErrorAlert.media.message", fileName), resource.getString("confirmFileDeletion.message"))));
                    //Then, create a mediaPlayer object with the instantiated media
                    try {
                        mediaPlayer = new MediaPlayer(media);
                        if (mediaPlayer.getError() == null) {
                            //Catch asynchronous error
                            mediaPlayer.setOnError(() -> initializeMediaErrorAlert(file, String.join(" ", resource.getAndFormatMessage("mediaErrorAlert.play.message", fileName), resource.getString("confirmFileDeletion.message"))));
                            mediaPlayer.setRate(speedRate);
                            isPaused = false;
                            //Ascertain if it is a video media file
                            if (!isAudio) {  //If it is,
                                mediaView.setMediaPlayer(mediaPlayer);
                                if (getCenter() != mediaViewHost) {
                                    setMediaViewSize();
                                    setCenter(mediaViewHost);
                                }
                            }
                            else {  //If it is an audio file
                                setCenter(null);
                            }

                            //A listener for reporting mediaPlayer error
                            mediaPlayer.errorProperty().addListener((val, oldVal, newVal) -> {
                                logger.log(Level.INFO, "MediaPlayer error: ", newVal);
                                if (newVal.getType() == javafx.scene.media.MediaException.Type.PLAYBACK_HALTED) {
                                    seekTime = preferences.getUserPreferences().getDouble("mediaStopPosition", 0.0);
                                    initiateStoppage(mediaPlayer, file, true);
                                    playMedia(filePosition, autoPlay, file);
                                }
                                else
                                    initializeMediaErrorAlert(file, String.join(" ", resource.getAndFormatMessage("mediaErrorAlert.play.message", fileName), resource.getString("confirmFileDeletion.message")));
                            });

                            mediaPlayer.setOnReady(() -> {  //When the media has been rolled in
                                logger.info("Mediaplayer is ready");
                                Platform.runLater(() -> setStageTitle(String.join(" - ", fileName, GPlayer.stageTitle)));
                                if (defaultPlayingPlayList == durationMatch || defaultPlayingPlayList == lastDurationMatch) {
                                    isMarked = true;
                                    if (defaultPlayingPlayList == lastDurationMatch) {
                                        //Reverse the order of things
                                        LinkedList<File> dm = durationMatch;  //Temporary reference holder
                                        ArrayList<Duration> dt = durationTime, ddt = defaultDurationTime;  //Temporary reference holders
                                        //Assign durationMatch the contents of lastDurationMatch
                                        durationMatch = lastDurationMatch;
                                        //Assign durationTime the contents of lastDurationTime
                                        durationTime = lastDurationTime;
                                        //and defaultDurationTime the contents of lastDefaultDurationTime
                                        defaultDurationTime = lastDefaultDurationTime;
                                        //Then, assign lastDurationMatch, lastDurationTime and defaultDurationTime the contents of dm, dt and ddt respectively
                                        lastDurationMatch = dm;
                                        lastDurationTime = dt;
                                        lastDefaultDurationTime = ddt;
                                    }
                                    if (defaultPlayingPlayList == playingPlayList && durationTime != defaultDurationTime)
                                        durationTime = defaultDurationTime;
                                }
                                else
                                    isMarked = false;
                                duration = mediaPlayer.getMedia().getDuration();  //The duration of the current media is retrieved
                                updateValues();
                            });

                            mediaPlayer.setOnPlaying(() -> {  //while the media is playing
                                isMediaReady = true;
                                if (!isPaused && seekTime > 0) {
                                    if (new File(preferences.getUserPreferences().get("currentFile", file.getPath())).equals(file)) {
                                        mediaPlayer.seek(Duration.millis(seekTime));
                                        isPaused = true;
                                    }
                                    seekTime = 0;
                                }
                                reflectPlayState(Status.PLAYING);
                                if (playingPlayList.isEmpty())
                                    play_pause.setDisable(false);
                                preferences.getUserPreferences().putInt("filePosition", filePosition);
                                preferences.getUserPreferences().put("currentFile", file.getPath());
                                if (!isPaused) {
                                    logger.info("Mediaplayer is playing");
                                    seekToMarkedPosition(file);
                                    fileProperty.setValue(file);
                                    updatePlayLists(file);
                                    lastFilePosition = filePosition;
                                    if (!autoPlay)
                                        autoPlay = true;
                                    if (isMarked)
                                        durationProperty.setValue(markedPlace);
                                    Platform.runLater(() -> makeListViewSelection(filePosition));
                                    if (playListSearch != null && filePosition != playListSearch.filePositionHolder)
                                        playListSearch.isModified = true;
                                    updateMediaInfo(false);
                                    if (!stage.isFocused())
                                        GPlayer.notify(resource.getAndFormatMessage("playing.message", fileName));
                                }  //End of the isPaused if expression
                                mediaPlayer.currentTimeProperty().addListener((observable) -> {  //Consistently updates media time
                                    Platform.runLater(() -> {
                                        double currentTime = mediaPlayer.getCurrentTime().toMillis();
                                        preferences.getUserPreferences().putDouble("mediaStopPosition", currentTime);
                                        if (!isAudio && stage.isFocused() && (currentTime < lastMouseMoveTime || (currentTime - lastMouseMoveTime) > 30000.0)) {
                                            lastMouseMoveTime = currentTime;
                                            mouseMove();
                                        }
                                    });
                                    updateValues();  //Calls the method responsible for updating media time
                                });
                            });

                            mediaPlayer.setOnPaused(() -> {  //while media is paused
                                reflectPlayState(Status.PAUSED);
                                if (!isPaused) {
                                    isPaused = true;
                                    logger.info("Mediaplayer is paused");
                                }
                            });

                            mediaPlayer.setOnEndOfMedia(() -> {  //When the media reaches its end
                                logger.info("Mediaplayer is ended");
                                endOfFile = true;
                                boolean exit = false;
                                if (quitApplicationOnMediaEnd.isSelected() || quitApplicationOnPlayListEnd.isSelected()) {
                                    exit = (quitApplicationOnMediaEnd.isSelected())? true: false;
                                    if (!exit)
                                        exit = filePosition == playingPlayList.size()-1;
                                    if (exit)
                                        exitApplication.fire();
                                }
                                if (!exit) {
                                    isPlaying = false;
                                    if (repeatIsSelected) {  //If repeat is on
                                        String stringRepeat = repeat.getText();  //Retrieves the current text displayed on the repeat check box
                                        if (playingPlayList.size() <= 1 || stringRepeat.equals(repeatOptions.get(1))) {
                                            //If the text is 'REPEAT ONE'
                                            playMedia(filePosition, true, file);  //Recommence playback
                                        }
                                        else {  //If the text is 'REPEAT ALL'
                                            previous_next = (backwardPlay.isSelected())? previous: next;
                                            previous_next.fire();
                                        }
                                    }
                                    else {  //If repaet is off
                                        if (multipleSelection) {
                                            if (!backwardPlay.isSelected() && filePosition < playingPlayList.size()-2)
                                                next.fire();
                                            else if (backwardPlay.isSelected() && filePosition > 0)
                                                previous.fire();
                                            else
                                                multipleSelection = false;
                                        }
                                        if (!multipleSelection)
                                            stop.fire();
                                    }
                                }
                            });

                            timeSlider.valueProperty().addListener((value, oldValue, newValue) -> {
                                if (isValueChanging) {
                                    mediaPlayer.seek(duration.multiply(((double)newValue) / 100.0));
                                    isValueChanging = false;
                                }
                                else {
                                    if (timeSlider.isValueChanging())  //As its value changes
                                        mediaPlayer.seek(duration.multiply(((double)newValue) / 100.0));
                                    }
                                });

                            mediaPlayer.setVolume(volumeSlider.getValue()/100.0);  //The volume is set to the value returned by the getValue() method
                            volumeSlider.valueProperty().addListener((observable) -> {  //To report value changes
                                mediaPlayer.setVolume(volumeSlider.getValue()/100.0);
                                volumeLevel = volumeSlider.getValue();
                                preferences.getUserPreferences().putDouble("volumeLevel", volumeLevel);
                            });

                            mediaPlayer.setBalance(audioBalanceLevel);
                            mediaPlayer.setMute(muteAudio);
                            mediaPlayer.muteProperty().addListener((observable) -> {
                                muteAudio = mediaPlayer.muteProperty().getValue();
                                String key = "audioMute.text";
                                audioMute.setText(resource.getString((muteAudio)? key + 2: key));
                            });

                            //Play media
                            mediaPlayer.setAutoPlay(autoPlay);
                            //If no error is encountered,
                            //ascertain if media file plays as expected
                            if (!sameFile && !encounteredMediaError)
                                verifyPlayStatus(mediaPlayer, file, fileName, bp, Duration.seconds(2));

                            if (!isAudio && bp) {
                                if ((!stage.isFocused() && !isDialogShowing && GPlayerSettings.viewPrefs().getBoolean("windowViewOnly", false)) || (isDialogShowing && GPlayerSettings.viewPrefs().getBoolean("stageViewOnly", true))) {
                                    mediaPlayer.pause();
                                    wasPaused=isMediaReady=true;
                                }
                                else
                                    wasPaused = false;
                            }
                        }  //End of mediaPlayer error if-block
                        else {  //If mediaPlayer.getError() does not equal null
                            logger.log(Level.INFO, "Syncronous media player error occurred", mediaPlayer.getError());
                            initializeMediaErrorAlert(file, String.join(" ", resource.getAndFormatMessage("mediaErrorAlert.play.message", fileName), resource.getString("confirmFileDeletion.message")));
                        }
                    }  //End of mediaPlayer try-block
                    catch (Exception ex) {
                        logger.log(Level.SEVERE, "media player error exception", ex);
                        initializeMediaErrorAlert(file, String.join(" ", resource.getAndFormatMessage("mediaErrorAlert.play.message", fileName), resource.getString("confirmFileDeletion.message")));
                    }  //End of mediaPlayer block
                }  //Closes the media error if-block
                else {  //If media.getError() does not equal null
                    logger.log(Level.INFO, "Syncronous media error occurred", media.getError());
                    initializeMediaErrorAlert(file, String.join(" ", resource.getAndFormatMessage("mediaErrorAlert.media.message", fileName), resource.getString("confirmFileDeletion.message")));
                }
            }  //End of media try-block
            catch (Exception ex) {
                logger.log(Level.SEVERE, "Media error exception", ex);
                initializeMediaErrorAlert(file, String.join(" ", resource.getAndFormatMessage("mediaErrorAlert.media.message", fileName), resource.getString("confirmFileDeletion.message")));
            }  //Closes the media block
        }
        catch (java.io.FileNotFoundException ex) {
            logger.warning("file location is unavailable");
            if (isInitialized && !isDisabled)
                disableControls(2, true);
            modifyPlayList(playingPlayList, defaultPlayingPlayList, file);
        }
    }


    private void verifyPlayStatus(MediaPlayer player, File file, String fileName, boolean playing, Duration duration) {
        logger.info((playing)? "Verifying playback status": "Not verifying playback status");
        if (playing) {
            Platform.runLater(() -> {
                //Wait for the duration passed
                Timeline timeline = new Timeline(new KeyFrame(duration, event -> {
                    if (mediaPlayer == player && !isMediaReady) {
                        //Alert user,
                        if (!showTempPopup(resource.getAndFormatMessage("filePlay.fail.message", fileName), null, (() -> tryNextFile(file))))
                            tryNextFile(file);
                    }
                }));
                timeline.play();
            });
        }
    }


    private void tryNextFile(File file) {
        Platform.runLater(() -> {
            if (file.equals(currentFile)) {
                if (previous_next.isDisable())
                    discard(file);
                else
                    previous_next.fire();
            }
            if (GPlayerSettings.playListPrefs().getBoolean("unplayableFiles", true))
                unplayableFiles.add(file);
        });
    }


    private boolean showTempPopup(String text, Runnable onShownTask, Runnable onHiddenTask) {
        if (stage == null || !stage.isFocused())
            return false;
        java.util.function.BinaryOperator<Runnable> operator = ((runnable, runnable2) -> {
            if (runnable2 == null)
                return runnable;
            return (() -> {
                runnable.run();
                runnable2.run();
            });
        });
        Runnable defaultOnShownTask = (() -> showLibraryPopup(false));
        Runnable defaultOnHiddenTask = (() -> showLibraryPopup(true));
        showTempPopup(stage, text, Duration.seconds(3), operator.apply(defaultOnShownTask, onShownTask), operator.apply(defaultOnHiddenTask, onHiddenTask));
        return true;
    }


    private boolean showTempPopup(String text) {
        return showTempPopup(text, null, null);
    }


    private boolean notify(String notification) {
        if (GPlayer.notify(notification))
            return true;
        return showTempPopup(notification);
    }


    private void initializeMediaErrorAlert(File file, String prompt) {
        int size = errorFiles.size();
        if (!errorFiles.isEmpty() && errorFiles.get(size-1) == file)
            return;
        errorFiles.add(file);
        if (mediaErrorAlert == null || !mediaErrorAlert.isShowing()) {
            //Construct mediaErrorAlert object
            mediaErrorAlert = createMediaErrorAlert(prompt);
            //Alert the user to the error
            alertUserTo(mediaErrorAlert, filePosition, errorFiles);
        }
        previous_next.fire();
    }


    private Alert createMediaErrorAlert(String prompt) {
        encounteredMediaError = true;
        if (mediaErrorAlert == null || !mediaErrorAlert.isShowing())
            return FileManager.createAlert(prompt, AlertType.ERROR, ButtonType.YES, ButtonType.NO);  //To alert the user to a media error
        return mediaErrorAlert;
    }


    private void alertUserTo(Alert mediaErrorAlert, int errorFilePosition, List<File> list) {
        mediaErrorAlert.setOnHidden((de) -> {
            ButtonType buttonType = mediaErrorAlert.resultProperty().getValue();
            if (buttonType != null && buttonType == mediaErrorAlert.getDialogPane().getButtonTypes().get(0))
                discard(list);
            list.clear();
            resumePlay(wasPaused);
        });
        if (stage.isShowing()) {
            mediaErrorAlert.show();
            dismissNotification(mediaErrorAlert);
        }
    }

    void playMedia(boolean play) {
        if (Thread.currentThread() != playListSearch.thread) {
            playListSearch.sem1.release();
            return;
        }
        Platform.runLater(() -> {
            nextGoToPlayList.setDisable(playListSearch.recentFiles.isEmpty());
            previousGoToPlayList.setDisable(playListSearch.recentFiles.isEmpty());
            if (play) {
                if (playListSearch.foundMatch || jumpPosition != -1) {
                    PlayListSearch.isModified = true;
                    isGoTo = true;
                    if (goToNext)
                        nextGoToPlayList.fire();
                    else
                        previousGoToPlayList.fire();
                }
            }
            goToNext = true;
        });
    }


    private void disableControls(int i, boolean state) {
        switch(i) {
            case 1:
                normalise.setDisable((state)? state: speedRate == 1.0);
                previousMediaMarker.setDisable((state)? state: durationSet.isEmpty());
                nextMediaMarker.setDisable((state)? state: durationSet.isEmpty());
                if (!isAudio)
                    fullScreen.setDisable((state)? state: stage.isFullScreen());
                else
                    stage.setFullScreen(false);
                quitApplicationOnPlayListEnd.setDisable(playingPlayList.isEmpty());
                break;  //Break out of the switch
            case 2:
                final boolean b = (mediaPlayer != null && mediaPlayer.getStatus() == Status.PLAYING)? false: true;
                stop.setDisable(b);
                if (state) {
                    stop.setDisable(state);
                    previous.setDisable(state);
                    play_pause.setDisable(state);
                    next.setDisable(state);
                }
                else {  //If state is set to false
                    boolean s = (playingPlayList.size() > 1)? false: true;
                    boolean st = (playingPlayList.size() > 0)? false: true;
                    previous.setDisable(s);
                    play_pause.setDisable(st);
                    next.setDisable(s);
                    if (isDisabled)
                        play_pause.requestFocus();
                }
                isDisabled = state;
        }
        deleteCurrentFile.setDisable(playingPlayList.isEmpty());
    }


    /**Binds a stream of properties to a specified property.
    *@param binders
    *the stream of properties to be bound.
    *@param bindee
    *the property to which the stream of properties would be bound.
    */
    @SuppressWarnings("unchecked")
    public static void bindProperties(Stream<javafx.beans.property.Property> binders, javafx.beans.property.ReadOnlyProperty bindee) {
        binders.forEach((binder) -> binder.bind(bindee));
    }


    private void bindDisableProperties() {
        MenuItem[] items = {recallMediaStopPosition, quitFile, defaultAudioBalance, leftAudioBalance, rightAudioBalance, audioMute, rewind, jumpForward, jumpForwardByTime, jumpBackward, jumpBackwardByTime, jumpToEnd, jumpToTime, speedUp, speedDown, mediaPositionMarker, quitApplicationOnMediaEnd, stopItem};
        bindProperties(Arrays.stream(items).map((item) -> item.disableProperty()), stop.disableProperty());
        play_pauseItem.disableProperty().bind(play_pause.disableProperty()); 
        play_pauseItem.textProperty().bind(play_pause.textProperty()); 
        play_pauseItem.graphicProperty().bind(play_pause.graphicProperty()); 
        nextItem.disableProperty().bind(next.disableProperty()); 
        previousItem.disableProperty().bind(previous.disableProperty()); 
    }


    void disablePlayLists(boolean state) {
        disableCreatedPlayLists(state);
        if (state) {
            MenuItem[] items = {openPlayList, openRecentPlayList, openMostPlayedPlayList, openLastSession, openPreviousPlayListSearch, openNextPlayListSearch, openFavouritePlayList, openUnfilteredPlayList};
            for (MenuItem item: items)
                item.setDisable(true);
        }
        else {  //If state is set to false
            openLastPlayedFile.setDisable(lastPlayedFile == null);
            openPlayList.setDisable(playListEntry.isEmpty() || defaultPlayingPlayList == playListEntry);
            openLastSession.setDisable(playedPlayList.isEmpty());
            openUnfilteredPlayList.setDisable(unfilteredPlayList.isEmpty());
            openFavouritePlayList.setDisable(favouritePlayList.isEmpty() || defaultPlayingPlayList == favouritePlayList);
            favouriteMenuItem.setDisable(playingPlayList.isEmpty());
            mediaMarkerSearch.setDisable(isEmptyOfUserEntry());
            if (!searchArrayString.isEmpty()) {
                if (defaultPlayingPlayList != previousPlayListSearch && defaultPlayingPlayList != nextPlayListSearch) {
                    openPreviousPlayListSearch.setDisable(false);
                    openNextPlayListSearch.setDisable(searchList >= searchArrayString.size()-1);
                }
                else {  //If defaultPlayingPlayList is either of previous or next PlayListSearch
                    openPreviousPlayListSearch.setDisable(searchList == 0);
                    openNextPlayListSearch.setDisable(searchList >= searchArrayString.size()-1);
                }
            }
        }
    }


    private boolean isEmptyOfUserEntry() {
        return mediaDurationSet.stream().noneMatch((map) -> !map.getKey().startsWith(" "));
    }


    private void seekToMarkedPosition(File file) {
        isMarked = (playingPlayList.isEmpty() || !playingPlayList.contains(file))? false: isMarked;
        if (isMarked) {  //If isMarked is set to true
            try {
                markedPlace = durationTime.get(filePosition);
                mediaPlayer.seek(markedPlace);  //Move to the marked position
            }
            catch (Exception ex) {
                durationTime.add(filePosition, Duration.millis(0.0));
            }
        }
        else {  //If isMarked is not set to true
            Trio<File, Duration, Boolean> t = mediaDurationMap.get(" " + file.getPath());
            if (t != null && (markMediaPosition(t.getKey()) || t.getExtension()))
                mediaPlayer.seek(t.getValue());  //Retrieve the associated duration, and have the player move to that location
        }  //End of the isMarked else-block
    }


    private void configureMediaMarkers(File file, Duration... durations) {
        Duration duration = (durations.length == 0)? Duration.millis(-1.0): durations[0];
        if (durations.length == 0) {
            durationProperty.setValue(duration);
            if (!durationSet.isEmpty()) {
                durationSet = new TreeMap<>();  //This object stores marked positions asociated with this file
                availableMediaMarks.getItems().setAll(FXCollections.emptyObservableList());
            }
            //Retrieve all saved marked positions associated with the current playing media, if any
            for (Map.Entry<String, Trio<File, Duration, Boolean>> map: mediaDurationSet) {
                String key = map.getKey();
                if (key.startsWith(" "))  //If the first character is a space
                    continue;  //Do not execute the subsequent lines
                if (map.getValue().getKey().equals(file)) {
                    durationSet.put(map.getValue().getValue(), key);
                    addToAvailableMediaMarks(key, map.getValue().getValue());
                    if (duration.equals(Duration.millis(-1.0)))
                        duration = map.getValue().getValue();
                }
            }  //End of loop
        }
        duration = (durationSet.isEmpty())? Duration.millis(-1.0): duration;
        durationProperty.setValue(duration);
        availableMediaMarks.setDisable(durationSet.isEmpty());
    }


    private void configureMediaMarkers(String string, File file, Duration duration, boolean put) {
        String str = String.join(File.separator, file.getPath(), string);
        Duration d = duration;
        if (put) {
            mediaDurationMap.put(str, new Trio<File, Duration, Boolean>(file, duration, false));
            if (file.equals(currentFile)) {
                durationSet.put(duration, str);
                addToAvailableMediaMarks(str, duration);
            }
        }
        else {
            mediaDurationMap.remove(str);
            if (!file.exists())
                mediaDurationMap.remove(" " + str);
            if (file.equals(currentFile)) {
                //Obtain a set of the keys in durationSet
                Set<Duration> durationKeys = durationSet.keySet();
                d = (isMarked)? markedPlace: Utility.nextItem(duration, new ArrayList<Duration>(durationKeys));
                durationSet.remove(duration);
                removeFromAvailableMediaMarks(str);
            }
            if (isMarked) {
                int fp = filePosition;
                playNextIf(deleteMediaMarkedFiles(playingPlayList, duration, fp), fp);
            }
        }
        if (file.equals(currentFile))
            configureMediaMarkers(file, d);
        updateMediaInfo(false);
    }


    private void updateMediaMarkEditor(Duration duration) {
        String string = durationSet.get(duration);
        if (string != null || duration.equals(Duration.millis(-1.0)))
            mediaMarkerEdit.setText((duration.equals(Duration.millis(-1.0)))? resource.getString("mediaMarkerEdit.text"): resource.getAndFormatMessage("mediaMarkerEditDialog.contentText", Utility.postCharacterString(string, separator, true)));
        mediaMarkerEdit.setDisable(duration.equals(Duration.millis(-1.0)));
        if ((!mediaMarkerEdit.isDisable() && nextMediaMarker.isDisable()) || (mediaMarkerEdit.isDisable() && !nextMediaMarker.isDisable())) {
            previousMediaMarker.setDisable(mediaMarkerEdit.isDisable());
            nextMediaMarker.setDisable(mediaMarkerEdit.isDisable());
        }
    }


    private void addToAvailableMediaMarks(String string, Duration duration) {
        String text = Utility.postCharacterString(string, separator) + " (".concat(Utility.convertTimeToString((long)duration.toMillis()) + ")");
        MenuItem item = new MenuItem(text);
        item.disableProperty().bind(stop.disableProperty());
        item.setOnAction((ae) -> {
            mediaPlayer.seek(duration);
            durationProperty.setValue(duration);
        });
        int index = 0, size = availableMediaMarks.getItems().size();
        String leadString = Utility.postCharacterString(string, separator, false);
        for (index = 0; index <= size; index++) {
            if (index == size)
                break;
            MenuItem m = availableMediaMarks.getItems().get(index);
            String str = leadString + separator + Utility.preCharacterString(m.getText(), '(').trim();
            if (!mediaDurationMap.get(str).getValue().lessThan(duration))
                break;
        }
        availableMediaMarks.getItems().add(index, item);
    }


    private void removeFromAvailableMediaMarks(String string) {
        String text = Utility.postCharacterString(string, separator);
        text = Utility.preCharacterString(text, '(').trim();
        for (MenuItem item: availableMediaMarks.getItems()) {
            if (item.getText().equals(text)) {
                availableMediaMarks.getItems().remove(item);
                break;
            }
        }
    }


    public static final Map<javafx.stage.Window, javafx.stage.Window> getDisplayedPopups() {
        return displayedPopups;
    }


    /**Creates and displays a popup for a specified time before being hidden.
    *@param ownerWindow
    *the window upon which the popup will be displayed.
    *@param text
    *the message displayed to the user upon showing.
    *@param duration
    *time duration to display the popup for.
    *@param onShownTask
    *the action to execute upon showing.
    *@param onHiddenTask
    *the action to execute upon hiding.
    *@return the popup being shown.
    */
    public static Popup showTempPopup(javafx.stage.Window ownerWindow, String text, Duration duration, Runnable onShownTask, Runnable onHiddenTask) {
        javafx.stage.Window displayWindow = ownerWindow;
        //Create a popup
        Popup popup = new Popup();
        //Create a label with the passed text
        Label label = new Label(text);
        //Add it to the popup
        popup.getContent().add(label);
        javafx.stage.Window displayedPopup = displayedPopups.put(ownerWindow, popup);
        while (displayedPopup != null) {
            if (!(displayedPopup instanceof Popup)) {
                displayedPopup = displayedPopups.get(displayedPopup);
                if (displayedPopup != null && displayedPopup.isShowing())
                    displayWindow = displayedPopup;
            }
            else {  //Hide the popup
                displayedPopup.hide();
                break;
            }
        }
        //A timer to hide the popup after specified time elapse
        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(duration);
        pt.setOnFinished((event) -> popup.hide());
        //After being shown
        popup.setOnShown((we) -> {
            pt.play();
            label.requestFocus();
            if (onShownTask != null)
                onShownTask.run();
        });
        //After being hidden
        popup.setOnHidden((we) -> {
            if (onHiddenTask != null)
                onHiddenTask.run();
        });
        popup.show(displayWindow);
        return popup;
    }


    /**Creates and displays a popup for a specified time before being hidden.
    *@param ownerWindow
    *the window upon which the popup will be displayed.
    *@param text
    *the message displayed to the user upon showing.
    *@param duration
    *time duration to display the popup for.
    *@param onShownTask
    *the action to execute upon showing.
    *@return the popup being shown.
    */
    public static Popup showTempPopup(javafx.stage.Window ownerWindow, String text, Duration duration, Runnable onShownTask) {
        return showTempPopup(ownerWindow, text, duration, onShownTask, null);
    }


    /**Creates and displays a popup for a specified time before being hidden.
    *@param ownerWindow
    *the window upon which the popup will be displayed.
    *@param text
    *the message displayed to the user upon showing.
    *@param duration
    *time duration to display the popup for.
    *@return the popup being shown.
    */
    public static Popup showTempPopup(javafx.stage.Window ownerWindow, String text, Duration duration) {
        return showTempPopup(ownerWindow, text, duration, null, null);
    }


    /**Creates and displays a popup for 3 seconds before being hidden.
    *@param ownerWindow
    *the window upon which the popup will be displayed.
    *@param text
    *the message displayed to the user upon showing.
    *@return the popup being shown.
    */
    public static Popup showTempPopup(javafx.stage.Window ownerWindow, String text) {
        return showTempPopup(ownerWindow, text, Duration.seconds(3), null, null);
    }


    /**Obtains the name of a file
    *by retrieving all characters following the last occurrence of some character
    *in the path name of a file object.
    *@param file,
    *the file whose name is to be obtained
    *@param arg,
    *the character to precede the part to return
    *@throws NullPointerException
    *if file is null
    *@return all characters following the index of character, if character was found in the path name,
    *or all characters in path name, if no index of character was found.
    */
    public static String fileToString(File file, char arg) {
        if (file == null)
            throw new NullPointerException("File cannot be null");
        return Utility.postCharacterString(file.getPath(), arg, true);
    }


    /**Obtains the name of a file
    *by retrieving all characters following the last occurrence of the char separator value
    *in the path name of a file object.
    *@param file,
    *the file whose name is to be obtained
    *@throws NullPointerException
    *if file is null
    *@return all characters following the index of separator, if found
    *or all characters in path name, if no index of separator was found.
    */
    public static String fileToString(File file) {
        return fileToString(file, separator);
    }


    /**Checks to see if the keys mapped by a KeyEvent match at least an item in a list of key combinations.
    *@param event
    *the KeyEvent that the mapped keys are associated with
    *@param modifier
    *a key representing one of control, alt, meta, and shift keys, could be null
    *@param codes
    *a vararg array containing all available keys
    *@return true if, at least, an item in the list of key combinations match the keys in KeyEvent
    *and false otherwise
    */
    public static boolean keyMatch(KeyEvent event, boolean anyMatch, KeyCombination.Modifier modifier, KeyCode... codes) {
        List<KeyCodeCombination> combos = new ArrayList<>();
        for (KeyCode code: codes) {
            if (anyMatch || modifier == null)
                combos.add(new KeyCodeCombination(code));
            if (modifier != null)
                combos.add(new KeyCodeCombination(code, modifier));
        }
        //Compare available combos with the key event
        //to find a match
        return combos.stream().anyMatch((combo) -> combo.match(event));
    }


    public static boolean keyMatch(KeyEvent event, KeyCombination.Modifier modifier, KeyCode... codes) {
        return keyMatch(event, true, modifier, codes);
    }

    public static boolean keyMatch(KeyEvent event, KeyCode code, KeyCombination.Modifier... modifiers) {
        return new KeyCodeCombination(code, modifiers).match(event);
    }


    public static String keyTyped(KeyEvent ke) {
        String keyTyped = ke.getCharacter();
        if (!keyTyped.isEmpty() && keyTyped.trim().isEmpty())
            keyTyped = (ke.isAltDown() || ke.isControlDown() || ke.isMetaDown() || ke.isShiftDown() || ke.isShortcutDown() || keyTyped.charAt(0) != 32)? "": keyTyped;
        return keyTyped;
    }


    /**Operates on items in a list that satisfy a certain condition.
    *@param elements
    *the list whose items constitute this process.
    *@param predicate
    *the condition that each item in the list must satisfy before being considered for further operation.
    *@param callback
    *further operation performed on filtered items.
    *@param <T>
    the type of the items in the list
    *@param <R>
    *the type of the result returned on each filtered item operation.
    *@return a list of the result returned on each filtered item operation.
    */
    public static <T, R> List<R> enlist(java.util.Collection<T> elements, Predicate<T> predicate, javafx.util.Callback<T, R> callback) {
        Stream<R> stream = elements.stream().filter((element) -> predicate.test(element)).map((element) -> callback.call(element));
        return stream.collect(Collectors.toList());
    }


    /**Operates on items in a collection that satisfy a certain condition.
    *@param elements
    *the collection whose items constitute this process.
    *@param predicate
    *the condition that each item in the collection must satisfy before being considered for further operation.
    *@param <T>
    the type of the items in the collection
    *@return a list of the filtered items.
    */
    public static <T> List<T> enlist(java.util.Collection<T> elements, Predicate<T> predicate) {
        return enlist(elements, predicate, ((element) -> element));
    }


    /**Swaps the positions of 2 items in a list.
    *@param list
    *the list containing the items whose positions are to be swapped
    *@param index
    *the list index of one of the items
    *@param otherIndex
    *the list index of the other item
    */
    public static <T> void swap(List<T> list, int index, int otherIndex) {
        if (index == otherIndex)
            return;
        T indexItem = list.get(index), otherIndexItem = list.get(otherIndex);
        list.set(index, otherIndexItem);
        list.set(otherIndex, indexItem);
    }


    /**Swaps the positions of 2 items in an array.
    *@param array
    *the array containing the items whose positions are to be swapped
    *@param index
    *the array index of one of the items
    *@param otherIndex
    *the array index of the other item
    */
    public static <T> void swap(T[] array, int index, int otherIndex) {
        if (index == otherIndex)
            return;
        T indexItem = array[index], otherIndexItem = array[otherIndex];
        array[index] = otherIndexItem;
        array[otherIndex] = indexItem;
    }


    /**Ascertains if a string matches the rear string of any element in an array.
    *@param extension
    *the string to be compared for rear-string equality
    *@param extensions
    *the array of strings on which the search is to occur
    *@return true if at least one element in the array ends with the string passed
    *and false otherwise
    */
    public static boolean ofExtensions(String extension, String... extensions) {
        String lowerCasedExtension = extension.toLowerCase();
        return Arrays.stream(extensions).anyMatch((ext) -> lowerCasedExtension.endsWith(Utility.postCharacterString(ext, '.')));
    }


    /**Ascertains if a string matches the rear string of any of the audio-extension array elements (AUDIO_EXTENSIONS).
    *@param extension
    *the string to compare for rear-string equality
    *@return true if any of the audio-extension array elements ends with the string passed in extension,
    *and false otherwise
    */
    public static boolean ofAudioExtensions(String extension) {
        return ofExtensions(extension, AUDIO_EXTENSIONS);
    }


    /**Ascertains if a string matches the rear string of any of the video-extension array elements (VIDEO_EXTENSIONS).
    *@param extension
    *the string to compare for rear-string equality
    *@return true if any of the video-extension array elements ends with the string passed in extension,
    *and false otherwise
    */
    public static boolean ofVideoExtensions(String extension) {
        return ofExtensions(extension, VIDEO_EXTENSIONS);
    }


    /**Ascertains if a string matches the rear string of any of the audio&video-extension array elements (ALL_EXTENSIONS).
    *@param extension
    *the string to compare for rear-string equality
    *@return true if any of the audio-extension array or video-extension array elements ends with the string passed in extension,
    *and false otherwise
    */
    public static boolean ofAllExtensions(String extension) {
        return ofExtensions(extension, ALL_EXTENSIONS);
    }


    /**Checks to see if a file has the supported audio extension.
    *@param file
    *the file to be assessed
    *@throws NullPointerException
    *if the file is null
    *@return true if the file extension is of the supported audio extensions
    *and false otherwise
    */
    private static boolean isAudioFile(File file) {
        if (file == null)
            throw new NullPointerException("File cannot be null");
        String fileExtension = new FileInfo(file).getType();
        return ofAudioExtensions(fileExtension);
    }


    /**Searches for the next audio file, beginning at a position in a playing playlist.
    *@param f
    *the position to begin the search
    *@return the index of a file if an audio file was found,
    *or -1 if no audio file was found
    */
    private int getNextAudioFileIndex(int f, boolean forward) {
        LinkedList<File> ppl = playingPlayList;
        File previousFile = playingPlayList.get(f);
        File nextFile = (forward)? Utility.nextItem(previousFile, FileMedia::isAudioFile, ppl): Utility.previousItem(previousFile, FileMedia::isAudioFile, ppl);
        return (nextFile == previousFile)? -1: ppl.indexOf(nextFile);
    }


    /**Confirms the equality of 2 strings, after all possible symbols must have been removed.
    *@param string1
    *first of the 2 strings
    *@param string2
    *second of the 2 strings
    *@throws NullPointerException
    *if one or both strings are null
    *@return true if the symbolless strings contain same characters
    *and false otherwise
    */
    private static boolean haveSameCharacters(String string1, String string2) {
        if (string1 == null || string2 == null)
            throw new NullPointerException("Strings cannot be null");
        //Get the characters contained in string1
        String myString1 = Utility.retainLettersAndDigits(string1);
        //Get the characters for the second string as well
        String myString2 = Utility.retainLettersAndDigits(string2);
        //Now, return the comparison of both colated characters
        return myString1.equalsIgnoreCase(myString2);
    }


    private MediaView configureMediaView() {
        MediaView mediaView = new MediaView();
        mediaView.setOnError(new EventHandler<MediaErrorEvent>() {
            @Override
            public void handle(MediaErrorEvent mee) {
                logger.log(Level.INFO, "Media view error occurred", mee);
                //Construct mediaErrorAlert object
                mediaErrorAlert = createMediaErrorAlert(String.join(" ", resource.getString("mediaErrorAlert.view.message"), resource.getString("confirmFileDeletion.message")));
            }
        });
        mediaView.setSmooth(true);
        mediaView.setPreserveRatio(false);
        return mediaView;
    }


    private void setMediaViewSize(String dim) {
        if (!isAudio) {
            Platform.runLater(() -> {
                if (dim.startsWith("w")) {
                    double width = (stage.isFullScreen())? getWidth(): getAvailableWidth();
                    mediaView.setFitWidth(width);
                }
                else {
                    double height = (stage.isFullScreen())? getHeight(): getAvailableHeight();
                    mediaView.setFitHeight(height);
                }
            });
        }
    }


    private void setMediaViewSize() {
        setMediaViewSize("width");
        setMediaViewSize("height");
    }


    private void adjustCenterNode() {
        if (getCenter() != null) {
            if (getCenter() != mediaViewHost) {
                //Setting a node at the center
                //depends on the placement of the top, right, bottom and left nodes, if at least 1 is present.
                //So, we'll have to give the top, right, bottom and left nodes a chance to be layed out
                //before applying the changes.
                Platform.runLater(() -> {
                    double width = getAvailableWidth();
                    double height = getAvailableHeight();
                });
            }
            else
                setMediaViewSize();
        }
    }


    /**Calculates and returns the difference between a specified number, and the widths of the left and right nodes.*/
    public double getAvailableWidth(double width) {
        //Let's get the width of the left node
        //Set width to 0 if there is no node or the node is not visible
        double leftWidth = (getLeft() == null || !getLeft().isVisible())? 0.0: getLeft().getLayoutBounds().getWidth();
        //Let's get the width of the right node
        //Set width to 0 if there is no node or the node is not visible
        double rightWidth = (getRight() == null || !getRight().isVisible())? 0.0: getRight().getLayoutBounds().getWidth();
        //Sum widths of left and right nodes
        double sum = leftWidth + rightWidth;
        //Subtract the sum from total width of this layout
        //and return the result
        return width - sum;
    }


    /**Calculates and returns the difference between the height of the scene root, and the widths of the left and right nodes.*/
    public double getAvailableWidth() {
        return getAvailableWidth(getWidth());
    }


    /**Calculates and returns the difference between a specified number, and the heights of the top and bottom nodes.*/
    public double getAvailableHeight(double height) {
        //Let's get the height of the top node
        //Set height to 0 if there is no node or the node is not visible
        double topHeight = (getTop() == null || !getTop().isVisible())? 0.0: getTop().getLayoutBounds().getHeight();
        //Let's get the height of the bottom node
        //Set height to 0 if there is no node or the node is not visible
        double bottomHeight = (getBottom() == null || !getBottom().isVisible())? 0.0: getBottom().getLayoutBounds().getHeight();
        //Sum heights of top and bottom nodes
        double sum = topHeight + bottomHeight;
        //Subtract the sum from the passed height
        //and return the result
        return height - sum;
    }


    /**Calculates and returns the difference between the height of the scene root, and the heights of the top and bottom nodes.*/
    public double getAvailableHeight() {
        return getAvailableHeight(getHeight());
    }


    /**Updates recentPlayList and mostPlayedPlayList.
    *@param file
    *the file to add to the outlined playlists
    */
    private void updatePlayLists(File file) {
        openRecentPlayList.setDisable(defaultPlayingPlayList == recentPlayList);
        updateRecentPlayList(file);
        updateMostPlayedFiles(file);
        openMostPlayedPlayList.setDisable(defaultPlayingPlayList == mostPlayedPlayList && mostPlayedPlayList.equals(mostRecentPlayList));
    }


    /**Updates recentPlayList.
    *@param file
    *the most recent file
    *@return true if the file was added
    *and false otherwise
    */
    private boolean updateRecentPlayList(File file) {
        boolean updated = false;
        switch(recentPlayList.size()) {
            case 0:
                recentPlayList.add(file);
                openRecentPlayList.setDisable(false);
                updated = true;
                break;
            default:
                if (!file.equals(recentPlayList.get(0))) {
                    updated = true;
                    //Remove any previous occurrence of this file
                    recentPlayList.remove(file);
                    //Now insert this file at the head of the playlist
                    recentPlayList.add(0, file);
                    openRecentPlayList.setDisable(false);
                }
        }
        return updated;
    }


    /**Updates mostPlayedPlayList.
    *@param file
    *the file to add on
    */
    private void updateMostPlayedFiles(File file) {
        int fileIndex = mostPlayedPlayList.indexOf(file);
        if (fileIndex == -1) {  //File has not been previously added
            mostPlayedPlayList.add(file);  //Include in mostPlayedPlayList
            timesPlayed.add(0);  //and another integer to timesPlayed
            fileIndex = mostPlayedPlayList.size()-1;
        }
        int frequency = timesPlayed.get(fileIndex);
        timesPlayed.set(fileIndex, ++frequency);
        int searchIndex = -1;
        //Let's find an index with a lower frequency between 0 and fileIndex
        while (++searchIndex < fileIndex && timesPlayed.get(searchIndex) >= frequency);
        if (searchIndex != fileIndex) {  //We found one!
            //Let's adjust the file sequence in mostPlayedPlayList and timesPlayed,
            //by removing their respective items at fileIndex, and inserting them at the index occupied by the file with the lower frequency
            mostPlayedPlayList.add(searchIndex, mostPlayedPlayList.remove(fileIndex));
            timesPlayed.add(searchIndex, timesPlayed.remove(fileIndex));
        }
    }


    /**Stores the files in either recentPlayList or mostPlayedPlayList into mostRecentPlayList.
    *@param ll
    *either of recentPlayList or mostPlayedPlayList
    *@param name
    *name of the invoking playlist
    *@return a reference to mostRecentPlayList
    */
    private LinkedList<File> getMostRecentPlayList(LinkedList<File> ll, String name) {
        mostRecentPlayList = new LinkedList<>();
        java.util.Iterator<File> iterator = null;
        String[] options = (name.startsWith("recent"))? GPlayerSettings.recentPlayListOptions: GPlayerSettings.mostPlayedPlayListOptions;
        if (GPlayerSettings.playListPrefs().get(name, options[0]).equals(options[0]))
            iterator = ll.iterator();
        else
            iterator = ll.descendingIterator();
        while (iterator.hasNext())
            mostRecentPlayList.add(iterator.next());
        return mostRecentPlayList;
    }


    /**Adjusts either of timeSlider or volumeSlider value at a time, depending on the key pressed.
    *The left and right arrow keys, (with or without control/meta key) will trigger a timeSlider value adjustment,
    *while the up and down arrow keys (with or without control/meta key) will trigger a volumeSlider value adjustment.
    *@param ke
    *the fired KeyEvent
    *@return the slider whose adjustment value was triggered
    */
    private Slider setSeekSlider(KeyEvent ke) {
        //mediaPlayer status
        boolean playing = (mediaPlayer != null && mediaPlayer.getStatus() == Status.PLAYING)? true: false;
        Slider slider = null;
        if (playing) {
            boolean consume = true;
            double time = GPlayerSettings.playBackPrefs().getDouble("timeStepLevel", 5.0), volume = GPlayerSettings.playBackPrefs().getDouble("volumeStepLevel", 3.0);
            //Ascertain the key pressed
            switch(ke.getCode()) {
                case LEFT:
                case RIGHT:
                    slider = (adjustedSliderValue(ke, ke.getCode(), timeSlider, (KeyCode.RIGHT == ke.getCode())? time: -time))? timeSlider: slider;
                    break;
                case DOWN:
                case UP:
                    slider = (adjustedSliderValue(ke, ke.getCode(), volumeSlider, (KeyCode.UP == ke.getCode())? volume: -volume))? volumeSlider: slider;
                    break;
                default:
                    consume = false;
            }  //Closes the switch block
            if (consume)
                ke.consume();
        } //Closes the if-block
        else if (timeSlider.isFocused())
            isValueChanging = true;
        return slider;
    }


    private boolean adjustedSliderValue(KeyEvent event, KeyCode code, Slider slider, double value) {
        boolean adjusted = false;
        if (new KeyCodeCombination(code).match(event) || new KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN).match(event)) {
            double v = (new KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN).match(event))? value * 2: value;
            if (slider == timeSlider)
                isValueChanging = true;
            slider.setValue(slider.getValue() + v);
            adjusted = true;
        }
        return adjusted;
    }


    private double setAndGetDisplayedSliderValue(Slider slider, Label label, int i, boolean adjust) {
        double value = -1;
        switch (i) {
            case 1:
                value = timeToSkipTo(label.getText(), 2);
                if (value > 0 && adjust)
                    mediaPlayer.seek(Duration.millis(value));
                return value;
            default:
                try {
                    value = Double.parseDouble(label.getText());
                    if (adjust)
                        slider.setValue(value);
                }
                catch (NumberFormatException ex) {}
                return value;
        }
    }


    private boolean triggeredSliderAction(KeyEvent event, boolean controlDown, KeyCode... codes) {
        return keyMatch(event, (controlDown)? KeyCombination.SHORTCUT_DOWN: null, codes);
    }


    /**Defines the course of action for a slider on mouse and key events.
    *@param slider
    *the slider to define actions for
    *@param st
    *the string to display when the mouse is hovered over the slider
    *@param s
    *the string to display when the mouse is moved past the valid range of the slider
    *@param f
    *the number to increase or decrease by when the slider track is clicked, or when some key events are fired
    *@param i
    *unique slider number ID
    */
    public void armSlider(Slider slider, String st, String s, float f, int i) {
        final int offset = 30;
        slider.setBlockIncrement(f);
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(5);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setTooltip(new Tooltip(st));
        slider.setSkin(sliderSkin(slider));
        //A Popup instance
        final Popup popup = new Popup();
        //A label to be displayed by the popup
        final Label label = new Label();
        popup.getContent().add(label);
        slider.setOnMouseMoved((me) -> {
            NumberAxis axis = (NumberAxis) slider.lookup(".axis");
            StackPane track = (StackPane) slider.lookup(".track");
            StackPane thumb = (StackPane) slider.lookup(".thumb");
            Point2D locationAxis = axis.sceneToLocal(me.getSceneX(), me.getSceneY());
            boolean isHorizontal = slider.getOrientation() == javafx.geometry.Orientation.HORIZONTAL;
            double mouseX = isHorizontal? locationAxis.getX(): locationAxis.getY();
            double value = axis.getValueForDisplay(mouseX).doubleValue();
            if (value >= slider.getMin() && value <= slider.getMax())
                label.setText("" + (i == 1? Utility.formatTime(Duration.millis(duration.toMillis() * (value / 100)), Duration.ZERO): (int) value));
                else
                    label.setText(s);
            popup.setAnchorX(me.getScreenX());
            popup.setAnchorY(me.getScreenY() + offset);
        });
        slider.setOnMouseEntered((me) -> popup.show(slider, me.getScreenX(), me.getScreenY() + offset));
        slider.setOnMouseExited((me) -> popup.hide());
        slider.setOnMouseClicked((me) -> setAndGetDisplayedSliderValue(slider, label, i, true));
        if (i <= 2) {
            slider.setOnKeyPressed((ke) -> {
                Status status = (mediaPlayer == null)? null: mediaPlayer.getStatus();
                boolean requestFocus = triggeredSliderAction(ke, status == Status.PLAYING, KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT);
                if (requestFocus) {
                    setSeekSlider(ke);
                    if (i == 1)
                        requestFocus = triggeredSliderAction(ke, status == Status.PLAYING, KeyCode.UP, KeyCode.DOWN);
                    else
                        requestFocus = triggeredSliderAction(ke, status == Status.PLAYING, KeyCode.LEFT, KeyCode.RIGHT);
                    if (requestFocus)
                        requestMomentaryFocus((i == 1)? volumeSlider: timeSlider, Duration.millis((i == 1)? 100: 500));
                }
            });
        }
    }


    private com.sun.javafx.scene.control.skin.SliderSkin sliderSkin(Slider slider) {
        return new com.sun.javafx.scene.control.skin.SliderSkin(slider) {
            //Hook for replacing the mouse pressed handler that's installed by super.
            protected void installListeners() {
                StackPane track = (StackPane) getSkinnable().lookup(".track");
                track.setOnMousePressed((me) -> {
                    invokeSetField("trackClicked", true);
                    double trackLength = invokeGetField("trackLength");
                    double trackStart = invokeGetField("trackStart");
                    // convert coordinates into slider
                    MouseEvent event = me.copyFor(getSkinnable(), getSkinnable());
                    double mouseX = event.getX(); 
                    double position;
                    if (mouseX < trackStart) {
                        position = 0;
                    } else if (mouseX > trackStart + trackLength) {
                        position = 1;
                    } else {
                        position = (mouseX - trackStart) / trackLength;
                    }
                    getBehavior().trackPress(event, position);
                    invokeSetField("trackClicked", false);
                });
            }
            private double invokeGetField(String name) {
                Class clazz = com.sun.javafx.scene.control.skin.SliderSkin.class;
                java.lang.reflect.Field field;
                try {
                    field = clazz.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.getDouble(this);
                }
                catch (Exception ex) {}
                return 0.;
            }
            private void invokeSetField(String name, Object value) {
                Class clazz = com.sun.javafx.scene.control.skin.SliderSkin.class;
                try {
                    java.lang.reflect.Field field = clazz.getDeclaredField(name);
                    field.setAccessible(true);
                    field.set(this, value);
                }
                catch (Exception ex) {}
            }
        };
    }


    private boolean markMediaPosition(File f) {
        boolean mark = GPlayerSettings.generalPrefs().getBoolean("mediaMark", true);
        if (mark) {
            String[] group = GPlayerSettings.fileGroup;
            String file = GPlayerSettings.generalPrefs().get("mediaMarkFiles", group[1]);
            if (file.equalsIgnoreCase(group[1]))
                mark = (!isAudioFile(f))? true: false;
            else if (file.equalsIgnoreCase(group[2]))
                mark = (isAudioFile(f))? true: false;
        }
        return mark;
    }


    private void goTo(int f, boolean b) {
        Task<Integer> task = new Task<Integer>() {
            @Override
            protected Integer call() {
                int position = playListSearch.findMatchIndex(f, b);
                if (position == -1)
                    //Cancel the process
                    cancel();
                if (isCancelled())
                    return -1;
                return position;
            }
        };
        task.setOnSucceeded((wse) -> playMedia(task.getValue(), true));
        //Begin process
        //On a background thread
        initiateBackgroundTask(task);
    }


    private void updateValues() {  //updateValues method
        if (playTime != null && timeSlider != null && volumeSlider != null) {
            Platform.runLater(() -> {
                Duration currentTime = mediaPlayer.getCurrentTime();  //Retrieve the current time of the active media
                mediaPosition = currentTime;
                playTime.setText(Utility.formatTime(currentTime, duration));
                timeSlider.setDisable(duration.isUnknown());  //Disable timeSlider if duration is unknown,
                if (!timeSlider.isDisabled() && duration.greaterThan(Duration.ZERO) && !timeSlider.isValueChanging()) {
                    double doubleDuration = duration.toMillis();  //duration is converted to a double value
                    timeSlider.setValue(currentTime.divide(doubleDuration).toMillis() * 100.0);
                }
            });
        }
    }


    private void displayPlayList(List<File> ppl, int filePosition) {
        final List<File> list = new ArrayList<>(ppl);
        disableControls(2, list.isEmpty());
        disablePlayLists(false);
        openGoToPlayList.setDisable(list.isEmpty());
        openPreviousGoToPlayList.setDisable(list.isEmpty());
        openPlayListSearch.setDisable(playListEntry.isEmpty());
        if (!list.isEmpty() && list.get(filePosition).exists()) {
            if (playListSearch != null) {
                previousGoToPlayList.setDisable(true);
                nextGoToPlayList.setDisable(true);
                runValue = true;
                playListSearch = new PlayListSearch(playListSearch.searchStringHolder, playingPlayList);
                playListSearch.isModified = true;
            }
        }
        Platform.runLater(() -> {
            if (ppl == playingPlayList) {
                if (!list.contains(null)) {
                    playListView.setAll(list);
                    makeListViewSelection(filePosition);
                    updateMediaInfo(false);
                }
                else {
                    File nullFile = null;
                    list.retainAll(Arrays.asList(nullFile));
                    discard(list);
                }
            }
        });
    }


    /**Updates serialized media info
    *@param exit
    *flags if the media is to be stopped prior to update
    */
    public void updateMediaInfo(boolean exit) {
        if (exit)
            serializeMediaInfo(exit);
        else
            Platform.runLater(() -> serializeMediaInfo(exit));
    }


    /**Culminates media info to be serialized
    *@return DataSerializer object
    */
    private DataSerializer serializableMediaInfo() {
        ArrayList<Duration> dt = new ArrayList<>();
        int size = durationTime.size() + defaultDurationTime.size();
        if (size > 0) {
            dt.ensureCapacity(size);
            dt.addAll(durationTime);
            dt.addAll(defaultDurationTime);
        }
        try {
            return new DataSerializer(isMarked, filePosition, playListEntry, recentPlayList, mostRecentPlayList, playingPlayList, mostPlayedPlayList, defaultPlayingPlayList, favouritePlayList, dt, timesPlayed, mediaDurationMap, createdPlayListMap, playListContents, videoFileFolder, audioFileFolder, filesFileFolder, FileSortParameter.getSortParameter());
        }
        catch (Exception ex) {
            return null;
        }
    }


    private void serializeMediaInfo(boolean exit) {
        if (exit)
            if (mediaPlayer != null && (mediaPlayer.getStatus() == Status.PLAYING || mediaPlayer.getStatus() == Status.PAUSED || endOfFile))
                initiateStoppage(mediaPlayer, currentFile, endOfFile);
        logger.info("Preparing to sync media info");
        if (isInitialized) {
            String fileName = "file", tempFileName = "tempfile";
            String file = (!preferences.getUserPreferences().get("serializedFile", "").equals(fileName))? SERIALIZATION_PATH: String.join("/", GPlayer.RESOURCE_PATH, tempFileName);
            File serializedFile = GPlayer.serializeMediaInfo(file, serializableMediaInfo());
            if (serializedFile != null) {
                preferences.getUserPreferences().put("serializedFile", (file == SERIALIZATION_PATH)? fileName: tempFileName);
                if (file != SERIALIZATION_PATH) {
                    File oldFile = new File(SERIALIZATION_PATH);
                    boolean rename = (oldFile.exists())? oldFile.delete(): true;
                    if (rename && serializedFile.renameTo(oldFile))
                        preferences.getUserPreferences().put("serializedFile", fileName);
                }
            }
        }
    }


    /**Compares a yet-to-serialize DataSerializer object
    *with an already serialized one
    *for content equality.
    *@param s
    *the object passed for comparison
    *Returns true if the objects contain equal elements
    *and false otherwise
    */
    private boolean ofEqualSerializableContent(DataSerializer s) {
        try {
            return GPlayer.deserializeMediaInfo(SERIALIZATION_PATH).equals(s);
        }
        catch (Exception ex) {
            return false;
        }
    }


    private String getFileInfo(File file) {
        String[] headers = resource.getStringArray("file.info.array");
        FileInfo f = new FileInfo(file);
        String name = String.join(" ", headers[0], f.getName());
        String type = String.join(" ", headers[1], f.getType());
        String size = String.join(" ", headers[2], Utility.convertFromByte(f.getSize()));
        String location = String.join(" ", headers[3], f.getLocation());
        String frequency = String.join(" ", headers[4], "" + f.getPlayFrequency());
        return name + '\n' + type + '\n' + size + '\n' + location + '\n' + frequency;
    }


    /**Gets and returns the exact index of an item in a list.
    *@param type
    *the item to be sought for
    *@param list
    *the list on which the search is to take place
    *@param <T>
    *the type of the item and elements in the list
    *@return a number ranging from 0 to array.length -1 if the exact item is found,
    *and -1 otherwise
    */
    public static <T> int indexOf(T type, List<T> list) {
        if (type == null)
            return -1;
        int index = 0, size = list.size();
        while (type != list.get(index) && ++index < size);
        return (index == size)? -1: index;
    }


    /**Gets and returns the exact index of an item in an array.
    *@param type
    *the item to be sought for
    *@param array
    *the array on which the search is to take place
    *@param <T>
    *the type of the item and elements in the array
    *@return a number ranging from 0 to array.length -1 if the exact item is found,
    *and -1 otherwise
    */
    @SafeVarargs
    public static <T> int indexOf(T type, T... array) {
        return indexOf(type, Arrays.asList(array));
    }


    /**Removes elements contained in an array
    *from a specified list, if present (contained in specified list as well).
    *@param <T>
    *the type of elements in the list and array
    *@param target
    *the list from which contained source elements are removed.
    *@param source
    *the array whose any of its elements contained in the list are removed from the list.
    *@return the source as a list.
    */
    @SafeVarargs
    public static <T> List<T> uniqueElements(List<T> target, T... source) {
        return uniqueElements(target, Arrays.<T>asList(source));
    }


    /**Removes elements contained in a certain list
    *from another list, if present (contained in the other list as well).
    *@param <T>
    *the type of elements in the lists
    *@param target
    *the list from which contained source elements are removed.
    *@param source
    *the list whose any of its elements contained in the other list are removed from the other list.
    *@return the source list.
    */
    public static <T> List<T> uniqueElements(List<T> target, List<T> source) {
        target.removeAll(source);
        return source;
    }


    /**Executes a specified task on a new thread.
    *@param task
    *the action/task to execute.
    *@throws NullPointerException
    *if the specified task is null.
    *@return a reference to the spawned thread.
    */
    public static Thread initiateBackgroundTask(Runnable task) {
        if (task == null)
            throw new NullPointerException("Task arg is null");
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
        return t;
    }


    /**Waits for a specified measure of time
    *before executing a certain task.
    *@param waitTime
    *time duration before task execution.
    *@param task
    *the task to execute after the wait duration must have been exhausted.
    */
    public static void waitThenRun(Duration waitTime, Runnable task) {
        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(waitTime);
        pt.setOnFinished((event) -> task.run());
        pt.play();
    }


    private void setStageTitle(String stageTitle) {
        boolean sameTitle = this.stageTitle != null && this.stageTitle.equals(stageTitle);
        this.stageTitle = stageTitle;
        if (!GPlayer.hasChildWindowOpen()) {
            stage.setTitle(stageTitle);
            if (stage.isFocused() && !sameTitle)
                requestMomentaryFocus(spacer, Duration.millis(100));
        }
    }


    /**Obtains and returns the text currently displayed in the title area of the stage.*/
    public String getStageTitle() {
        return stageTitle;
    }


    /**Obtains and returns the value of current file index.
    *@return the index of the currently focused file in the currently focused playlist.
    */
    public static int getFilePosition() {
        return filePosition;
    }


    /**Obtains the number of times a file must have been played.
    *@param file
    *the file whose corresponding times played is to be returned
    *@return a number greater than 0 if the file can be found in mostPlayedPlayList,
    *or 0 if the file cannot be found
    */
    public int getPlayFrequency(File file) {
        int index = mostPlayedPlayList.indexOf(file);
        return (index == -1)? 0: timesPlayed.get(index);
    }


    /**Dismisses notification after a given period of time
    *@param dialog
    *the notification dialog to dismiss
    */
    private void dismissNotification(javafx.scene.control.Dialog dialog) {
        wasPaused = pauseMedia();
        boolean waitFurther = !stage.isFocused() && !isDialogShowing;
        long waitTime = Utility.convertTimeToMillis(GPlayerSettings.viewPrefs().get("notificationTime", Utility.formatTime(5, 2)));
        waitTime = (waitFurther)? Math.max(waitTime, Utility.convertTimeToMillis(Utility.formatTime(30, 2))): waitTime;
        final Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(waitTime), event -> {
            //If the display time has been exhausted
            //and the dialog hasn't been dismissed
            if (dialog.isShowing()) {
                if (GPlayerSettings.viewPrefs().getBoolean("executeNotificationTask", true)) {
                    ObservableList<ButtonType> buttonTypes = dialog.getDialogPane().getButtonTypes();
                    Node[] nodes = buttonTypes.stream().map((buttonType) -> dialog.getDialogPane().lookupButton(buttonType)).toArray(Node[]::new);
                    ((Button) getFocusedNode(nodes)).fire();
                }
                else
                    dialog.hide();
            }
        }));
        if (waitFurther)
            GPlayer.notify(resource.getAndFormatMessage("dialog.notification", GPlayer.stageTitle));
        timeline.play();
    }


    private java.awt.Robot createRobot() {
        try {
            return new java.awt.Robot();
        }
        catch (Exception ex) {
            return null;
        }
    }


    private void mouseMove() {
        if (ROBOT == null)
            return;
        java.awt.PointerInfo pointer = java.awt.MouseInfo.getPointerInfo();
        //We need not make the movement obvious to the user
        //So, just simulate mouse movement to the same location of the pointer on the screen
        ROBOT.mouseMove(pointer.getLocation().x, pointer.getLocation().y);
    }


    /**Retrieves input sent by other instances of this app.*/
    private void retrieveInput() {
        if (inputThread != null && inputThread.isAlive()) {
            inputThread.interrupt();
            if (!inputThread.isInterrupted())
                playSubmittedArguments(false);
            return;
        }
        inputThread = new Thread(() -> {
            playSubmittedArguments(true);
            while (true) {
                try {
                    //Sleep for 2 seconds for any further entry
                    Thread.sleep(2000);
                    //sleep time out!
                    playSubmittedArguments(false);
                    break;
                }
                catch (InterruptedException ex) {  //Oops!
                    //Sleep interrupted
                    //continue the loop then
                }
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();
    }


    /**Initiates programatic application exit after a timespan
    *@param time
    *signifies time to allow (starting from time of initialization) before programatic exit
    */
    public void quitApplicationIn(String time) {
        exitTime = time;
        quitApplication.setText(resource.getAndFormatMessage("quitApplication.text", exitTime));
        quitApplicationIn(Utility.convertTimeToMillis(exitTime));
    }


    /**Quits the application in relation to the specified quit time
    *@param quitTime
    *the time after which the application would be exited
    */
    public void quitApplicationIn(long quitTime) {
        if (quitTime > 0) {
            if (quitApplicationThread != null && quitApplicationThread.isAlive())
                quitApplicationThread.interrupt();
            quitApplicationThread = new Thread(() -> {
                try {
                    Thread.sleep(quitTime);
                }
                catch (InterruptedException ex) {
                    flagOff = true;
                }
                if (!flagOff)
                    Platform.runLater(() -> Platform.exit());
                else
                    flagOff = false;
            });
            quitApplicationThread.setDaemon(true);
            quitApplicationThread.start();
        }
    }

}