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

package gplayer.com.exec;

import gplayer.com.lang.BaseResourcePacket;
import gplayer.com.prefs.GPlayerPreferences;
import gplayer.com.prefs.GPlayerSettings;
import gplayer.com.service.DataSerializer;
import gplayer.com.service.FileMedia;
import gplayer.com.util.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.URL;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import static java.util.stream.Stream.of;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**Instantiates a window
*for manipulating and playing media files,
*namely: mp3, wav, mp4, and flv formats.
*@author Ganiyu Emilandu
*/

public class GPlayer extends Application {
    private static final String USER_HOME = System.getProperty("user.home");  //The user home directory
    public static final String RESOURCE_PATH = java.nio.file.Paths.get(USER_HOME, "gplayer", "resources").toString();  //Directory where media resources are to be saved
    private static BaseResourcePacket resource;
    private static java.awt.SystemTray systemTray;
    private static java.awt.TrayIcon trayIcon;
    private static java.awt.MenuItem hideItem;  //For hiding and unhiding app window
    private static java.util.ArrayList<Logger> registeredLoggers = new java.util.ArrayList<>();  //Holds a list of all registered loggers
    private static boolean systemTrayIsSupported = false;
    public static final URL DEFAULT_CSS = GPlayer.class.getResource("/resources/css/theme.css");  //CSS file from which app window and controls are styled
    public static final String PREFS_ROOT = "gplayer/current_user";  //The base node for registry-based media data
    public static final GPlayerPreferences PREFERENCES = new GPlayerPreferences(PREFS_ROOT.concat("/media/info"), "");  //media playback registry node
    public static final int LAST_PROGRAM_EXECUTION_PHASE = PREFERENCES.getUserPreferences().getInt("programExecutionPhase", 0);  //Denotes various stages of program execution (-1, 1, 0; starting, running, terminating, respectively)
    private static String className = GPlayer.class.getName();  //Definitive name of this class
    public static boolean platformExiting;  //Flags true if the program is to exit, and false otherwise
    public static final Logger LOGGER = Logger.getLogger(className);  //records app exceptions and media info
    private static FileMedia fileMedia;  //class FileMedia reference
    public static final String stageTitle = "GPlayer";  //Title of the appication
    private static javafx.geometry.Rectangle2D visualScreenBounds;
    private static Stage primaryStage;  //References the stage created on initialization
    private static String exitTime;  //Timespan for which the application is to run before exiting
    private static String[] callArgument;  //References the passed array in args of main()
    private static final File ACCESS_CONTROL_FILE = new File(getResourcePath("access file"));  //Created and locked by the first-to-attempt instance of this app, and deleted on exit
    private static final File INPUT_FILE = new File(getResourcePath("input file"));
    private static FileOutputStream fout;  //Opens an output stream to the above file
    private static String appID = "{{A84AABFC-6EB0-4C85-8DD4-F34A3D8B759D}";

    /**main()
    *Entry point for the application
    *invoked by the Java Virtual Machine (JVM)
    *@param args
    *arguments passed to the app
    */
    public static void main(String[] args) {
        callArgument = args;
        launch(args);
    }  //Close the main method

    /**init()
    *initialization point for the application
    *Inherited from javafx.application.Application class
    *invoked by the FX Launcher
    *after launch() must have been invoked
    */
    @Override
    public void init() {
        if (acquireExecutionAccess()) {
            //Initialize app
            prepareLogFile(LOGGER, "logs/".concat(className + ".log"));
            LOGGER.info("Initializing application");
            resource = BaseResourcePacket.getPacket("GPlayerResource");
            boolean enabled = GPlayerSettings.programPrefs().getBoolean("enableAppTimeout", false);
            exitTime = (enabled)? GPlayerSettings.programPrefs().get("appTimeout", Utility.formatTime(3, 0)): "...";
            PREFERENCES.getUserPreferences().putInt("programExecutionPhase", -1);  //Portrays program initialization phase
        }
        else {
            Runtime.getRuntime().exit(0);
        }
    }

