package tn.rnu.eniso.fwk.chat.common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String from;
    private final String text;
    private final long timestamp;

    public Message(String from, String text) {
        this(from, text, System.currentTimeMillis());
    }

    public Message(String from, String text, long timestamp) {
        this.from = from;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String from() {
        return from;
    }

    public String text() {
        return text;
    }

    public long timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp)) + "] " + from + ": " + text;
    }
}
