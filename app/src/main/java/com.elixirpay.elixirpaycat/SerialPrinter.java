package com.elixirpay.elixirpaycat;

import android_serialport_api.SerialPort;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SerialPrinter implements IPrinter {
    private File mFile;
    private ByteBuffer mBuffer;
    private int mBaudRate;

    public SerialPrinter(Builder builder) {
        mFile = new File(builder.tty);
        mBaudRate = builder.baudRate;
    }

    public boolean isConnected() {
        return mFile.exists();
    }

    @Override
    public void setBuffer(byte[] buffer) {
        mBuffer = ByteBuffer.wrap(buffer);
    }

    @Override
    public void print() throws IOException {
        SerialPort serialPort = new SerialPort(mFile, mBaudRate, 0);
        serialPort.getOutputStream().write(mBuffer.array());
        serialPort.close();
    }

    public static final class Builder {
        String tty;
        int baudRate;

        public Builder() {
            tty = null;
            baudRate = 115200;
        }

        public Builder tty(String tty) {
            this.tty = tty;
            return this;
        }

        public Builder baudRate(int baudRate) {
            this.baudRate = baudRate;
            return this;
        }

        public SerialPrinter build() {
            return new SerialPrinter(this);
        }
    }
}