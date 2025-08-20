package com.elixirpay.elixirpaycat;

import java.io.IOException;

public interface IPrinter {
    void setBuffer(byte[] buffer);
    void print() throws IOException;
}