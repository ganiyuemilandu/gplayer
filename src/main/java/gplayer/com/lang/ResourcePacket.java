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

public class ResourcePacket extends BaseResourcePacket {
    public ResourcePacket() {}

    public ResourcePacket(String file) {
        super(file);
    }

    @Override
    public java.io.InputStream getResource() {
        return this.getClass().getResourceAsStream("/resources/properties/FileMediaResource.properties");
    }

    @Override
    public String defaultArraySeparator() {
        return ", ";
    }

    @Override
    public String localeName() {
        return "";
    }

}