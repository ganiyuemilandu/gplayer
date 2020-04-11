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

import gplayer.com.util.Duo;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.animation.PauseTransition;
import static javafx.application.Platform.runLater;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**Oversees the transfer/deletion of files
*on user device.
*@author Ganiyu Emilandu
*/

public class FileManager {
    private gplayer.com.util.SerialTaskExecutor taskExecutor = new gplayer.com.util.SerialTaskExecutor();
    private MediaLibrary library;
    private FileTask fileTask;
    private List<Path> systemFiles = new ArrayList<>(), systemDirectories = new ArrayList<>();
    private List<Path> playlistFiles = new ArrayList<>(), playlistDirectories = new ArrayList<>();
    private Path target;
    private int filesSize, directoriesSize, totalFilesSize;
    private int completedDirectoriesSize, completedFilesSize, completedTotalFilesSize;
    private FileAction FILEACTION;
    private Set<Path> sources = new HashSet<>();
    private String separator = MediaLibrary.PATH_EXTENSION;
    private javafx.scene.control.Label label = gplayer.com.prefs.GPlayerSettings.createLabel("");
    private Map<Node, Duo<Path, Boolean>> map = new HashMap<>();
    private CheckBox replaceOrSkip = new CheckBox(FileAction.get("apply", ".prompt"));
    private CheckBox retryOrCancel = new CheckBox(replaceOrSkip.getText());
    private CheckBox silenceNotification = new CheckBox(FileAction.get("silent_notification", ".prompt"));
    private ProgressBar progressBar = createProgressBar();
    private ToolBar toolBar = createToolBar();
    private VBox layout;
    private static WatchEvent.Kind<?>[] watchEventKinds = {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY};
    private static WatchService watchService;
    static {
        try {
            watchService = java.nio.file.FileSystems.getDefault().newWatchService();
        }
        catch (Exception ex) {}
    }

    /**Creates a new instance of this class.*/
    public FileManager() {}

    /**Creates a new instance of this class.
    *@param layout
    *the pane on which file operation option and progress are made visible to user.
    */
    public FileManager(VBox layout) {
        this(layout, null);
    }

    public FileManager(VBox layout, MediaLibrary library) {
        this.layout = layout;
        this.library = library;
        if (layout != null)
            layout.getChildren().addAll(label, toolBar, progressBar);
    }

    /**Watches a directory for changes, ranging from file/directory creation, deletion, to modification.
    *@param directory
    *a path object pointing to a valid system directory
    *@param consumer
    *course of action for the changes observed in the watched directory
    *@param watchEventKinds
    *the type of changes to watch out for.
    *@return the watch key from which observed changes can be retrieved.
    */
    public static WatchKey watch(Path directory, Consumer<WatchEvent<?>> consumer, WatchEvent.Kind<?>... watchEventKinds) {
        try {
            WatchEvent.Kind<?>[] kinds = (watchEventKinds.length > 0)? watchEventKinds: FileManager.watchEventKinds;
            WatchKey key = directory.register(watchService, kinds);
            Thread thread = new Thread(() -> {
                try {
                    WatchKey threadKey = key;
                    while ((threadKey = watchService.take()) != null) {
                        if (consumer != null)
                            threadKey.pollEvents().forEach((event) -> consumer.accept(event));
                        threadKey.reset();
                    }
                }
                catch (InterruptedException ex) {
                    System.out.println("Watch key Interrupted!");
                }
            });
            thread.setDaemon(true);
            thread.start();
            return key;
        }
        catch (Exception ex) {
            return null;
        }
    }

    public static WatchKey watch(Path directory, WatchEvent.Kind<?>... watchEventKinds) {
        return watch(directory, null, watchEventKinds);
    }

    public static WatchEvent.Kind<?>[] copyOfWatchEventKinds(int from, int to) {
        int length = to - from;
        WatchEvent.Kind<?>[] watchEventKindsCopy = new WatchEvent.Kind<?>[length];
        System.arraycopy(watchEventKinds, from, watchEventKindsCopy, 0, length);
        return watchEventKindsCopy;
    }

