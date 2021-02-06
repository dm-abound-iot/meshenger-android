package d.d.meshenger;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;


public class AboutActivity extends MeshengerActivity {
    private final String TAG = "AboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setTitle(getResources().getString(R.string.menu_about));

        ((TextView) findViewById(R.id.versionTv)).setText(
            Utils.getApplicationVersion(this)
        );

        findViewById(R.id.licenseTV).setOnClickListener(v -> {
            Intent intent = new Intent(this, LicenseActivity.class);
            startActivity(intent);
        });
    }
}
