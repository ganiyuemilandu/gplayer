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

package gplayer.com.util;

import gplayer.com.exec.GPlayer;
import gplayer.com.service.FileMedia;
import java.net.URL;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**Facilitates the creation of a stage object
*that can be displayed on another stage object as a Popup,
*or displayed on its own.
*Note:
*most of the implementations here puts GPlayer app window into high consideration.
*So for optimal performance in other app windows,
*endeavour to override some of the methods.
*/

public abstract class PopupStage {
    protected final Stage popupStage = new Stage();  //The child window
    private Stage primaryStage;  //Parent window
    private String popupStageTitle;  //child window title
    private Scene scene;  //child window scene
    protected boolean resumePlay;  //Asserts play resumption of paused media (GPlayer app)
    protected Parent sceneRoot;  //Root of child window scene
    protected URL cssURL;  //Path to css file to apply as style
    protected boolean hideOnEscape = true;  //Asserts closing of child window on escape-key press

    /**Creates a new instance of this class.
    *@param stage
    *the parent window
    */
    protected PopupStage(Stage stage) {
        this(stage, null, null, null);
    }

    /**Creates a new instance of this class.
    *@param stage
    *the parent window
    *@param parent
    *the root of the child window scene
    */
    protected PopupStage(Stage stage, Parent parent) {
        this(stage, parent, null, null);
    }

    /**Creates a new instance of this class.
    *@param stage
    *the parent window
    *@param parent
    *the root of the child window scene
    *@param title
    *the title of the child window
    */
    protected PopupStage(Stage stage, Parent parent, String title) {
        this(stage, parent, title, null);
    }

    /**Creates a new instance of this class.
    *@param stage
    *the parent window
    *@param parent
    *the root of the child window scene
    *@param title
    *the title of the child window
    *@param url
    *the path to the css file to use for styling child window scene graph
    */
    protected PopupStage(Stage stage, Parent parent, String title, URL url) {
        primaryStage = stage;
        sceneRoot = (parent== null)? new Group(): parent;
        popupStageTitle = (title == null)? "": title;
        cssURL = (url == null)? GPlayer.DEFAULT_CSS: url;
    }

    /**Decorates and prepares the popup stage for display.*/
    protected void initialize() {
        popupStage.setTitle(popupStageTitle);
        popupStage.initModality(getModality());
        if (primaryStage != null)
            popupStage.initOwner(primaryStage);
        popupStage.setOnShowing((we) -> openAction());
        popupStage.setOnHiding((we) -> closeAction());
        popupStage.addEventHandler(javafx.scene.input.KeyEvent.KEY_RELEASED, getCloseEventHandler());
        setSceneToStage();
        populateSceneRoot();
    }

    /**Configures a scene object,
    *to host the nodes to display on the popup stage.
    */
    private void setSceneToStage() {
        scene = new Scene(sceneRoot, getWidth(), getHeight());
        scene.getStylesheets().add(cssURL.toExternalForm());
        popupStage.setScene(scene);
        popupStage.sizeToScene();
    }

    /**Calculates and returns the width to which popupStage will be set.
    *If primaryStage is non-null,
    *popupStage is set to the same width as that of primaryStage;
    *else,
    *it is set to half the view width of the display screen.
    */
    protected double getWidth() {
        return (primaryStage != null)? primaryStage.getWidth(): GPlayer.getScreenWidth() / 2;
    }

    /**Calculates and returns the height to which popupStage will be set.
    *If primaryStage is non-null,
    *popupStage is set to the same height as that of primaryStage;
    *else,
    *it is set to half the view height of the display screen.
    */
    protected double getHeight() {
        return (primaryStage != null)? primaryStage.getHeight(): GPlayer.getScreenHeight() / 2;
    }

    /**Returns the Modality type to be applied to the child window.
    *Unless overridden,
    *the modality returned blocks all events generated on the child window from reaching the parent window.
    */
    protected Modality getModality() {
        return Modality.APPLICATION_MODAL;
    }

    /**Closes the stage if hideOnEscape is set to true.
    *@return an event handler to be registered for key release events on the popupStage.
    */
    protected final javafx.event.EventHandler<? super javafx.scene.input.KeyEvent> getCloseEventHandler() {
        return ((ke) -> {
            if (hideOnEscape && new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.ESCAPE).match(ke))
                popupStage.hide();
        });
    }

    /**Displays the child window.*/
    protected void showPopupStage() {
        popupStage.show();
    }

    /**Hides the child window.*/
    protected void hidePopupStage() {
        popupStage.hide();
    }

    /**Defines a set of functions to perform just before the child window is closed.
    *Note:
    *if this method is overridden,
    *be sure to call
    *super.closeAction()
    *in your implementation
    *for certain adjustments to used objects.
    */
    protected void closeAction() {
        if (primaryStage != null) {
            if (primaryStage == GPlayer.getPrimaryStage()) {
                primaryStage.setTitle(GPlayer.getSceneRoot().getStageTitle());
                GPlayer.getSceneRoot().resumePlay(resumePlay);
            }
            FileMedia.getDisplayedPopups().remove(primaryStage);
        }
    }

    /**Defines a set of functions to perform just before the child window is displayed.
    *Note:
    *if this method is overridden,
    *be sure to call
    *super.openAction()
    *for certain adjustments to used objects.
    */
    protected void openAction() {
        if (primaryStage != null) {
            if (primaryStage == GPlayer.getPrimaryStage())
                primaryStage.setTitle(GPlayer.stageTitle);
            FileMedia.getDisplayedPopups().put(primaryStage, popupStage);
        }
    }

    /**Affirms if the owner of the popup stage is the specified window.
    *@return true if owner stage equals the specified window,
    *and false otherwise.
    */
    public final boolean isChildWindowOf(javafx.stage.Window window) {
        if (window == null)
            return false;
        return popupStage.getOwner() == window;
    }

    /**Asserts the display state of the popup stage.
    *@return true if the popup stage is showing
    *and false otherwise.
    */
    public final boolean isPopupShowing() {
        return popupStage.isShowing();
    }

    /**Gets the property/wrapper for monitoring the show state of the popup stage.
    *@return a read-only wrapper for the show state of the popup stage.
    */
    public final javafx.beans.property.ReadOnlyBooleanProperty popupShowingProperty() {
        return popupStage.showingProperty();
    }

    /**Populates the scene graph
    *by placing nodes on the child window scene root.
    */
    protected abstract void populateSceneRoot();
}