    public static WatchEvent.Kind<?>[] copyOfWatchEventKinds(int from) {
        return copyOfWatchEventKinds(from, watchEventKinds.length);
    }

    public static WatchEvent.Kind<?>[] copyOfWatchEventKinds() {
        return copyOfWatchEventKinds(0, watchEventKinds.length);
    }

    /**Initiates the Creation of options to cancel, pause/resume, etc, file operation in progress.
    *@return a toolbar object with the available options.
    */
    private ToolBar createToolBar() {
        ToolBar bar = new ToolBar();
        Button cancel = new Button("Cancel");  //internationalize
        cancel.setOnAction((ae) -> {
            if (showDialog(null, MediaLibrary.resource.getString("cancelDialog.contentText"), AlertType.CONFIRMATION, ButtonType.YES, ButtonType.NO))
                fileTask.cancel();
        });
        String pauseText = MediaLibrary.resource.getString("pause.text"), resumeText = MediaLibrary.resource.getString("resume.text");
        Button resume_pause = new Button(pauseText);
        resume_pause.setOnAction((ae) -> {
            String text = resume_pause.getText();
            resume_pause.setText((text.equals(pauseText))? resumeText: pauseText);
            if (resume_pause.getText().equals(pauseText))
                fileTask.resume();
            else
                fileTask.suspend();
        });
        bar.getItems().addAll(cancel, resume_pause);
        bar.setVisible(false);
        return bar;
    }

    private static void focus(Node node, Node... nodes) {
        runLater(() -> {
            Node toRefocus = gplayer.com.service.FileMedia.getFocusedNode(nodes);
            if (node != null)
                node.requestFocus();
            if (toRefocus != null)
                gplayer.com.service.FileMedia.waitThenRun(javafx.util.Duration.millis(500), (() -> runLater(toRefocus::requestFocus)));
        });
    }

    private static Node grid(Node node1, Node node2) {
        javafx.scene.layout.GridPane pane = gplayer.com.prefs.GPlayerSettings.createPane();
        pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
javafx.scene.layout.GridPane.setHgrow(node2, javafx.scene.layout.Priority.ALWAYS);
javafx.scene.layout.GridPane.setFillWidth(node2, true);
        pane.add(node1, 0, 0);
        pane.add(node2, 1, 0);
        return pane;
    }

    /**Creates an alert dialog with a label and another node (if specified), displayed in the content area of the dialog.
    *@param node
    *the node (if nonnull) to include alongside the label to display.
    *@param contentText
    *the text to set the contentTextProperty() of the dialog to, as well as to display on the label.
    *@param alertType
    *the type enum of the to-create alert.
    *@param buttonTypes
    *Optional button types to overwrite the to-create alert default.
    *@return an instance of the javafx Alert dialog.
    */
    public static Alert createAlert(Node node, String contentText, AlertType alertType, ButtonType... buttonTypes) {
        javafx.scene.control.Label label = gplayer.com.prefs.GPlayerSettings.createLabel(contentText);
        Alert alert = new Alert(alertType, contentText, buttonTypes);
        Node[] nodes = java.util.Arrays.stream(buttonTypes).map(alert.getDialogPane()::lookupButton).toArray(Node[]::new);
        Node paneContent = (node == null)? label: grid(label, node);
        label.textProperty().addListener((listener) -> {
        alert.getDialogPane().setContent(paneContent);
focus(label, nodes);
});
        label.textProperty().bind(alert.contentTextProperty());
        alert.setOnShown((we) -> focus(label, nodes));
        return alert;
    }

    /**Creates an alert dialog with a label displayed in the content area of the dialog.
    *@param contentText
    *the text to set the contentTextProperty() of the dialog to, as well as to display on the label.
    *@param alertType
    *the type enum of the to-create alert.
    *@param buttonTypes
    *Optional button types to overwrite the to-create alert default.
    *@return an instance of the javafx Alert dialog.
    */
    public static Alert createAlert(String contentText, AlertType alertType, ButtonType... buttonTypes) {
        return createAlert(null, contentText, alertType, buttonTypes);
    }

