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

/**Defines the order in which items are to be sorted
*among an array or collection of items.
*@author Ganiyu Emilandu
*/

public enum FileSortParameter {
    /**Flags natural ordering (in order of insertion).*/
    DEFAULT,
    /**Flags alphabetical/numeric ordering from bottom to top.*/
    ASCENDING,
    /**Flags alphabetical/numberic ordering from top to bottom.*/
    DESCENDING,
    /**Flags random ordering of elements.*/
    SHUFFLE;
    /**References current parameter.*/
    static FileSortParameter PARAMETER;
    /**References prior parameter.*/
    static FileSortParameter PRIORPARAMETER;

    /**Sets current parameter value
    *by updating the value of PARAMETER
    *@param P
    *the value to assign to PARAMETER
    */
    static public void setSortParameter(FileSortParameter P) {
        PARAMETER = P;
    }

    /**Gets current sort parameter
    *@return the value of PARAMETER,
    *or DEFAULT if PARAMETER is null
    */
    static public FileSortParameter getSortParameter() {
        return (PARAMETER = (PARAMETER == null)? FileSortParameter.DEFAULT: PARAMETER);
    }

    /**Sets prior parameter value
    *by updating the value of PRIORPARAMETER
    *@param PP
    *the value to assign to PRIORPARAMETER
    */
    static public void setPriorSortParameter(FileSortParameter PP) {
        PRIORPARAMETER = PP;
    }

    /**Gets prior sort parameter
    *@return the value of PRIORPARAMETER,
    *or DEFAULT if PRIORPARAMETER is null
    */
    static public FileSortParameter getPriorSortParameter() {
        return (PRIORPARAMETER = (PRIORPARAMETER == null)? FileSortParameter.DEFAULT: PRIORPARAMETER);
    }
}