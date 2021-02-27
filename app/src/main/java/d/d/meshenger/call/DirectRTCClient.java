/*
 *  Copyright 2016 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package d.d.meshenger.call;

import android.support.annotation.Nullable;
import android.util.Log;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.libsodium.jni.Sodium;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.ThreadUtils;

import d.d.meshenger.AddressUtils;
import d.d.meshenger.Contact;
import d.d.meshenger.Crypto;
import d.d.meshenger.MainService;

/*

*/
public class DirectRTCClient extends Thread implements AppRTCClient {
    private static final String TAG = "DirectRTCClient";

    private static DirectRTCClient currentCall = null;
    private static final Object currentCallLock = new Object();

    // call context for events
    private final ExecutorService executor;
    private final ThreadUtils.ThreadChecker executorThreadCheck;
    private AppRTCClient.SignalingEvents events;
    private final CallDirection callDirection;
    private final Object socketLock;
    private SocketWrapper socket;
    private final Contact contact;
    private final byte[] ownSecretKey;
    private final byte[] ownPublicKey;

    private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }
    public enum CallDirection { INCOMING, OUTGOING };

    // All alterations of the room state should be done from inside the looper thread.
    private ConnectionState roomState;

    public DirectRTCClient(@Nullable final SocketWrapper socket, final Contact contact, final CallDirection callDirection) {
        this.socket = socket;
        this.contact = contact;
        this.callDirection = callDirection;
        this.socketLock = new Object();
        this.executor = Executors.newSingleThreadExecutor();
        this.roomState = ConnectionState.NEW;
        this.ownSecretKey = MainService.instance.getSettings().getSecretKey();
        this.ownPublicKey = MainService.instance.getSettings().getPublicKey();
        executorThreadCheck = new ThreadUtils.ThreadChecker();
        executorThreadCheck.detachThread();
    }

    public Contact getContact() {
        return contact;
    }

    public void setEventListener(SignalingEvents events) {
        this.events = events;
    }

    public CallDirection getCallDirection() {
        return callDirection;
    }

    private static Socket establishConnection(InetSocketAddress[] addresses, int timeout) {
        for (InetSocketAddress address : addresses) {
            Log.d(TAG, "try address: '" + address.getAddress() + "', port: " + address.getPort());
            Socket socket = new Socket();
            try {
                // timeout to establish connection
                socket.connect(address, timeout);
                return socket;
            } catch (SocketTimeoutException e) {
                Log.d(TAG, "SocketTimeoutException: " + e);
                // ignore
            } catch (ConnectException e) {
                // device is online, but does not listen on the given port
                Log.d(TAG, "ConnectException: " + e); // probably "Connection refused"
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Log.d(TAG, "close socket()");
                socket.close();
            } catch (Exception e) {
            // ignore
            }
        }

      return null;
    }
