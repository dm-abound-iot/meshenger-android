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
    private int versionClicked = 0;

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

        findViewById(R.id.versionTv).setOnClickListener(v -> {
            versionClicked += 1;
            if (versionClicked < 4) {
                Toast.makeText(this, (4 - versionClicked) + " Clicks left for Development Mode", Toast.LENGTH_SHORT).show();
            } else {
                if (!MainService.instance.getSettings().getDevelopmentMode()) {
                    MainService.instance.getSettings().setDevelopmentMode(true);
                    MainService.instance.saveDatabase();
                }
                Toast.makeText(this, "Development Mode Active", Toast.LENGTH_SHORT).show();
            }
        });

        //bindService(new Intent(this, MainService.class), this, Service.BIND_AUTO_CREATE);
    }
/*
    @Override
    protected void onDestroy() {
        unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        this.binder = (MainService.MainBinder) iBinder;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        this.binder = null;
    }
 */
}
