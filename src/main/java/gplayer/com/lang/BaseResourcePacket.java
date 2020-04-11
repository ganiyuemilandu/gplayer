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

package gplayer.com.lang;

import java.io.InputStream;
import java.text.ChoiceFormat;
import java.text.CollationKey;
import java.text.Collator;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

/**Base class for loading and managing locale resource bundles.*/

public abstract class BaseResourcePacket extends ResourceBundle {
    private NumberFormat numberFormatter = NumberFormat.getIntegerInstance(getLocale());
    private MessageFormat messageFormatter = new MessageFormat("", getLocale());
    private Collator collator = Collator.getInstance(getLocale());
    private Map<String, Object> map = null;
    private static Class<?> cachedResourcePacket = null;
    public BaseResourcePacket() {}

/**Creates a new instance of this class.
*@param file
*the property-formatted resource file to be loaded for use.
*/
    public BaseResourcePacket(String file) {
        Objects.requireNonNull(file);
        loadResource(gplayer.com.service.FileMedia.inputStream(resolveResourceName(localeName(), file)));
    }

/**Creates a new instance of this class.
*@param map
*the property resource to be used.
*/
    public BaseResourcePacket(Map<String, Object> map) {
        Objects.requireNonNull(map);
        this.map = map;
    }

    private void loadResource(InputStream ins) {
        try {
            Properties properties = new Properties();
            properties.load(ins);
            map = new HashMap<String, Object>();
            process(properties.propertyNames(), ((key) -> {
                Object value = properties.get(key);
                if (key.endsWith("array"))
                    map.put(key, ((String)value).split(arraySeparator(key, properties)));
                else
                    map.put(key, value);
            }));
        }
        catch (Exception ex) {}
        finally {
            closeStream(ins);
        }
    }

    private void process(Enumeration<?> enumeration, Consumer<String> consumer) {
        while (enumeration.hasMoreElements())
            consumer.accept((String)enumeration.nextElement());
    }

    private void closeStream(InputStream stream) {
        try {
            stream.close();
        }
        catch (Exception ex) {}
    }

    protected String arraySeparator(String key, Properties property) {
        try {
            return property.get(key.concat(".separator")).toString();
        }
        catch (Exception ex) {
            return defaultArraySeparator();
        }
    }

    private static String resolveResourceName(String locale, String file) {
        int index = file.indexOf('_');
        String extension = (index != -1 || locale == null || locale.isEmpty())? "": locale;
        return (extension.isEmpty())? file: file.concat((extension.startsWith("_"))? extension: "_" + extension);
    }

    public String[] getStringFamily(String key) {
        String arrayKey = key + ".array";
        if (handleGetObject(arrayKey) != null)
            return getStringArray(arrayKey);
        List<String> list = new java.util.ArrayList<>();
        list.add(getString(key));
        for (int i = 2; ; i++) {
            try {
                String str = getString(key + i);
                list.add(str);
            }
            catch (Exception ex) {
                break;
            }
        }
        String[] array = list.toArray(new String[list.size()]);
        map.put(arrayKey, array);
        return array;
    }

    public static BaseResourcePacket getPacket() {
        return getPacket(null, false);
    }

    public static BaseResourcePacket getPacket(String file) {
        return getPacket(file, true);
    }

    private synchronized static BaseResourcePacket getPacket(String resourceFile, boolean nullify) {
        if (nullify)
            Objects.requireNonNull(resourceFile);
        try {
            if (cachedResourcePacket == null)
                cachedResourcePacket = getBundle("gplayer.com.lang.ResourcePacket").getClass();
            BaseResourcePacket brp = (BaseResourcePacket) cachedResourcePacket.newInstance();
            brp.loadResource((resourceFile == null)? brp.getResource(): gplayer.com.service.FileMedia.inputStream(resolveResourceName(brp.localeName(), resourceFile)));
            return brp;
        }
        catch (Exception ex) {
            return null;
        }
    }

    public synchronized String formatMessage(String messagePattern, Object... messages) {
if (messages.length == 0)
return messagePattern;
        String pattern = messagePattern.replace("\'", "\'\'");
        if (!messageFormatter.toPattern().equals(pattern))
            messageFormatter.applyPattern(pattern);
        return messageFormatter.format(messages);
    }

    public String getAndFormatMessage(String key, Object... messages) {
        return formatMessage(getString(key), messages);
    }

    public synchronized String localizeNumber(Number number) {
        return numberFormatter.format(number);
    }

    public String getString(String key, int index) {
        Objects.requireNonNull(key);
        if (key.endsWith("array"))
            return getStringArray(key)[index];
        return getString(key);
    }

    @Override
    public Locale getLocale() {
        return (super.getLocale() == null)? Locale.ENGLISH: super.getLocale();
    }

    @Override
    public Object handleGetObject(String key) {
        Objects.requireNonNull(key);
        return (map.isEmpty())? null: map.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        Set<String> set = handleKeySet();
        if (parent != null)
            process(parent.getKeys(), ((key) -> set.add(key)));
        return java.util.Collections.enumeration(set);
    }

    @Override
    protected Set<String> handleKeySet() {
        return map.keySet();
    }

    protected abstract InputStream getResource();
    protected abstract String defaultArraySeparator();
    protected abstract String localeName();
}