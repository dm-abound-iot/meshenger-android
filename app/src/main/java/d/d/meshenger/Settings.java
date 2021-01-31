package d.d.meshenger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class Settings {
    private String username;
    private byte[] secretKey;
    private byte[] publicKey;
    private boolean nightMode;
    private boolean blockUnknown;
    private boolean developmentMode;

    // new settings
    private boolean sendVideo;
    private boolean receiveVideo;
    private boolean sendAudio;
    private boolean receiveAudio;
    private boolean autoAcceptCall;

    private List<String> addresses;
    // ICE (Interactive Connectivity Establishment) servers implement STUN and TURN
    private List<String> iceServers;

    public Settings() {
        this.username = "";
        this.secretKey = null;
        this.publicKey = null;
        this.nightMode = false;
        this.blockUnknown = false;
        this.developmentMode = false;
        this.sendVideo = true;
        this.receiveVideo = true;
        this.sendAudio = true;
        this.receiveAudio = true;
        this.autoAcceptCall = false;
        this.addresses = new ArrayList<>();
        this.iceServers = new ArrayList<>();
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(byte[] secretKey) {
        this.secretKey = secretKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean getNightMode() {
        return nightMode;
    }

    public void setNightMode(boolean nightModeEnabled) {
        this.nightMode = nightModeEnabled;
    }

    public boolean getBlockUnknown() {
        return blockUnknown;
    }

    public void setBlockUnknown(boolean blockUnknownEnabled) {
        this.blockUnknown = blockUnknownEnabled;
    }

    public boolean getDevelopmentMode() {
        return developmentMode;
    }

    public void setDevelopmentMode(boolean developmentModeEnabled) {
        this.developmentMode = developmentModeEnabled;
    }

    public boolean getSendVideo() {
        return sendVideo;
    }

    public void setSendVideo(boolean sendVideo) {
        this.sendVideo = sendVideo;
    }

    public boolean getReceiveVideo() {
        return receiveVideo;
    }

    public void setReceiveVideo(boolean receiveVideo) {
        this.receiveVideo = receiveVideo;
    }

    public boolean getSendAudio() {
        return sendAudio;
    }

    public void setSendAudio(boolean sendAudio) {
        this.sendAudio = sendAudio;
    }

    public boolean getReceiveAudio() {
        return receiveAudio;
    }

    public void setReceiveAudio(boolean receiveAudio) {
        this.receiveAudio = receiveAudio;
    }

    public boolean getAutoAcceptCall() {
        return autoAcceptCall;
    }

    public void setAutoAcceptCall(boolean autoAcceptCall) {
        this.autoAcceptCall = autoAcceptCall;
    }

    public List<String> getAddresses() {
        return this.addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    public void addAddress(String address) {
        for (String addr : this.getAddresses()) {
            if (addr.equalsIgnoreCase(address)) {
                Log.w("Settings", "Try to add duplicate address: " + addr);
                return;
            }
        }
        this.addresses.add(address);
    }

    public List<String> getIceServers() {
        return this.iceServers;
    }

    public void setIceServers(List<String> iceServers) {
        this.iceServers = iceServers;
    }

    public static Settings importJSON(JSONObject obj) throws JSONException {
        Settings s = new Settings();
        s.username = obj.getString("username");
        s.secretKey = Utils.hexStringToByteArray(obj.getString("secret_key"));
        s.publicKey = Utils.hexStringToByteArray(obj.getString("public_key"));
        s.nightMode = obj.getBoolean("night_mode");
        s.blockUnknown = obj.getBoolean("block_unknown");
        s.developmentMode = obj.getBoolean("development_mode");
        s.sendVideo = obj.getBoolean("send_video");
        s.receiveVideo = obj.getBoolean("receive_video");
        s.sendAudio = obj.getBoolean("send_audio");
        s.receiveAudio = obj.getBoolean("receive_audio");
        s.autoAcceptCall = obj.getBoolean("auto_accept_call");

        JSONArray addresses = obj.getJSONArray("addresses");
        for (int i = 0; i < addresses.length(); i += 1) {
            s.addresses.add(addresses.getString(i));
        }

        JSONArray iceServers = obj.getJSONArray("ice_servers");
        for (int i = 0; i < iceServers.length(); i += 1) {
            s.iceServers.add(iceServers.getString(i));
        }

        return s;
    }

    public static JSONObject exportJSON(Settings s) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("username", s.username);
        obj.put("secret_key", Utils.byteArrayToHexString(s.secretKey));
        obj.put("public_key", Utils.byteArrayToHexString(s.publicKey));
        obj.put("night_mode", s.nightMode);
        obj.put("block_unknown", s.blockUnknown);
        obj.put("development_mode", s.developmentMode);
        obj.put("send_video", s.sendVideo);
        obj.put("receive_video", s.receiveVideo);
        obj.put("send_audio", s.sendAudio);
        obj.put("receive_audio", s.receiveAudio);
        obj.put("auto_accept_call", s.autoAcceptCall);

        JSONArray addresses = new JSONArray();
        for (int i = 0; i < s.addresses.size(); i += 1) {
            addresses.put(s.addresses.get(i));
        }
        obj.put("addresses", addresses);

        JSONArray iceServers = new JSONArray();
        for (int i = 0; i < s.iceServers.size(); i += 1) {
            iceServers.put(s.iceServers.get(i));
        }
        obj.put("ice_servers", iceServers);

        return obj;
    }

    public Contact getOwnContact() {
        return new Contact(this.username, this.publicKey, this.addresses);
    }
}
