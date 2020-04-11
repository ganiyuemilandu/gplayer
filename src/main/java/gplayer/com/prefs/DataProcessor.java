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

import static gplayer.com.util.Utility.ofEqualContent;

/**Provides overloaded methods
*for unboxing primitive data types from objects,
*so as to sync them to host OS registry.
*@author Ganiyu Emilandu
*/

public class DataProcessor {
    private GPlayerPreferences gp;
    private String key;
    private Object value;
    private java.util.prefs.Preferences prefs;
    public DataProcessor(GPlayerPreferences gp, String key, Object value) {
        this.gp = gp;
        this.key = key;
        this.value = value;
        prefs = gp.getUserPreferences();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void save(String value) {
        prefs.put(key, value);
    }

    public void save(int value) {
        prefs.putInt(key, value);
    }

    public void save(boolean value) {
        prefs.putBoolean(key, value);
    }

    public void save(double value) {
        prefs.putDouble(key, value);
    }

    public void processData() {
        if (value instanceof String)
            save((String) value);
        else if (value instanceof Integer)
            save((int) value);
        else if (value instanceof Double)
            save((double) value);
        else if (value instanceof Boolean)
            save((boolean) value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof DataProcessor) {
            DataProcessor d = (DataProcessor) obj;
            if (!(ofEqualContent(key, d.key) && ofEqualContent(gp, d.gp)))
                return false;
            return ofEqualContent(value, d.value);
        }
        return false;
    }
}