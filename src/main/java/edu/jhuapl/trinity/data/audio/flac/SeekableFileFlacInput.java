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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;


/**
 * A FLAC input stream based on a {@link RandomAccessFile}.
 */
public final class SeekableFileFlacInput extends AbstractFlacLowLevelInput {
	
	/*---- Fields ----*/
	
	// The underlying byte-based input stream to read from.
	private RandomAccessFile raf;
	
	
	
	/*---- Constructors ----*/
	
	public SeekableFileFlacInput(File file) throws IOException {
		super();
		Objects.requireNonNull(file);
		this.raf = new RandomAccessFile(file, "r");
	}
	
	
	
	/*---- Methods ----*/
	
        @Override
	public long getLength() {
		try {
			return raf.length();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
        @Override
	public void seekTo(long pos) throws IOException {
		raf.seek(pos);
		positionChanged(pos);
	}
	
	
        @Override
	protected int readUnderlying(byte[] buf, int off, int len) throws IOException {
		return raf.read(buf, off, len);
	}
	
	
	// Closes the underlying RandomAccessFile stream (very important).
        @Override
	public void close() throws IOException {
		if (raf != null) {
			raf.close();
			raf = null;
			super.close();
		}
	}
	
}
