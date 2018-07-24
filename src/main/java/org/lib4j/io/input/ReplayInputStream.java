/* Copyright (c) 2016 lib4j
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.lib4j.io.input;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReplayInputStream extends FilterInputStream {
  private class ReplayByteArrayOutputStream extends ByteArrayOutputStream {
    private int length;
    private int mark;

    public ReplayByteArrayOutputStream(final int size) {
      super(size);
    }

    public ReplayByteArrayOutputStream() {
      super();
    }

    @Override
    public void write(final int c) {
      super.write(c);
      ++length;
    }

    @Override
    public void write(final byte[] c, final int off, final int len) {
      super.write(c, off, len);
      length += len;
    }

    public int read() {
      if (!isReadable())
        throw new IllegalStateException("Stream has not been closed");

      return count == length ? -1 : buf[count++];
    }

    public int read(final byte[] b) {
      return read(b, 0, b.length);
    }

    public int read(final byte[] b, final int off, final int len) {
      if (!isReadable())
        throw new IllegalStateException("Stream has not been closed");

      final int check = length - count - len;
      final int length = check < 0 ? len + check : len;
      System.arraycopy(buf, count, b, off, length);
      count += length;
      return length;
    }

    public long skip(final long n) {
      if (!isReadable())
        throw new IllegalStateException("Stream has not been closed");

      if (n <= 0)
        return 0;

      final long check = length - count - n;
      final long length = check < 0 ? n + check : n;
      count += length;
      return length;
    }

    public void mark() {
      mark = count;
    }

    @Override
    public void reset() {
      length = count;
      count = mark;
    }

    public boolean isReadable() {
      return length != count;
    }

    @Override
    public void close() throws IOException {
      count = 0;
    }
  }

  protected final ReplayByteArrayOutputStream buffer;

  public ReplayInputStream(final InputStream in, final int size) {
    super(in);
    this.buffer = new ReplayByteArrayOutputStream(size);
  }

  public ReplayInputStream(final InputStream in) {
    super(in);
    this.buffer = new ReplayByteArrayOutputStream();
  }

  @Override
  public int read() throws IOException {
    if (buffer.isReadable())
      return buffer.read();

    final int by = in.read();
    if (by != -1)
      buffer.write(by);

    return by;
  }

  @Override
  public int read(final byte[] b) throws IOException {
    if (buffer.isReadable())
      return buffer.read(b);

    final int by = in.read(b);
    if (by > 0)
      buffer.write(b, 0, by);

    return by;
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    if (buffer.isReadable())
      return buffer.read(b, off, len);

    final int by = in.read(b, off, len);
    if (by > 0)
      buffer.write(b, off, by);

    return by;
  }

  @Override
  public int available() throws IOException {
    return super.available() + buffer.length - buffer.size();
  }

  @Override
  public long skip(final long n) throws IOException {
    if (buffer.isReadable())
      return buffer.skip(n);

    for (int i = 0; i < n; i++)
      if (read() == -1)
        return i;

    return n;
  }

  @Override
  public void close() throws IOException {
    in.close();
    buffer.close();
  }

  @Override
  public void mark(final int readlimit) {
    in.mark(readlimit);
    buffer.mark();
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  @Override
  public void reset() throws IOException {
    buffer.reset();
  }
}