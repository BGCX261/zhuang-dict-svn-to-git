/* @(#)DictZipHeader.java
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

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Deflater;

/**
 * DictZip file header.
 * <p>
 * More information please refer to doc/StarDictFileFormat file.
 * And, the source original author is Ho Ngoc Duc, please visit his <tt>JDictd Project</tt> 
 * web page: 
 *  <a href="http://www.informatik.uni-leipzig.de/~duc/Java/JDictd/">JDictd's homepage</a>
 * </p>
 * @author Ho Ngoc Duc
 * @author 88250
 * @version 1.1.1.0, Feb 16, 2008
 * 
 */
public class DictZipHeader {

    int headerLength;

    int[] chunks;

    private int[] offsets;

    int extraLength;

    byte subfieldID1, subfieldID2;

    int subfieldLength;

    int subfieldVersion;

    private int chunkLength;

    int chunkCount;

    /**
     * GZIP header magic number & file header flags
     */
    final static int GZIP_MAGIC = 0x8b1f;

    final static int FTEXT = 1;	// Extra text

    final static int FHCRC = 2;	// Header CRC

    final static int FEXTRA = 4;	// Extra field

    final static int FNAME = 8;	// File name

    final static int FCOMMENT = 16;	// File comment

    /**
     * Read the file header.
     * @param fileName DictZip file name
     * @return the file header
     * @throws java.io.IOException
     */
    public static DictZipHeader readHeader(String fileName) throws IOException {
        DictZipHeader h = new DictZipHeader();
        CRC32 crc = new CRC32();
        InputStream in = new FileInputStream(fileName);
        readHeader(h, in, crc);
        in.close();
        return h;
    }

    /**
     * Read the file header.
     * @param h file header
     * @param is input stream
     * @param crc CRC32 checkout
     * @throws java.io.IOException
     */
    public static void readHeader(DictZipHeader h, InputStream is, CRC32 crc)
            throws IOException {
        CheckedInputStream in = new CheckedInputStream(is, crc);
        crc.reset();

        // Check header magic
        if (readUShort(in) != GZIP_MAGIC) {
            throw new IOException("Not the GZIP format !!!!");
        }
        // Check compression method
        if (readUByte(in) != 8) {
            throw new IOException("Unsupported compression method !!!!");
        }
        // Read flags
        int flg = readUByte(in);
        // Skip MTIME, XFL, and OS fields
        skipBytes(in, 6);
        h.headerLength = 10;
        /* 2 bytes header magic, 1 byte compression method, 1 byte flags
        4 bytes time, 1 byte extra flags, 1 byte OS */
        // Optional extra field
        if ((flg & FEXTRA) == FEXTRA) {
            h.extraLength = readUShort(in);
            h.headerLength += h.extraLength + 2;
            h.subfieldID1 = (byte) readUByte(in);
            h.subfieldID2 = (byte) readUByte(in);
            h.subfieldLength = readUShort(in); // 2 bytes subfield length
            h.subfieldVersion = readUShort(in); // 2 bytes subfield version
            h.setChunkLength(readUShort(in)); // 2 bytes chunk length
            h.chunkCount = readUShort(in); // 2 bytes chunk count
            h.chunks = new int[h.chunkCount];
            for (int i = 0; i < h.chunkCount; i++) {
                h.chunks[i] = readUShort(in);
            }
        }
        // Skip optional file name
        if ((flg & FNAME) == FNAME) {
            while (readUByte(in) != 0) {
                h.headerLength++;
            }
            h.headerLength++;
        }
        // Skip optional file comment
        if ((flg & FCOMMENT) == FCOMMENT) {
            while (readUByte(in) != 0) {
                h.headerLength++;
            }
            h.headerLength++;
        }
        // Check optional header CRC
        if ((flg & FHCRC) == FHCRC) {
            int v = (int) crc.getValue() & 0xffff;
            if (readUShort(in) != v) {
                throw new IOException("Corrupt GZIP header");
            }
            h.headerLength += 2;
        }
        h.initOffsets();
    }

