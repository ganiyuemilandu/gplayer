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

import gplayer.com.service.FileMedia;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javafx.util.Duration;

/**Defines several static methods
*to perform a variety of functions.
*@author Ganiyu Emilandu
*/

public class Utility {
    public static final String[] TIME_UNIT = FileMedia.resource.getStringArray("timeUnit.single.array");
    public static final String[] TIME_UNITS = FileMedia.resource.getStringArray("timeUnit.multiple.array");
    public static final String[] TIME_UNIT_INITIALS = FileMedia.resource.getStringArray("timeUnit.initials.array");
    private static String[] timeUnitInitials = Arrays.copyOf(TIME_UNIT_INITIALS, TIME_UNIT_INITIALS.length);
    public static final String[] FILE_UNIT = FileMedia.resource.getStringArray("fileUnit.single.array");
    public static String[] FILE_UNITS = FileMedia.resource.getStringArray("fileUnit.multiple.array");
    public static final String TIME_UNIT_PATTERN = FileMedia.resource.getString("timeUnit.pattern");
    public static final String FILE_UNIT_PATTERN = FileMedia.resource.getString("fileUnit.pattern");
    public static final String TIME_UNIT_SEPARATOR = FileMedia.resource.getString("timeUnit.separator");
    private static boolean sorted = false;

    private Utility() {}

    /**Eliminates all characters in a string,
    *except for digits and a specified character.
    *@param string,
    *the string from which non-digits are to be removed
    *@param character
    *the character to ignore
    *@throws NullPointerException
    *if the string is null
    *@return a string of digits and the ignored character only
    */
    public static String retainDigits(String string, char character) {
        return eliminateCharacters(string, ((charNum) -> test(charNum, character, !Character.isDigit(charNum))));
    }

    /**Eliminates all characters in a string,
    *except for digits and 0 or more specified character.
    *@param string,
    *the string from which non-digits are to be removed
    *@param characters
    *the characters to ignore
    *@throws NullPointerException
    *if the string is null
    *@return a string of digits and ignored characters only
    */
    public static String retainDigits(String string, String characters) {
        return eliminateCharacters(string, ((charNum) -> test(charNum, characters, !Character.isDigit(charNum))));
    }

    /**Eliminates all characters in a string,
    *except for digits only.
    *@param string,
    *the string from which non-digits are to be removed
    *@throws NullPointerException
    *if the string is null
    *@return a string of digits only
    */
    public static String retainDigits(String string) {
        return eliminateCharacters(string, ((charNum) -> !Character.isDigit(charNum)));
    }

    /**Eliminates all characters in a string,
    *except for letters and a specified character.
    *@param string,
    *the string from which non-letters are to be removed
    *@param character
    *the character to ignore
    *@throws NullPointerException
    *if the string is null
    *@return a string of letters and the ignored character only
    */
    public static String retainLetters(String string, char character) {
        return eliminateCharacters(string, ((charNum) -> test(charNum, character, !Character.isLetter(charNum))));
    }

    /**Eliminates all characters in a string,
    *except for letters and 0 or more specified characters.
    *@param string,
    *the string from which non-letters are to be removed
    *@param characters
    *the characters to ignore
    *@throws NullPointerException
    *if the string is null
    *@return a string of letters and the ignored characters only
    */
    public static String retainLetters(String string, String characters) {
        return eliminateCharacters(string, ((charNum) -> test(charNum, characters, !Character.isLetter(charNum))));
    }

    /**Eliminates all characters in a string,
    *except for letters only.
    *@param string,
    *the string from which non-letters are to be removed
    *@throws NullPointerException
    *if the string is null
    *@return a string of letters only
    */
    public static String retainLetters(String string) {
        return eliminateCharacters(string, ((charNum) -> !Character.isLetter(charNum)));
    }

    /**Eliminates all possible symbols in a string,
    *except for a specified character.
    *@param string,
    *the string from which symbols are to be removed
    *@param character
    *the symbol to ignore
    *@throws NullPointerException
    *if the string is null
    *@return a string of letters and numbers only
    */
    private     static String retainLettersAndDigits(String string, char character) {
        return eliminateCharacters(string, ((charNum) -> test(charNum, character, !Character.isLetterOrDigit(charNum))));
    }

    /**Eliminates all possible symbols in a string,
    *except for 1 or more specified characters.
    *@param string,
    *the string from which symbols are to be removed
    *@param characters
    *the symbols to ignore
    *@throws NullPointerException
    *if the string is null
    *@return a string of letters and numbers only
    */
    public static String retainLettersAndDigits(String string, String characters) {
        return eliminateCharacters(string, ((charNum) -> test(charNum, characters, !Character.isLetterOrDigit(charNum))));
    }

    /**Eliminates all possible symbols in a string,
    *@param string,
    *the string from which symbols are to be removed
    *@throws NullPointerException
    *if the string is null
    *@return a string of letters and numbers only
    */
    public static String retainLettersAndDigits(String string) {
        return eliminateCharacters(string, ((charNum) -> !Character.isLetterOrDigit(charNum)));
    }

