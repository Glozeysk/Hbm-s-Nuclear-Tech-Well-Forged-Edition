package com.hbm.packet.threading;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public abstract class ThreadedPacket implements IMessage {
    private ByteBuf compiledBuffer;

    private void compile0() {
        if (compiledBuffer != null) {
            return;
        }

        ByteBuf newBuf = PooledByteBufAllocator.DEFAULT.directBuffer();
        try {
            this.toBytes(newBuf);
            this.compiledBuffer = newBuf;
        } catch (Throwable t) {
            newBuf.release();
            this.compiledBuffer = null;
            throw t;
        }
    }

    public abstract void fromBytes(ByteBuf buf);

    public long hbm$getSyncKey() {
        return 0L;
    }

    public synchronized final void releaseBuffer() {
        if (compiledBuffer != null) {
            compiledBuffer.release();
            compiledBuffer = null;
        }
    }

    public synchronized final ByteBuf consumeCompiledBuffer() {
        ByteBuf buf = this.compiledBuffer;
        this.compiledBuffer = null;
        if (buf == null) {
            this.compile0();
            buf = this.compiledBuffer;
            this.compiledBuffer = null;
        }
        return buf;
    }

    public synchronized final ByteBuf getCompiledBuffer() {
        if (compiledBuffer == null) {
            this.compile0();
        }
        return compiledBuffer;
    }
}