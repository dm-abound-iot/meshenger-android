package d.d.meshenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.json.JSONObject;
import org.libsodium.jni.Sodium;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import d.d.meshenger.call.CallActivity;
import d.d.meshenger.call.DirectRTCClient;
//import d.d.meshenger.call.PacketWriter;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;


public class MainService extends Service implements Runnable {
    private static final String TAG = "MainService";
    public static MainService instance = null;
    public static DirectRTCClient currentCall = null;

    private Database database = null;
    private boolean first_start = false;
    private String database_path = "";
    private String database_password = "";
    private ServerSocket server;
    private volatile boolean run = true;
    //private MainBinder mainBinder = new MainBinder(this);

    private final IBinder mBinder = new LocalBinder();

    public static final int serverPort = 10001;
    private static final int NOTIFICATION = 42;

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        this.instance = this;
        database_path = this.getFilesDir() + "/database.bin";

        // handle incoming connections
        new Thread(this).start();
    }

    public void loadDatabase() {
        try {
            if ((new File(database_path)).exists()) {
                // open existing database
                database = Database.load(database_path, database_password);
                first_start = false;
            } else {
                // create new database
                database = new Database();
                first_start = true;
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public void replaceDatabase(Database database) {
        if (database != null) {
            if (this.database == null) {
                this.database = database;
            } else {
                this.database = database;
                saveDatabase();
            }
        }
    }

    public boolean isFirstStart() {
        return first_start;
    }

    public String getDatabasePassword() {
        return database_password;
    }

    public void setDatabasePassword(String password) {
        database_password = password;
    }

    public Database getDatabase() {
        return database;
    }

    public Settings getSettings() {
        return database.getSettings();
    }

    public Contacts getContacts() {
        return database.getContacts();
    }

    public Events getEvents() {
        return database.getEvents();
    }

    public void saveDatabase() {
        try {
            Database.store(database_path, database, database_password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        run = false;

        // The database might be null here if no correct
        // database password was supplied to open it.

        if (database != null) {
            try {
                Database.store(database_path, database, database_password);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // shutdown listening socket and say goodbye
        if (database != null && server != null && server.isBound() && !server.isClosed()) {
            try {
                /*
                byte[] ownPublicKey = getSettings().getPublicKey();
                byte[] ownSecretKey = getSettings().getSecretKey();
                String message = "{\"action\": \"status_change\", \"status\", \"offline\"}";

                for (Contact contact : getContacts().getContactList()) {
                    if (contact.getState() == Contact.State.OFFLINE) {
                        continue;
                    }

                    // TODO: request security tocken to prevent replay attack?
                    byte[] encrypted = Crypto.encryptMessage(message, contact.getPublicKey(), ownPublicKey, ownSecretKey);
                    if (encrypted == null) {
                        continue;
                    }

                    Socket socket = null;
                    try {
                        socket = contact.createSocket();
                        if (socket == null) {
                            continue;
                        }

                        PacketWriter pw = new PacketWriter(socket);
                        pw.writeMessage(encrypted);
                        socket.close();
                    } catch (Exception e) {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (Exception ee) {
                                // ignore
                            }
                        }
                    }
                }
                */
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (database != null) {
            // zero keys from memory
            database.onDestroy();
        }
    }

    private void showNotification() {
        String channelId = "meshenger_service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(channelId, "Meshenger Background Service", NotificationManager.IMPORTANCE_DEFAULT);
            chan.setLightColor(Color.RED);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            service.createNotificationChannel(chan);
        }

        // start MainActivity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Context mActivity = getApplicationContext();
        Notification notification = new NotificationCompat.Builder(mActivity, channelId)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_logo)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo_small))
                .setPriority(PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentText(getResources().getText(R.string.listen_for_incoming_calls))
                .setContentIntent(pendingNotificationIntent)
                .build();

        startForeground(NOTIFICATION, notification);
    }

    final static String START_FOREGROUND_ACTION = "START_FOREGROUND_ACTION";
    final static String STOP_FOREGROUND_ACTION = "STOP_FOREGROUND_ACTION";

    public static void start(Context ctx) {
        Intent startIntent = new Intent(ctx, MainService.class);
        startIntent.setAction(START_FOREGROUND_ACTION);
        ContextCompat.startForegroundService(ctx, startIntent);
    }

    public static void stop(Context ctx) {
        Intent stopIntent = new Intent(ctx, MainService.class);
        stopIntent.setAction(STOP_FOREGROUND_ACTION);
        ctx.startService(stopIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            // ignore
        } else if (intent.getAction().equals(START_FOREGROUND_ACTION)) {
            Log.d(TAG, "Received Start Foreground Intent");
            showNotification();
        } else if (intent.getAction().equals(STOP_FOREGROUND_ACTION)) {
            Log.d(TAG, "Received Stop Foreground Intent");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION);
            stopForeground(true);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

/*
    private void handleClient(MainBinder binder, Socket socket) {
        // just a precaution
        if (this.database == null) {
            return;
        }

        byte[] clientPublicKey = new byte[Sodium.crypto_sign_publickeybytes()];
        byte[] ownSecretKey = this.database.getSettings().getSecretKey();
        byte[] ownPublicKey = this.database.getSettings().getPublicKey();

        try {
            PacketWriter pw = new PacketWriter(socket);
            PacketReader pr = new PacketReader(socket);
            Contact contact = null;

            InetSocketAddress remote_address = (InetSocketAddress) socket.getRemoteSocketAddress();
            log("incoming connection from " + remote_address);

            while (true) {
                byte[] request = pr.readMessage();
                if (request == null) {
                    break;
                }

                String decrypted = Crypto.decryptMessage(request, clientPublicKey, ownPublicKey, ownSecretKey);
                if (decrypted == null) {
                    log("decryption failed");
                    break;
                }

                if (contact == null) {
                    // search for contact identity
                    for (Contact c : this.database.getContacts()) {
                        if (Arrays.equals(c.getPublicKey(), clientPublicKey)) {
                            contact = c;
                        }
                    }

                    if (contact == null && this.database.getSettings().getBlockUnknown()) {
                        if (this.currentCall != null) {
                            log("block unknown contact => decline");
                            this.currentCall.decline();
                        }
                        break;
                    }

                    if (contact != null && contact.getBlocked()) {
                        if (this.currentCall != null) {
                            log("blocked contact => decline");
                            this.currentCall.decline();
                        }
                        break;
                    }

                    if (contact == null) {
                        // unknown caller
                        contact = new Contact("", clientPublicKey.clone(), new ArrayList<>());
                    }
                }

                // suspicious change of identity during connection...
                if (!Arrays.equals(contact.getPublicKey(), clientPublicKey)) {
                    log("suspicious change of identity");
                    continue;
                }

                // remember last good address (the outgoing port is random and not the server port)
                contact.setLastWorkingAddress(
                    new InetSocketAddress(remote_address.getAddress(), MainService.serverPort)
                );

                JSONObject obj = new JSONObject(decrypted);
                String action = obj.optString("action", "");

                switch (action) {
                    case "call": {
                        // someone calls us
                        log("got call...");
                        String offer = obj.getString("offer");
                        //this.currentCall = new RTCCall(this, binder, contact, socket, offer);

                        // respond that we accept the call

                        byte[] encrypted = Crypto.encryptMessage("{\"action\":\"ringing\"}", contact.getPublicKey(), ownPublicKey, ownSecretKey);
                        pw.writeMessage(encrypted);

                        Intent intent = new Intent(this, CallActivity.class);
                        intent.setAction("ACTION_INCOMING_CALL");
                        intent.putExtra("EXTRA_SDP_OFFER", offer);
                        intent.putExtra("EXTRA_CONTACT_NAME", contact.getName());
                        intent.putExtra("EXTRA_CONTACT_ADDRESS", remote_address.getAddress());
                        intent.putExtra("EXTRA_CONTACT_PORT", MainService.serverPort);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        // we still use the socket here to send 
                        return;
                    }
                    case "ping": {
                        log("got ping...");
                        // someone wants to know if we are online
                        binder.setContactState(contact.getPublicKey(), Contact.State.ONLINE);
                        byte[] encrypted = Crypto.encryptMessage("{\"action\":\"pong\"}", contact.getPublicKey(), ownPublicKey, ownSecretKey);
                        pw.writeMessage(encrypted);
                        break;
                    }
                    case "status_change": {
                        if (obj.optString("status", "").equals("offline")) {
                            binder.setContactState(contact.getPublicKey(), Contact.State.OFFLINE);
                        } else {
                            log("Received unknown status_change: " + obj.getString("status"));
                        }
                    }
                }
            }

            log("call disconnected");
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("call_declined"));

        } catch (Exception e) {
            e.printStackTrace();
            log("socket disconnected (exception)");
            if (this.currentCall != null) {
                this.currentCall.decline();
            }
        }

        // zero out key
        Arrays.fill(clientPublicKey, (byte) 0);
    }
*/
    // runs in a thread
    @Override
    public void run() {
        try {
            // wait until database is ready
            while (this.database == null && this.run) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    break;
                }
            }

            server = new ServerSocket(serverPort);
            LocalBinder binder = (LocalBinder) onBind(null);

            while (this.run) {
                try {
                    Socket socket = server.accept();
                    if (MainService.currentCall == null) {
                        MainService.currentCall = new DirectRTCClient(socket);
                        Intent intent = new Intent(this, CallActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Thread.sleep(50); // mitigate DDOS attack
                    } else {
                        // TODO: record missed call?
                        socket.close();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Handler(getMainLooper()).post(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            stopSelf();
            return;
        }
    }

    /*
    static class PingRunnable implements Runnable {
        private List<Contact> contacts;
        byte[] ownPublicKey;
        byte[] ownSecretKey;
        MainBinder binder;

        PingRunnable(MainBinder binder, List<Contact> contacts, byte[] ownPublicKey, byte[] ownSecretKey) {
            this.binder = binder;
            this.contacts = contacts;
            this.ownPublicKey = ownPublicKey;
            this.ownSecretKey = ownSecretKey;
        }

        @Override
        public void run() {
            // otherwise we trigger calls
            for (Contact contact : contacts) {
                Socket socket = null;
                byte[] publicKey = contact.getPublicKey();

                try {
                    socket = contact.createSocket();
                    if (socket == null) {
                        this.binder.setContactState(publicKey, Contact.State.OFFLINE);
                        continue;
                    }

                    PacketWriter pw = new PacketWriter(socket);
                    PacketReader pr = new PacketReader(socket);

                    log("send ping to " + contact.getName());

                    byte[] encrypted = Crypto.encryptMessage("{\"action\":\"ping\"}", publicKey, ownPublicKey, ownSecretKey);
                    if (encrypted == null) {
                        socket.close();
                        continue;
                    }

                    pw.writeMessage(encrypted);

                    byte[] request = pr.readMessage();
                    if (request == null) {
                        socket.close();
                        continue;
                    }

                    String decrypted = Crypto.decryptMessage(request, publicKey, ownPublicKey, ownSecretKey);
                    if (decrypted == null) {
                        log("decryption failed");
                        socket.close();
                        continue;
                    }

                    JSONObject obj = new JSONObject(decrypted);
                    String action = obj.optString("action", "");
                    if (action.equals("pong")) {
                        log("got pong");
                        this.binder.setContactState(publicKey, Contact.State.ONLINE);
                    }

                    socket.close();
                } catch (Exception e) {
                    this.binder.setContactState(publicKey, Contact.State.OFFLINE);
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (Exception ee) {
                            // ignore
                        }
                    }
                    e.printStackTrace();
                }
            }

            log("send refresh_contact_list");
            LocalBroadcastManager.getInstance(this.binder.getContext()).sendBroadcast(new Intent("refresh_contact_list"));
        }

        private void log(String data) {
            Log.d(TAG, data);
        }
    }
*/
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    /*
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }*/
}
