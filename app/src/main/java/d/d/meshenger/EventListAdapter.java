package d.d.meshenger;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import static d.d.meshenger.call.DirectRTCClient.CallDirection.INCOMING;
import static d.d.meshenger.call.DirectRTCClient.CallDirection.OUTGOING;


class EventListAdapter extends ArrayAdapter<Event> {
    private LayoutInflater inflater;
    private List<Event> events;
    private List<Contact> contacts;
    private Context context;

    public EventListAdapter(@NonNull Context context, int resource, List<Event> events, List<Contact> contacts) {
        super(context, resource, events);

        this.events = events;
        this.contacts = contacts;
        this.context = context;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void update(List<Event> events, List<Contact> contacts) {
        this.events = events;
        this.contacts = contacts;
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public Event getItem(int position) {
        return events.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        // show list in reverse, latest element first
        Event event = this.events.get(this.events.size() - position - 1);

        if (view == null) {
            view = inflater.inflate(R.layout.item_event, null);
        }

        // find name
        String name = "";
        for (Contact contact : this.contacts) {
            if (Arrays.equals(contact.getPublicKey(), event.publicKey)) {
                name = contact.getName();
                break;
            }
        }

        TextView name_tv = view.findViewById(R.id.call_name);
        if (name.isEmpty()) {
            name_tv.setText(this.context.getResources().getString(R.string.unknown_caller));
        } else {
            name_tv.setText(name);
        }

        TextView date_tv = view.findViewById(R.id.call_date);
        if (DateUtils.isToday(event.date.getTime())) {
            SimpleDateFormat ft = new SimpleDateFormat("'Today at' hh:mm:ss");
            date_tv.setText(ft.format(event.date));
        } else {
            SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss");
            date_tv.setText(ft.format(event.date));
        }

        ImageView type_iv = view.findViewById(R.id.call_type);

        if (event.callDirection == INCOMING) {
            switch (event.callType) {
                case ACCEPTED:
                case DECLINED:
                    type_iv.setImageResource(R.drawable.call_incoming);
                    break;
                case MISSED:
                case ERROR:
                    type_iv.setImageResource(R.drawable.call_incoming_missed);
                    break;
            }
        }

        if (event.callDirection == OUTGOING) {
            switch (event.callType) {
                case ACCEPTED:
                case DECLINED:
                    type_iv.setImageResource(R.drawable.call_outgoing);
                    break;
                case MISSED:
                case ERROR:
                    type_iv.setImageResource(R.drawable.call_outgoing_missed);
                    break;
            }
        }

        TextView address_tv = view.findViewById(R.id.call_address);
        if (event.address != null) {
            address_tv.setText("(" + event.address + ")");
        } else {
            address_tv.setText("");
        }
        return view;
    }
}
