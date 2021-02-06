package d.d.meshenger;

import java.net.InetAddress;
import java.util.Date;

import d.d.meshenger.call.DirectRTCClient.CallDirection;


class Event {
    enum CallType {
        UNKNOWN,
        ACCEPTED,
        DECLINED,
        MISSED,
        ERROR
    };

    byte[] pubKey;
    InetAddress address; // may be null in case the call attempt failed
	CallDirection callDirection;
    CallType callType;
    Date date;
    // duration?

    public Event(byte[] pubKey, InetAddress address, CallDirection callDirection, CallType callType) {
        this.pubKey = pubKey;
        this.address = address;
        this.callDirection = callDirection;
        this.callType = callType;
        this.date = new Date();
    }

    public boolean isMissedCall() {
        return callDirection == CallDirection.INCOMING && (callType != CallType.ACCEPTED);
    }
}