/*
    private void addCallEvent(Event.CallType callType) {
        MainService.instance.getEvents().addEvent(contact, callDirection, callType);
    }
*/
    @Override
    public void run() {
        Log.d(TAG, "Listening thread started...");
/*
        // wait for listner to be attached
        try {
            for (int i = 0; i < 50 && listener == null; i += 1) {
                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "Wait for listener interrupted: " + e);
            return;
        }
*/
        if (events == null) {
            disconnectSocket();
            Log.e(TAG, "No listener found!");
            return;
        }

        if (callDirection == CallDirection.OUTGOING) {
            assert(contact != null);
            assert(socket == null);

            // contact is only set for outgoing call
            if (contact.getAddresses().isEmpty()) {
                reportError("No addresses set for contact.");
                return;
            }

            //synchronized (socketLock) {
            try {
                Log.d(TAG, "Create outgoing socket");
                assert(contact != null);
                assert(socket == null);

                InetSocketAddress[] addresses = AddressUtils.getAllSocketAddresses(
                        contact.getAddresses(), contact.getLastWorkingAddress(), MainService.serverPort);
                Socket socket = establishConnection(addresses, 500 /* connection timeout ms */);
                // Connecting failed, error has already been reported, just exit.
                if (socket == null) {
                    reportError("Connection failed.");
                    return;
                }

                this.socket = new SocketWrapper(socket);

                //out = new PacketWriter(socket.getOutputStream());
                //in = new PacketReader(socket.getInputStream());

                executor.execute(() -> {
                    sendMessage("{\"type\":\"call\"}");
                });
            } catch (IOException e) {
                disconnectSocket();
                //disconnectFromRoom();
                reportError("Failed to open IO on rawSocket: " + e.getMessage());
                return;
            }
            //}
        } else {
            assert(callDirection == CallDirection.INCOMING);
            assert(contact != null);
            assert(socket != null);
/*
            //synchronized (socketLock) {
            try {
                out = new PacketWriter(socket.getOutputStream());
                in = new PacketReader(socket.getInputStream());
            } catch (IOException e) {
                disconnectSocket();
                //disconnectFromRoom();
                reportError("Failed to open IO on rawSocket: " + e.getMessage());
                this.roomState = ConnectionState.ERROR;
                return;
            }
            //}
*/
            Log.v(TAG, "Execute onConnectedToRoom");
            executor.execute(() -> {
                roomState = ConnectionState.CONNECTED;

                SignalingParameters parameters = new SignalingParameters(
                    // Ice servers are not needed for direct connections.
                    new ArrayList<>(),
                    (callDirection == CallDirection.INCOMING), // Server side acts as the initiator on direct connections.
                    null, // clientId
                    null, // wssUrl
                    null, // wwsPostUrl
                    null, // offerSdp
                    null // iceCandidates
                );
                // call to CallActivity
                events.onConnectedToRoom(parameters);
            });
        }

        Log.v(TAG, "done so far");

        InetSocketAddress remote_address = (InetSocketAddress) socket.getRemoteSocketAddress();
        // remember last good address (the outgoing port is random and not the server port)
        contact.setLastWorkingAddress(
            new InetSocketAddress(remote_address.getAddress(), MainService.serverPort)
        );

        // read data
        while (true) {
            final byte[] message;
            //final String message;
            try {
                Log.d(TAG, "in.readMessage()");
                message = /*in.readLine(); */ this.socket.readMessage();
            } catch (IOException e) {
                //synchronized (socketLock) {
                    // If socket was closed, this is expected.
                //    if (socket == null) {
                //        break;
                //    }
                //}

                // using reportError will cause the executor.shutdown() and also assignment of a task afterwards
                Log.d(TAG, "Failed to read from rawSocket: " + e.getMessage());
                break;
            }

            // No data received, rawSocket probably closed.
            if (message == null) {
                Log.d(TAG, "message is null");
                // hm, call hangup?
                break;
            }

            String decrypted = Crypto.decryptMessage(message, contact.getPublicKey(), ownPublicKey, ownSecretKey);
            if (decrypted == null) {
                reportError("decryption failed");
                break;
            }

            Log.d(TAG, "decrypted: " + decrypted);
            executor.execute(() -> {
                /*eventListener.*/onTCPMessage(decrypted);
            });
        }

        Log.d(TAG, "Receiving thread exiting...");

        // Close the rawSocket if it is still open.
        disconnectSocket();
    }

    /** Closes the rawSocket if it is still open. Also fires the onTCPClose event. */

    private void disconnectSocket() {
        try {
            synchronized (socketLock) {
                //roomState = ConnectionState.CLOSED;
                if (socket != null) {
                    socket.close();
                    socket = null;
                    //out = null;

                    executor.execute(() -> {
                        events.onChannelClose();
                        ///*eventListener.*/onTCPClose();
                    });
                }
            }
        } catch (IOException e) {
            reportError("Failed to close rawSocket: " + e.getMessage());
        }
    }

    /**
    * Connects to the room, roomId in connectionsParameters is required. roomId must be a valid
    * IP address matching IP_PATTERN.
    */

    @Override
    public void connectToRoom(/*String address, int port*/) {
        //this.address = address;
        //this.port = port;

        executor.execute(() -> {
          connectToRoomInternal();
        });
    }

    @Override
    public void disconnectFromRoom() {
        executor.execute(() -> {
            disconnectFromRoomInternal();
        });
    }

    /**
    * Connects to the room.
    *
    * Runs on the looper thread.
    */
    private void connectToRoomInternal() {
        this.roomState = ConnectionState.NEW;
/*
    String endpoint = connectionParameters.roomId;

    Matcher matcher = IP_PATTERN.matcher(endpoint);
    if (!matcher.matches()) {
      reportError("roomId must match IP_PATTERN for DirectRTCClient.");
      return;
    }

    String ip = matcher.group(1);
    String portStr = matcher.group(matcher.groupCount());
    int port;

    if (portStr != null) {
      try {
        port = Integer.parseInt(portStr);
      } catch (NumberFormatException e) {
        reportError("Invalid port number: " + portStr);
        return;
      }
    } else {
      port = DEFAULT_PORT;
    }
*/
        //tcpClient = MainService.currentCall; // my addition
        //tcpClient = new TCPChannelClient.TCPSocketClient(executor, this /*, this.address, this.port*/ /*, DEFAULT_PORT*/);
        //tcpClient.start();

        // start thread / starts run() method
        if (!this.isAlive()) {
            this.start();
        } else {
            Log.w(TAG, "Thread is already running!");
        }
    }

    /**
    * Disconnects from the room.
    *
    * Runs on the looper thread.
    */
    private void disconnectFromRoomInternal() {
        executorThreadCheck.checkIsOnValidThread();
        roomState = ConnectionState.CLOSED;

        //if (tcpClient != null) {
        /*tcpClient.*/disconnectSocket();
        //  tcpClient = null;
        //}
        Log.d(TAG, "shutdown executor");
        executor.shutdown();
    }

    @Override
    public void sendOfferSdp(final SessionDescription sdp) {
        if (callDirection != CallDirection.INCOMING) {
            Log.e(TAG, "we send offer as client?");
        }
        executor.execute(() -> {
            if (roomState != ConnectionState.CONNECTED) {
                reportError("Sending offer SDP in non connected state.");
                return;
            }
            JSONObject json = new JSONObject();
            jsonPut(json, "sdp", sdp.description);
            jsonPut(json, "type", "offer");
            sendMessage(json.toString());
        });
    }

    @Override
    public void sendAnswerSdp(final SessionDescription sdp) {
        executor.execute(() -> {
            JSONObject json = new JSONObject();
            jsonPut(json, "sdp", sdp.description);
            jsonPut(json, "type", "answer");
            sendMessage(json.toString());
        });
    }

    @Override
    public void sendLocalIceCandidate(final IceCandidate candidate) {
        executor.execute(() -> {
            JSONObject json = new JSONObject();
            jsonPut(json, "type", "candidate");
            jsonPut(json, "label", candidate.sdpMLineIndex);
            jsonPut(json, "id", candidate.sdpMid);
            jsonPut(json, "candidate", candidate.sdp);

            if (roomState != ConnectionState.CONNECTED) {
                reportError("Sending ICE candidate in non connected state.");
                return;
            }
            sendMessage(json.toString());
        });
    }

    /** Send removed Ice candidates to the other participant. */
    @Override
    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        executor.execute(() -> {
            JSONObject json = new JSONObject();
            jsonPut(json, "type", "remove-candidates");
            JSONArray jsonArray = new JSONArray();
            for (final IceCandidate candidate : candidates) {
                jsonArray.put(toJsonCandidate(candidate));
            }
            jsonPut(json, "candidates", jsonArray);

            if (roomState != ConnectionState.CONNECTED) {
                reportError("Sending ICE candidate removals in non connected state.");
                return;
            }
            sendMessage(json.toString());
        });
    }

    private void onTCPMessage(String msg) {
        //String msg = ""; // dummy
        Log.d(TAG, "onTCPMessage: " + msg);
        try {
            JSONObject json = new JSONObject(msg);
            String type = json.optString("type");

            if (type.equals("call")) {
                // ignore - first encrypted of incoming call
            } else if (type.equals("candidate")) {
                events.onRemoteIceCandidate(toJavaCandidate(json));
            } else if (type.equals("remove-candidates")) {
                JSONArray candidateArray = json.getJSONArray("candidates");
                IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
                for (int i = 0; i < candidateArray.length(); i += 1) {
                    candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
                }
                events.onRemoteIceCandidatesRemoved(candidates);
            } else if (type.equals("answer")) {
                if (callDirection != CallDirection.INCOMING) {
                    Log.e(TAG, "Dang, we are the client but got an answer?");
                    reportError("Unexpected answer: " + msg);
                }

                SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
                events.onRemoteDescription(sdp);
            } else if (type.equals("offer")) {
                SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));

                if (callDirection != CallDirection.OUTGOING) {
                    Log.e(TAG, "Dang, we are the server but got an offer?");
                    reportError("Unexpected offer: " + msg);
                }

                SignalingParameters parameters = new SignalingParameters(
                    // Ice servers are not needed for direct connections.
                    new ArrayList<>(),
                    false, // This code will only be run on the client side. So, we are not the initiator.
                    null, // clientId
                    null, // wssUrl
                    null, // wssPostUrl
                    sdp, // offerSdp
                    null // iceCandidates
                );
                roomState = ConnectionState.CONNECTED;
                // call to CallActivity
                events.onConnectedToRoom(parameters);
            } else {
                reportError("Unexpected message: " + msg);
            }
        } catch (JSONException e) {
            reportError("TCP message JSON parsing error: " + e.toString());
        }
    }

