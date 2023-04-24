package cn.edu.sustech.cs209.chatting.client;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class OOS extends ObjectOutputStream {

    public OOS(OutputStream out) throws IOException {
        super(out);
    }

    @Override
    public void writeStreamHeader() throws IOException {
        super.reset();
    }
}