    /**Creates an alert dialog with a label displayed in the content area of the dialog.
    *@param alertType
    *the type enum of the to-create alert.
    *@param buttonTypes
    *Optional button types to overwrite the to-create alert default.
    *@return an instance of the javafx Alert dialog.
    */
    public static Alert createAlert(AlertType alertType, ButtonType... buttonTypes) {
        return createAlert(null, "", alertType, buttonTypes);
    }

    private boolean showDialog(Node content, String contentText, AlertType alertType, ButtonType... buttonTypes) {
        Duo<Path, Boolean> value = (content == null)? null: map.get(content);
        if (value != null) {
            if (content instanceof CheckBox && ((CheckBox) content).isSelected()) {
                if (value.getValue() && value.getKey() != null)  //This action has been applied to this file already
                    return (showFailedDialog(value.getKey()))? false: false;
                return value.getValue();
            }
        }
        boolean[] decisionHolder = new boolean[1];
        Runnable runnable = (() -> {
            Alert alert = createAlert(content, contentText, alertType, buttonTypes);
            List<ButtonType> list = alert.getDialogPane().getButtonTypes();
            ButtonType expectedButtonType = list.get(0);
            PauseTransition pt = (list.size() > 1)? null: new PauseTransition(Duration.seconds(5));
            if (pt != null) {
                pt.setOnFinished((event) -> alert.hide());
                runLater(pt::play);
            }
            java.util.Optional<ButtonType> response = (library == null)? alert.showAndWait(): library.executeTask(alert::showAndWait);
            if (response.isPresent() && response.get() == expectedButtonType)
                decisionHolder[0] = true;
            fileTask.resume();
        });
        if (!javafx.application.Platform.isFxApplicationThread()) {
            runLater(runnable::run);
            fileTask.suspend(true);
        }
        else
            runnable.run();
        if (content != null)
            map.put(content, new Duo<Path, Boolean>(null, decisionHolder[0]));
        return decisionHolder[0];
    }

    private boolean showFileAlreadyExistsDialog(List<Path> list, Path file) {
        String contentText = FileAction.get("existing_file", ".info", MediaLibrary.resolvePathName(file));
        String replaceText = MediaLibrary.resource.getString("fileAction.replace.prompt"), skipText = MediaLibrary.resource.getString("fileAction.skip.prompt");
        int index = list.size() - 1;
        Node node = (!list.isEmpty() && list.get(index) != file)? replaceOrSkip: null;
        ButtonType[] options = {new ButtonType(replaceText, ButtonData.YES), new ButtonType(skipText, ButtonData.NO)};
        return showDialog(node, contentText, AlertType.CONFIRMATION, options);
    }

    private boolean showErrorDialog(Path file) {
        String contentText = FileAction.get(FILEACTION.getActionID(), ".error", MediaLibrary.resolvePathName(file));
        String retryText = MediaLibrary.resource.getString("fileAction.retry.prompt");
        ButtonType[] options = {new ButtonType(retryText, ButtonData.OK_DONE), ButtonType.CANCEL};
        boolean result = showDialog(retryOrCancel, contentText, AlertType.ERROR, options);
        //Register the passed file
        map.get(retryOrCancel).setKey(file);
        return result;
    }

    private boolean showFailedDialog(Path file) {
        String text = FileAction.getMessage(FILEACTION.getUnexecutedActionID(), MediaLibrary.resolvePathName(file));
        return showDialog(silenceNotification, text, AlertType.ERROR);
    }

    /**Creates and returns a progress bar to calculate and relay progress update on file operations.*/
    private ProgressBar createProgressBar() {
        ProgressBar pb = new ProgressBar();
        pb.setPrefWidth(600.0);
        pb.setVisible(false);
        return pb;
    }

