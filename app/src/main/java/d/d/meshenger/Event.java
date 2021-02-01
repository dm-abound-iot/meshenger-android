package d.d.meshenger;

import java.net.InetAddress;
import java.util.Date;


class Event {
    enum Type {
        OUTGOING_UNKNOWN,
        OUTGOING_ACCEPTED,
        OUTGOING_DECLINED,
        OUTGOING_MISSED,
        OUTGOING_ERROR,
        INCOMING_UNKNOWN,
        INCOMING_ACCEPTED,
        INCOMING_DECLINED,
        INCOMING_MISSED,
        INCOMING_ERROR
    };

    byte[] pubKey;
    InetAddress address; // may be null in case the call attempt failed
    Type type;
    Date date;

    public Event(byte[] pubKey, InetAddress address, Type type) {
        this.pubKey = pubKey;
        this.address = address;
        this.type = type;
        this.date = new Date();
    }

    public boolean isMissedCall() {
        return this.type == Event.Type.INCOMING_UNKNOWN
            || this.type == Event.Type.INCOMING_MISSED
            || this.type == Event.Type.INCOMING_ERROR;
    }
}
