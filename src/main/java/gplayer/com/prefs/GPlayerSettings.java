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

package gplayer.com.prefs;

import gplayer.com.exec.GPlayer;
import gplayer.com.lang.BaseResourcePacket;
import gplayer.com.util.Duo;
import gplayer.com.util.Utility;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**Coordinates user preferences
*and saves them to user OS registry for future reference
*@author Ganiyu Emilandu
*/

public class GPlayerSettings extends gplayer.com.util.PopupStage {
    private static BaseResourcePacket resource = BaseResourcePacket.getPacket("GPlayerSettingsResource");
    private Logger LOGGER;
    private TabPane pane;
    private Button applyButton;  //To effect changes
    private Button defaultButton = createButton("defaultButton");  //To restore default settings
    private Map<String, DataProcessor> map = new HashMap<>();
    private Map<Integer, Duo<Map<String, DataProcessor>, Map<String, DataProcessor>>> defaultMaps = new HashMap<>();
    private Map<String, DataProcessor> defaultMap, defaultMapClone;
    private static boolean isShowing = false;
    private boolean withinTab = true;
    private int selectedTabIndex, tabIndex;
    private Pane contentLayout;
    private static final ObservableList<String> jumpTimeOptions = FXCollections.observableArrayList(Utility.generateMinutesStringTimeFormat(0, 10, 5, 10, 15, 20, 30, 45));
    private static final ObservableList<String> recallTimeOptions = FXCollections.observableArrayList(Utility.generateMinutesStringTimeFormat(0, 10, 1, 2, 3, 5, 10, 15, 20, 30, 45));
    private static final ObservableList<String> notificationTimeOptions = getNotificationTimeOptions();
    private static final ObservableList<String> markedPositionTimeOptions = FXCollections.observableArrayList(Utility.generateMinutesStringTimeFormat(0, 1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).subList(0, 10));
    public static final ObservableList<String> exitTimeOptions = FXCollections.observableArrayList(Utility.generateHoursStringTimeFormat(1, 10, 5, 10, 15, 20, 30, 45));
    public static final String[] fileGroup = resource.getStringArray("fileGroup.array"), searchParameters = resource.getStringArray("searchParameters.array");
    public static final String[] recentPlayListOptions = resource.getStringArray("recentPlayListOptions.array"), mostPlayedPlayListOptions = resource.getStringArray("mostPlayedPlayListOptions.array");
    public static final String GENERAL_PREFERENCES_PATH = GPlayer.PREFS_ROOT.concat("/settings/general_preferences");
    public static final String PLAYBACK_PREFERENCES_PATH = GPlayer.PREFS_ROOT.concat("/settings/playback_preferences");
    public static final String PLAYLIST_PREFERENCES_PATH = GPlayer.PREFS_ROOT.concat("/settings/playlist_preferences");
    public static final String VIEW_PREFERENCES_PATH = GPlayer.PREFS_ROOT.concat("/settings/view_preferences");
    public static final String PROGRAM_PREFERENCES_PATH = GPlayer.PREFS_ROOT.concat("/settings/program_preferences");
    public static final GPlayerPreferences GENERAL_PREFERENCES = userPreferences(GENERAL_PREFERENCES_PATH), PLAYBACK_PREFERENCES = userPreferences(PLAYBACK_PREFERENCES_PATH), PLAYLIST_PREFERENCES = userPreferences(PLAYLIST_PREFERENCES_PATH), VIEW_PREFERENCES = userPreferences(VIEW_PREFERENCES_PATH), PROGRAM_PREFERENCES = userPreferences(PROGRAM_PREFERENCES_PATH);

    /**constructor
    *@param primaryStage
    *program root stage
    *@param b
    *flags true if media was playing prior to invokation
    *@throws NullPointerException
    *if primaryStage is null
    */
    public GPlayerSettings(javafx.stage.Stage primaryStage, boolean b) {
        super(primaryStage, new javafx.scene.layout.VBox(10), resource.getString("stage.title"));
        resumePlay = b;
        hideOnEscape = false;
        initialize();
        showPopupStage();
    }

    @Override
    protected void initialize() {
        if (LOGGER == null)
            LOGGER = GPlayer.createLogger(GPlayerSettings.class);
        LOGGER.config("Initializing settings stage");
        super.initialize();
    }