    private void performPreTaskRoutine() {
        replaceOrSkip.setSelected(false);
        retryOrCancel.setSelected(false);
        silenceNotification.setSelected(false);
        toolBar.setVisible(true);
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        progressBar.progressProperty().bind(fileTask.progressProperty());
        label.textProperty().bind(fileTask.messageProperty());
    }

    private void performPostTaskRoutine() {
        toolBar.setVisible(false);
        progressBar.setVisible(false);
        progressBar.progressProperty().unbind();
        label.textProperty().unbind();
        systemFiles = new ArrayList<>();
        systemDirectories = new ArrayList<>();
        playlistFiles = new ArrayList<>();
        playlistDirectories = new ArrayList<>();
        map = new HashMap<>();
    }

    private void updatePreActionStatus(FileAction ACTION, Path path) {
        fileTask.updateMessage(ACTION.getMessage(ACTION.getActionID(), MediaLibrary.resolvePathName(path)));
    }

    private void updatePostActionStatus(FileAction ACTION, Path path) {
        fileTask.updateMessage(ACTION.getMessage(ACTION.getExecutedActionID(), MediaLibrary.resolvePathName(path)));
        if (Files.isDirectory(path))
            completedDirectoriesSize++;
        else
            completedFilesSize++;
    }

    List<Path> collectLikeItems(Collection<? extends TreeItem<Path>> collection) {
        Map<Boolean, List<TreeItem<Path>>> map = collection.stream().collect(Collectors.partitioningBy((item) -> item instanceof PlaylistCatalogue));
        map.get(true).sort((item1, item2) -> ((PlaylistCatalogue)item1).index - ((PlaylistCatalogue)item2).index);
        List<String> elements = new ArrayList<>();
        map.get(true).forEach((item) -> {
            PlaylistCatalogue pc = (PlaylistCatalogue) item;
            if (pc.isLeaf()) {
                Path path = pc.getPathChain();
                String string = pc.getValue().toString() + separator + path.toString();
                if (!contains(path))
                    playlistFiles.add(Paths.get(string));
                else
                    elements.add(string);
            }
            else if (pc.isSelected())
                playlistDirectories.add(pc.getPathChain());
        });
        if (!elements.isEmpty())
            playlistFiles.addAll(generateUniquePaths(elements));
        map = map.get(false).stream().collect(Collectors.partitioningBy((item) -> MediaLibrary.isValidPath(item.getValue()) && ((javafx.scene.control.CheckBoxTreeItem<Path>)item).isSelected()));
        return map.get(true).stream().map((item) -> item.getValue()).collect(Collectors.toList());
    }

    private int sumAvailableFiles() {
        Map<Boolean, List<Path>> map = systemFiles.stream().collect(Collectors.partitioningBy((path) -> Files.isDirectory(path)));
        directoriesSize = map.get(true).size() + playlistDirectories.size();
        filesSize = map.get(false).size() + playlistFiles.size();
        return directoriesSize + filesSize;
    }

    private Path resolvePath(List<Path> list, Path path) {
        Path root = target;
        int startIndex = root.getNameCount();
        int endIndex = path.getNameCount() - 1;
        for (int i = startIndex; i < endIndex; i++) {
            Path p = root.resolve(path.getName(i));
            if (Files.exists(p) && list.contains(p))
                root = p;
        }
        return root.resolve(path.getName(endIndex));
    }

    private List<Path> generateUniquePaths(List<String> list) {
        List<Path> uniqueElements = new ArrayList<>();
        while (!list.isEmpty()) {
            int index = 0;
            int uniqueNum = 2;
            Path path = addToUniqueElements(uniqueElements, list.remove(index), uniqueNum++);
            while ((index = indexOf(path, list)) != -1)
                addToUniqueElements(uniqueElements, list.remove(index), uniqueNum++);
        }
        return uniqueElements;
    }