/*
    //@Override
    private void onTCPError(String description) {
        reportError("TCP connection error: " + description);
    }

    //@Override
    private void onTCPClose() {
        events.onChannelClose();
    }
*/
    // --------------------------------------------------------------------
    // Helper functions.
    private void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage + " (" + roomState.name() + ")");
        executor.execute(() -> {
            if (roomState != ConnectionState.ERROR) {
                roomState = ConnectionState.ERROR;
                events.onChannelError(errorMessage);
            }
        });
    }

    private void sendMessage(final String message) {
        executorThreadCheck.checkIsOnValidThread();
        //executor.execute(() -> {
            byte[] encrypted = Crypto.encryptMessage(message, contact.getPublicKey(), ownPublicKey, ownSecretKey);
            synchronized (socketLock) {
                if (this.socket == null) { //roomState != ConnectionState.CONNECTED) {
                    reportError("Sending data on closed socket.");
                    return;
                }

                try {
                    this.socket.writeMessage(encrypted);
                } catch (IOException e) {
                    reportError("Failed to write message: " + e);
                }
            }
            ///*tcpClient.*/send(message);
        //});
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Converts a Java candidate to a JSONObject.
    private static JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        return json;
    }

    // Converts a JSON candidate to a Java object.
    private static IceCandidate toJavaCandidate(JSONObject json) throws JSONException {
        return new IceCandidate(
            json.getString("id"), json.getInt("label"), json.getString("candidate"));
    }

    public static DirectRTCClient getCurrentCall() {
        synchronized (currentCallLock) {
            return currentCall;
        }
    }

    public static void setCurrentCall(DirectRTCClient client) {
        synchronized (currentCallLock) {
            currentCall = client;
        }
    }

    public static boolean createOutgoingCall(Contact contact) {
        Log.d(TAG, "createOutgoingCall");

        if (contact == null) {
            return false;
        }

        synchronized (currentCallLock) {
            if (currentCall != null) {
                Log.w(TAG, "Cannot handle outgoing call. Call in progress!");
                return false;
            }
            currentCall = new DirectRTCClient(null, contact, DirectRTCClient.CallDirection.OUTGOING);
            return true;
        }
    }

    public static boolean createIncomingCall(Socket rawSocket) {
        Log.d(TAG, "createIncomingCall");

        if (rawSocket == null) {
            return false;
        }

        try {
            // search for contact identity
            byte[] clientPublicKeyOut = new byte[Sodium.crypto_sign_publickeybytes()];
            byte[] ownSecretKey = MainService.instance.getSettings().getSecretKey();
            byte[] ownPublicKey = MainService.instance.getSettings().getPublicKey();

            // hm, need to pass on streams?
            SocketWrapper socket = new SocketWrapper(rawSocket);
            //PacketWriter pw = new PacketWriter(socket.getOutputStream());
            //PacketReader pr = new PacketReader(socket.getInputStream());

            Log.d(TAG, "readMessage");
            byte[] request = socket.readMessage();

            if (request == null) {
                Log.d(TAG, "request is null");
                // invalid or timed out packet
                return false;
            }

            Log.d(TAG, "decrypt message");
            // receive public key of contact
            String decrypted = Crypto.decryptMessage(request, clientPublicKeyOut, ownPublicKey, ownSecretKey);
            if (decrypted == null) {
                Log.d(TAG, "decryption failed");
                return false;
            }
            Log.d(TAG, "decrypted message: " + decrypted);

            Contact contact = MainService.instance.getContacts().getContactByPublicKey(clientPublicKeyOut);

            if (contact == null) {
                Log.d(TAG, "unknown contact");
                if (MainService.instance.getSettings().getBlockUnknown()) {
                    Log.d(TAG, "block unknown contact => decline");
                    return false;
                }

                // unknown caller
                contact = new Contact("" /* unknown caller */, clientPublicKeyOut.clone(), new ArrayList<>());
            } else {
                if (contact.getBlocked()) {
                    Log.d(TAG, "blocked contact => decline");
                    return false;
                }
            }

            synchronized (currentCallLock) {
                if (currentCall != null) {
                    Log.w(TAG, "Cannot handle incoming call. Call in progress!");
                    return false;
                }
                Log.d(TAG, "create DirectRTCClient");
                currentCall = new DirectRTCClient(socket, contact, DirectRTCClient.CallDirection.INCOMING);
            }
            return true;
        } catch (IOException e) {
            if (rawSocket != null) {
                try {
                    rawSocket.close();
                } catch (IOException _e) {
                    // ignore
                }
            }
            Log.e(TAG, "exception in createIncomingCall");
            return false;
        }
    }
}
