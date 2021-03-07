package d.d.meshenger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
        InetAddress address = contact.getLastWorkingAddress();
        String address_str = (address != null) ? address.toString() : null;
        Event event = new Event(contact.getPublicKey(), address_str, callDirection, callType);

        if (events.size() > 100) {
            // remove first item
            events.remove(0);
        }

        events.add(event);
    }

    public static Events fromJSON(JSONObject obj) throws JSONException {
        Events events = new Events();

        JSONArray array = obj.getJSONArray("entries");
        for (int i = 0; i < array.length(); i += 1) {
            events.events.add(
                Event.fromJSON(array.getJSONObject(i))
            );
        }

        // sort by date / oldest first
        Collections.sort(events.events, (Event lhs, Event rhs) -> {
            return lhs.date.compareTo(rhs.date);
        });

        return events;
    }

    public static JSONObject toJSON(Events events) throws JSONException {
        JSONObject obj = new JSONObject();

        JSONArray array = new JSONArray();
        for (Event event : events.events) {
            array.put(Event.toJSON(event));
        }
        obj.put("entries", array);

        return obj;
    }
}