    /**Modifies a string
    *by eliminating the characters that return true when applied on the passed Predicate.
    *If the predicate passed is null,
    *the string is returned as is.
    *@param string
    *the string from which 0 or more characters would be removed from
    *@param p
    *the predicate to apply
    *@throws NullPointerException
    *if the passed string is null
    *@return a modified string void of the characters that satisfy the predicate condition in the original string
    */
    public static String eliminateCharacters(String string, java.util.function.IntPredicate predicate) {
        if (predicate == null)
            return string;
        if (string == null)
            throw new NullPointerException("string cannot be null");
        StringBuilder sb = new StringBuilder(string);
        for (int i = 0; i < sb.length(); i++) {
            int c = sb.charAt(i);
            if (predicate.test(c)) {
                sb.deleteCharAt(i);
                if (i < sb.length())
                    i--;
            }
        }
        return sb.toString();
    }

    private static boolean test(int character, String characters, boolean eliminate) {
        char c = (char) character;
        return (characters == null || characters.isEmpty())? eliminate: characters.indexOf(c) == -1 && eliminate;
    }

    private static boolean test(int i, Character character, boolean eliminate) {
        return (character == null)? eliminate: i != character.charValue() && eliminate;
    }

    /**Ensures unique names among a group of string objects.
    *In a situation in which there is a name clash,
    *name modification is performed on the newest entry by appending a number at the end.
    *@param suggestedName
    *the string to be tested against clashes among existing list of names
    *@param existingNames
    *the list of names already present
    *@return the same string if there was no name clash,
    *and a modified string otherwise.
    */
    public static String generateUniqueName(String suggestedName, List<String> existingNames) {
        String sn = (suggestedName == null)? "null": trimToSize(suggestedName);
        if (sn.equals("null") || sn.isEmpty()) {
            if (!existingNames.isEmpty())
                sn = existingNames.get(existingNames.size()-1);
            else
                sn = (sn.isEmpty())? "null": sn;
        }
        int k = sn.length()-1;
        String num = "";
        //Ascertain if sn has a number at the extreme end
        for (int j = k; j >= 0; j--) {
            try {
                int l = Integer.parseInt("" + sn.charAt(j));
                num = l + num;
            }
            catch (NumberFormatException ex) {
                k = j;
                break;
            }
        }
        sn = (sn.equals(num))? (num = "" + (Integer.parseInt(num) + 1)): sn.substring(0, ++k) + num;
        int i = (!num.isEmpty())? Integer.parseInt(num): 0;
        List<String> names = new ArrayList<>(existingNames);
        while (!hasUniqueName(sn, names, true)) {
            if (i != 0)
                sn = sn.replace("" + i, "");
            else
                i++;
            sn += (++i);
        }
        return sn;
    }

    /**Ensures unique names among a group of string objects.
    *In a situation in which there is a name clash,
    *name modification is performed on the newest entry by appending a number at the end.
    *@param suggestedName
    *the string to be tested against clashes among existing array of names
    *@param existingNames
    *the array of names already present
    *@return the same string if there was no name clash,
    *and a modified string otherwise.
    */
    public static String generateUniqueName(String suggestedName, String... existingNames) {
        return generateUniqueName(suggestedName, Arrays.<String>asList(existingNames));
    }

    /**Confirms the uniqueness of an item among a list of items
    *@param name
    *the item to be sought for
    *@param names
    *the list on which the search is to be performed
    *@param <T>
    *the type of name and the elements of the list
    *@param remove
    *flags for the removal of the item if another is found
    *@throws NullPointerException
    *if the item is null
    *@return true,
    *if there is only one of the item in the list
    *and false otherwise
    */
    private static <T> boolean hasUniqueName(T name, List<T> allNames, boolean remove) {
        if (name == null)
            throw new NullPointerException("Name value cannot be null");
        List<T> names = new ArrayList<>(allNames);
        boolean unique = true;
        String stringName = null, stringItem = null;
        for (T item: names) {
            if (name instanceof String) {
                stringName = retainLettersAndDigits((String)name);
                stringItem = retainLettersAndDigits((String)item);
            }
            boolean isEqual = (name instanceof String)? stringItem.equalsIgnoreCase(stringName): item.equals(name);
            if (isEqual) {
                if (remove)
                    names.remove(item);
                unique = false;
                break;
            }
        }
        return unique;
    }

    /**Confirms the uniqueness of an item among a vararg array of items
    *@param name
    *the item to be sought for
    *@param names
    *the array on which the search is to be performed
    *@param <T>
    *the type of name and the elements of the array
    *@return true,
    *if there is only one of the item in the list
    *and false otherwise
    */
    @SafeVarargs
    public static <T> boolean hasUniqueName(T name, T... names) {
        return hasUniqueName(name, Arrays.<T>asList(names), false);
    }

    /**Confirms the uniqueness of an item among a list of items
    *@param name
    *the item to be sought for
    *@param names
    *the list on which the search is to be performed
    *@param <T>
    *the type of name and the elements of the list
    *@return true,
    *if there is only one of the item in the list
    *and false otherwise
    */
    public static <T> boolean hasUniqueName(T name, List<T> names) {
        return hasUniqueName(name, names, false);
    }