    @Override
    protected void populateSceneRoot() {
        final TabPane pane = new TabPane();
        javafx.scene.layout.VBox root = (javafx.scene.layout.VBox) sceneRoot;
        root.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.layout.VBox.setVgrow(pane, javafx.scene.layout.Priority.ALWAYS);
        //Add tabs to the pane
        pane.getTabs().addAll(generalTab(), playBackTab(), playListTab(), viewTab(), programTab());
        this.pane = pane;
        pane.getSelectionModel().selectedIndexProperty().addListener((value, oldValue, newValue) -> {
            defaultMap = defaultMaps.get(newValue).getKey();
            defaultMapClone = defaultMaps.get(newValue).getValue();
            defaultButton.setDisable(defaultMap.isEmpty());
            contentLayout = (Pane) this.pane.getTabs().get(newValue.intValue()).getContent();
            Platform.runLater(() -> {
                if (withinTab)
                    focusContent(contentLayout.getChildren());
            });
        });
        //Create a ButtonBar object
        ButtonBar bar = new ButtonBar();
        final Button applyButton = createButton("applyButton");  //To save changes made
        ButtonBar.setButtonData(applyButton, ButtonData.APPLY);
        this.applyButton = applyButton;
        //Disable on start-up
        applyButton.setDisable(true);
        applyButton.setOnAction((ae) -> {
            final Map<String, DataProcessor> m = map;
            //Save the changes made on a background thread
            new Thread(() -> applyChanges(m)).start();
            //Instantiate a new Map object to track further changes
            map = new HashMap<>();
        });
        ButtonBar.setButtonData(defaultButton, ButtonData.APPLY);
        defaultButton.setDisable(defaultMap.isEmpty());
        defaultButton.setOnAction((ae) -> applyChanges(defaultMap));
        final Button cancelButton = createButton("cancelButton");  //Closes settings stage
        ButtonBar.setButtonData(cancelButton, ButtonData.CANCEL_CLOSE);
        cancelButton.setCancelButton(true);  //Allows the cancel button to be fired on Escape-key press, regardless of focus
        cancelButton.setOnAction((ae) -> {
            if (!applyButton.isDisable()) {  //If there are changes yet to be saved
                //Prompt the user to decide what to do with the changes, (save or discard)
                javafx.scene.control.Alert alert = gplayer.com.service.library.FileManager.createAlert(resource.getString("cancelButtonDialog.message"), javafx.scene.control.Alert.AlertType.INFORMATION, javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
                //Save changes on yes-button press
                //and discard otherwise
                alert.showAndWait().filter((response) -> response == javafx.scene.control.ButtonType.YES).ifPresent((response) -> applyButton.fire());
            }
            //Close settings stage
            popupStage.close();
        });
        final Button okButton = createButton("okButton");  //Closes the settings stage, saving changes, if available
        ButtonBar.setButtonData(okButton, ButtonData.OK_DONE);
        okButton.setDefaultButton(true);  //Allows the okButton to be activated on enter-key press, regardless of focus
        okButton.setOnAction((ae) -> {
            if (!applyButton.isDisable())
                //Initiate the process of saving changes made
                applyButton.fire();
            //Have settings stage closed
            popupStage.hide();
        });
        bar.getButtons().addAll(applyButton, okButton, cancelButton, defaultButton);
        //Register bar contents to recieve key events
        bar.getButtons().forEach((button) -> addListenerTo(button));
        //Place the buttons in this order: applyButton, okButton and cancelButton
        bar.setButtonOrder("+AOCI");
        root.getChildren().addAll(pane, bar);
        popupStage.addEventFilter(javafx.scene.input.KeyEvent.ANY, (ke) -> {
            if (ke.getEventType() == javafx.scene.input.KeyEvent.KEY_RELEASED)
                makeTargetInquiry(ke);
            if (ke.getEventType() == javafx.scene.input.KeyEvent.KEY_TYPED) {
                try {
                    int num = Integer.parseInt(ke.getCharacter());
                    if (num > 0 && num <= pane.getTabs().size())
                        pane.getSelectionModel().select(--num);
                }
                catch (Exception ex) {}
            }
        });
        popupStage.addEventHandler(Tab.SELECTION_CHANGED_EVENT, (sce) -> System.out.println(sce.getTarget()));
    }

    @Override
    protected void showPopupStage() {
        popupStage.show();
        focusContent(contentLayout.getChildren());
        LOGGER.info("Showing settings stage");
    }

    @Override
    protected void closeAction() {
        super.closeAction();
        LOGGER.info("Closing settings stage");
        //Release all used resources
        GPlayer.releaseLogResourceFor(LOGGER);
    }

    private void makeTargetInquiry(javafx.scene.input.InputEvent event) {
        if (gplayer.com.service.FileMedia.keyMatch((javafx.scene.input.KeyEvent) event, javafx.scene.input.KeyCombination.SHIFT_DOWN, javafx.scene.input.KeyCode.TAB))
            assertWithinTab(event);
    }

    private void assertWithinTab(javafx.scene.input.InputEvent event) {
        withinTab = (gplayer.com.service.FileMedia.getTraversableChildren(contentLayout.getChildren()).indexOf((javafx.scene.Node) event.getTarget()) == -1)? false: true;
    }

    private void focusContent(ObservableList<javafx.scene.Node> list) {
        for (javafx.scene.Node item: list) {
            javafx.scene.Node node = null;
            if (item instanceof Pane || item instanceof javafx.scene.Group) {
                java.util.List<javafx.scene.Node> nodes = null;
                if (item instanceof Pane)
                    nodes = gplayer.com.service.FileMedia.getTraversableChildren(((Pane)item).getChildren());
                else
                    nodes = gplayer.com.service.FileMedia.getTraversableChildren(((javafx.scene.Group)item).getChildren());
                if (nodes != null && !nodes.isEmpty())
                    node = nodes.get(0);
            }
            else if (item.isFocusTraversable())
                node = item;
            if (node != null) {
                javafx.scene.Node traversableNode = node;
                //Pause for a fifth of a second before requesting focus
                javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
                pt.setOnFinished((e) -> traversableNode.requestFocus());
                pt.play();
                break;
            }
        }
    }

    private void addListenerTo(javafx.scene.Node button) {
        button.setOnKeyPressed((ke) -> {
            int index = -2;
            if (gplayer.com.service.FileMedia.keyMatch(ke, false, KeyCombination.SHORTCUT_DOWN, KeyCode.TAB, KeyCode.PAGE_DOWN)) {
                index = pane.getSelectionModel().getSelectedIndex() + 1;
                if (index >= pane.getTabs().size())
                    index = 0;
            }
            else if (gplayer.com.service.FileMedia.keyMatch(ke, KeyCode.TAB, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN) || gplayer.com.service.FileMedia.keyMatch(ke, false, KeyCombination.SHORTCUT_DOWN, KeyCode.PAGE_UP)) {
                index = pane.getSelectionModel().getSelectedIndex() - 1;
                if (index < 0)
                    index = pane.getTabs().size()-1;
            }
            if (index != -2) {
                withinTab = true;
                pane.getSelectionModel().select(index);
            }
        });
        button.setOnMouseClicked((me) -> withinTab = true);
    }

    private void applyChanges(Map<String, DataProcessor> map) {
        boolean isDefault = map == defaultMap;
        //Obtain a set of the keys in map
        Set<String> keys = map.keySet();
        for (String key: keys) {
            DataProcessor value = map.get(key);
            value.processData();
            if (value.getKey().equals("implicitExit"))
                Platform.runLater(() -> Platform.setImplicitExit(!(boolean)value.getValue()));
            if (!isDefault && defaultMapClone.containsKey(key)) {
                DataProcessor m = defaultMapClone.get(key);
                if (value.equals(m))
                    defaultMap.remove(key);
                else
                    defaultMap.put(key, m);
            }
        }
        if (isDefault)
            map.clear();
        defaultButton.setDisable(defaultMap.isEmpty());
        applyButton.setDisable(true);
    }

    private void discardChanges(Map<String, DataProcessor> m) {
        m.keySet().forEach((key) -> map.remove(key));
        defaultButton.setDisable(defaultMap.isEmpty());
        applyButton.setDisable(map.isEmpty());
    }

    private static GPlayerPreferences userPreferences(String path) {
        GPlayerPreferences gp = new GPlayerPreferences();
        gp.setUserPreferences(path);
        return gp;
    }

    @SuppressWarnings("unchecked")
    private <T> void managePreferences(Map<String, DataProcessor> defaultMap, Map<String, DataProcessor> defaultMapClone, GPlayerPreferences gp, String key, ComboBox<T> cb, T defaultValue) {
        try {
            final String keyPath = gp.userPath + key;
            Class<?> myClass = Class.forName("java.util.prefs.Preferences");
            java.lang.reflect.Method method = null;
            if (defaultValue instanceof String) {
                method = myClass.getDeclaredMethod("get", String.class, String.class);
            }
            else if (defaultValue instanceof Double) {
                cb.setCellFactory((javafx.scene.control.ListView<T> l) -> new NumberLocalizer());
                method = myClass.getDeclaredMethod("getDouble", String.class, double.class);
            }
            final java.lang.reflect.Method m = method;
            T object = (T) method.invoke(gp.getUserPreferences(), key, defaultValue);
            if (object != null) {
                if (object != defaultValue && !cb.getItems().contains(object)) {
                    object = defaultValue;
                    gp.getUserPreferences().remove(key);
                }
                cb.getSelectionModel().select(object);
            }
            if (!object.equals(defaultValue))
                defaultMap.put(keyPath, new DataProcessor(gp, key, defaultValue));
            defaultMapClone.put(keyPath, new DataProcessor(gp, key, defaultValue));
            cb.getSelectionModel().selectedItemProperty().addListener((val, oldVal, newVal) -> {
                try {
                    T v = (T) m.invoke(gp.getUserPreferences(), key, defaultValue);
                    if (v != null) {
                        if (!v.equals(newVal))
                            map.put(keyPath, new DataProcessor(gp, key, newVal));
                        if (v.equals(newVal))
                            map.remove(keyPath);
                    }
                    else
                        map.put(keyPath, new DataProcessor(gp, key, newVal));
                    applyButton.setDisable(map.isEmpty());
                }
                catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "ComboBox selection listener triggered a method reflection exception", ex);
                }
            });
            defaultButton.disableProperty().addListener((listener) -> {
                if (popupStage != null && popupStage.isShowing()) {
                    if (defaultButton.isDisable() && this.defaultMap == defaultMap && !cb.getValue().equals(defaultValue)) {
                        cb.setValue(defaultValue);
                        discardChanges(defaultMapClone);
                    }
                }
            });
        }
        catch (Exception ex) {
            LOGGER.log(Level.WARNING, "A method reflection exception was thrown", ex);
        }
    }

    private void managePreferences(Map<String, DataProcessor> defaultMap, Map<String, DataProcessor> defaultMapClone, GPlayerPreferences gp, String key, Slider slider, double defaultValue) {
        slider.setMinWidth(100);
        slider.setPrefWidth(300);
        final String keyPath = gp.userPath + key;
        double v = gp.getUserPreferences().getDouble(key, defaultValue);
        slider.setValue(v);
        if (v != defaultValue)
            defaultMap.put(keyPath, new DataProcessor(gp, key, defaultValue));
        GPlayer.getSceneRoot().armSlider(slider, resource.getString("slider.tooltip"), resource.getString("slider.popup"), 1, 3);
        slider.valueProperty().addListener((val, oldVal, newVal) -> {
            double value = gp.getUserPreferences().getDouble(key, defaultValue);
            if (value != (double)newVal)
                map.put(keyPath, new DataProcessor(gp, key, (Double)newVal));
            if (value != -1.0 && value == (double)newVal)
                map.remove(keyPath);
            applyButton.setDisable(map.isEmpty());
        });
        defaultButton.disableProperty().addListener((listener) -> {
            if (popupStage != null && popupStage.isShowing()) {
                if (defaultButton.isDisable() && this.defaultMap == defaultMap && slider.getValue() != defaultValue) {
                    slider.setValue(defaultValue);
                    discardChanges(defaultMapClone);
                }
            }
        });
    }

    private void managePreferences(Map<String, DataProcessor> defaultMap, Map<String, DataProcessor> defaultMapClone, GPlayerPreferences gp, String key, CheckBox cb, boolean defaultValue) {
        final String keyPath = gp.userPath + key;
        boolean value = gp.getUserPreferences().getBoolean(key, defaultValue);
        cb.setSelected(value);
        if (value != defaultValue)
            defaultMap.put(keyPath, new DataProcessor(gp, key, defaultValue));
        defaultMapClone.put(keyPath, new DataProcessor(gp, key, defaultValue));
        cb.selectedProperty().addListener((val, oldVal, newVal) -> {
            boolean v = gp.getUserPreferences().getBoolean(key, defaultValue);
            if (v != newVal)
                map.put(keyPath, new DataProcessor(gp, key, newVal));
            if (v == newVal)
                map.remove(keyPath);
            applyButton.setDisable(map.isEmpty());
        });
        defaultButton.disableProperty().addListener((listener) -> {
            if (popupStage != null && popupStage.isShowing()) {
                if (defaultButton.isDisable() && this.defaultMap == defaultMap && cb.isSelected() != defaultValue) {
                    cb.setSelected(defaultValue);
                    discardChanges(defaultMapClone);
                }
            }
        });
    }

    private void bindPreferences(GPlayerPreferences gp, Label label, CheckBox checkBox, ComboBox<String> cb, String key, String defaultValue, String... texts) {
        cb.setDisable(!checkBox.isSelected());
        javafx.beans.property.BooleanProperty bp = new javafx.beans.property.SimpleBooleanProperty();
        bp.booleanProperty(checkBox.selectedProperty()).addListener((listener) -> {
            cb.setDisable(!checkBox.isSelected());
            if (cb.isDisable())
                cb.setValue(gp.getUserPreferences().get(key, defaultValue));
            if (texts.length > 0)
                label.setText(Utility.nextItem(label.getText(), texts));
        });
    }

    private Button createButton(String key) {
        String prefix = key + '.';
        String text = resource.getString(prefix + "text");  //The text to be displayed on the button
        String tooltip = resource.getString(prefix + "tooltip");  //The text shown when the mouse is hovered over the button
        Button button = new Button(text);
        button.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return button;
    }

    public static Label createLabel(javafx.scene.Node node, String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.setWrapText(true);
        label.setPrefWidth(360);
        if (node != null)
            label.setLabelFor(node);
        return label;
    }

    public static Label createLabel(String text) {
        return createLabel(null, text);
    }

    public static GridPane createPane() {
        GridPane gridPane = new GridPane();
        gridPane.setVgap(6);
        gridPane.setHgap(6);
        gridPane.setPadding(new javafx.geometry.Insets(5, 5, 5, 5));
        return gridPane;
    }

    public static GridPane createPane(int columns) {
        return createPane(columns, 50);
    }

    public static GridPane createPane(int columns, double percentage) {
        GridPane pane = createPane();
        for (int i = 0; i < columns; i++) {
            javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
            column.setPercentWidth(percentage);
            pane.getColumnConstraints().add(column);
        }
        return pane;
    }

    private HBox gridLayout(javafx.scene.Node... nodes) {
        return gridLayout(1, nodes);
    }

    private HBox gridLayout(int spacing, javafx.scene.Node... nodes) {
        HBox layout = new HBox(spacing);
        layout.setAlignment(javafx.geometry.Pos.CENTER);
        if (nodes.length > 0)
            layout.getChildren().addAll(nodes);
        return layout;
    }

    private Tab createTab(String text, GridPane gridPane) {
        Tab tab = new Tab(text, gridPane);
        return tab;
    }

    private Tab generalTab() {
        LOGGER.config("Initializing contents of the general tab");
        Map<String, DataProcessor> defaultMap = new HashMap<>(), defaultMapClone = new HashMap<>();
        defaultMaps.put(0, new Duo<Map<String, DataProcessor>, Map<String, DataProcessor>>(defaultMap, defaultMapClone));
        this.defaultMap = defaultMap;
        this.defaultMapClone = defaultMapClone;
        GPlayerPreferences prefs = GENERAL_PREFERENCES;
        GridPane gridPane = createPane(2);
        contentLayout = gridPane;
        String[] texts = resource.getStringFamily("generalTab.prompt");
        CheckBox checkBox = new CheckBox(texts[0]);
        managePreferences(defaultMap, defaultMapClone, prefs, "wrapNext", checkBox, true);
        gridPane.add(gridLayout(checkBox), 0, 0);
        checkBox = new CheckBox(texts[1]);
        managePreferences(defaultMap, defaultMapClone, prefs, "wrapPrevious", checkBox, true);
        gridPane.add(gridLayout(checkBox), 1, 0);
        HBox layout = gridLayout(20);
        checkBox = new CheckBox(texts[2]);
        managePreferences(defaultMap, defaultMapClone, prefs, "mediaMark", checkBox, true);
        layout.getChildren().add(gridLayout(checkBox));
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(fileGroup));
        Label label = createLabel(cb, texts[3]);
        managePreferences(defaultMap, defaultMapClone, prefs, "mediaMarkFiles", cb, fileGroup[1]);
        bindPreferences(prefs, label, checkBox, cb, "mediaMarkFiles", fileGroup[1]);
        layout.getChildren().add(gridLayout(label, cb));
        cb = new ComboBox<>(recallTimeOptions);
        label = createLabel(cb, texts[4]);
        managePreferences(defaultMap, defaultMapClone, prefs, "mediaMarkTime", cb, Utility.formatTime(1, 1));
        bindPreferences(prefs, label, checkBox, cb, "mediaMarkTime", Utility.formatTime(1, 1));
        layout.getChildren().add(gridLayout(label, cb));
        gridPane.add(layout, 0, 1, 2, 1);
        layout = gridLayout(20);
        checkBox = new CheckBox(texts[5]);
        managePreferences(defaultMap, defaultMapClone, prefs, "showMediaMarkDialog", checkBox, true);
        layout.getChildren().add(gridLayout(checkBox));
        cb = new ComboBox<>(markedPositionTimeOptions);
        label = createLabel(cb, texts[6]);
        managePreferences(defaultMap, defaultMapClone, prefs, "previousMarkedTime", cb, Utility.formatTime(3, 2));
        layout.getChildren().add(gridLayout(label, cb));
        gridPane.add(layout, 0, 2, 2, 1);
        Tab tab = createTab(resource.getString("generalTab.name"), gridPane);
        LOGGER.info("Done initializing contents of the general tab");
        return tab;
    }

    private Tab playBackTab() {
        LOGGER.config("Initializing contents of the playback tab");
        Map<String, DataProcessor> defaultMap = new HashMap<>(), defaultMapClone = new HashMap<>();
        defaultMaps.put(1, new Duo<Map<String, DataProcessor>, Map<String, DataProcessor>>(defaultMap, defaultMapClone));
        GPlayerPreferences prefs = PLAYBACK_PREFERENCES;
        GridPane gridPane = createPane(2);
        String[] texts = resource.getStringFamily("playBackTab.prompt");
        HBox layout = gridLayout(12);
        ComboBox<Double> cd = new ComboBox<>(FXCollections.observableArrayList(1.0, 1.1, 1.2, 1.3, 1.4, 1.5));
        Label label = createLabel(cd, texts[0]);
        managePreferences(defaultMap, defaultMapClone, prefs, "speedRate", cd, 1.0);
        layout.getChildren().add(gridLayout(label, cd));
        cd = new ComboBox<>(FXCollections.observableArrayList(0.05, 0.5, 1.0, 1.05, 1.5, 2.0));
        label = createLabel(cd, texts[1]);
        managePreferences(defaultMap, defaultMapClone, prefs, "speedStepLevel", cd, 0.05);
        layout.getChildren().add(gridLayout(label, cd));
        cd = new ComboBox<>(FXCollections.observableArrayList(0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0));
        label = createLabel(cd, texts[2]);
        managePreferences(defaultMap, defaultMapClone, prefs, "speedLowestValue", cd, 0.8);
        layout.getChildren().add(gridLayout(label, cd));
        cd = new ComboBox<>(FXCollections.observableArrayList(1.5, 2.0, 2.5, 3.0));
        label = createLabel(cd, texts[3]);
        managePreferences(defaultMap, defaultMapClone, prefs, "speedHighestValue", cd, 1.5);
        layout.getChildren().add(gridLayout(label, cd));
        CheckBox checkBox = new CheckBox(texts[4]);
        managePreferences(defaultMap, defaultMapClone, prefs, "revertSpeedRate", checkBox, false);
        layout.getChildren().add(gridLayout(checkBox));
        gridPane.add(layout, 0, 0, 2, 1);
        Slider slider = new Slider(0.0, 100.0, 5.0);
        label = createLabel(slider, texts[5]);
        managePreferences(defaultMap, defaultMapClone, prefs, "timeStepLevel", slider, 5.0);
        gridPane.add(gridLayout(label, slider), 0, 1);
        slider = new Slider(0.0, 100.0, 1.0);
        label = createLabel(slider, texts[6]);
        managePreferences(defaultMap, defaultMapClone, prefs, "volumeStepLevel", slider, 3.0);
        gridPane.add(gridLayout(label, slider), 1, 1);
        ComboBox<String> cb = new ComboBox<String>(jumpTimeOptions);
        label = createLabel(cb, texts[7]);
        managePreferences(defaultMap, defaultMapClone, prefs, "forwardJumpTime", cb, Utility.formatTime(20, 2));
        gridPane.add(gridLayout(label, cb), 0, 2);
        cb = new ComboBox<>(jumpTimeOptions);
        label = createLabel(cb, texts[8]);
        managePreferences(defaultMap, defaultMapClone, prefs, "backwardJumpTime", cb, Utility.formatTime(10, 2));
        gridPane.add(gridLayout(label, cb), 1, 2);
        Tab tab = createTab(resource.getString("playBackTab.name"), gridPane);
        LOGGER.info("Done initializing contents of the playback tab");
        return tab;
    }

    private Tab playListTab() {
        LOGGER.config("Initializing contents of the playlist tab");
        Map<String, DataProcessor> defaultMap = new HashMap<>(), defaultMapClone = new HashMap<>();
        defaultMaps.put(2, new Duo<Map<String, DataProcessor>, Map<String, DataProcessor>>(defaultMap, defaultMapClone));
        GPlayerPreferences prefs = PLAYLIST_PREFERENCES;
        GridPane gridPane = createPane(2);
        String[] texts = resource.getStringFamily("playListTab.prompt");
        ObservableList<String> options = GPlayer.getSceneRoot().getPlayListNames();
        ComboBox<String> cb = new ComboBox<>(options);
        Label label = createLabel(cb, texts[0]);
        managePreferences(defaultMap, defaultMapClone, prefs, "playList", cb, gplayer.com.service.FileMedia.DEFAULT_PLAYLIST_NAMES.get(4));
        gridPane.add(gridLayout(label, cb), 0, 0);
        cb = new ComboBox<>(FXCollections.observableArrayList(searchParameters));
        label = createLabel(cb, texts[1]);
        managePreferences(defaultMap, defaultMapClone, prefs, "searchParameter", cb, searchParameters[0]);
        gridPane.add(gridLayout(label, cb), 1, 0);
        options = FXCollections.observableArrayList(recentPlayListOptions);
        cb = new ComboBox<>(options);
        label = createLabel(cb, texts[2]);
        managePreferences(defaultMap, defaultMapClone, prefs, "recentPlayList", cb, options.get(0));
        gridPane.add(gridLayout(label, cb), 0, 1);
        options = FXCollections.observableArrayList(mostPlayedPlayListOptions);
        cb = new ComboBox<String>(options);
        label = createLabel(cb, texts[3]);
        managePreferences(defaultMap, defaultMapClone, prefs, "mostPlayedPlayList", cb, options.get(0));
        gridPane.add(gridLayout(label, cb), 1, 1);
        CheckBox checkBox = new CheckBox(texts[4]);
        managePreferences(defaultMap, defaultMapClone, prefs, "unavailableFiles", checkBox, true);
        gridPane.add(gridLayout(checkBox), 0, 2);
        checkBox = new CheckBox(texts[5]);
        managePreferences(defaultMap, defaultMapClone, prefs, "unplayableFiles", checkBox, true);
        gridPane.add(gridLayout(checkBox), 1, 2);
        Tab tab = createTab(resource.getString("playListTab.name"), gridPane);
        LOGGER.info("Done initializing contents of the playlist tab");
        return tab;
    }

    private Tab viewTab() {
        LOGGER.config("Initializing contents of the view tab");
        Map<String, DataProcessor> defaultMap = new HashMap<>(), defaultMapClone = new HashMap<>();
        defaultMaps.put(3, new Duo<Map<String, DataProcessor>, Map<String, DataProcessor>>(defaultMap, defaultMapClone));
        GPlayerPreferences prefs = VIEW_PREFERENCES;
        GridPane gridPane = createPane(2);
        String[] texts = resource.getStringFamily("viewTab.prompt");
        CheckBox checkBox = new CheckBox(texts[0]);
        managePreferences(defaultMap, defaultMapClone, prefs, "displayLibrary", checkBox, false);
        gridPane.add(gridLayout(checkBox), 0, 0, 2, 1);
        checkBox = new CheckBox(texts[1]);
        managePreferences(defaultMap, defaultMapClone, prefs, "windowViewOnly", checkBox, false);
        gridPane.add(gridLayout(checkBox), 0, 1);
        checkBox = new CheckBox(texts[2]);
        managePreferences(defaultMap, defaultMapClone, prefs, "stageViewOnly", checkBox, true);
        gridPane.add(gridLayout(checkBox), 1, 1);
        HBox layout = gridLayout(10);
        ComboBox<String> cb = new ComboBox<>(notificationTimeOptions);
        Label label = createLabel(cb, texts[3]);
        managePreferences(defaultMap, defaultMapClone, prefs, "notificationTime", cb, notificationTimeOptions.get(2));
        layout.getChildren().add(gridLayout(label, cb));
        checkBox = new CheckBox(texts[4]);
        managePreferences(defaultMap, defaultMapClone, prefs, "executeNotificationTask", checkBox, true);
        layout.getChildren().add(gridLayout(checkBox));
        gridPane.add(layout, 0, 2, 2, 1);
        Tab tab = createTab(resource.getString("viewTab.name"), gridPane);
        LOGGER.info("Done initializing contents of the view tab");
        return tab;
    }

    private Tab programTab() {
        LOGGER.config("Initializing contents of the program tab");
        Map<String, DataProcessor> defaultMap = new HashMap<>(), defaultMapClone = new HashMap<>();
        defaultMaps.put(4, new Duo<Map<String, DataProcessor>, Map<String, DataProcessor>>(defaultMap, defaultMapClone));
        GPlayerPreferences prefs = PROGRAM_PREFERENCES;
        GridPane gridPane = createPane(2);
        String[] texts = resource.getStringFamily("programTab.prompt");
        CheckBox checkBox = new CheckBox(texts[0]);
        managePreferences(defaultMap, defaultMapClone, prefs, "implicitExit", checkBox, true);
        gridPane.add(gridLayout(checkBox), 0, 0, 2, 1);
        HBox layout = gridLayout(12);
        checkBox = new CheckBox(texts[1]);
        managePreferences(defaultMap, defaultMapClone, prefs, "enableAppTimeout", checkBox, false);
        layout.getChildren().add(gridLayout(checkBox));
        ComboBox<String> cb = new ComboBox<>(exitTimeOptions);
        Label label = createLabel(cb, texts[2]);
        managePreferences(defaultMap, defaultMapClone, prefs, "appTimeout", cb, Utility.formatTime(3, 0));
        bindPreferences(prefs, label, checkBox, cb, "appTimeout", Utility.formatTime(3, 0));
        layout.getChildren().add(gridLayout(label, cb));
        gridPane.add(layout, 0, 1, 2, 1);
        checkBox = new CheckBox(texts[3]);
        managePreferences(defaultMap, defaultMapClone, prefs, "autoUpdateCheck", checkBox, true);
        gridPane.add(gridLayout(checkBox), 0, 2, 2, 1);
        Tab tab = createTab(resource.getString("programTab.name"), gridPane);
        LOGGER.info("Done initializing contents of the program tab");
        return tab;
    }

    /**Obtains the registry path to where user general preferences are stored, (GENERAL_PREFERENCES).
    *@return a java.util.prefs.Preferences node pointing to that path
    */
    public static Preferences generalPrefs() {
        return GENERAL_PREFERENCES.getUserPreferences();
    }

    /**Obtains the registry path to where user playback preferences are stored, (PLAYBACK_PREFERENCES).
    *@return a java.util.prefs.Preferences node pointing to that path
    */
    public static Preferences playBackPrefs() {
        return PLAYBACK_PREFERENCES.getUserPreferences();
    }

    /**Obtains the registry path to where user playlist preferences are stored, (PLAYLIST_PREFERENCES).
    *@return a java.util.prefs.Preferences node pointing to that path
    */
    public static Preferences playListPrefs() {
        return PLAYLIST_PREFERENCES.getUserPreferences();
    }

    /**Obtains the registry path to where user view preferences are stored, (VIEW_PREFERENCES).
    *@return a java.util.prefs.Preferences node pointing to that path
    */
    public static Preferences viewPrefs() {
        return VIEW_PREFERENCES.getUserPreferences();
    }

    /**Obtains the registry path to where user program preferences are stored, (PROGRAM_PREFERENCES).
    *@return a java.util.prefs.Preferences node pointing to that path
    */
    public static Preferences programPrefs() {
        return PROGRAM_PREFERENCES.getUserPreferences();
    }

    private static ObservableList<String> getNotificationTimeOptions() {
        ObservableList<String> obs = FXCollections.observableArrayList(Utility.generateMinutesStringTimeFormat(0, 1, 2, 3, 5, 10, 15, 20, 30, 45));
        obs.addAll(Utility.formatTime(2, 1), Utility.formatTime(3, 1), Utility.formatTime(5, 1), Utility.formatTime(10, 1), Utility.formatTime(15, 1));
        return obs;
    }

    public static class NumberLocalizer<T extends Number> extends javafx.scene.control.ListCell<T> {
        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
            }
            else {
                if (item != null)
                    setText(resource.localizeNumber(item));
            }
        }
    }

}