    private Path addToUniqueElements(List<Path> uniqueElements, String element, int uniqueNum) {
        Path path = Paths.get(element.split(separator)[1]);
        int lastIndex = element.lastIndexOf('.');
        String firstHalf = element.substring(0, lastIndex), lastHalf = element.substring(lastIndex);
        uniqueElements.add(Paths.get(firstHalf + uniqueNum + lastHalf));
        return path;
    }

    private boolean contains(Path path) {
        return indexOf(path, playlistFiles) != -1;
    }

    private <T> int indexOf(Path path, List<T> list) {
        String string = path.toString();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            T t = list.get(i);
            String[] strings = null;
            if (t instanceof Path)
                strings = ((Path)t).toString().split(separator);
            else
                strings = ((String)t).split(separator);
            if (strings[1].equals(string))
                return i;
        }
        return -1;
    }

    private Set<Path> subpaths(Collection<? extends Path> collection, int startIndex, int endIndex) {
        return collection.stream().map((item) -> {
            if (item.getNameCount() == 0)
                return item;
            Path root = item.getRoot();
            return root.resolve(item.subpath(startIndex, endIndex));
        }).collect(Collectors.toCollection(HashSet::new));
    }

    private Set<Path> subpaths(Collection<? extends Path> collection, int endIndex) {
        return subpaths(collection, 0, endIndex);
    }

    private Set<Path> getParents(Collection<? extends Path> children) {
        Set<Path> parents = new HashSet<>();
        for (Path child: children) {
            Path parent = child.getParent();
            if (parent != null)
                parents.add(parent);
        }
        return parents;
    }

    private Set<Path> findCommonAncestryRoot(Collection<? extends Path> collection) {
        java.util.TreeSet<Path> children = new java.util.TreeSet<>((a, b) -> a.getNameCount() - b.getNameCount());
        children.addAll(collection);
        Path firstChild = (children.isEmpty())? null: children.first();
        if (firstChild == null)
            return children;
        int firstChildNameCount = firstChild.getNameCount();
        Set<Path> parents = new HashSet<>();
        for (Path child: children) {
            int childNameCount = child.getNameCount();
            if (Files.isDirectory(child) || childNameCount > firstChildNameCount) {
                Path root = child.getRoot();
                int limit = (Files.isDirectory(child))? childNameCount+1: childNameCount;
                for (int nameEndIndex = firstChildNameCount; nameEndIndex < limit; nameEndIndex++)
                    parents.add((childNameCount == 0)? child: root.resolve(child.subpath(0, nameEndIndex)));
            }
        }
        if (!parents.isEmpty())
            systemDirectories.addAll(parents);
        return subpaths(children, firstChildNameCount);
    }

    private Set<Path> findCommonAncestry(Collection<? extends Path> collection) {
        Set<Path> children = findCommonAncestryRoot(collection);
        boolean childrenModified;
        do {
            childrenModified = false;
            Set<Path> parents = getParents(children);
            if (!parents.isEmpty()) {
                systemDirectories.addAll(parents);
                children = parents;
                childrenModified = true;
            }
        }
        while (children.size() > 1 && childrenModified);
        return children;
    }

    public void performFileAction(FileAction ACTION, Path target, Collection<? extends Path> files) {
        performFileAction(ACTION, target, files, ((collection) -> collection));
    }

    public <T> void performFileAction(FileAction ACTION, Path target, Collection<T> collection, javafx.util.Callback<Collection<T>, Collection<? extends Path>> callback) {
        taskExecutor.run((() -> {
                Collection<? extends Path> files = callback.call(collection);
                this.target = target;
                sources = findCommonAncestry(files);
                systemFiles.addAll(files);
                FILEACTION = ACTION;
                commenceFileOperation();
        }));
    }

    public List<Path> getProcessedFiles() {
        try {
            return java.util.Objects.requireNonNull(fileTask.getProcessedFiles());
        }
        catch (NullPointerException ex) {
            return new ArrayList<Path>();
        }
    }

    /**Gets and returns the transfer target/destination.*/
    public final Path getTransferTarget() {
        return target;
    }

    private void commenceFileOperation() {
        totalFilesSize = sumAvailableFiles();
        completedDirectoriesSize = completedFilesSize = completedTotalFilesSize = 0;
        fileTask = new FileTask();
        runLater(this::performPreTaskRoutine);
        fileTask.run();
    }

    private void systemFileOperation() {
        for (Path source: sources) {
            try {
                if (!systemFiles.isEmpty())
                    Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new ExtendedFileVisitor(systemFiles, source));
            }
            catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }

    private void playlistFileOperation() {
        if (FILEACTION != FileAction.DELETE)
            playlistFileOperation(playlistDirectories, false);
        playlistFileOperation(playlistFiles, true);
    }

    private void playlistFileOperation(List<Path> list, boolean fileOperation) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (fileTask.isCancelled())
                break;
            Path path = list.get(i);
            if (fileOperation) {
                String[] components = path.toString().split(separator);
                Path source = Paths.get(components[0]);
                Path target = resolvePath(playlistDirectories, this.target.resolve(Paths.get(components[1])));
                switch (FILEACTION) {
                    case COPY:
                    case MOVE:
                        transferFile(list, source, target, (FILEACTION == FileAction.COPY), true);
                        break;
                    case DELETE:
                        deleteFile(list, source, true, true);
                        break;
                    default:
                        throw new IllegalArgumentException("Not yet implemented!");
                }
            }
            else {
                Path dir = resolvePath(playlistDirectories, target.resolve(path));
                int index = i;
                list.set(index, dir);
                executeTask(dir, true, (() -> {
                    boolean created = false,  terminate = false;
                    do {
                        terminate = true;
                        try {
                            Files.createDirectory(dir);
                            created = true;
                        }
                        catch (FileAlreadyExistsException ex) {}
                        catch (Exception ex) {
                            terminate = !showErrorDialog(dir);
                        }
                    }
                    while (!created && !terminate);
                    return created;
                }));
            }
        }
    }

    private boolean deleteFile(List<Path> list, Path file, boolean delete, boolean run) {
        return executeTask(file, run, (() -> {
            boolean deleted = false;
            try {
                Files.delete(file);
                deleted = true;
            }
            catch (java.nio.file.DirectoryNotEmptyException ex) {
                System.out.println("Unable to delete this directory.");
                showFailedDialog(file);
            }
            catch (Exception ex) {
                System.out.println("Couldn't delete file.");
                if (showErrorDialog(file))
                    deleteFile(list, file, delete, run);
            }
            return deleted;
        }));
    }

    private boolean deleteFile(List<Path> list, Path file, boolean delete) {
        return deleteFile(list, file, delete, (!list.isEmpty() && list.contains(file) && delete));
    }

    private boolean transferFile(List<Path> list, Path source, Path target, boolean copy, boolean run, StandardCopyOption... transferOptions) {
        return executeTask(source, run, (() -> {
            boolean transferred = false;
            try {
                int index = list.indexOf(source);
                if (Files.isDirectory(source))
                    list.set(index, target);
                if (copy)
                    Files.copy(source, target, transferOptions);
                else
                    Files.move(source, target, transferOptions);
                transferred = true;
            }
            catch (FileAlreadyExistsException ex) {
                System.out.println("Already exists");
                if (showFileAlreadyExistsDialog(list, source))
                    transferFile(list, source, target, copy, run, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (Exception ex) {
                System.out.println("exception");
                showErrorDialog(source);
            }
            return transferred;
        }));
    }

    private boolean transferFile(List<Path> list, Path source, Path target, boolean copy, StandardCopyOption... transferOptions) {
        return transferFile(list, source, target, copy, (!list.isEmpty() && list.contains(source)), transferOptions);
    }

    private boolean executeTask(Path file, boolean execute, java.util.function.BooleanSupplier supplier) {
        fileTask.trySuspend();
        boolean executed = false;
        if (execute) {
            updatePreActionStatus(FILEACTION, file);
            if (supplier.getAsBoolean()) {
                updatePostActionStatus(FILEACTION, file);
                executed = true;
            }
        }
        fileTask.updateProgress(execute, file);
        return executed;
    }

    private class ExtendedFileVisitor extends SimpleFileVisitor<Path> {
        private List<Path> list;
        private Path source;
        private FileAction ACTION = FILEACTION;
        ExtendedFileVisitor(List<Path> list, Path source) {
            super();
            this.list = list;
            this.source = source;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
            return performFileAction(dir, ACTION.performPreVisitDirectoryAction(), 0);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
            return performFileAction(file, ACTION.performVisitFileAction(), 1);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException ex) {
            return performFileAction(dir, ACTION.performPostVisitDirectoryAction(), 2);
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException io) {
            if (io instanceof java.nio.file.AccessDeniedException)
                return FileVisitResult.SKIP_SUBTREE;
            return performFileAction(file, ACTION.performVisitFileFailedAction(), 3);
        }

        private FileVisitResult performFileAction(Path file, boolean performAction, int level) {
            if (fileTask.isCancelled())
                return FileVisitResult.TERMINATE;
            if (!performAction) {
                fileTask.updateProgress((!list.isEmpty() && list.contains(file)), file);
                return FileVisitResult.CONTINUE;
            }
            switch(ACTION) {
                case COPY:
                case MOVE:
                    return transferFiles(file, level);
                case DELETE:
                    return deleteFiles(file, level);
                default:
                    throw new IllegalArgumentException("Not yet implemented!");
            }
        }

        private FileVisitResult transferFiles(Path file, int level) {
            boolean isDirectory = Files.isDirectory(file);
            if (isDirectory) {
                if (!systemDirectories.contains(file))
                    return FileVisitResult.SKIP_SUBTREE;
                if (ACTION == FileAction.MOVE && level == 2)
                    return deleteFiles(file, level);
            }
            boolean copy = (ACTION == FileAction.COPY || (level == 0 && isDirectory));
            Path transferTarget = resolvePath(list, target.resolve(source.relativize(file)));
            transferFile(list, file, transferTarget, copy);
            return FileVisitResult.CONTINUE;
        }

        private FileVisitResult deleteFiles(Path file, int level) {
            deleteFile(list, file, level != 0);
            return FileVisitResult.CONTINUE;
        }

    }

    private class FileTask extends Task<List<Path>> {
        private Set<Path> processedFiles = new HashSet<>();
        private volatile boolean suspendTask;

        FileTask() {}

        protected List<Path> call() {
            long startTime = System.currentTimeMillis();
            updateMessage(FILEACTION.getMessage(FILEACTION.getPreActionID(), FILEACTION.getParameters(directoriesSize, filesSize, target)));
            if (!systemFiles.isEmpty())
                systemFileOperation();
            if (!playlistDirectories.isEmpty() || !playlistFiles.isEmpty())
                playlistFileOperation();
            long endTime = System.currentTimeMillis() - startTime;
            List<Path> files = new ArrayList<>(processedFiles);
            processedFiles = new HashSet<>();
            return files;
        }

        @Override
        public void cancelled() {
            super.cancelled();
            relayTaskReport(FILEACTION.getMessage("cancelled"));
        }

        @Override
        public void failed() {
            super.failed();
            relayTaskReport(FILEACTION.getMessage("failed"));
        }

        @Override
        public void succeeded() {
            super.succeeded();
            relayTaskReport(FILEACTION.getMessage(FILEACTION.getPostActionID(), FILEACTION.getParameters(completedDirectoriesSize, completedFilesSize, target)));
            System.out.println(FILEACTION.getMessage(FILEACTION.getPostActionID(), FILEACTION.getParameters(completedDirectoriesSize, completedFilesSize, target)));
        }

        @Override
        public void updateMessage(String msg) {
            super.updateMessage(msg);
        }

        @Override
        public void updateProgress(double workDone, double totalWork) {
            super.updateProgress(workDone, totalWork);
        }

        public void updateProgress(boolean update, Path processedFile) {
            if (update && processedFiles.add(processedFile))
                updateProgress(++completedTotalFilesSize, totalFilesSize);
        }

        public List<Path> getProcessedFiles() {
            return (isRunning())? new ArrayList<Path>(processedFiles): getValue();
        }

        private void relayTaskReport(String report) {
            if (library != null)
                library.refreshSystemView(MediaLibrary.baseRoot, getProgress() > 0.0);
            if (report != null && !report.trim().isEmpty())
                updateMessage(report);
            performPostTaskRoutine();
        }

        synchronized void resume() {
            if (!suspendTask)
                return;
            suspendTask = false;
            notify();
        }

        synchronized void suspend() {
            suspend(false);
        }

        synchronized void suspend(boolean immediate) {
            suspendTask = true;
            if (immediate)
                trySuspend();
        }

        synchronized void trySuspend() {
            synchronized(this) {
                while (suspendTask) {
                    try {
                        wait();
                    }
                    catch (InterruptedException ex) {}
                }
            }
        }

    }

    public static enum FileAction {
        COPY(getString("copy"), true, true),
        MOVE(getString("move"), true, true, true),
        DELETE(getString("delete"), true, true, true);

        private String id = super.toString().toLowerCase();
        private static String idPrefix = "fileAction.";
        private String action;
        private boolean preVisitDirectory, visitFile, postVisitDirectory, visitFileFailed;

        private FileAction(String a, boolean pvd, boolean vf, boolean psvd, boolean vff) {
            action = a;
            preVisitDirectory = pvd;
            visitFile = vf;
            postVisitDirectory = psvd;
            visitFileFailed = vff;
        }

        private FileAction(String a, boolean pvd, boolean vf, boolean psvd) {
            this(a, pvd, vf, psvd, false);
        }

        private FileAction(String a, boolean pvd, boolean vf) {
            this(a, pvd, vf, false, false);
        }

        public String getAction() {
            return action;
        }

        public String getActionID() {
            return id;
        }

        public String getExecutedActionID() {
            return id + "_success";
        }

        public String getUnexecutedActionID() {
            return id + "_failed";
        }

        public String getPreActionID() {
            return "pre_" + id;
        }

        public String getPostActionID() {
            return "post_" + id;
        }

        public String getText() {
            return getText("");
        }

        public String getText(String extension) {
            return get(id.concat(extension), ".text");
        }

        public boolean performPreVisitDirectoryAction() {
            return preVisitDirectory;
        }

        public boolean performVisitFileAction() {
            return visitFile;
        }

        public boolean performVisitFileFailedAction() {
            return visitFileFailed;
        }

        public boolean performPostVisitDirectoryAction() {
            return postVisitDirectory;
        }

        public Object[] getParameters(int directoriesSize, int filesSize, Path target) {
            Object[] parameters = {new Integer(directoriesSize), new Integer(filesSize), MediaLibrary.resolvePathName(target)};
            switch (this) {
                case COPY:
                case MOVE:
                    return parameters;
                case DELETE:
                    return java.util.Arrays.copyOf(parameters, 2);
                default:
                    throw new IllegalArgumentException("Not yet implemented!!!");
            }
        }

        private static String getString(String string) {
            return getString("fileAction.", string, ".string");
        }

        public static String getMessage(String id, Object... objects) {
            return get(id, ".message", objects);
        }

        private static String get(String id, String idSuffix, Object... objects) {
            return getString(idPrefix, id, idSuffix, objects);
        }

        public static String getString(String idPrefix, String id, String idSuffix, Object... objects) {
            String joinedString = String.join(id, idPrefix, idSuffix);
            return (objects.length == 0)? MediaLibrary.resource.getString(joinedString): MediaLibrary.resource.getAndFormatMessage(joinedString, objects);
        }
    }

}