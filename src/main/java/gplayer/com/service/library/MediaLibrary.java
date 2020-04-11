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

import gplayer.com.exec.GPlayer;
import gplayer.com.service.FileMedia;
import gplayer.com.service.library.FileManager.FileAction;
import gplayer.com.util.DataSearch;
import static gplayer.com.util.Utility.trimLeadingCharacter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.swing.filechooser.FileSystemView;

/**Co-ordinates system file and playlist cataloging.
*@author Ganiyu Emilandu
*/

public class MediaLibrary extends gplayer.com.util.PopupStage {
    static final gplayer.com.lang.BaseResourcePacket resource = GPlayer.getResourcePacket();
    private static FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private gplayer.com.util.SerialTaskExecutor libraryUpdater = new gplayer.com.util.SerialTaskExecutor();
    private final LinkedList<File> ALL_PLAYLISTS = new LinkedList<>();
    private ObservableList<TreeItem<Path>> expandedItems = FXCollections.observableArrayList();
    private DataSearch<TreeItem<Path>> expandedItemsSearch = new DataSearch<>(expandedItems, (expandedItem -> resolvePathName(expandedItem.getValue())));
    private Set<TreeItem<Path>> checkedItems = new HashSet<>(), checkedItemsTempStorage = new HashSet<>();  //For storing checked items
    private VBox rootLayout;
    private TreeView<Path> tree;
    private FileManager fm;
    static final String[] libraryContent = resource.getStringArray("libraryContent.array");
    static Path baseRoot = Paths.get(GPlayer.PREFERENCES.getUserPreferences().get("systemLibraryRoot", System.getProperty("user.home")));
    static final Map<Integer, List<String>> appPlaylist = new HashMap<>();
    static final Map<Integer, List<String>> userPlaylist = new HashMap<>();
    static final String SYSTEM_ROOT = resource.getString("systemRoot.name");
    static final String PATH_EXTENSION = java.util.UUID.randomUUID().toString();
    private Node secondaryNode;
    private HBox sceneRoot;
    private Stage stage;
    private double stageX, stageY, stageWidth, stageHeight;
    private boolean stageShownOnce;

    /**Creates a new instance of this class.
    *@param stage
    *The parent stage of this window.
    */
    public MediaLibrary(Stage stage) {
        super(stage, new HBox(20), "Libraries");  //Internationalize
        this.stage = stage;
    }

    public void initialize(Node node) {
        secondaryNode = node;
        popupStage.widthProperty().addListener((listener) -> stageWidth = popupStage.getWidth());
        popupStage.heightProperty().addListener((listener) -> stageHeight = popupStage.getHeight());
        popupStage.xProperty().addListener((listener) -> stageX = popupStage.getX());
        popupStage.yProperty().addListener((listener) -> stageY = popupStage.getY());
        stageX = getX();
        stageY = getY();
        stageWidth = getWidth();
        stageHeight = getHeight();
        initialize();
    }

    @Override
    protected void openAction() {
        popupStage.setX(stageX);
        popupStage.setY(stageY);
        popupStage.setWidth(stageWidth);
        popupStage.setHeight(stageHeight);
        super.openAction();
    }

    @Override
    public void showPopupStage() {
        if (!stageShownOnce) {
            if (!stage.isMaximized())
                stage.setMaximized(true);
            stageShownOnce = true;
        }
        super.showPopupStage();
    }

    @Override
    public void hidePopupStage() {
        super.hidePopupStage();
    }

    @Override
    protected javafx.stage.Modality getModality() {
        return javafx.stage.Modality.NONE;
    }

    @Override
    protected double getWidth() {
        return Math.floor(GPlayer.getSceneRoot().getAvailableWidth(GPlayer.getScreenWidth()) - getX());
    }

    @Override
    protected double getHeight() {
        return Math.floor(GPlayer.getSceneRoot().getAvailableHeight(GPlayer.getScreenHeight()));
    }

    private double getX() {
        return Math.ceil(GPlayer.getScreenWidth() / 3);
    }

    private double getY() {
        return (GPlayer.getSceneRoot().getTop() == null)? 0.0: Math.ceil(GPlayer.getSceneRoot().getTop().getLayoutBounds().getHeight());
    }