    /**start()
    *stage and scene creation phase for the application
    *inherited from javafx.application.Application class
    *invoked by the JavaFX Application thread
    *after either of init(), if present, and launch(), if init() is absent, must have been invoked
    *@param stage
    *the stage created by the JavaFX Application thread
    */
    @Override
    public void start(Stage stage) {
        try {
            LOGGER.config("Configuring stage, scene and media controls");
            long configTime = System.currentTimeMillis();
            primaryStage = stage;
            stage.setTitle(stageTitle);
            //Set stage icon
            stage.getIcons().add(FileMedia.iconify("gplayer.ico"));
            //Add app to system tray on Swing thread
            javax.swing.SwingUtilities.invokeLater(this::addAppToSystemTray);
            double screenWidth = getScreenWidth(), screenHeight = getScreenHeight();
            //Instantiate the File Media class
            fileMedia = new FileMedia();
            //Create a scene object with fileMedia as the base root
            Scene scene = new Scene(fileMedia, Math.ceil(screenWidth/2), Math.ceil(screenHeight/2), Color.BLACK);
            //Initialize menus and play controls
            fileMedia.initialize(stage, scene);
            //Add the css file to style with to the scene style sheets
            scene.getStylesheets().add(DEFAULT_CSS.toExternalForm());
            //Set the scene to stage
            stage.setScene(scene);
            //Fit stage to scene
            stage.sizeToScene();
            //Show the stage
            LOGGER.info("About to show stage");
            long showTime = System.currentTimeMillis();
            stage.show();
            showTime = System.currentTimeMillis() - showTime;
            LOGGER.info("Stage shown after " + showTime + " millis.");
            fileMedia.quitApplicationIn(exitTime);
            //Initialize play
            fileMedia.initializePlay(callArgument);
            //Update program run phase
            PREFERENCES.getUserPreferences().putInt("programExecutionPhase", 1);  //Portrays program running phase
            configTime = System.currentTimeMillis() - configTime;
            LOGGER.info("Configuration done in " + configTime + " milis.");
        }
        catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.log(Level.SEVERE, "Exception in start()", ex);
            Runtime.getRuntime().exit(0);
        }
    }

    /**stop()
    *stage, scene and affiliates destruction phase of the application
    *inherited from javafx.application.Application class
    *called by the JavaFX Application thread
    *before application shutdown
    */
    @Override
    public void stop() {
        LOGGER.info("Exiting application\n");
        if (!platformExiting)
            exitProgram();
        removeAppFromSystemTray();
    }

    /**Initiates saving of all necessary media info before app termination
    */
    public static void exitProgram() {
        fileMedia.updateMediaInfo(true);
        fileMedia.quitApplicationIn("...");
        PREFERENCES.getUserPreferences().putInt("programExecutionPhase", 0);  //Portrays program exit phase
        platformExiting = true;
    }

    /**Opens an output stream to a file
    *and writes/serializes DataSerializer info to that file
    *for future recovery.
    *@param file
    *the name of the file to which an output stream is to be opened on
    *@param serialization
    *the object with info to be serialized.
    *@return the serialized file if the serialization process was successful
    *and null otherwise
    */
    public static File serializeMediaInfo(String file, DataSerializer serialization) {
        //Let's try writing the passed info to the passed file
        try(ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            //If file is successfully opened
            LOGGER.info("About to sync media info");
            //Write out the passed information to the file
            outputStream.writeObject(serialization);
            //Then,
            LOGGER.info("Data sync was successful");
            return new File(file);
        }
        //If error is encountered while trying to open the file
        catch(Exception e) {
            LOGGER.log(Level.WARNING, "Unable to serialize media info", e);
            return null;
        }
    }

    /**Deserializes media info
    *@param file
    *the name of the serialized file
    *@throws java.io.IOException
    *if an io exception occurs during deserialization
    *@throws ClassNotFoundException
    *if the cast class cannot be found
    *@return the deserialized info if no exception occurs.
    */
    public static DataSerializer deserializeMediaInfo(String file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
            return (DataSerializer) inputStream.readObject();
        }
        catch (Exception ex) {
            throw ex;
        }
    }

    /**Gets and returns a path string to an existing file
    *or initiates creation of the file, if nonexistent, and returns the path string.
    *@param file
    *the file whose path is retrieved as a string.
    *@param newFile
    *flags creation and return a file string, if false, independent of RESOURCE_PATH directory; or as a subdirectory/subfile of RESOURCE_PATH, if true
    *@return a string, encapsulating the path to the file
    */
    public static String getResourcePath(String file, boolean newFile) {
        if (file == null)
            throw new NullPointerException("Null arguments cannot be resolved");
        if (file.isEmpty())
            throw new IllegalArgumentException("Empty arguments cannot be resolved");
        //If newFile is false
        //append file to RESOURCE_PATH
        //or leave it as is, if false
        java.nio.file.Path resource = java.nio.file.Paths.get((newFile)? file: RESOURCE_PATH, file);
        if (!java.nio.file.Files.exists(resource))
            //File is nonexistent!
            //Create file, thus
            resource = createResourcePath(resource);
        return (resource == null)? "": resource.toString();
    }

    /**Gets and returns a path string to an existing file, a subfile of RESOURCE_PATH directory,
    *or initiates creation of the file, if nonexistent, and returns the path string.
    *@param file
    *the file whose path is retrieved as a string.
    *@return a string, encapsulating the path to the file
    */
    public static String getResourcePath(String file) {
        return getResourcePath(file, false);
    }

    /**Initiates the creation of a file/directory,
    *creating any non-existent parent directories in the process.
    *In scenarios where the name of the file passed ends with the default file separator,
    *the file is assumed to be a directory;
    *else, it is assumed to be a regular file.
    *@param resource
    *the path to the file or directory
    *@return null if the file is nonexistent and an attempt to create one fails,
    *or a Path instance referencing the file, if the file was previously existent or the creation attempt succeeds.
    */
    private static java.nio.file.Path createResourcePath(java.nio.file.Path resource) {
        try {
            if (resource.toString().endsWith(File.separator))
                //Create a directory of the resource
                return java.nio.file.Files.createDirectories(resource);
            int nameCount = resource.getNameCount();
            java.nio.file.Files.createDirectories(resource.getRoot().resolve(resource.subpath(0, nameCount-1)));
            return java.nio.file.Files.createFile(resource);
        }
        catch (Exception ex) {
            return null;
        }
    }

    /**Attempts to begin the execution of this app instance
    *by trying to gain a lock on ACCESS_CONTROL_FILE.
    *@return true if the lock-request succeeds, that is, the file is not locked by another instance of this app,
    *and false otherwise
    */
    private boolean acquireExecutionAccess() {
        boolean run = false;
        java.nio.channels.FileLock lock = null;
        try {
            fout = new FileOutputStream(ACCESS_CONTROL_FILE);
            lock = fout.getChannel().tryLock();
            run = lock != null;
        }
        catch (Exception ex) {}
        if (run) {
            submitArguments(false, null);
            //Register a shutdown hook to clean up used resources
            final java.nio.channels.FileLock l = lock;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down");
                try {
                    if (l != null)
                        l.release();
                    if (fout != null)
                        fout.close();
                }
                catch (IOException ex) {}
                //Release all open log resources
                for (Logger logger: registeredLoggers)
                    releaseLogResourceFor(logger);
                ACCESS_CONTROL_FILE.delete();
                INPUT_FILE.delete();
            }));
        }
        else {
            try {
                fout.close();
            }
            catch (Exception ex) {}
            submitArguments(true, callArgument);
        }
        return run;
    }

    /**Opens an output stream to a file
    *and writes out the argument passed via main().
    *@param append
    *Overwrite existing contents, if false; or append to existing contents, if true
    *@param args
    *the arguments to write out
    */
    public static void submitArguments(boolean append, String[] args) {
        try(FileOutputStream outStream = new FileOutputStream(INPUT_FILE, append)) {
            if (args != null && args.length > 0) {
                for (String str: args)
                    outStream.write(str.concat("\n").getBytes());
            }
            else if (append)
                outStream.write(appID.concat("\n").getBytes());
        }
        catch (Exception ex) {}
    }

    /**Opens an input stream to a file
    *and retrieves the entries, if any.
    *@return a string array of the entries.
    */
    public static String[] getSubmittedArguments() {
        try(java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(INPUT_FILE))) {
            return reader.lines().toArray(String[]::new);
        }
        catch (Exception ex) {
            return new String[0];
        }
    }

    /**Forces this app window to the foreground, if it is open in the background
    *@param stage,
    *the stage to focus.
    *@throws NullPointerException
    *if stage is null
    */
    public static void focusStage(Stage stage) {
        if (stage == null)
            throw new NullPointerException("Stage cannot be null");
        if (!stage.isShowing()) {
            LOGGER.info("unhiding stage");
            stage.setIconified(false);
            stage.show();
        }
        LOGGER.info("Bringing stage to front");
        stage.toFront();
    }

    /**Creates a Logger object with a file handler for record keeping.
    *@param logClass
    *the class after which the created Logger and file handler are to be named
    *@return a Logger object
    */
    public static Logger createLogger(Class<?> logClass) {
        //Create a logger with the name of the class passed
        Logger logger = Logger.getLogger(logClass.getName());
        String fileName = String.join("/", "logs", logClass.getName().concat(".log"));
        prepareLogFile(logger, fileName);
        return logger;
    }

    /**Creates and binds a logger object to a medium (FileHandler) through which log outputs could be recorded.
    *@param logger
    *activity/log recorder
    *@param name
    *the name of the file to which log output is written
    */
    private static void prepareLogFile(Logger logger, String name) {
        try {
            //Uncomment the following 3 lines to have log activities recorded in the file handler
            //FileHandler fileHandler = new FileHandler(getResourcePath(name));
            //fileHandler.setFormatter(new SimpleFormatter());
            //logger.addHandler(fileHandler);  //logger can now write to file handler
            logger.setUseParentHandlers(false);  //Disables writing log activities to default output stream (console)
            registeredLoggers.add(logger);
        }
        catch (Exception ex) {}
    }

    /**Adds this app to OS system tray if supported.*/
    private void addAppToSystemTray() {
        try {
            LOGGER.info("Trying to add app to system tray");
            //Initialize awt toolkit
            java.awt.Toolkit.getDefaultToolkit();
            if (java.awt.SystemTray.isSupported()) {
                systemTrayIsSupported = true;
                LOGGER.info("System tray is supported");
                //Obtain an instance of the system tray
                java.awt.SystemTray systemTray = java.awt.SystemTray.getSystemTray();
                this.systemTray = systemTray;
                java.awt.Image image = javax.imageio.ImageIO.read(this.getClass().getResource("/resources/icons/GPlayer.png"));
                java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image, stageTitle);
                this.trayIcon = trayIcon;
                //Clicking on the tray icon should focus stage
                trayIcon.addActionListener((event) -> javafx.application.Platform.runLater(() -> focusStage(primaryStage)));
                //Instantiate an open menu item for opening the app
                java.awt.MenuItem openItem = new java.awt.MenuItem(resource.getString("open.text"));
                //Set openItem font to bold
                java.awt.Font defaultFont = java.awt.Font.decode(null);
                openItem.setFont(defaultFont.deriveFont(java.awt.Font.BOLD));
                //Clicking on this menu item should focus stage
                openItem.addActionListener((event) -> javafx.application.Platform.runLater(() -> focusStage(primaryStage)));
                hideItem = new java.awt.MenuItem();
                //Clicking this menu item should hide/unhide the stage
                hideItem.addActionListener((event) -> fireHideItem(primaryStage.isShowing(), true));
                //A menu item for exiting app
                java.awt.MenuItem exitItem = new java.awt.MenuItem(resource.getString("exit.text"));
                //Clicking this menu item should remove and cancel all tray and app run processes
                exitItem.addActionListener((event) -> javafx.application.Platform.exit());
                //Create a popup menu to hold the open and exit menu items
                java.awt.PopupMenu popupMenu = new java.awt.PopupMenu(stageTitle);
                popupMenu.add(openItem);
                popupMenu.add(hideItem);
                popupMenu.addSeparator();
                popupMenu.add(exitItem);
                trayIcon.setPopupMenu(popupMenu);
                //Add trayIcon to systemTray
                systemTray.add(trayIcon);
            }
        }
        catch (Exception ex) {
            LOGGER.log(Level.INFO, "Exception initializing system tray", ex);
        }
    }

    /**Removes this app from OS system tray.*/
    private void removeAppFromSystemTray() {
        if (systemTray != null)
            systemTray.remove(trayIcon);
    }

    /**Adds a java.awt.MenuItem instance
    *to app tray icon.
    *By default, items are added to the end of the tray icon popup;
    *but this behaviour could be overridden by specifying the index/position to insert the item
    *@param item
    *the item to add to tray icon
    *@param index
    *a vararg array whose first index, if present, points to the position to insert the menu
    */
    public static void addToTrayIcon(java.awt.MenuItem item, int... index) {
        if (trayIcon == null)
            return;
        if (index.length == 0)
            trayIcon.getPopupMenu().add(item);
        else
            trayIcon.getPopupMenu().insert(item, index[0]);
    }

    /**Displays notifications on the tray icon.
    *@param message
    *the message to display on the tray icon
    *@return true if the tray icon isn't null, as well as the passed string,
    *return false otherwise.
    */
    public static boolean notify(String message) {
        if (trayIcon == null || message == null)
            return false;
        javax.swing.SwingUtilities.invokeLater(() -> trayIcon.displayMessage(stageTitle, message, java.awt.TrayIcon.MessageType.INFO));
        return true;
    }

    /**Modifies the text displayed on the menu item for hiding/unhiding app window.
    *Displays hide when app window is open,
    *and unhide when app window is closed.
    *@param showing
    *Asserts the open state of app window
    *@param change
    *Asserts reversal of the state of app window
    */
    public static void fireHideItem(boolean showing, boolean change) {
        if (hideItem == null)
            return;
        hideItem.setLabel(resource.getString((showing)? "hide.text": "unhide.text"));
        if (change) {
            javafx.application.Platform.runLater(() -> {
                if (showing) {
                    javafx.application.Platform.setImplicitExit(false);
                    primaryStage.hide();
                }
                else
                    focusStage(primaryStage);
            });
        }
    }

    /**Releases all resources held by a Logger object
    *@param l
    *the Logger whose resources are to be released
    */
    public static void releaseLogResourceFor(Logger l) {
        of(l.getHandlers()).forEach((handler) -> handler.close());
    }

    /**Initializes app update.
    *@return true if the update process was successfully initialized
    *and false otherwise
    */
    public static boolean checkUpdates() {
        try {
            java.util.Properties props = new java.util.Properties();
            //Load app properties
            props.load(GPlayer.class.getResourceAsStream("/resources/properties/app-info.properties"));
            com.briksoftware.updatefx.core.UpdateFX updater = new com.briksoftware.updatefx.core.UpdateFX(new URL((String)props.get("app.url")), Integer.parseInt((String)props.get("app.release")), (String)props.get("app.version"), Integer.parseInt((String)props.get("app.licenseVersion")), GPlayer.class.getResource("/resources/css/theme.css"));
            updater.checkUpdates();
            return true;
        }
        catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Problems checking updates", ex);
            return false;
        }
    }

    /**Returns the width of the display screen for this window.*/
    public static final double getScreenWidth() {
        return (visualScreenBounds != null)? visualScreenBounds.getWidth(): (visualScreenBounds = javafx.stage.Screen.getPrimary().getVisualBounds()).getWidth();
    }

    /**Returns the height of the display screen for this window.*/
    public static final double getScreenHeight() {
        return (visualScreenBounds != null)? visualScreenBounds.getHeight(): (visualScreenBounds = javafx.stage.Screen.getPrimary().getVisualBounds()).getHeight();
    }

    /**Checks to see if primaryStage has a child window open.
    *@return true if primaryStage is nonnull, is contained in FileMedia.getDisplayedPopups(), and its registered child window is showing,
    *return false otherwise.
    */
    public static final boolean hasChildWindowOpen() {
        return hasChildWindowOpen(primaryStage);
    }

    /**Checks to see if a certain window has a child window open.
    *@return true if the specified window is nonnull, is contained in FileMedia.getDisplayedPopups(), and its registered child window is showing,
    *return false otherwise.
    */
    public static final boolean hasChildWindowOpen(javafx.stage.Window window) {
        if (window == null)
            return false;
        javafx.stage.Window childWindow = FileMedia.getDisplayedPopups().get(window);
        return (childWindow == null)? false: childWindow.isShowing();
    }

    /**Returns reference to the active resource bundle file.*/
    public static final BaseResourcePacket getResourcePacket() {
        return resource;
    }

    /**Returns a reference to the main window.*/
    public static final Stage getPrimaryStage() {
        return primaryStage;
    }

    /**Returns the root node of this window.*/
    public static final FileMedia getSceneRoot() {
        return fileMedia;
    }
}