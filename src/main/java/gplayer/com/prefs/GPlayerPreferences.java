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

import java.util.prefs.Preferences;

/**Creates java.util.Preferences nodes
*to enable media info be synced
*to host OS registry
*@author Ganiyu Emilandu
*/

public class GPlayerPreferences {
    private Preferences userPreferences;
    private Preferences systemPreferences;
    public String userPath, systemPath;

    public GPlayerPreferences() {
        userPreferences = Preferences.userRoot();
        systemPreferences = Preferences.systemRoot();
    }

    /**constructor
    *@param userPreferences
    *used to create user-based java.util.prefs.Preferences node
    *@param systemPreferences
    *used to create system-based java.util.prefs.Preferences node
    */
    public GPlayerPreferences(String userPreferences, String systemPreferences) {
        this();
        setUserPreferences(userPreferences);
        setSystemPreferences(systemPreferences);
    }

    /**Sets userPreferences to point to a particular user node
    *@param userPath
    *the path to the node in the registry
    */
    public void setUserPreferences(String userPath) {
        this.userPath = userPath;
        if (userPath != null && !userPath.isEmpty())
            userPreferences = Preferences.userRoot().node(userPath);
    }

    /**Sets userPreferences to point to a particular system node
    *@param systemPath
    *the path to the node in the registry
    */
    public void setSystemPreferences(String systemPath) {
        this.systemPath = systemPath;
        if (systemPath != null && !systemPath.isEmpty())
            systemPreferences = Preferences.systemRoot().node(systemPath);
    }

    /**Gets and returns user node
    *@return userPreferences
    */
    public Preferences getUserPreferences() {
        return userPreferences;
    }

    /**Gets and returns system node
    *@return systemPreferences
    */
    public Preferences getSystemPreferences() {
        return systemPreferences;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof GPlayerPreferences) {
            GPlayerPreferences f = (GPlayerPreferences) obj;
            return gplayer.com.util.Utility.ofEqualContent(userPath, f.userPath) && gplayer.com.util.Utility.ofEqualContent(systemPath, f.systemPath);
        }
        return false;
    }
}