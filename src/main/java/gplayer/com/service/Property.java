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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**Wraps passed objects in an ObservableValue object
*to enable them be registered and observed for changes
*through listener objects
*@param <T>
*the type of the wrapped object
*@author Ganiyu Emilandu
*/

public class Property<T> {
    private ObjectProperty<T> value;  //Holds current value
    private ObjectProperty<T> priorValue;  //Holds prior value

    /**constructor*/
    public Property() {
        this(null);
    }

    /**Constructor.
    *@param t
    *the value to set the property to on instantiation.
    */
    public Property(T t) {
        value = new SimpleObjectProperty<T>(this, "value", t);
        priorValue = new SimpleObjectProperty<T>(this, "priorValue");
    }

    /**Gets and returns current value object.*/
    public     T getValue() {
        return value.get();
    }

    /**Updates the object wrapped in value,
    *but first attempts to assign the current value object to priorValue
    *@param t
    *the new value object.
    */
    public final void setValue(T t) {
        if (t != null && !t.equals(getValue()))
            setPriorValue(getValue());
        value.set(t);
    }

    /**Gets and returns the read-only  wrapper of value.*/
    public final javafx.beans.property.ReadOnlyObjectProperty<T> valueProperty() {
        return value;
    }

    /**Gets and returns the current priorValue object.*/
    public final T getPriorValue() {
        return priorValue.get();
    }

    /**Updates the value wrapped in priorValue.
    *@param t
    *the value to set priorValue to.
    */
    private final void setPriorValue(T t) {
        priorValue.set(t);
    }

    /**Gets and returns the read-only  wrapper of priorValue.*/
    public final javafx.beans.property.ReadOnlyObjectProperty<T> priorValueProperty() {
        return priorValue;
    }
}