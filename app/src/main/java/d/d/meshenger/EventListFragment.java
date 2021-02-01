package d.d.meshenger;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.os.Looper.getMainLooper;


public class EventListFragment extends Fragment implements AdapterView.OnItemClickListener {
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

    void refreshEventList() {
        Log.d(this, "refreshEventList");

        if (this.mainActivity == null) {
            Log.d(this, "refreshEventList early return");
            return;
        }

        new Handler(getMainLooper()).post(() -> {
            List<Event> events = MainService.instance.getEvents().getEventListCopy();
            List<Contact> contacts = MainService.instance.getContacts().getContactListCopy(); // EventListFragment.this.mainActivity.binder.getContactsCopy();

            Log.d(this, "refreshEventList update: " + events.size());
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
                Contact contact = MainService.instance.getContacts().getContactByPublicKey(event.pubKey); // EventListFragment.this.mainActivity.binder.getContactByPublicKey(event.pubKey);

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
        Contact contact = MainService.instance.getContacts().getContactByPublicKey(event.pubKey);
        if (contact != null) {
            contact.setBlocked(blocked);
            MainService.instance.saveDatabase();
        } else {
            // unknown contact
        }
    }

    private void showAddDialog(Event event) {
        log("showAddDialog");

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

            String address = Utils.getGeneralizedAddress(event.address);
            MainService.instance.getContacts().addContact(
                new Contact(name, event.pubKey, Arrays.asList(address))
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        log("onItemClick");
        Event event = this.eventListAdapter.getItem(i);

        String address = Utils.getGeneralizedAddress(event.address);
        Contact contact = new Contact("", event.pubKey, Arrays.asList(address));
        contact.setLastWorkingAddress(Utils.parseInetSocketAddress(address, MainService.serverPort));

        Intent intent = new Intent(this.mainActivity, CallActivity.class);
        intent.setAction("ACTION_OUTGOING_CALL");
        intent.putExtra("EXTRA_CONTACT", contact);
        startActivity(intent);
    }

    private static void log(String s) {
        Log.d(EventListFragment.class.getSimpleName(), s);
    }
}
