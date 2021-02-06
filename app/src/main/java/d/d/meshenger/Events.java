package d.d.meshenger;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.libsodium.jni.Sodium;

import java.io.Serializable;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import d.d.meshenger.call.DirectRTCClient;


public class Events {
    private List<Event> events;

    public Events() {
        events = new ArrayList<Event>();
    }

    public List<Event> getEventList() {
        return events;
    }

    public List<Event> getEventListCopy() {
        return new ArrayList<>(events);
    }

    public void clear() {
        events.clear();
    }

    public void addEvent(Contact contact, DirectRTCClient.CallDirection callDirection, Event.CallType callType) {
        InetSocketAddress last_working = contact.getLastWorkingAddress();
        events.add(new Event(
            contact.getPublicKey(),
                (last_working != null) ? last_working.getAddress() : null,
            callDirection,
            callType
        ));
        //LocalBroadcastManager.getInstance(this.service).sendBroadcast(new Intent("refresh_event_list"));
    }
}
