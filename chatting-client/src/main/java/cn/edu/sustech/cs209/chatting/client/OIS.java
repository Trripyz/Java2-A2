package cn.edu.sustech.cs209.chatting.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class OIS extends ObjectInputStream {

    public OIS(InputStream in) throws IOException {
        super(in);
    }

    @Override
    public void readStreamHeader(){

    }
}
