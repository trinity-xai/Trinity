package edu.jhuapl.trinity.data.audio;

/*-
 * #%L
 * trinity-2023.10.03
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

import java.io.DataInputStream;
import java.io.InputStream;

public class EndianDataInputStream extends DataInputStream {
    public EndianDataInputStream(InputStream in) {
        super(in);
    }

    public String read4ByteString() throws Exception {
        byte[] bytes = new byte[4];
        readFully(bytes);
        return new String(bytes, "US-ASCII");
    }

    public short readShortLittleEndian() throws Exception {
        int result = readUnsignedByte();
        result |= readUnsignedByte() << 8;
        return (short) result;
    }

    public int readIntLittleEndian() throws Exception {
        int result = readUnsignedByte();
        result |= readUnsignedByte() << 8;
        result |= readUnsignedByte() << 16;
        result |= readUnsignedByte() << 24;
        return result;
    }

    public int readInt24BitLittleEndian() throws Exception {
        int result = readUnsignedByte();
        result |= readUnsignedByte() << 8;
        result |= readUnsignedByte() << 16;
        if ((result & (1 << 23)) == 8388608)
            result |= 0xff000000;
        return result;
    }

    public int readInt24Bit() throws Exception {
        int result = readUnsignedByte() << 16;
        result |= readUnsignedByte() << 8;
        result |= readUnsignedByte();
        return result;
    }
}