    /**
     * Reads unsigned byte.
     * @param in <code>InputStream</code>
     * @return <code>byte</code> type data
     * @throws java.io.IOException
     */
    public static int readUByte(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }

    /**
     * Reads unsigned integer in Intel byte order.
     * @param in <code>InputStream</code>
     * @return <code>long</code> type data
     * @throws java.io.IOException
     */
    public static long readUInt(InputStream in) throws IOException {
        long s = readUShort(in);
        return ((long) readUShort(in) << 16) | s;
    }

    /**
     * Reads unsigned short in Intel byte order.
     * @param in <code>InputStream</code>
     * @return <code>int</code> type data
     * @throws java.io.IOException
     */
    public static int readUShort(InputStream in) throws IOException {
        int b = readUByte(in);
        return (readUByte(in) << 8) | b;
    }

    /**
     * Skips bytes of input data blocking until all bytes are skipped.
     * Does not assume that the input stream is capable of seeking.
     * @param in
     * @param n
     * @throws java.io.IOException
     */
    public static void skipBytes(InputStream in, int n) throws IOException {
        byte[] buf = new byte[128];
        while (n > 0) {
            int len = in.read(buf, 0, n < buf.length ? n : buf.length);
            if (len == -1) {
                throw new EOFException();
            }
            n -= len;
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nHeader length = " + headerLength);
        sb.append("\nSubfield ID = " + (char) subfieldID1 + (char) subfieldID2);
        sb.append("\nSubfield length = " + subfieldLength);
        sb.append("\nSubfield version = " + subfieldVersion);
        sb.append("\nChunk length = " + getChunkLength());
        sb.append("\nNumber of chunks = " + chunkCount);
        return sb.toString();
    }

    /**
     * Writes GZIP member header.
     * @param h
     * @param out
     * @throws java.io.IOException
     */
    public static void writeHeader(DictZipHeader h, OutputStream out) throws IOException {
        writeShort(out, GZIP_MAGIC);	    // Magic number
        out.write(Deflater.DEFLATED);       // Compression method (CM)
        out.write(FEXTRA);                  // Flags (FLG)
        writeInt(out, 0);                   // Modification time (MTIME)
        out.write(0);                       // Extra flags (XFL)
        out.write(0);                       // Operating system (OS)
        writeShort(out, h.extraLength);     // extra field length
        out.write(h.subfieldID1);
        out.write(h.subfieldID2);           // subfield ID
        writeShort(out, h.extraLength);     // extra field length
        writeShort(out, h.subfieldVersion); // extra field length
        writeShort(out, h.getChunkLength());     // extra field length
        writeShort(out, h.chunkCount);      // extra field length
        for (int i = 0; i < h.chunkCount; i++) {
            writeShort(out, h.chunks[i]);
        }
    }

    /**
     * Writes integer in Intel byte order.
     * @param out
     * @param i
     * @throws java.io.IOException
     */
    public static void writeInt(OutputStream out, int i) throws IOException {
        writeShort(out, i & 0xffff);
        writeShort(out, (i >> 16) & 0xffff);
    }

    /**
     * Writes short integer in Intel byte order.
     * @param out
     * @param s
     * @throws java.io.IOException
     */
    public static void writeShort(OutputStream out, int s) throws IOException {
        out.write(s & 0xff);
        out.write((s >> 8) & 0xff);
    }

    private void initOffsets() {
        setOffsets(new int[chunks.length]);
        getOffsets()[0] = headerLength;
        for (int i = 1; i < chunks.length; i++) {
            getOffsets()[i] = getOffsets()[i - 1] + chunks[i - 1];
        }
    }

	public void setChunkLength(int chunkLength) {
		this.chunkLength = chunkLength;
	}

	public int getChunkLength() {
		return chunkLength;
	}

	public void setOffsets(int[] offsets) {
		this.offsets = offsets;
	}

	public int[] getOffsets() {
		return offsets;
	}
}