    @Override
    protected void populateSceneRoot() {
        sceneRoot = (HBox) super.sceneRoot;
        sceneRoot.setPadding(new Insets(10, 20, 10, 20));
        sceneRoot.getChildren().add(configureLibrary());
        if (secondaryNode != null) {
            HBox.setHgrow(secondaryNode, Priority.ALWAYS);
            @SuppressWarnings("unchecked")
            javafx.scene.control.ListView listView = getListView(secondaryNode);
            secondaryNode.setOnKeyPressed((ke) -> handleKeyEvent(ke, listView, tree, listView));
            secondaryNode.addEventFilter(javafx.scene.input.InputEvent.ANY, ((event) -> registerAccelerators(listView)));
            sceneRoot.getChildren().add(secondaryNode);
        }
    }

    /**Configures a tree view
    *of items representing user system and playlist files.
    *@return a VBox hosting the configured tree view.
    */
    public VBox configureLibrary() {
        configureTree();
        rootLayout = new VBox(20);
        VBox.setVgrow(tree, Priority.ALWAYS);
        @SuppressWarnings("unchecked")
        javafx.scene.control.ListView listView = getListView(secondaryNode);
        rootLayout.setOnKeyPressed((ke) -> handleKeyEvent(ke, tree, tree, listView));
        rootLayout.addEventFilter(javafx.scene.input.InputEvent.ANY, ((event) -> registerAccelerators(tree)));
        javafx.scene.control.Label label = new javafx.scene.control.Label(resource.getString("tree.label"));
        label.setLabelFor(tree);
        rootLayout.getChildren().addAll(label, tree);
        return rootLayout;
    }

    private HBox createHLayout() {
        HBox layout = new HBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        return layout;
    }

