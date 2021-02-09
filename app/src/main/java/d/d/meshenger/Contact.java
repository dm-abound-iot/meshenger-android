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


public class Contact implements Serializable {
    private static final String TAG = "Contact";
    enum State { ONLINE, OFFLINE, UNKNOWN };

    private String name;
    private byte[] pubkey;
    private boolean blocked;
    private List<String> addresses;

    // contact state
    private State state = State.UNKNOWN;
    private long state_last_updated = System.currentTimeMillis();
    public static long STATE_TIMEOUT = 60 * 1000;

    // last working address (use this address next connection
    // and for unknown contact initialization)
    private InetSocketAddress last_working_address = null;

    public Contact(String name, byte[] pubkey, List<String> addresses) {
        this.name = name;
        this.pubkey = pubkey;
        this.blocked = false;
        this.addresses = addresses;
    }

    private Contact() {
        this.name = "";
        this.pubkey = null;
        this.blocked = false;
        this.addresses = new ArrayList<>();
    }

    public State getState() {
        if ((state_last_updated + STATE_TIMEOUT) > System.currentTimeMillis()) {
            state = Contact.State.UNKNOWN;
        }
        return state;
    }

    public void setState(State state) {
        this.state_last_updated = System.currentTimeMillis();
        this.state = state;
    }

    public long getStateLastUpdated() {
        return this.state_last_updated;
    }

    public List<String> getAddresses() {
        return this.addresses;
    }

    public void addAddress(String address) {
        if (address.isEmpty()) {
            return;
        }

        for (String addr : this.addresses) {
            if (addr.equalsIgnoreCase(address)) {
                return;
            }
        }
        this.addresses.add(address);
    }

    public byte[] getPublicKey() {
        return pubkey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    // set good address to try first next time,
    // this is not stored in the database
    public void setLastWorkingAddress(InetSocketAddress address) {
        this.last_working_address = address;
    }

    public InetSocketAddress getLastWorkingAddress() {
        return this.last_working_address;
    }

    public static JSONObject exportJSON(Contact contact, boolean all) throws JSONException {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();

        object.put("name", contact.name);
        object.put("public_key", Utils.byteArrayToHexString(contact.pubkey));

        for (String address : contact.getAddresses()) {
            array.put(address);
        }
        object.put("addresses", array);

        if (all) {
            object.put("blocked", contact.blocked);
        }

        return object;
    }

    public static Contact importJSON(JSONObject object, boolean all) throws JSONException {
        Contact contact = new Contact();

        contact.name = object.getString("name");
        contact.pubkey = Utils.hexStringToByteArray(object.getString("public_key"));

        if (!Utils.isValidContactName(contact.name)) {
            throw new JSONException("Invalid Name.");
        }

        if (contact.pubkey == null || contact.pubkey.length != Sodium.crypto_sign_publickeybytes()) {
            throw new JSONException("Invalid Public Key.");
        }

        JSONArray array = object.getJSONArray("addresses");
        for (int i = 0; i < array.length(); i += 1) {
            contact.addAddress(array.getString(i).toUpperCase().trim());
        }

        if (all) {
            contact.blocked = object.getBoolean("blocked");
        }

        return contact;
    }
}
