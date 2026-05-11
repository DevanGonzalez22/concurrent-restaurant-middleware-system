package io.grpc.examples.helloworld;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TcpWire {
    public static RestaurantProto.ProtocolMessage readMessage(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);

        int len;
        try {
            len = dis.readInt();
        } catch (EOFException eof) {
            return null;
        }

        if (len <= 0 || len > 10 * 1024 * 1024) {
            throw new IOException("Invalid message length: " + len);
        }

        byte[] buffer = new byte[len];
        dis.readFully(buffer);

        return RestaurantProto.ProtocolMessage.parseFrom(buffer);
    }

    public static void writeMessage(OutputStream out, RestaurantProto.ProtocolMessage msg) throws IOException {
        byte[] payload = msg.toByteArray();

        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(payload.length);
        dos.write(payload);
        dos.flush();
    }
}