    private VBox createVLayout() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(5, 12, 5, 12));
        layout.setAlignment(Pos.CENTER);
        return layout;
    }

    private void handleKeyEvent(KeyEvent ke, Node focusedNode, Node... traversableNodes) {
        Node node = null;
        if (FileMedia.keyMatch(ke, KeyCode.RIGHT, KeyCombination.CONTROL_DOWN))
            node = gplayer.com.util.Utility.nextItem(focusedNode, traversableNodes);
        if (FileMedia.keyMatch(ke, KeyCode.LEFT, KeyCombination.CONTROL_DOWN))
            node = gplayer.com.util.Utility.previousItem(focusedNode, traversableNodes);
        if (node != null)
            node.requestFocus();
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.control.ListView getListView(Node node) {
        return (javafx.scene.control.ListView) ((javafx.scene.layout.Pane) node).getChildren().get(1);
    }

    private void registerAccelerators(javafx.scene.control.Control control) {
        control.getContextMenu().getItems().forEach((item) -> {
            if (item.getAccelerator() != null)
                popupStage.getScene().getAccelerators().put(item.getAccelerator(), item::fire);
        });
    }

    private ContextMenu getContextMenu() {
        String prefix = "getContextMenu.", suffix = "Item.text";
        ContextMenu cm = new ContextMenu();
        Runnable runnable = (() -> playPlaylist(tree.getSelectionModel().getSelectedItem()));
        java.util.function.Supplier<List<File>> supplier = (() -> FileMedia.enlist(checkedItems, ((item) -> java.nio.file.Files.isRegularFile(item.getValue())), ((item) -> item.getValue().toFile())));
        MenuItem[] playlistOperations = GPlayer.getSceneRoot().playListOperations(resource, tree.getSelectionModel(), runnable, supplier, ((item) -> new File(resolvePathName(item.getValue(), false))));
        cm.getItems().addAll(playlistOperations);
        MenuItem checkItem = new MenuItem(resource.getString(String.join("check", prefix, suffix)));
        checkItem.setAccelerator(KeyCombination.keyCombination("SHORTCUT+SPACE"));
        checkItem.setOnAction((ae) -> setCheckState(new ArrayList<TreeItem<Path>>(tree.getSelectionModel().getSelectedItems()), true));
        MenuItem uncheckItem = new MenuItem(resource.getString(String.join("uncheck", prefix, suffix)));
        uncheckItem.setAccelerator(KeyCombination.keyCombination("SHORTCUT+ALT+SPACE"));
        uncheckItem.setOnAction((ae) -> setCheckState(new ArrayList<TreeItem<Path>>(checkedItems), false));
        MenuItem sourceItem = new MenuItem(resource.getString(String.join("source", prefix, suffix)));
        sourceItem.setAccelerator(KeyCombination.keyCombination("SHORTCUT+O"));
        sourceItem.setOnAction((ae) -> changeSourceDirectory());
        MenuItem expandItem = new MenuItem(resource.getString(String.join("expand", prefix, suffix)));
        expandItem.setAccelerator(KeyCombination.keyCombination("SHORTCUT+SHIFT+RIGHT"));
        expandItem.setOnAction((ae) -> expandOrCollapseEntireTree(true));
        MenuItem collapseItem = new MenuItem(resource.getString(String.join("collapse", prefix, suffix)));
        collapseItem.setAccelerator(KeyCombination.keyCombination("SHORTCUT+SHIFT+LEFT"));
        collapseItem.setOnAction((ae) -> expandOrCollapseEntireTree(false));
        cm.getItems().addAll(checkItem, uncheckItem, sourceItem, expandItem, collapseItem);
        cm.getItems().addAll(availableFileActions("_checked", cm.getItems().get(0), this::performFileAction));
        popupOpenAction(cm);
        popupCloseAction(cm, (() -> setItemSelection(tree.getSelectionModel().getSelectedItem())));
        return cm;
    }

    public void popupOpenAction(javafx.stage.Window window, Runnable task) {
        window.setOnShown((we) -> {
            hideOnEscape = false;
            if (task != null)
                task.run();
        });
    }

    public void popupOpenAction(javafx.stage.Window window) {
        popupOpenAction(window, null);
    }

    public void popupCloseAction(javafx.stage.Window window, Runnable task) {
        window.setOnHidden((we) -> setToHideOnEscape(task));
    }

    public void popupCloseAction(javafx.stage.Window window) {
        popupCloseAction(window, null);
    }

    public static MenuItem[] availableFileActions(String extension, MenuItem disabler, java.util.function.Consumer<FileAction> consumer) {
        java.util.function.BiFunction<FileAction, String, MenuItem> constructor = ((action, accelerator) -> {
            MenuItem item = new MenuItem(action.getText(extension));
            item.setAccelerator(KeyCombination.keyCombination(accelerator));
            item.setOnAction((ae) -> consumer.accept(action));
            if (disabler != null)
                item.disableProperty().bind(disabler.disableProperty());
            return item;
        });
        MenuItem copyItem = constructor.apply(FileAction.COPY, "SHORTCUT+C");
        MenuItem moveItem = constructor.apply(FileAction.MOVE, "SHORTCUT+X");
        MenuItem deleteItem = constructor.apply(FileAction.DELETE, "SHORTCUT+DELETE");
        MenuItem[] items = {copyItem, moveItem, deleteItem};
        return items;
    }

    private void setToHideOnEscape(Runnable task) {
        if (task != null)
            task.run();
        FileMedia.waitThenRun(javafx.util.Duration.millis(200), (() -> hideOnEscape = true));
    }

    private void setToHideOnEscape() {
        setToHideOnEscape(null);
    }

    /**Creates and populates the library tree view.*/
    @SuppressWarnings("unchecked")
    private void configureTree() {
        tree = new TreeView<>(getRoot());
        tree.setTooltip(new javafx.scene.control.Tooltip(resource.getString("tree.tooltip")));
        //Enable multi-selection on the tree view
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        //Arm the tree view on item display
        tree.setCellFactory((TreeView<Path> t) -> new MediaLibraryCell());
        tree.setContextMenu(getContextMenu());
        GPlayer.getSceneRoot().playListProperty().addListener((listener) -> libraryUpdater.run(this::refreshPlaylistView));
        tree.setOnKeyPressed((ke) -> {
            if (FileMedia.keyMatch(ke, null, KeyCode.ENTER))  //Enter key pressed!
                playPlaylist(tree.getSelectionModel().getSelectedItem());  //Initiate item play
        });
        tree.addEventFilter(KeyEvent.KEY_TYPED, (ke) -> {
            //index of the TreeView currently selected item in expandedItems
            int selectedIndex = expandedItems.indexOf(tree.getSelectionModel().getSelectedItem());
            expandedItemsSearch.search(FileMedia.keyTyped(ke), selectedIndex+1);
        });
        expandedItemsSearch.setOnSucceeded((wse) -> {
            TreeItem<Path> item = expandedItems.get(expandedItemsSearch.getMatchIndex());
            setItemSelection(item);
        });
    }

    @SuppressWarnings("unchecked")
    private CheckBoxTreeItem[] retrieveLibraryContents() {
        SystemCatalogue sc = new SystemCatalogue(Paths.get(SYSTEM_ROOT + PATH_EXTENSION));
        AppPlaylistCatalogue apc = new AppPlaylistCatalogue(Paths.get(resource.getString("appPlaylist.name") + PATH_EXTENSION));
        UserPlaylistCatalogue upc = new UserPlaylistCatalogue(Paths.get(resource.getString("userPlaylist.name") + PATH_EXTENSION));
        CheckBoxTreeItem[] array = {sc, apc, upc};
        return array;
    }

    @SuppressWarnings("unchecked")
    private CheckBoxTreeItem<Path> getRoot() {
        populateMaps();
        CheckBoxTreeItem<Path> root = new CheckBoxTreeItem<>(Paths.get(resource.getString("libraryRoot.name") + PATH_EXTENSION));
        root.addEventHandler(CheckBoxTreeItem.<Path> checkBoxSelectionChangedEvent(), (CheckBoxTreeItem.TreeModificationEvent<Path> event) -> handleCheckedItemEvent(event));
        root.addEventHandler(TreeItem.<Path> branchExpandedEvent(), (TreeItem.TreeModificationEvent<Path> event) -> modifyExpandedItems(event.getTreeItem(), true));
        root.addEventHandler(TreeItem.<Path> branchCollapsedEvent(), (TreeItem.TreeModificationEvent<Path> event) -> modifyExpandedItems(event.getTreeItem(), false));
        CheckBoxTreeItem[] contents = retrieveLibraryContents();
        root.getChildren().addAll(contents);
        expandedItems.add(root);
        root.setExpanded(true);
        return root;
    }

    @SuppressWarnings("unchecked")
    private void expandOrCollapseEntireTree(boolean expand) {
        CheckBoxTreeItem<Path> c = (CheckBoxTreeItem) tree.getRoot();
        expandOrCollapseTree(c, expand);
    }

    @SuppressWarnings("unchecked")
    private void expandOrCollapseTree(CheckBoxTreeItem<Path> item, boolean expand) {
        if (!item.isLeaf()) {
            item.setExpanded(expand);
            item.getChildren().forEach((child) -> expandOrCollapseTree((CheckBoxTreeItem)child, expand));
        }
    }

    private void modifyExpandedItems(TreeItem<Path> parent, boolean add) {
        if (add) {
            //Begin insertion of children from parent immediate subsequent  index,
            //to align content storage with the item-display arrangement on the tree view.
            int parentIndex = expandedItems.indexOf(parent);
            if (parentIndex == -1)
                //Find the parent of the parent
                //and insert parent 1 index past parent's parent
                expandedItems.add(expandedItems.indexOf(parent.getParent())+1, parent);
            expandedItems.addAll(expandedItems.indexOf(parent)+1, parent.getChildren());
        }
        else
            expandedItems.removeAll(parent.getChildren());
        parent.getChildren().forEach((child) -> {
            if (!child.isLeaf() && child.isExpanded())
                modifyExpandedItems(child, add);
        });
    }

    private void changeSourceDirectory() {
        Path source = chooseDirectory(resource.getString("directorySelection.title"));
        refreshSystemView(source, !baseRoot.equals(source));
    }

    private void refreshTreeView() {
        javafx.application.Platform.runLater(() -> disposeFormerItems(null));
        tree.setRoot(getRoot());
    }

    void refreshSystemView(Path baseRoot, boolean refresh) {
        if (!refresh || baseRoot == null)
            return;
        this.baseRoot = baseRoot;
        GPlayer.PREFERENCES.getUserPreferences().put("systemLibraryRoot", baseRoot.toString());
        libraryUpdater.run(this::refreshSystemView);
    }

    private void refreshSystemView() {
        javafx.application.Platform.runLater(() -> disposeFormerItems((item) -> item instanceof SystemCatalogue));
        TreeItem<Path> root = tree.getRoot();
        TreeItem<Path> rootChild = root.getChildren().get(0), newRootChild = new SystemCatalogue(Paths.get(SYSTEM_ROOT + PATH_EXTENSION));
        TreeItem<Path> itemSelection = newRootChild;
        if (((Catalogue) rootChild).hasCachedChildren()) {
            TreeItem<Path> subrootChild = rootChild.getChildren().get(0), newSubrootChild = newRootChild.getChildren().get(0);
            TreeItem<Path> itemEquivalent = findItemEquivalent(subrootChild, newSubrootChild);
            if (itemEquivalent != null)
                itemSelection = mirrorItemAttributes(itemEquivalent, newSubrootChild, newSubrootChild);
        }
        updateView(itemSelection, newRootChild);
    }

    @SuppressWarnings("unchecked")
    private void refreshPlaylistView() {
        javafx.application.Platform.runLater(() -> disposeFormerItems((item) -> item instanceof PlaylistCatalogue));
        populateMaps();
        TreeItem[] contents = retrieveLibraryContents();
        TreeItem<Path> root = tree.getRoot();
        TreeItem<Path> appPlaylist = root.getChildren().get(1);
        TreeItem<Path> userPlaylist = root.getChildren().get(2);
        TreeItem<Path> itemSelection = mirrorItemAttributes(appPlaylist, contents[1], contents[1]);
        itemSelection = mirrorItemAttributes(userPlaylist, contents[2], itemSelection);
        updateView(itemSelection, contents[1], contents[2]);
    }

    private TreeItem<Path> mirrorItemAttributes(TreeItem<Path> oldItem, TreeItem<Path> newItem, TreeItem<Path> itemSelection) {
        TreeItem<Path> is = itemSelection;
        //Mirror the expanded and selected states of oldItem on newItem
        mirrorItemAttribute(oldItem, newItem);
        if (((Catalogue) oldItem).hasCachedChildren()) {
            //oldItem has, at least once, had its getChildren() method called
            //Let's cycle through the children of oldItem, mirroring attributes of each retrieved child on child-copy in newItem.
            for (TreeItem<Path> oldItemChild: oldItem.getChildren()) {
                int newItemChildIndex = newItem.getChildren().indexOf(oldItemChild);
                if (newItemChildIndex == -1)
                    //newItem doesn't contain this item,
                    //Proceed
                    continue;
                //Retrieve oldItemChild copy in newItem
                TreeItem<Path> newItemChild = newItem.getChildren().get(newItemChildIndex);
                if (oldItemChild.equals(tree.getSelectionModel().getSelectedItem()))
                    is = getItemSelection(newItem.getChildren(), oldItemChild, is);
                is = mirrorItemAttributes(oldItemChild, newItemChild, is);
            }
        }
        return is;
    }

    @SuppressWarnings("unchecked")
    private void mirrorItemAttribute(TreeItem<Path> oldItem, TreeItem<Path> newItem) {
        newItem.setExpanded(oldItem.isExpanded());
        if (oldItem instanceof CheckBoxTreeItem) {
            CheckBoxTreeItem<Path> checkedItem = (CheckBoxTreeItem<Path>) oldItem;
            if (checkedItem.isSelected() || checkedItem.isIndeterminate()) {
                CheckBoxTreeItem<Path> uncheckedItem = (CheckBoxTreeItem<Path>) newItem;
                uncheckedItem.setIndeterminate(checkedItem.isIndeterminate());
                uncheckedItem.setSelected(checkedItem.isSelected());
                checkedItemsTempStorage.add(newItem);
            }
        }
        if (newItem.isExpanded())
            javafx.application.Platform.runLater(() -> modifyExpandedItems(newItem, true));
    }

    @SuppressWarnings("unchecked")
    private void updateView(TreeItem<Path> itemSelection, TreeItem... items) {
        Set<TreeItem<Path>> checkedItemsTempStorage = this.checkedItemsTempStorage;
        if (!this.checkedItemsTempStorage.isEmpty())
            this.checkedItemsTempStorage = new HashSet<>();
        javafx.application.Platform.runLater(() -> {
            checkedItems.addAll(checkedItemsTempStorage);
            TreeItem<Path> root = tree.getRoot();
            for (TreeItem item: items) {
                boolean expanded = false;
                int index = root.getChildren().indexOf(item);
                if (index > -1) {
                    expanded = root.getChildren().get(index).isExpanded();
                    root.getChildren().set(index, item);
                }
                index = expandedItems.indexOf(item);
                if (index > -1) {
                    expandedItems.set(index, item);
                    item.setExpanded(expanded);
                }
            }
            setItemSelection(itemSelection);
        });
    }

    private TreeItem<Path> findItemEquivalent(TreeItem<Path> first, TreeItem<Path> second) {
        if (first.equals(second))
            return first;
        TreeItem<Path> parent = null, child = null;
        Predicate<TreeItem<Path>> pred = null;
        if (first.getValue().startsWith(second.getValue())) {  //the first item is a descendant of the second
            child = first;
            parent = second;
            pred = ((item) -> true);
        }
        else if (second.getValue().startsWith(first.getValue())) {  //the second item is a descendant of the first
            child = second;
            parent = first;
            pred = ((item) -> ((Catalogue) item).hasCachedChildren());
        }
        if (parent != null)
            //Cycle through until we hit the child
            //or the predicate condition is false
            while (pred.test(parent) && !(parent = getDirectAncestor(child, parent.getChildren())).equals(child));
        if (parent == null || !parent.equals(child))
            return null;
        return (child == first)? child: parent;
    }

    private TreeItem<Path> getDirectAncestor(TreeItem<Path> descendant, List<TreeItem<Path>> ancestors) {
        for (TreeItem<Path> ancestor: ancestors) {
            if (descendant.getValue().startsWith(ancestor.getValue()))
                return ancestor;
        }
        return null;
    }

    private TreeItem<Path> getItemSelection(List<TreeItem<Path>> items, TreeItem<Path> item, TreeItem<Path> defaultItem) {
        if (item != null && items.contains(item))
            return item;
        if (item.nextSibling() != null && items.contains(item.nextSibling()))
            return item.nextSibling();
        if (item.previousSibling() != null && items.contains(item.previousSibling()))
            return item.previousSibling();
        if (item.getParent() != null && items.contains(item.getParent()))
            return item.getParent();
        return defaultItem;
    }

    private void setItemSelection(TreeItem<Path> itemSelection) {
        if (itemSelection != null) {
            tree.getSelectionModel().clearSelection();
            tree.getSelectionModel().select(itemSelection);
        }
    }

    private void setCheckState(List<TreeItem<Path>> list, boolean state) {
        list.forEach((item) -> ((CheckBoxTreeItem) item).setSelected(state));
    }

    /**Executes tasks that requires the popup stage to be hidden (if showing), and reshown after task completion.*/
    public <T> T executeTask(java.util.function.Supplier<T> supplier) {
        boolean showing = popupStage.isShowing();
        if (showing)
            popupStage.hide();
        T value = supplier.get();
        if (showing) {
            hideOnEscape = false;
            setToHideOnEscape(popupStage::show);
        }
        return value;
    }

    public javafx.stage.Popup showTempPopup(String message) {
        return FileMedia.showTempPopup(popupStage, message, javafx.util.Duration.seconds(3), (() -> hideOnEscape = false), this::setToHideOnEscape);
    }

    private Path chooseDirectory(String title) {
        DirectoryChooser dc = new DirectoryChooser();
        if (title != null && !title.isEmpty())
            dc.setTitle(title);
        File result = executeTask((() -> dc.showDialog(GPlayer.getPrimaryStage())));
        return (result == null)? null: result.toPath();
    }

    private void populateMaps() {
        populateAppPlaylist();
        populateUserPlaylist();
    }

    private void populateAppPlaylist() {
        appPlaylist.clear();
        appPlaylist.put(0, modifyList(getChildren(resource.getStringArray("appPlaylistContent.array"))));
        appPlaylist.put(1, modifyList(getMainChildren()));
    }

    private void populateUserPlaylist() {
        userPlaylist.clear();
        userPlaylist.put(0, modifyList(getChildren(resource.getStringArray("userPlaylistContent.array"))));
        userPlaylist.put(1, modifyList(GPlayer.getSceneRoot().getMapKeys(0)));
        userPlaylist.put(2, modifyList(GPlayer.getSceneRoot().getMapKeys(1)));
        userPlaylist.put(3, modifyList(getMainChildren()));
    }

    static List<String> getMainChildren() {
        return getChildren(libraryContent);
    }

    static List<String> getChildren(String... children) {
        //Create a new copy of the passed contents
        //to avoid accidental modification of the source.
        String[] childrenCopy = java.util.Arrays.copyOf(children, children.length);
        return java.util.Arrays.asList(childrenCopy);
    }

    static List<String> modifyList(List<String> list) {
        return modifyList(list, PATH_EXTENSION);
    }

    static List<String> modifyList(List<String> list, String extension) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            String item = list.get(i);
            list.set(i, item + extension);
        }
        return list;
    }

    private void handleCheckedItemEvent(CheckBoxTreeItem.TreeModificationEvent<Path> event) {
        CheckBoxTreeItem<Path> item = event.getTreeItem();
        if (item.isSelected() || item.isIndeterminate())
            collectCheckedItems(item);
        else
            collectUncheckedItems(item);
    }

    @SuppressWarnings("unchecked")
    private void collectCheckedItems(CheckBoxTreeItem<Path> item) {
        if (item.isSelected() || item.isIndeterminate())
            if (checkedItems.add(item))
                item.getChildren().forEach((child) -> collectCheckedItems((CheckBoxTreeItem)child));
    }

    @SuppressWarnings("unchecked")
    private void collectUncheckedItems(CheckBoxTreeItem<Path> item) {
        if (!item.isSelected())
            if (checkedItems.remove(item))
                item.getChildren().forEach((child) -> collectUncheckedItems((CheckBoxTreeItem)child));
    }

    private void performFileAction(FileAction ACTION) {
        fm = (fm == null)? new FileManager(rootLayout, this): fm;
        invokeFileAction(fm, rootLayout, ACTION, resource.getString("fileOperation.empty_list.message"), new ArrayList<TreeItem<Path>>(checkedItems), fm::collectLikeItems);
    }

    public <T> FileManager invokeFileAction(FileManager manager, VBox layout, FileAction action, String message, Collection<T> collection, javafx.util.Callback<Collection<T>, Collection<? extends Path>> callback) {
        try {
            if (collection.isEmpty()) {
                showTempPopup(message);
                return manager;
            }
            String prompt = resource.getString(String.join(action.getActionID(), "fileAction.", ".prompt"));
            Path path = (manager == null)? null: manager.getTransferTarget();
            switch(action) {
                case COPY:
                case MOVE:
                    if (path != null) {
                        ButtonType[] options = {ButtonType.OK, ButtonType.CANCEL};
                        javafx.scene.control.Alert alert = FileManager.createAlert(resource.getAndFormatMessage("fileAction.transfer_location.prompt", path), javafx.scene.control.Alert.AlertType.CONFIRMATION, options);
                        java.util.Optional<ButtonType> response = executeTask(alert::showAndWait);
                        path = (response.isPresent() && response.get() == options[0])? path: null;
                    }
                    if (path == null)
                        path = java.util.Objects.requireNonNull(chooseDirectory(prompt));
                    break;
                case DELETE:
                    ButtonType[] options = {new ButtonType(resource.getString("fileAction.delete.action"), ButtonData.OK_DONE), ButtonType.CANCEL};
                    javafx.scene.control.Alert alert = FileManager.createAlert(prompt, javafx.scene.control.Alert.AlertType.CONFIRMATION, options);
                    java.util.Optional<ButtonType> response = executeTask(alert::showAndWait);
                    if (!response.isPresent() || response.get() != options[0])
                        throw new NullPointerException();
                    break;
                default:
                    throw new IllegalArgumentException("File action not yet defined!");
            }
            FileManager fm = (manager != null)? manager: new FileManager(layout, this);
            fm.performFileAction(action, path, collection, callback);
            return fm;
        }
        catch (NullPointerException ex) {
            return manager;
        }
    }

    private void disposeFormerItems(Predicate<TreeItem<Path>> pred) {
        List<TreeItem<Path>> list = new ArrayList<>(tree.getRoot().getChildren());
        list.add(tree.getRoot());
        if (pred == null) {
            checkedItems.clear();
            expandedItems.retainAll(list);
            return;
        }
        checkedItems.removeAll(FileMedia.enlist(checkedItems, pred));
        expandedItems.removeAll(FileMedia.enlist(expandedItems, pred.and(((item) -> !list.contains(item)))));
    }

    static String getName(TreeItem<Path> item) {
        try {
            Catalogue cat = (Catalogue) item;
            return cat.getName();
        }
        catch (Exception ex) {
            return resolvePathName(item.getValue());
        }
    }

    static Path getPathChain(TreeItem<Path> item) {
        try {
            return ((Catalogue)item).getPathChain();
        }
        catch (Exception ex) {
            return Paths.get("");
        }
    }

    private LinkedList<File> getPlaylist(TreeItem<Path> item) {
        try {
            Catalogue cat = (Catalogue) item;
            return cat.getPlaylist();
        }
        catch (Exception ex) {
            return new LinkedList<>();
        }
    }

    private LinkedList<File> getAllPlaylists(TreeItem<Path> item) {
        try {
            Catalogue cat = (Catalogue) item;
            return cat.getAllPlaylists();
        }
        catch (ClassCastException ex) {
            return getAllPlaylists(tree.getRoot().getChildren().get(0));
        }
        catch (Exception ex) {
            return new LinkedList<>();
        }
    }

    private void playPlaylist(TreeItem<Path> item) {
        if (!(item instanceof TreeItem))
            return;
        Catalogue cat = (Catalogue) ((item instanceof Catalogue)? item: tree.getRoot().getChildren().get(0));
        if (!item.isLeaf())
            showTempPopup(resource.getAndFormatMessage("playPlaylist.load.popup", resolvePathName(item.getValue())));
        FileMedia.initiateBackgroundTask(cat::playPlaylist);
    }

    public static String getFileName(Path path) {
        try {
            return path.getFileName().toString();
        }
        catch (Exception ex) {
            String name = (path == null)? "": fileSystemView.getSystemTypeDescription(path.toFile());
            name = (name == null)? "": name;
            if (path != null)
                return String.format("%s (%s)", name, path.toString().replace(File.separator, ""));
            return resource.getString("anonymous.string");
        }
    }

    public static boolean isValidPath(Path path) {
        String pathName = getFileName(path);
        return !pathName.endsWith(PATH_EXTENSION);
    }

    public static String resolvePathName(Path path, boolean getFileName) {
        String name = (getFileName)? getFileName(path): path.toString();
        return resolvePathName(name, PATH_EXTENSION);
    }

    public static String resolvePathName(Path path) {
        return resolvePathName(path, true);
    }

    public static String resolvePathName(String name, String extension) {
        if (!name.endsWith(extension))
            return name;
        return name.substring(0, name.indexOf(extension));
    }

    public static TreeItem<Path> getParent(TreeItem<Path> child, int index) {
        try {
            TreeItem<Path> parent = child;
            List<TreeItem<Path>> parents = new ArrayList<>();
            while ((parent = parent.getParent()) != null)
                parents.add(0, parent);
            return parents.get(index);
        }
        catch (Exception ex) {
            return null;
        }
    }

    private class MediaLibraryCell extends javafx.scene.control.cell.CheckBoxTreeCell<Path> {
        @Override
        public void updateItem(Path path, boolean empty) {
            super.updateItem(path, empty);
            if (empty) {
                setText(null);
            }
            else {
                if (path != null)
                    setText(resolvePathName(path));
            }
        }
    }

}