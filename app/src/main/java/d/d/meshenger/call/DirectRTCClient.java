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

import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.PeerConnection;
import d.d.meshenger.Contact;
import d.d.meshenger.Settings;
import d.d.meshenger.R;
/**
 * Implementation of AppRTCClient that uses direct TCP connection as the signaling channel.
 * This eliminates the need for an external server. This class does not support loopback
 * connections.
 */
public class DirectRTCClient extends Thread implements AppRTCClient /*, TCPChannelClient.TCPChannelEvents*/ {
  private static final String TAG = "DirectRTCClient";

  private final ExecutorService executor;
  private AppRTCClient.SignalingEvents events;
  private final boolean isServer;
  private final CallDirection callDirection;
  private Socket socket;
  private Contact contact;
  private final Object socketLock;
  private PrintWriter out;

  private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }
  public enum CallDirection {INCOMING, OUTGOING};

  // All alterations of the room state should be done from inside the looper thread.
  private ConnectionState roomState;

  public DirectRTCClient(Socket socket) {
    this(socket, null, CallDirection.INCOMING);
  }

  public DirectRTCClient(Contact contact) {
    this(null, contact, CallDirection.OUTGOING);
  }

  private DirectRTCClient(Socket socket, Contact contact, CallDirection callDirection) {
    this.socket = socket;
    this.contact = contact;
    this.callDirection = callDirection;
    this.isServer = (socket != null);
    this.socketLock = new Object();
    this.executor = Executors.newSingleThreadExecutor();
    this.roomState = ConnectionState.NEW;
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

  @Override
  public void run() {
    Log.d(TAG, "Listening thread started...");

    BufferedReader in;

    // contact is only set for outgoing call
    if (contact != null && contact.getAddresses().isEmpty()) {
      reportError("No addresses set for contact.");
      return;
    }

    synchronized (socketLock) {
      if (!isServer) {
        Log.d(TAG, "Create outgoing socket contact.createSocket() (client)");
        for (int i = 0; i < 5 && socket == null; i += 1) {
          Log.d(TAG, "try number " + i);
          socket = contact.createSocket();
        }
      } else {
        Log.d(TAG, "Incoming socket already present (server).");
      }

      // Connecting failed, error has already been reported, just exit.
      if (socket == null) {
        reportError("Connection failed.");
        return;
      }

      try {
        out = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8")), true);
        in = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));
      } catch (IOException e) {
        reportError("Failed to open IO on rawSocket: " + e.getMessage());
        return;
      }
    }

    Log.v(TAG, "Execute onTCPConnected");
    executor.execute(() -> {
        Log.v(TAG, "Run onTCPConnected (isServer: " + isServer + ")");
        /*eventListener.*/ onTCPConnected(isServer);
    });

    while (true) {
      final String message;
      try {
        message = in.readLine();
      } catch (IOException e) {
        synchronized (socketLock) {
          // If socket was closed, this is expected.
          if (socket == null) {
            break;
          }
        }

        reportError("Failed to read from rawSocket: " + e.getMessage());
        break;
      }

      // No data received, rawSocket probably closed.
      if (message == null) {
        break;
      }

      if (contact == null) {
        // TODO: try to decrypt and set contact
      } else {
        // TODO: decrypt
      }

      // what if caller is unknown?

      executor.execute(() -> {
        Log.v(TAG, "Receive: " + message);
        /*eventListener.*/onTCPMessage(message);
      });
    }

    Log.d(TAG, "Receiving thread exiting...");

    // Close the rawSocket if it is still open.
    disconnectSocket();
    //disconnectFromRoom();
  }

  /** Closes the rawSocket if it is still open. Also fires the onTCPClose event. */
  
  private void disconnectSocket() {
    try {
      synchronized (socketLock) {
        if (socket != null) {
          socket.close();
          socket = null;
          out = null;

          executor.execute(() -> {
            /*eventListener.*/onTCPClose();
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

    // start thread
    this.start();
  }

  /**
   * Disconnects from the room.
   *
   * Runs on the looper thread.
   */
  private void disconnectFromRoomInternal() {
    roomState = ConnectionState.CLOSED;

    //if (tcpClient != null) {
      /*tcpClient.*/disconnectSocket();
    //  tcpClient = null;
    //}
    executor.shutdown();
  }

  @Override
  public void sendOfferSdp(final SessionDescription sdp) {
    if (!isServer) {
      Log.e(TAG, "we send offer as client?");
    }
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending offer SDP in non connected state.");
          return;
        }
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");
        sendMessage(json.toString());
      }
    });
  }

  @Override
  public void sendAnswerSdp(final SessionDescription sdp) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");
        sendMessage(json.toString());
      }
    });
  }

  @Override
  public void sendLocalIceCandidate(final IceCandidate candidate) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
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
      }
    });
  }

  /** Send removed Ice candidates to the other participant. */
  @Override
  public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
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
      }
    });
  }

  // -------------------------------------------------------------------
  // TCPChannelClient event handlers

  /**
   * If the client is the server side, this will trigger onConnectedToRoom.
   */
  //@Override
  private void onTCPConnected(boolean isServer) {
    if (isServer) {
      roomState = ConnectionState.CONNECTED;

      SignalingParameters parameters = new SignalingParameters(
          // Ice servers are not needed for direct connections.
          new ArrayList<>(),
          isServer, // Server side acts as the initiator on direct connections.
          null, // clientId
          null, // wssUrl
          null, // wwsPostUrl
          null, // offerSdp
          null // iceCandidates
          );
      // call to CallActivity
      events.onConnectedToRoom(parameters);
    }
  }

  //@Override
  private void onTCPMessage(String msg) {
    //String msg = ""; // dummy
    Log.d(TAG, "onTCPMessage: " + msg);
    try {
      JSONObject json = new JSONObject(msg);
      String type = json.optString("type");

      if (type.equals("candidate")) {
        events.onRemoteIceCandidate(toJavaCandidate(json));
      } else if (type.equals("remove-candidates")) {
        JSONArray candidateArray = json.getJSONArray("candidates");
        IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
        for (int i = 0; i < candidateArray.length(); ++i) {
          candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
        }
        events.onRemoteIceCandidatesRemoved(candidates);
      } else if (type.equals("answer")) {
          if (!isServer) {
            Log.e(TAG, "Dang, we are the client but got an answer?");
          }

        SessionDescription sdp = new SessionDescription(
            SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
        events.onRemoteDescription(sdp);
      } else if (type.equals("offer")) {
        SessionDescription sdp = new SessionDescription(
            SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));

        if (isServer) {
          Log.e(TAG, "Dang, we are the server but got an offer?");
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
        reportError("Unexpected TCP message: " + msg);
      }
    } catch (JSONException e) {
      reportError("TCP message JSON parsing error: " + e.toString());
    }
  }

  //@Override
  private void onTCPError(String description) {
    reportError("TCP connection error: " + description);
  }

  //@Override
  private void onTCPClose() {
    events.onChannelClose();
  }

  // --------------------------------------------------------------------
  // Helper functions.
  private void reportError(final String errorMessage) {
    Log.e(TAG, errorMessage);
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if (roomState != ConnectionState.ERROR) {
          roomState = ConnectionState.ERROR;
          events.onChannelError(errorMessage);
        }
      }
    });
  }

  /**
   * Sends a message on the socket. Should only be called on the executor thread.
   */
  private void send(String message) {
    Log.v(TAG, "Send: " + message);

    synchronized (socketLock) {
      if (out == null) {
        reportError("Sending data on closed socket.");
        return;
      }

      out.write(message + "\n");
      out.flush();
    }
  }

  private void sendMessage(final String message) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        /*tcpClient.*/send(message);
      }
    });
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
}
