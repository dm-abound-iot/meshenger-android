package d.d.meshenger;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import d.d.meshenger.call.CallActivity;
import d.d.meshenger.call.DirectRTCClient;

import static android.os.Looper.getMainLooper;


public class EventListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "EventListFragment";
    private MainActivity mainActivity;
    private ListView eventListView;
    private EventListAdapter eventListAdapter;
    private FloatingActionButton fabDelete;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        mainActivity = (MainActivity) getActivity();

        eventListView = view.findViewById(R.id.eventList);
        fabDelete = view.findViewById(R.id.fabDelete);
        fabDelete.setOnClickListener(v -> {
            MainService.instance.getEvents().clear();
            refreshEventList();
        });

        eventListAdapter = new EventListAdapter(mainActivity, R.layout.item_event, Collections.emptyList(), Collections.emptyList());
        eventListView.setAdapter(eventListAdapter);
        eventListView.setOnItemClickListener(this);

        refreshEventList();

        return view;
    }

    public void refreshEventList() {
        Log.d(TAG, "refreshEventList");

        if (this.mainActivity == null) {
            Log.d(TAG, "refreshEventList early return");
            return;
        }

        new Handler(getMainLooper()).post(() -> {
            List<Event> events = MainService.instance.getEvents().getEventListCopy();
            List<Contact> contacts = MainService.instance.getContacts().getContactListCopy();

            Log.d(TAG, "refreshEventList update: " + events.size());
            eventListAdapter.update(events, contacts);
            eventListAdapter.notifyDataSetChanged();
            eventListView.setAdapter(eventListAdapter);

            eventListView.setOnItemLongClickListener((AdapterView<?> adapterView, View view, int i, long l) -> {
                Event event = events.get(i);
                PopupMenu menu = new PopupMenu(EventListFragment.this.mainActivity, view);
                Resources res = getResources();
                String add = res.getString(R.string.add);
                String block = res.getString(R.string.block);
                String unblock = res.getString(R.string.unblock);
                Contact contact = MainService.instance.getContacts().getContactByPublicKey(event.publicKey);

                // allow to add unknown caller
                if (contact == null) {
                    menu.getMenu().add(add);
                }

                // we can only block/unblock contacts
                // (or we need to need maintain a separate bocklist)
                if (contact != null) {
                    if (contact.getBlocked()) {
                        menu.getMenu().add(unblock);
                    } else {
                        menu.getMenu().add(block);
                    }
                }

                menu.setOnMenuItemClickListener((MenuItem menuItem) -> {
                    String title = menuItem.getTitle().toString();
                    if (title.equals(add)) {
                        showAddDialog(event);
                    } else if (title.equals(block)) {
                        setBlocked(event, true);
                    } else if (title.equals(unblock)) {
                        setBlocked(event, false);
                    }
                    return false;
                });
                menu.show();
                return true;
            });
        });
    }

    private void setBlocked(Event event, boolean blocked) {
        Contact contact = MainService.instance.getContacts().getContactByPublicKey(event.publicKey);
        if (contact != null) {
            contact.setBlocked(blocked);
            MainService.instance.saveDatabase();
        } else {
            // unknown contact
        }
    }

    private void showAddDialog(Event event) {
        Log.d(TAG, "showAddDialog");

        Dialog dialog = new Dialog(this.mainActivity);
        dialog.setContentView(R.layout.dialog_add_contact);

        EditText nameEditText = dialog.findViewById(R.id.nameEditText);
        Button exitButton = dialog.findViewById(R.id.ExitButton);
        Button okButton = dialog.findViewById(R.id.OkButton);

        okButton.setOnClickListener((View v) -> {
            String name = nameEditText.getText().toString();

            if (name.isEmpty()) {
                Toast.makeText(this.mainActivity, R.string.contact_name_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            if (MainService.instance.getContacts().getContactByName(name) != null) {
                Toast.makeText(this.mainActivity, R.string.contact_name_exists, Toast.LENGTH_LONG).show();
                return;
            }

            String address = getGeneralizedAddress(InetSocketAddress.createUnresolved(event.address, 0).getAddress());
            MainService.instance.getContacts().addContact(
                new Contact(name, event.publicKey, Arrays.asList(address), false)
            );

            Toast.makeText(this.mainActivity, R.string.done, Toast.LENGTH_SHORT).show();

            refreshEventList();

            // close dialog
            dialog.dismiss();
        });

        exitButton.setOnClickListener((View v) -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    /*
     * When adding an unknown contact, try to
     * extract a MAC address from the IP address.
     */
    private static String getGeneralizedAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            // if the IPv6 address contains a MAC address, take that.
            byte[] mac = AddressUtils.getEUI64MAC((Inet6Address) address);
            if (mac != null) {
                return Utils.bytesToMacAddress(mac);
            }
        }

        return address.getHostAddress();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Log.d(TAG, "onItemClick");
        Event event = this.eventListAdapter.getItem(i);

        if (event.address == null) {
            Toast.makeText(this.getContext(), "No address set for call!", Toast.LENGTH_SHORT).show();
            return;
        }

        Contact contact = MainService.instance.getContacts().getContactByPublicKey(event.publicKey);
        if (contact == null) {
            String address = getGeneralizedAddress(InetSocketAddress.createUnresolved(event.address, 0).getAddress());
            contact = new Contact("", event.publicKey, Arrays.asList(address), false);
            contact.addAddress(address);
        }

        if (DirectRTCClient.createOutgoingCall(contact)) {
            Intent intent = new Intent(getContext(), CallActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
