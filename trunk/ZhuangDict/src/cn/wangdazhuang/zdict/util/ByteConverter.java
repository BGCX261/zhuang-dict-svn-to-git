/*
 * @(#)SignedByteConverter.java
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package cn.wangdazhuang.zdict.util;

/**
 * Convert the unsigned byte(s) into a integer or hexadecimal digit form. 
 * @author 88250
 * @version 1.1.1.5, Feb 16, 2008
 */
public class ByteConverter {

    /**
     * convert unsigned one byte into a 32-bit integer
     * @param b byte
     * @return convert result
     */
    public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    /**
     * convert unsigned one byte into a hexadecimal digit
     * @param b byte
     * @return convert result
     */
    public static String byteToHex(byte b) {
        int i = b & 0xFF;
        return Integer.toHexString(i);
    }

    /**
     * convert unsigned 4 bytes into a 32-bit integer
     * @param buf bytes buffer
     * @param pos beginning <code>byte</code>> for converting
     * @return convert result
     */
    public static long unsigned4BytesToInt(byte[] buf, int pos) {
        int ret = 0;

        ret += unsignedByteToInt(buf[pos++]) << 24;
        ret += unsignedByteToInt(buf[pos++]) << 16;
        ret += unsignedByteToInt(buf[pos++]) << 8;
        ret += unsignedByteToInt(buf[pos++]) << 0;

        return ret;
    }
    
   
}
