/*
 * @(#)FileAccessor.java
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * This class extends <code>InputStream</code>, and delegate <code>RandomAccessFile</code>
 * for some random access operations. So, this class can manipulate file with "high/low level"
 * methods as <b>Stream</b> manipulatation. In other words, we can read(many methods for reading file) or write file
 * <code>byte</code> by <code>byte</code>, or line by line.
 * @author 88250
 * @version 1.1.3.6, Mar 2, 2008
 * @see java.io.InputStream
 * @see java.io.RandomAccessFile
 */
public class FileAccessor extends InputStream {

    private RandomAccessFile randomAccessFile;

    private int mark = 0;

    /**
     * Constructor with argument.
     * @param file file to access
     * @param mode access mode
     * @throws java.io.IOException
     */
    public FileAccessor(File file, String mode) throws IOException,FileNotFoundException {
        randomAccessFile = new RandomAccessFile(file, mode);
    }

    /**
     * See Also.
     * @param utfStr
     * @throws java.io.IOException
     * @see java.io.RandomAccessFile#writeUTF
     */
    public void writeUTF(String utfStr) throws IOException {
        randomAccessFile.writeUTF(utfStr);
    }

    /**
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), or a carriage return
     * followed immediately by a linefeed.
     * @return A String containing the contents of the line, not including
     *         any line-termination characters, or null if the end of the
     *         stream has been reached
     * @exception IOException
     */
    public String readLine() throws IOException {
        return randomAccessFile.readLine();
    }

    /**
     * Return the file's length.
     * @return file's length
     * @throws java.io.IOException
     */
    public int getLength() throws IOException {
        return (int) randomAccessFile.length();
    }

    /**
     * Return the current file pointer position.
     * @return current file pointer position
     * @throws java.io.IOException
     */
    public int getPos() throws IOException {
        return (int) randomAccessFile.getFilePointer();
    }

    @Override
    public int available() throws IOException {
        return getLength() - getPos();
    }

    @Override
    public void close() throws IOException {
        randomAccessFile.close();
    }

    @Override
    public synchronized void mark(int markpos) {
        try {
            mark = getPos();
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized int read() throws IOException {
        return randomAccessFile.read();
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        return randomAccessFile.read(b, off, len);
    }

    @Override
    public synchronized void reset() throws IOException {
        randomAccessFile.seek(mark);
    }

    @Override
    public long skip(long n) throws IOException {
        return (long) randomAccessFile.skipBytes((int) n);
    }

    /**
     * See Also.
     * @param pos
     * @throws java.io.IOException
     * @see java.io.RandomAccessFile#seek
     */
    public void seek(long pos) throws IOException {
        randomAccessFile.seek(pos);
    }
}
