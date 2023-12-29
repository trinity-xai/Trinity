/* 
 * FLAC library (Java)
 * 
 * Copyright (c) Project Nayuki
 * https://www.nayuki.io/page/flac-library-java
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program (see COPYING.txt and COPYING.LESSER.txt).
 * If not, see <http://www.gnu.org/licenses/>.
 */

package edu.jhuapl.trinity.data.audio.flac;

/*-
 * #%L
 * trinity-2023.11.01
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.util.Objects;


/**
 * A FLAC input stream based on a fixed byte array.
 */
public final class ByteArrayFlacInput extends AbstractFlacLowLevelInput {
	
	/*---- Fields ----*/
	
	// The underlying byte array to read from.
	private byte[] data;
	private int offset;
	
	
	
	/*---- Constructors ----*/
	
	public ByteArrayFlacInput(byte[] b) {
		super();
		data = Objects.requireNonNull(b);
		offset = 0;
	}
	
	
	
	/*---- Methods ----*/
	
        @Override
	public long getLength() {
		return data.length;
	}
	
	
        @Override
	public void seekTo(long pos) {
		offset = (int)pos;
		positionChanged(pos);
	}
	
	
        @Override
	protected int readUnderlying(byte[] buf, int off, int len) {
		if (off < 0 || off > buf.length || len < 0 || len > buf.length - off)
			throw new ArrayIndexOutOfBoundsException();
		int n = Math.min(data.length - offset, len);
		if (n == 0)
			return -1;
		System.arraycopy(data, offset, buf, off, n);
		offset += n;
		return n;
	}
	
	
	// Discards data buffers and invalidates this stream. Because this class and its superclass
	// only use memory and have no native resources, it's okay to simply let a ByteArrayFlacInput
	// be garbage-collected without calling close().
        @Override
	public void close() throws IOException {
		if (data != null) {
			data = null;
			super.close();
		}
	}
	
}
