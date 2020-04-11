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

/**Maps a pair of objects to a key and value duo.
*@param <K>
*the type of the first object, (key)
*@param <V>
*the type of the second object, (value)
*@author Ganiyu Emilandu
*/

public class Duo<K, V> implements java.io.Serializable {
    private K key;
    private K priorKey;
    private V value;
    private V priorValue;

    /**constructor
    *@param k
    *to be assigned to key
    *@param v
    *to be assigned to value
    */
    public Duo(K k, V v) {
        priorKey = key;
        key = k;
        priorValue = value;
        value = v;
    }

    /**Updates the value of the key object.
    *@param k
    *new key value
    */
    public void setKey(K k) {
        priorKey = key;
        key = k;
    }

    /**Obtains and returns the value of the key object.
    *@return key
    */
    public K getKey() {
        return key;
    }

    /**Obtains and returns the value of the prior key object.
    *@return priorKey
    */
    public K getPriorKey() {
        return priorKey;
    }

    /**Updates the value of the value object.
    *@param v
    *new value value
    */
    public void setValue(V v) {
        priorValue = value;
        value = v;
    }

    /**Obtains and returns the value of the value object.
    *@return value
    */
    public V getValue() {
        return value;
    }

    /**Obtains and returns the value of the prior value object.
    *@return priorValue
    */
    public V getPriorValue() {
        return priorValue;
    }

    protected final String toString(Object object) {
        return (object == null)? "null": object.toString();
    }

    @Override
    public String toString() {
        return String.format("Key=%s; Value=%s", toString(key), toString(value));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof Duo) {
            Duo d = (Duo) obj;
            return gplayer.com.util.Utility.ofEqualContent(key, d.key) && gplayer.com.util.Utility.ofEqualContent(value, d.value);
        }
        return false;
    }
}