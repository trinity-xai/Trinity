package edu.jhuapl.trinity.utils.loaders;

import java.nio.MappedByteBuffer;

/**
 *
 * @author Sean Phillips
 */
public class CounterThread extends Thread {
    private long count;
    private MappedByteBuffer buffer;

    private byte letter;

    public CounterThread(MappedByteBuffer buffer, byte letter) {
        this.buffer = buffer;
        this.letter = letter;
    }

    @Override
    public void run() {
        while (buffer.hasRemaining()) {
            if (buffer.get() == letter){
                count++;
            }
        }
    }

    public long count() {
        return count;
    }
}
