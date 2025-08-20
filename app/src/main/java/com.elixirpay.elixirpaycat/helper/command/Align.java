package com.elixirpay.elixirpaycat.helper.command;

public enum Align {
    LEFT((byte)0x00),
    CENTER((byte)0x01),
    RIGHT((byte)0x02);

    byte val;

    Align(byte val) {
        this.val = val;
    }

    public byte value() {
        return val;
    }
}
