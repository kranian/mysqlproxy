package com.mysqlproxy.mysql;

import com.mysqlproxy.buffer.MyByteBuff;
import com.mysqlproxy.buffer.MyByteBuffAllocator;
import com.mysqlproxy.mysql.codec.Decoder;
import com.mysqlproxy.mysql.codec.Encoder;
import com.mysqlproxy.mysql.protocol.MysqlPacket;
import com.mysqlproxy.mysql.state.AuthenticatingState;
import com.mysqlproxy.mysql.state.CloseState;
import com.mysqlproxy.mysql.state.MysqlConnectionState;
import com.mysqlproxy.net.Reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by ynfeng on 2017/5/11.
 */
public abstract class MysqlConnection<T> implements Connection, StatefulConnection, NIOConnection {
    private static final int DEFAULT_BUFFER_LENGTH = 1024 * 1024 * 16;
    private MysqlConnectionState state;

    private SocketChannel socketChannel;
    private SelectionKey selectionKey;
    private Selector selector;

    private MyByteBuff readBuff;
    private MyByteBuff writeBuff;
    private Reactor reactor;
    protected MyByteBuffAllocator myByteBuffAllocator;
    private int directTransferPacketLen;
    private int directTransferPacketWriteLen;
    private boolean writeFlag;
    private boolean readFlag;
    private int packetScanPos;
    private String schema;


    public void setState(MysqlConnectionState state) {
        this.state = state;
    }

    @Override
    public MysqlConnectionState getState() {
        return state;
    }

    @Override
    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    @Override
    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    @Override
    public void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    @Override
    public Selector getSelector() {
        return selector;
    }

    @Override
    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public MyByteBuff read() throws IOException {
        MyByteBuff readBuffer = getReadBuffer();
        if (readBuffer == null) {
            readBuffer = myByteBuffAllocator.allocate(DEFAULT_BUFFER_LENGTH);
            setReadBuff(readBuffer);
        }
        readBuffer.transferFromChannel(getSocketChannel());
        return readBuffer;
    }

    public boolean flushWriteBuffer() throws IOException {
        MyByteBuff writeBuff = getWriteBuffer();
        write(writeBuff);
        if (!writeBuff.hasRemaining()) {
            return true;
        }
        return false;
    }

    public int write(MyByteBuff myByteBuff) throws IOException {
        return myByteBuff.transferToChannel(getSocketChannel());
    }

    public void enableRead() {
        getSelectionKey().interestOps(getSelectionKey().interestOps() | SelectionKey.OP_WRITE);
        readFlag = true;
    }

    public void disableRead() {
        getSelectionKey().interestOps(getSelectionKey().interestOps() & ~SelectionKey.OP_READ);
        readFlag = false;
    }

    public void disableReadAndEnableWrite() {
        getSelectionKey().interestOps((getSelectionKey().interestOps() & ~SelectionKey.OP_READ) | SelectionKey.OP_WRITE);
        readFlag = false;
        writeFlag = true;
    }

    public void disableWriteAndEnableRead() {
        getSelectionKey().interestOps((getSelectionKey().interestOps() & ~SelectionKey.OP_WRITE) | SelectionKey.OP_READ);
        writeFlag = false;
        readFlag = true;
    }

    public void enableWrite() {
        getSelectionKey().interestOps(getSelectionKey().interestOps() | SelectionKey.OP_WRITE);
        writeFlag = true;
    }

    public void disableWrite() {
        getSelectionKey().interestOps(getSelectionKey().interestOps() & ~SelectionKey.OP_WRITE);
    }

    public MysqlPacket readPacket(Decoder<MysqlPacket> decoder) throws IOException {
        MyByteBuff buff = read();
        return decoder.decode(buff);
    }

    public void writePacket(MysqlPacket packet, Encoder<MysqlPacket> encoder) throws IOException {
        MyByteBuff buff = getWriteBuffer();
        if (buff == null) {
            buff = myByteBuffAllocator.allocate(DEFAULT_BUFFER_LENGTH);
        }
        encoder.encode(packet, buff);
        setWriteBuff(buff);
        enableWrite();
    }

    @Override
    public MyByteBuff getReadBuffer() {
        return readBuff;
    }

    @Override
    public MyByteBuff getWriteBuffer() {
        return writeBuff;
    }

    public boolean isDirectTransferComplete() {
        return directTransferPacketLen <= directTransferPacketWriteLen;
    }

    public void setReadBuff(MyByteBuff readBuff) {
        if (this.readBuff != null && this.readBuff != readBuff) {
            myByteBuffAllocator.recyle(this.readBuff);
        }
        this.readBuff = readBuff;
    }

    public void setWriteBuff(MyByteBuff writeBuff) {
        if (this.writeBuff != null && this.writeBuff != writeBuff) {
            myByteBuffAllocator.recyle(this.writeBuff);
        }
        this.writeBuff = writeBuff;
    }

    public void setMyByteBuffAllocator(MyByteBuffAllocator myByteBuffAllocator) {
        this.myByteBuffAllocator = myByteBuffAllocator;
    }

    public Reactor getReactor() {
        return reactor;
    }

    public void setReactor(Reactor reactor) {
        this.reactor = reactor;
    }

    public int getDirectTransferPacketLen() {
        return directTransferPacketLen;
    }

    public void setDirectTransferPacketLen(int directTransferPacketLen) {
        this.directTransferPacketLen = directTransferPacketLen;
    }

    public int getDirectTransferPacketWriteLen() {
        return directTransferPacketWriteLen;
    }

    public void setDirectTransferPacketWriteLen(int directTransferPacketWriteLen) {
        this.directTransferPacketWriteLen = directTransferPacketWriteLen;
    }

    public boolean isWriteMode() {
        return writeFlag;
    }

    public boolean isReadMode() {
        return readFlag;
    }

    public int getPacketScanPos() {
        return packetScanPos;
    }

    public void setPacketScanPos(int packetScanPos) {
        this.packetScanPos = packetScanPos;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
}
