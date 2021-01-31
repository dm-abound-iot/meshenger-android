package d.d.meshenger;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.libsodium.jni.Sodium;

import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Contact implements Serializable {
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

    private InetSocketAddress[] getAllSocketAddresses() {
        Set<InetSocketAddress> list = new HashSet<>();

        if (this.last_working_address != null) {
            list.add(this.last_working_address);
        }

        for (String address : this.addresses) {
            try {
                if (Utils.isMAC(address)) {
                    list.addAll(Utils.getAddressPermutations(address, MainService.serverPort));
                } else {
                    // also resolves domains
                    list.add(Utils.parseInetSocketAddress(address, MainService.serverPort));
                }
            } catch (Exception e) {
                Log.e(this, "invalid address: " + address);
                e.printStackTrace();
            }
        }

        for (InetSocketAddress address : list) {
            Log.d(this, "got address: " + address);
        }

        InetSocketAddress[] addresses = list.toArray(new InetSocketAddress[0]);
        //Arrays.sort(addresses);
/*
        Collections.sort(addrs, new Comparator<InetSocketAddress>() {
            @Override
            public int compare(InetSocketAddress lhs, InetSocketAddress rhs) {
                if (lhs instanceof )
                    getAddress​()
                isUnresolved()
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                return lhs.customInt > rhs.customInt ? -1 : (lhs.customInt < rhs.customInt) ? 1 : 0;
            }
        });
*/
        return addresses;
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

    private static Socket establishConnection(InetSocketAddress address, int timeout) {
        Socket socket = new Socket();
        try {
            // timeout to establish connection
            socket.connect(address, timeout);
            return socket;
        } catch (SocketTimeoutException e) {
            // ignore
        } catch (ConnectException e) {
            // device is online, but does not listen on the given port
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                // ignore
            }
        }

        return null;
    }

    /*
    * Create a connection to the contact.
    * Try/Remember the last successful address.
    */
    public Socket createSocket() {
        Socket socket = null;
        int connectionTimeout = 500;

        /*
        // try last successful address first
        if (this.last_working_address != null) {
            Log.d(this, "try latest address: " + this.last_working_address);
            socket = this.establishConnection(this.last_working_address, connectionTimeout);
            if (socket != null) {
                return socket;
            }
        }*/

        for (InetSocketAddress address : this.getAllSocketAddresses()) {
            Log.d(this, "try address: '" + address.getAddress​() + "', port: " + address.getPort());
            socket = this.establishConnection(address, connectionTimeout);
            if (socket != null) {
                return socket;
            }
        }

        return null;
    }

    // set good address to try first next time
    public void setLastWorkingAddress(InetSocketAddress address) {
        Log.d(this, "setLatestWorkingAddress: " + address);
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

        if (!Utils.isValidName(contact.name)) {
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
