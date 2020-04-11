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

/**Maps a triplet of objects to a key, value and extension trio.
*Extends gplayer.com.util.Duo
*@param <K>
*the type of the first object, (key)
*@param <V>
*the type of the second object, (value)
*@param <E>
*the type of the third object, (extension)
*@author Ganiyu Emilandu
*/

public class Trio<K, V, E> extends Duo<K, V> {
    private E extension;
    private E priorExtension;
    /**constructor
    *@param k
    *to be assigned to super.key
    *@param v
    *to be assigned to super.value
    *@param e
    *to be assigned to extension
    */
    public Trio(K k, V v, E e) {
        super(k, v);
        priorExtension = extension;
        extension = e;
    }

    /**Updates the value of the extension object.
    *@param e
    *new extension value
    */
    public void setExtension(E e) {
        priorExtension = extension;
        extension = e;
    }

    /**Obtains and returns the value of the extension object.
    *@return extension
    */
    public E getExtension() {
        return extension;
    }

    /**Obtains and returns the value of the prior extension object.
    *@return priorExtension
    */
    public E getPriorExtension() {
        return priorExtension;
    }

    @Override
    public String toString() {
        return String.format("%s; Extension=%s", super.toString(), toString(extension));
    }

    @Override
    public boolean equals(Object object) {
        if (super.equals(object)) {
            Trio t = (Trio) object;
            return (extension == null)? t.extension == null: extension.equals(t.extension);
        }
        return false;
    }
}