    /**Converts a string time format to milliseconds
    *@param time
    *the string to be converted
    *@return the millisecond equivalent
    */
    public static long convertTimeToMillis(String time) {
        long longTime = 0;
        String delimiters = eliminateCharacters(time, ((c) -> Character.isLetterOrDigit(c)));
        StringTokenizer st = new StringTokenizer(time, delimiters);
        while (st.hasMoreTokens()) {
            int[] timeArray = resolveTimeFormat(st.nextToken(), st.nextToken());
            int intTime = timeArray[0];
            //Now, convert time to its milliseconds equivalent
            switch (timeArray[1]) {
                case 0:  //hour time unit
                    longTime +=(intTime * 1000 * 60 * 60);
                    break;
                case 1:  //minute time unit
                    longTime +=(intTime * 1000 * 60);
                    break;
                case 2:  //second time unit
                    longTime +=(intTime * 1000);
                    break;
                default:
                    throw new IllegalArgumentException("Time unit specified is not supported. The available ones are:\n" + String.join(": ", TIME_UNIT));
            }
        }
        return longTime;
    }

    private static int[] resolveTimeFormat(String... time) {
        if (!sorted)
            Arrays.sort(timeUnitInitials, (a, b) -> a.length() - b.length());
        try {
            int[] timeArray = {-1, -1};
            int unitIndex = 1;
            timeArray[0] = Integer.parseInt((isNumber(time[0]))? time[--unitIndex]: time[unitIndex]);
            unitIndex = (unitIndex == 1)? 0: 1;
            String unit = time[unitIndex];
            int length = unit.length();
            for (int i = timeUnitInitials.length -1; i >= 0; i--) {
                String initials = timeUnitInitials[i];
                boolean begins = (length >= initials.length())? unit.startsWith(initials): initials.startsWith(unit);
                if (begins) {
                    timeArray[1] = i;
                    break;
                }
            }
            return timeArray;
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean isNumber(String number) {
        try {
            Integer.parseInt(number);
            return true;
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }

    private static int[] getStringLength(String... array) {
        int[] lengths = new int[array.length];
        int length = lengths.length;
        for (int i = 0; i < length; i++)
            lengths[i] = array[i].length();
        return lengths;
    }

    /**Sorts a list of string time format objects
    *@param list
    *a list of string time objects
    *@return a sorted set of the list
    */
    public static TreeSet<String> sortStringTimeFormat(List<String> list) {
        final TreeSet<String> modifiedSet = new TreeSet<>((a, b) -> (int) (convertTimeToMillis(a) - convertTimeToMillis(b)));
        list.forEach((item) -> {
            long longTime = convertTimeToMillis(item);
            String itemResult = convertTimeToString(longTime);
            if (!itemResult.isEmpty())
                modifiedSet.add(itemResult);
        });
        return modifiedSet;
    }

    /**Converts a time numeric value
    *to a string format of hours:minutes:seconds
    *@param time
    *The numeric value
    *@return the string equivalent
    */
    public static String convertTimeToString(long time) {
        String stringTime = "";  //To hold converted string time
        String reversedTime = reverseString(formatTime(Duration.millis(time), Duration.ZERO));
        String[] timeArray = reversedTime.split(TIME_UNIT_SEPARATOR);
        int timeArrayLength = timeArray.length;
        for (int i = 0, j = TIME_UNIT.length; i < timeArrayLength; i++) {
            reversedTime = reverseString(timeArray[i]);
            int intTime = Integer.parseInt(reversedTime);
            String unit = (intTime == 1)? TIME_UNIT[--j]: TIME_UNITS[--j];
            if (intTime > 0)
                stringTime = (stringTime.isEmpty())? formatTime(intTime, unit): formatTime(intTime, unit) + TIME_UNIT_SEPARATOR + stringTime;
        }
        return stringTime;
    }

    /**Generates a list of string time objects
    *from the value passed in floor to the value passed in ceiling (inclusive)
    *using the string parameters passed in stringFloor and stringLayers.
    *@param floor
    *the lowest generated time value
    *@param ceiling
    *the highest generated time value
    *@param stringFloor
    *the base/parent time unit
    *@param stringLayers
    *the child time unit
    *@param layers
    *child-unit values attached to parent-unit values
    *@return a list of string time objects ranging from floor to ceiling, inclusive
    */
    private static final List<String> generateStringTimeFormat(int floor, int ceiling, String stringFloor, String stringLayers, int... layers) {
        List<String> list = new ArrayList<>();
        //Assign intFloor the smaller of floor and ceiling values
        int intFloor = Math.min(floor, ceiling);
        //and assign intCeiling the bigger of floor and ceiling values
        int intCeiling = Math.max(floor, ceiling);
        //Generate values beginning with intFloor
        for (int i = intFloor; i <= intCeiling; i++) {
            String time = convertTimeToString(convertTimeToMillis(i + " " + stringFloor));
            if (!time.isEmpty())
                list.add(time);
            if (i != intCeiling) {
                //Apply stringLayers
                for (int l: layers)
                    list.add(convertTimeToString(convertTimeToMillis(i + " " + stringFloor + " " + l + " " + stringLayers)));
            }
        }
        return list;
    }

    /**Generates hour string time objects
    *with hours as the parent time unit
    *and minutes as the child time unit.
    *@param floor
    *the start hour value
    *@param ceiling
    *the end hour value
    *@param layers
    *minute-unit values attached to hour-unit values
    *@return a list of string time objects
    */
    public static List<String> generateHoursStringTimeFormat(int floor, int ceiling, int... layers) {
        return generateStringTimeFormat(floor, ceiling, TIME_UNIT[0], TIME_UNIT[1], layers);
    }

    /**Generates minute string time objects
    *with minutes as the parent time unit
    *and seconds as the child time unit.
    *@param floor
    *the start minute value
    *@param ceiling
    *the end minute value
    *@param layers
    *second-unit values attached to minute-unit values
    *@return a list of string time objects
    */
    public static List<String> generateMinutesStringTimeFormat(int floor, int ceiling, int... layers) {
        return generateStringTimeFormat(floor, ceiling, TIME_UNIT[1], TIME_UNIT[2], layers);
    }

    /**Reverses the content of a string
    *@param originalString
    *the string to be reversed
    *@throws NullPointerException
    *if the string is null
    *@return the resulting string.
    */
    public static String reverseString(String originalString) {
        if (originalString == null)
            throw new NullPointerException("String cannot be null");
        StringBuffer sb = new StringBuffer(originalString);
        return new String(sb.reverse());
    }

    /**Converts media duration to hours/minutes/seconds time units
    *@param elapsed
    *the used-up duration since playback
    *@param duration
    *total media duration
    *@return a string of the converted time
    */
    public static String formatTime(Duration elapsed, Duration duration) {
        String separator = TIME_UNIT_SEPARATOR;
        int intElapse = (int) Math.floor(elapsed.toSeconds());
        int elapseHours = intElapse / (60 * 60);
        if (elapseHours > 0) {
            //Subtract the number of hours
            //multiplied by 3600 from intElapsed (the overall available seconds)
            intElapse -= elapseHours * 60 * 60;
        }
        int elapseMinutes = intElapse / 60;
        int elapseSeconds = intElapse - elapseMinutes * 60;
        if (duration.greaterThan(Duration.ZERO)) {  //If duration is greater than 0
            int intDuration = (int) Math.floor(duration.toSeconds());  //duration is converted to seconds
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationMinutes * 60;
            if (durationHours > 0) {  //If durationHours is 1 or more
                return String.format("%d%s%02d%s%02d/%d%s%02d%s%02d", elapseHours, separator, elapseMinutes, separator, elapseSeconds, durationHours, separator, durationMinutes, separator, durationSeconds);  //Return the conjured string
            }
            else {  //If durationHours = 0
                return String.format("%02d%s%02d/%02d%s%02d", elapseMinutes, separator, elapseSeconds, durationMinutes, separator, durationSeconds);
            }
        }
        else {
            if (elapseHours > 0) {  //If elapseHours is 1 or more
                return String.format("%d%s%02d%s%02d", elapseHours, separator, elapseMinutes, separator, elapseSeconds);
            }
            else {  //If elapseHours equals 0
                return String.format("%02d%s%02d", elapseMinutes, separator, elapseSeconds);
            }
        }
    }

    /**Formats the passed time in relation to the pattern outlined in TIME_UNIT_PATTERN.
    *@param time
    *the time measurement
    *@param unit
    *the time unit
    *@return the formatted time.
    */
    public static String formatTime(Integer time, String unit) {
        return FileMedia.resource.formatMessage(TIME_UNIT_PATTERN, time, unit);
    }

    /**Formats the passed time in relation to the pattern outlined in TIME_UNIT_PATTERN.
    *@param time
    *the time measurement
    *@param unit
    *the index of one of the available time units
    *@return the formatted time.
    */
    public static String formatTime(int time, int unit) {
        return formatTime(time, (time == 1)? TIME_UNIT[unit]: TIME_UNITS[unit]);
    }

    /**Modifies a string
    *in a manner in which leading and trailing occurrences of a character are eliminated.
    *In situations where there are consecutive occurrences of the character between the string,
    *the extra is/are discarded
    *allowing for only one occurrence of the character between other characters in the string.
    *@param string
    *the string to be modified
    *@param character
    *the character to be eliminated
    *@return the resulting string
    */
    public static String trimToSize(String string, char character) {
        String s = trim(string, character), result = "";
        int i = -1;
        do {
            //Get the first occurrence of the character passed
            i = s.indexOf(character);
            result += (i != -1)? s.substring(0, i+1): s.substring(0);
            if (i != -1)
                s = trim(s.substring(i+1), character);
        }
        while (i != -1);
        return trim(result, character);
    }

    /**Modifies a string
    *in a manner in which leading and trailing whitespaces are eliminated.
    *In situations where there are consecutive whitespaces between the string
    *the extra is/are discarded
    *allowing for only one whitespace between other characters in the string.
    *@param string
    *the string to be modified
    *@return the resulting string
    */
    public static String trimToSize(String string) {
        return trimToSize(string, ' ');
    }

    /**Trims a given string at the extremes by eliminating all leading and trailing occurrences of a given character from the string
    *@param str
    *the string to be trimmed
    *@param character
    *the character to be eliminated
    *@return the resulting string
    */
    public static String trim(String str, char character) {
        //Have all leading occurrences of character trimmed from str
        String string = trimLeadingCharacter(str, character);
        //Do likewise for the trailing occurrences, and return the result
        return trimTrailingCharacter(string, character);
    }

    /**Trims a given string at the left extreme by eliminating all leading occurrences of a given character from the string
    *@param str
    *the string to be trimmed
    *@param character
    *the character to be eliminated
    *@throws NullPointerException
    *if the passed string is null
    *@return the resulting string
    */
    public static String trimLeadingCharacter(String str, char character) {
        if (str == null)
            throw new NullPointerException("String cannot be null");
        int i = -1;
        //Increase the value of i with every iteration
        //Terminate the loop if the value of i equals the length of the passed string
        //or the character at index i in the passed string does not equal that of the passed character
        while (++i < str.length() && str.charAt(i) == character);
        //Return an empty string if i equals the length of str
        //or all characters beginning at index i from str, if i is less than the length of str
        return (i == str.length())? "": str.substring(i);
    }

    /**Trims a given string at the right extreme by eliminating all trailing occurrences of a given character from the string
    *@param str
    *the string to be trimmed
    *@param character
    *the character to be eliminated
    *@throws NullPointerException
    *if the passed string is null
    *@return the resulting string
    */
    public static String trimTrailingCharacter(String str, char character) {
        if (str == null)
            throw new NullPointerException("String cannot be null");
        //Have str reversed,
        //have leading occurrences of character eliminated from the reversed string,
        //and have the trimmed string reversed, and returned
        return reverseString(trimLeadingCharacter(reverseString(str), character));
    }

    /**Obtains all characters preceding or following the first occurrence of a certain character in a string.
    *If no index of the character cannot be found in the passed string,
    *all characters in the string are returned.
    *@param string
    *the string whose part will be returned
    *@param character
    *the character to use as index parameter
    *@param before
    *true if the part preceding; or false if part succeeding first occurrence of character is required
    *@throws NullPointerException
    *if string is null
    *@return a substring of string from 0 to first index of character, if before is true
    *or a substring of all characters in string after first index of character, if before is false
    */
    public static String preCharacterString(String string, char character, boolean before) {
        if (string == null)
            throw new NullPointerException("String cannot be null");
        //Get the first occurrence of character in the passed string
        int index = string.indexOf(character);
        if (!before)
            //Return a substring of the string beginning at index+1
            return string.substring(index+1);
        else
            //Return all characters preceeding index
            return (index <= 0)? "": string.substring(0, index);
    }

    /**Obtains all characters preceding the first occurrence of a certain character in a string.
    *If no index of the character cannot be found in the passed string,
    *all characters in the string are returned.
    *@param string
    *the string whose part will be returned
    *@param character
    *the character to use as index parameter
    *@throws NullPointerException
    *if string is null
    *@return a substring of string from 0 to first index of character
    */
    public static String preCharacterString(String string, char character) {
        return preCharacterString(string, character, true);
    }

    /**Obtains all characters preceding or following the last occurrence of a certain character in a string.
    *If no index of the character cannot be found in the passed string,
    *all characters in the string are returned.
    *@param string
    *the string whose part will be returned
    *@param character
    *the character to use as index parameter
    *@param after
    *true if the part succeeding; or false if part preceding last occurrence of character is required
    *@throws NullPointerException
    *if string is null
    *@return a substring of all characters in string after last index of character, if after is true
    *or a substring of string from 0 to last index of character, if after is false
    */
    public static String postCharacterString(String string, char character, boolean after) {
        if (string == null)
            throw new NullPointerException("String cannot be null");
        //Get the last occurrence of character in the passed string
        int lastIndex = string.lastIndexOf(character);
        if (after)
            //Return a substring of the string beginning at index+1
            return string.substring(lastIndex+1);
        else
            //Return the string preceeding lastIndex
            return (lastIndex <= 0)? "": string.substring(0, lastIndex);
    }

    /**Obtains all characters following the last occurrence of a certain character in a string.
    *If no index of the character cannot be found in the passed string,
    *all characters in the string are returned.
    *@param string
    *the string whose part will be returned
    *@param character
    *the character to use as index parameter
    *@throws NullPointerException
    *if string is null
    *@return a substring of all characters in string after last index of character
    */
    public static String postCharacterString(String string, char character) {
        return postCharacterString(string, character, true);
    }

    /**Traverses a list in ascending order,
    *beginning at the index of a specified item in the list +1,
    *or the index of the first item in the list, if the item specified is either the last or not contained in the list,
    *inn order to retrieve the item following the current item, (relative to traversal direction).
    *@param currentItem
    *the item whose index would be 1 less-than the start index.
    *@param predicate
    *the condition that the returned item must satisfy.
    *@param callback
    *the interface to convert the discovered item into the desired returned type.
    *@param list
    *the list to traverse.
    *@param <T>
    *the type of currentItem and the elements of the list
    *@param <R>
    *the type of the returned item.
    *@return the result of applying the first encountered item that satisfies the condition of the specified predicate to the specified callback,
    *or nul if none satisfies the condition, or the list is empty.
    */
    public static <T, R> R nextItem(T currentItem, java.util.function.Predicate<T> predicate, javafx.util.Callback<T, R> callback, List<T> list) {
        return nextOrPreviousItem(currentItem, true, predicate, callback, list);
    }

    /**Traverses a list in ascending order,
    *beginning at the index of a specified item in the list +1,
    *or the index of the first item in the list, if the item specified is either the last or not contained in the list,
    *inn order to retrieve the item following the current item, (relative to traversal direction).
    *@param currentItem
    *the item whose index would be 1 less-than the start index.
    *@param predicate
    *the condition that the returned item must satisfy.
    *@param list
    *the list to traverse.
    *@param <T>
    *the type of currentItem and the elements of the list
    *@return the first encountered item that satisfies the condition of the specified predicate,
    *or nul if none satisfies the condition, or the list is empty.
    */
    public static <T> T nextItem(T currentItem, java.util.function.Predicate<T> predicate, List<T> list) {
        return nextOrPreviousItem(currentItem, true, predicate, list);
    }

    /**Traverses a list in ascending order,
    *beginning at the index of a specified item in the list +1,
    *or the index of the first item in the list, if the item specified is either the last or not contained in the list,
    *inn order to retrieve the item following the current item, (relative to traversal direction).
    *@param currentItem
    *the item whose index would be 1 less-than the start index.
    *@param list
    *the list to traverse.
    *@param <T>
    *the type of currentItem and the elements of the list
    *@return the item following the current item,
    *or the same item if the list only contains the specified item,
    *or nul if the list is empty.
    */
    public static <T> T nextItem(T currentItem, List<T> list) {
        return nextItem(currentItem, ((item) -> true), list);
    }

    /**Traverses a vararg array in ascending order,
    *beginning at the index of a specified item in the array +1,
    *or the index of the first item in the array, if the item specified is either the last or not contained in the array,
    *inn order to retrieve the item following the current item, (relative to traversal direction).
    *@param currentItem
    *the item whose index would be 1 less-than the start index.
    *@param array
    *the vararg array to traverse.
    *@param <T>
    *the type of currentItem and the elements of the array
    *@return the item following the current item,
    *or the same item if the array only contains the specified item,
    *or nul if the array is empty.
    */
    @SafeVarargs
    public static <T> T nextItem(T currentItem, T... array) {
        return nextItem(currentItem, Arrays.<T>asList(array));
    }

    /**Traverses a list in descending order,
    *beginning at the index of a specified item in the list -1,
    *or the index of the last item in the list, if the item specified is either the first or not contained in the list,
    *inn order to retrieve the first item (relative to traversal direction) that satisfy the specified predicate.
    *@param currentItem
    *the item whose index would be 1 greater-than the start index.
    *@param predicate
    *the condition that the returned item must satisfy.
    *@param callback
    *the interface to convert the discovered item into the desired returned type.
    *@param list
    *the list to traverse.
    *@param <T>
    *the type of currentItem and the elements of list
    *@param <R>
    *the type of the returned item.
    *@return the result of applying the first encountered item that satisfies the condition of the specified predicate to the specified callback,
    *or nul if none satisfies the condition, or the list is empty.
    */
    public static <T, R> R previousItem(T currentItem, java.util.function.Predicate<T> predicate, javafx.util.Callback<T, R> callback, List<T> list) {
        return nextOrPreviousItem(currentItem, false, predicate, callback, list);
    }

    /**Traverses a list in descending order,
    *beginning at the index of a specified item in the list -1,
    *or the index of the last item in the list, if the item specified is either the first or not contained in the list,
    *inn order to retrieve the first item (relative to traversal direction) that satisfy the specified predicate.
    *@param currentItem
    *the item whose index would be 1 greater-than the start index.
    *@param predicate
    *the condition that the returned item must satisfy.
    *@param list
    *the list to traverse.
    *@param <T>
    *the type of currentItem and the elements of list
    *@return the first encountered item that satisfies the condition of the specified predicate,
    *or nul if none satisfies the condition, or the list is empty.
    */
    public static <T> T previousItem(T currentItem, java.util.function.Predicate<T> predicate, List<T> list) {
        return nextOrPreviousItem(currentItem, false, predicate, list);
    }

    /**Traverses a list in descending order,
    *beginning at the index of a specified item in the list -1,
    *or the index of the last item in the list, if the item specified is either the first or not contained in the list,
    *inn order to retrieve the item following the current item, (relative to traversal direction).
    *@param currentItem
    *the item whose index would be 1 greater-than the start index.
    *@param list
    *the list to traverse.
    *@param <T>
    *the type of currentItem and the elements of list
    *@return the item following the current item,
    *or the same item if list only contains the specified item,
    *or nul if the list is empty.
    */
    public static <T> T previousItem(T currentItem, List<T> list) {
        return previousItem(currentItem, ((item) -> true), list);
    }

    /**Traverses a vararg array in descending order,
    *beginning at the index of a specified item in the array -1,
    *or the index of the last item in the array, if the item specified is either the first or not contained in the array,
    *inn order to retrieve the item following the current item, (relative to traversal direction).
    *@param currentItem
    *the item whose index would be 1 greater-than the start index.
    *@param array
    *the vararg array to traverse.
    *@param <T>
    *the type of currentItem and the elements of the array
    *@return the item following the current item,
    *or the same item if the array only contains the specified item,
    *or nul if the array is empty.
    */
    @SafeVarargs
    public static <T> T previousItem(T currentItem, T... array) {
        return previousItem(currentItem, Arrays.<T>asList(array));
    }

    private static <T, R> R nextOrPreviousItem(T currentItem, boolean forwardProgression, java.util.function.Predicate<T> predicate, javafx.util.Callback<T, R> callback, List<T> list) {
        if (list == null || list.isEmpty())
            return null;
        int currentItemIndex = list.indexOf(currentItem);
        int endIndex = (currentItemIndex == -1)? 0: currentItemIndex;
        int startIndex = (forwardProgression)? endIndex+1: endIndex-1;
        for (int i = startIndex; ;) {  //Loop through the items in the list
            //beginning at the immediate index after or before the current item index
            //until either the predicate condition is true,
            //or the end-point is reached
            if (forwardProgression && i > list.size()-1)  //If the value of i exceeds the highest index in the list
                i = 0;  //Revert the value of i to 0
            if (!forwardProgression && i < 0)
                i = list.size()-1;
            T item = list.get(i);  //Retrieve the item at the index specified in i
            if (predicate.test(item))
                //We found an item that meets the required condition
                return callback.call(item);
            if (i == endIndex)  //If we are at the calculated end index
                break;  //Terminate the loop; no item satisfy the predicate condition
            //Iterate
            if (forwardProgression)
                i++;
            else
                i--;
        }
        return null;
    }


    private static <T> T nextOrPreviousItem(T currentItem, boolean forwardProgression, java.util.function.Predicate<T> predicate, List<T> list) {
        return nextOrPreviousItem(currentItem, forwardProgression, predicate, ((item) -> item), list);
    }


    /**Updates key/value pair in a map
    *@param map
    *the map to be updated
    *@param key
    *the key to be sought for
    *@param newKey
    *the new key value
    *@param <K>
    *the type of key and newKey
    *@param value
    *the value input
    *@param <V>
    *the type of the value
    *@return true, if replacement was made
    *and false otherwise
    */
    public static <K, V> boolean updateMap(Map<K, V> map, K key, K newKey, V value) {
        if (!map.containsKey(key))
            return false;
        //Instantiate another map object with the entries passed in map
        Map<K, V> mapClone = new LinkedHashMap<>(map);
        //Clear the contents of map
        map.clear();
        Set<Map.Entry<K, V>> set = mapClone.entrySet();
        //Let's put the items back into map
        for (Map.Entry<K, V> entry: set) {
            if (entry.getKey().equals(key))
                //Insert the change here
                map.put(newKey, value);
            else
                map.put(entry.getKey(), entry.getValue());
        }
        return true;
    }

    /**Converts byte figures
    *to the most appropriate unit alternative
    *@param bytes,
    *the byte figure to be converted
    *@throws IllegalArgumentException
    *if the number of bytes is less than 0
    *@return a string of the derived figure and unit
    */
    public static String convertFromByte(long bytes) {
        if (bytes < 0)
            throw new IllegalArgumentException("Number of bytes cannot be less than 0");
        //Create an array of available units
        //in order of the smallest to the highest
        //The index of each unit in the array is reflective of the power to which 1000 will be raised, (the valid range of each unit)
        String[] units = FILE_UNIT;
        int unitIndex = 0;
        double byts = bytes;
        int j = 0;
        if ((byts - Math.pow(1000, units.length)) < 0) {
            //Byte figure is below available time units
            //we need to loop through the contents of units
            //To figure what unit range the figure falls
            for (int i = 1; i <= units.length; i++) {
                //The value of i is the power to which 1000 will be raised
                if ((byts - Math.pow(1000, i)) < 0) {
                    byts = byts / Math.pow(1000, i-1);
                    unitIndex = i-1;
                    j = i - 1;
                    break;
                }
            }
        }
        String byteString = trimTrailingCharacter(String.format("%.2f", byts), '0');
        byteString = (byteString.endsWith("."))? byteString.substring(0, byteString.length()-1): byteString;
        if (Double.parseDouble(byteString) == 1000 && j < units.length-1) {
            byteString = "1";
            unitIndex = ++j;
        }
        return formatByteConversion(byteString, unitIndex);
    }

    private static String formatByteConversion(String string, int index) {
        double size = Double.parseDouble(string);
        if (string.contains("."))
            return FileMedia.resource.formatMessage(FILE_UNIT_PATTERN, Double.parseDouble(string), ((int)size == 1)? FILE_UNIT[index]: FILE_UNITS[index]);
        return FileMedia.resource.formatMessage(FILE_UNIT_PATTERN, Integer.parseInt(string), ((int)size == 1)? FILE_UNIT[index]: FILE_UNITS[index]);
    }

    /**Concats 2 arrays of same object contents
    *@param array1
    *first of the 2 passed arrays
    *@param array2
    *second of the 2 passed arrays
    *@param <T>
    *the type of the elements in both arrays
    *@throws NullPointerException if  1 or both arrays are null
    *@return an array of the contents of both arrays
    */
    public static <T> T[] concatArrays(T[] array1, T[] array2) {
        if (array1 == null || array2 == null)
            throw new NullPointerException("Null arrays are not allowed");
        //Create a new array equal to the lenths of both array1 and array2
        T[] array3 = Arrays.copyOf(array1, array1.length + array2.length);
        //At this point, the contents of array1 have been copied into array3, (from index 0, inclusive, through array1.length, exclusive)
        //We now need to copy the contents of array2 to array3 as well, beginning at index array1.length
        System.arraycopy(array2, 0, array3, array1.length, array2.length);
        return array3;
    }

    /**Checks for the number of occurrences of an item in a list
    *@param name
    *the item to be sought for
    *@param names
    *the list on which the search is to be performed
    *@param <T>
    *the type of name and the elements in the list
    *@return the number of name occurrences
    */
    public static <T> int nameOccurrenceSize(T name, List<T> names) {
        int i = 0;
        for (T item: names) {
            if (item.equals(name))
                i++;
        }
        return i;
    }

    /**Checks for the number of occurrences of an item in a vararg array
    *@param name
    *the item to be sought for
    *@param names
    *the array on which the search is to be performed
    *@param <T>
    *the type of name and the the elements in the array
    *@return the number of name occurrences
    */
    @SafeVarargs
    public static <T> int nameOccurrenceSize(T name, T... names) {
        return nameOccurrenceSize(name, Arrays.<T>asList(names));
    }

    /**Compares 2 objects to ascertain their equality
    *@param item1
    *first of the items
    *@param item2
    *second and last of the items
    *@param <T>
    *the type of the 2 items
    *@return true if the objects contain similar contents
    *and return false otherwise
    */
    public static <T> boolean ofEqualContent(T item1, T item2) {
        if (item1 == item2)  //Same object
            return true;
        //If at least 1 of the items is null
        if (item1 == null || item2 == null)
            return (item1 == null)? item2 == null: item1 == null;
        //Else
        return item1.equals(item2);
    }

    /**Returns a valid value between 2 integer numbers, the numbers themselves inclusive.
    *@param from
    *the lower of the 2 numbers
    *@param to
    *the higher of the 2 numbers
    *@param value
    *the value to return if it falls between the expected range
    *@param defaultValue
    *the value to return if value doesn't fall between the expected range
    *In the case where both value and defaultValue don't fall within range,
    *the lower of the 2 extreme numbers is returned.
    *@return a value within the range of lower value and the higher value.
    */
    public static int rangeValue(int from, int to, int value, int defaultValue) {
        int min = Math.min(from, to), max = Math.max(from, to);
        if (value >= min && value <= max)
            return value;
        return rangeValue(min, max, defaultValue, min);
    }

    /**Returns a valid value between 2 double numbers, the numbers themselves inclusive.
    *@param from
    *the lower of the 2 numbers
    *@param to
    *the higher of the 2 numbers
    *@param value
    *the value to return if it falls between the expected range
    *@param defaultValue
    *the value to return if value doesn't fall between the expected range
    *In the case where both value and defaultValue don't fall within range,
    *the lower of the 2 extreme numbers is returned.
    *@return a value within the range of lower value and the higher value.
    */
    public static double rangeValue(double from, double to, double value, double defaultValue) {
        double min = Math.min(from, to), max = Math.max(from, to);
        if (value >= min && value <= max)
            return value;
        return rangeValue(min, max, defaultValue, min);
    }

    /**Returns the highest number among an array of integer values.
    *@param values
    *the numbers to cycle through.
    */
    public static int maxValue(int... values) {
        int value = values[0];
        for (int i = 1; i < values.length; i++)
            value = Math.max(value, values[i]);
        return value;
    }

    /**Returns the highest number among an array of double values.
    *@param values
    *the numbers to cycle through.
    */
    public static double maxValue(double... values) {
        double value = values[0];
        for (int i = 1; i < values.length; i++)
            value = Math.max(value, values[i]);
        return value;
    }

    /**Returns the lowest number among an array of integer values.
    *@param values
    *the numbers to cycle through.
    */
    public static int minValue(int... values) {
        int value = values[0];
        for (int i = 1; i < values.length; i++)
            value = Math.min(value, values[i]);
        return value;
    }

    /**Returns the lowest number among an array of double values.
    *@param values
    *the numbers to cycle through.
    */
    public static double minValue(double... values) {
        double value = values[0];
        for (int i = 1; i < values.length; i++)
            value = Math.min(value, values[i]);
        return value;
    }

}