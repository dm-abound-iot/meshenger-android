package d.d.meshenger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;


class ContactListAdapter extends ArrayAdapter<Contact> {
    private static final String TAG = "ContactListAdapter";
    private List<Contact> contacts;
    private Context context;

    private LayoutInflater inflater;

    public ContactListAdapter(@NonNull Context context, int resource, @NonNull List<Contact> contacts) {
        super(context, resource, contacts);
        this.contacts = contacts;
        this.context = context;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Contact contact = contacts.get(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_contact, null);
        }

        ((TextView) convertView.findViewById(R.id.contact_name)).setText(contact.getName());

        //if (contact.getState() != Contact.State.UNKNOWN) {
            //convertView.findViewById(R.id.contact_waiting).setVisibility(View.GONE); // no animation
            ImageView stateView = convertView.findViewById(R.id.contact_state);
            stateView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    contact.setState(Contact.State.UNKNOWN);
                    // TODO: send ping
                }
            });

            //stateView.setVisibility(View.VISIBLE);
            Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint p = new Paint();

            switch (contact.getState()) {
                case ONLINE:
                    p.setColor(0xFF7AE12D); // green
                    float pc = (System.currentTimeMillis() - contact.getStateLastUpdated()) / Contact.STATE_TIMEOUT;
                    if (pc >= 0.0 && pc <= 1.0) {
                        p.setColor(0xFFFCBA03); // orange
                        //canvas.drawCircle(100, 100, 70, p);
                        canvas.drawArc(
                        100, // left 
                        100, // top
                        100, // right 
                        100, // bottom 
                        0, // startAngle
                        360 * pc, // sweepAngle
                        false, // useCenter
                        p);
                    }
                    break;
                case OFFLINE:
                    p.setColor(0xFFEC3E3E); // red
                    break;
                default:
                    p.setColor(0xFFB5B5B5); // gray
                    break;
            }

            canvas.drawCircle(100, 100, 100, p);

            if (contact.getBlocked()) {
                // draw smaller red circle on top
                p.setColor(0xFFEC3E3E); // red
                canvas.drawCircle(100, 100, 70, p);
            }

            stateView.setImageBitmap(bitmap);
        //}
/*
        if (contact.recent) {
            contact.recent = false;
            ScaleAnimation anim = new ScaleAnimation(0f, 1f, 0f, 1f);
            anim.setDuration(1000);
            convertView.setAnimation(anim);
        }
*/
        return convertView;
    }
}
