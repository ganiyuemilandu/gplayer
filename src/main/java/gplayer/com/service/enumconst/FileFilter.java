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

package gplayer.com.service.enumconst;

/**Defines filtering parameter
*for conducting searches
*among an array or collection of items.
*@author Ganiyu Emilandu
*/

public enum FileFilter {
    /**Flags search matches to include the search parameter,
    *could be at the beginning, middle, or at the end
    */
    CONTAINS,
    /**Flags search matches to include the search parameter,
    *at the beginning only
    */
    BEGINSWITH,
    /**Flags search matches to include the search parameter,
    *at the end only
    */
    ENDSWITH;
    /**References default filter.*/
    static FileFilter FILTER;
    /**References prior default filter.*/
    static FileFilter PRIORFILTER;

    /**Sets default filter value
    *by updating the value of FILTER
    *@param F
    *the value to assign to FILTER
    */
    static public void setDefaultFilter(FileFilter F) {
        FILTER = F;
    }

    /**Gets default FileFilter
    *@return the value of FILTER,
    *or CONTAINS if FILTER is null
    */
    static public FileFilter getDefaultFilter() {
        return (FILTER = (FILTER == null)? FileFilter.CONTAINS: FILTER);
    }

    /**Sets prior default filter value
    *by updating the value of PRIORFILTER
    *@param F
    *the value to assign to PRIORFILTER
    */
    static void setPriorDefaultFilter(FileFilter F) {
        PRIORFILTER = F;
    }

    /**Gets prior default FileFilter
    *@return the value of PRIORFILTER,
    *or CONTAINS if PRIORFILTER is null
    */
    static FileFilter getPriorDefaultFilter() {
        return (PRIORFILTER = (PRIORFILTER == null)? FileFilter.CONTAINS: PRIORFILTER);
    }
}