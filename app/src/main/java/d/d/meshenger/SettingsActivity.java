package d.d.meshenger;

import android.app.Dialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;


public class SettingsActivity extends MeshengerActivity {
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        setTitle(getResources().getString(R.string.menu_settings));

        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean getIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pMgr = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            return pMgr.isIgnoringBatteryOptimizations(this.getPackageName());
        }
        return false;
    }

    private void initViews() {
        Settings settings = MainService.instance.getSettings();

        findViewById(R.id.nameLayout).setOnClickListener((View view) -> {
            showChangeNameDialog();
        });

        findViewById(R.id.addressLayout).setOnClickListener((View view) -> {
            Intent intent = new Intent(this, AddressActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.passwordLayout).setOnClickListener((View view) -> {
            showChangePasswordDialog();
        });

        findViewById(R.id.iceServersLayout).setOnClickListener((View view) -> {
            showChangeIceServersDialog();
        });

        String username = settings.getUsername();
        ((TextView) findViewById(R.id.nameTv)).setText(
            username.length() == 0 ? getResources().getString(R.string.none) : username
        );

        List<String> addresses = settings.getAddresses();
        ((TextView) findViewById(R.id.addressTv)).setText(
            addresses.size() == 0 ? getResources().getString(R.string.none) : Utils.join(addresses)
        );

        String password = MainService.instance.getDatabasePassword();
        ((TextView) findViewById(R.id.passwordTv)).setText(
            password.isEmpty() ? getResources().getString(R.string.none) : "********"
        );

        List<String> iceServers = settings.getIceServers();
        ((TextView) findViewById(R.id.iceServersTv)).setText(
            iceServers.isEmpty() ? getResources().getString(R.string.none) : Utils.join(iceServers)
        );

        boolean blockUnknown = settings.getBlockUnknown();
        CheckBox blockUnknownCB = findViewById(R.id.checkBoxBlockUnknown);
        blockUnknownCB.setChecked(blockUnknown);
        blockUnknownCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // save value
            settings.setBlockUnknown(isChecked);
            MainService.instance.saveDatabase();
        });

        boolean nightMode = MainService.instance.getSettings().getNightMode();
        CheckBox nightModeCB = findViewById(R.id.checkBoxNightMode);
        nightModeCB.setChecked(nightMode);
        nightModeCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // apply value
            AppCompatDelegate.setDefaultNightMode(
                isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );

            // save value
            settings.setNightMode(isChecked);
            MainService.instance.saveDatabase();

            // apply theme
            this.recreate();
        });

        boolean sendAudio = settings.getSendAudio();
        CheckBox sendAudioCB = findViewById(R.id.checkBoxSendAudio);
        sendAudioCB.setChecked(sendAudio);
        sendAudioCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // save value
            settings.setSendAudio(isChecked);
            MainService.instance.saveDatabase();
        });

        boolean receiveAudio = settings.getReceiveAudio();
        CheckBox receiveAudioCB = findViewById(R.id.checkBoxReceiveAudio);
        receiveAudioCB.setChecked(receiveAudio);
        receiveAudioCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // save value
            settings.setReceiveAudio(isChecked);
            MainService.instance.saveDatabase();
        });

        boolean sendVideo = settings.getSendVideo();
        CheckBox sendVideoCB = findViewById(R.id.checkBoxSendVideo);
        sendVideoCB.setChecked(sendVideo);
        sendVideoCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // save value
            settings.setSendVideo(isChecked);
            MainService.instance.saveDatabase();
        });

        boolean receiveVideo = settings.getReceiveVideo();
        CheckBox receiveVideoCB = findViewById(R.id.checkBoxReceiveVideo);
        receiveVideoCB.setChecked(receiveVideo);
        receiveVideoCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // save value
            settings.setReceiveVideo(isChecked);
            MainService.instance.saveDatabase();
        });

        boolean autoAcceptCall = settings.getAutoAcceptCall();
        CheckBox autoAcceptCallCB = findViewById(R.id.checkBoxAutoAcceptCall);
        autoAcceptCallCB.setChecked(autoAcceptCall);
        autoAcceptCallCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // save value
            settings.setAutoAcceptCall(isChecked);
            MainService.instance.saveDatabase();
        });

        boolean ignoreBatteryOptimizations = getIgnoreBatteryOptimizations();
        CheckBox ignoreBatteryOptimizationsCB = findViewById(R.id.checkBoxIgnoreBatteryOptimizations);
        ignoreBatteryOptimizationsCB.setChecked(ignoreBatteryOptimizations);
        ignoreBatteryOptimizationsCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // Only required for Android 6 or later
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                this.startActivity(intent);
            }
        });

        String settingsMode = settings.getSettingsMode();
        Spinner settingsModeSpinner = findViewById(R.id.spinnerSettingsMode);
        settingsModeSpinner.setSelection(((ArrayAdapter<CharSequence>) settingsModeSpinner.getAdapter()).getPosition(settingsMode));
        settingsModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                final TypedArray selectedValues = getResources().obtainTypedArray(R.array.settingsModeValues);
                final String settingsMode = selectedValues.getString(pos);
                settings.setSettingsMode(settingsMode);
                MainService.instance.saveDatabase();
                applySettingsMode(settingsMode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // ignore
            }
        });

        String videoCodec = settings.getVideoCodec();
        Spinner videoCodecSpinner = findViewById(R.id.spinnerVideoCodecs);
        videoCodecSpinner.setSelection(((ArrayAdapter<CharSequence>) videoCodecSpinner.getAdapter()).getPosition(videoCodec));
        videoCodecSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String videoCodec = parent.getItemAtPosition(pos).toString();
                settings.setVideoCodec(videoCodec);
                MainService.instance.saveDatabase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // ignore
            }
        });

        String audioCodec = settings.getAudioCodec();
        Spinner audioCodecSpinner = findViewById(R.id.spinnerAudioCodecs);
        audioCodecSpinner.setSelection(((ArrayAdapter<CharSequence>) audioCodecSpinner.getAdapter()).getPosition(audioCodec));
        audioCodecSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String audioCodec = parent.getItemAtPosition(pos).toString();
                settings.setAudioCodec(audioCodec);
                MainService.instance.saveDatabase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // ignore
            }
        });

        String videoResolution = settings.getVideoResolution();
        Spinner videoResolutionSpinner = findViewById(R.id.spinnerVideoResolutions);
        videoResolutionSpinner.setSelection(((ArrayAdapter<CharSequence>) videoResolutionSpinner.getAdapter()).getPosition(videoResolution));
        videoResolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                final TypedArray selectedValues = getResources().obtainTypedArray(R.array.videoResolutionsValues);
                final String videoResolution = selectedValues.getString(pos);
                settings.setVideoResolution(videoResolution);
                MainService.instance.saveDatabase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // ignore
            }
        });

        String speakerphone = settings.getSpeakerphone();
        Spinner speakerphoneSpinner = findViewById(R.id.spinnerSpeakerphone);
        speakerphoneSpinner.setSelection(((ArrayAdapter<CharSequence>) speakerphoneSpinner.getAdapter()).getPosition(speakerphone));
        speakerphoneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                final TypedArray selectedValues = getResources().obtainTypedArray(R.array.speakerphoneValues);
                final String speakerphone = selectedValues.getString(pos);
                settings.setSpeakerphone(speakerphone);
                MainService.instance.saveDatabase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // ignore
            }
        });

        applySettingsMode(settingsMode);
    }

    private void applySettingsMode(String settingsMode) {
        switch (settingsMode) {
            case "compact":
                findViewById(R.id.ignoreBatteryOptimizationsLayout).setVisibility(View.GONE);
                findViewById(R.id.iceServersLayout).setVisibility(View.GONE);
            case "advanced":
                findViewById(R.id.videoCodecsLayout).setVisibility(View.GONE);
                findViewById(R.id.audioCodecsLayout).setVisibility(View.GONE);
                findViewById(R.id.autoAcceptCallLayout).setVisibility(View.GONE);
            case "expert":
                break;
            default:
                Log.e(TAG, "Invalid settings mode: " + settingsMode);
                break;
        }
    }

    private void showChangeNameDialog() {
        Settings settings = MainService.instance.getSettings();
        String username = settings.getUsername();
        EditText et = new EditText(this);
        et.setText(username);
        et.setSelection(username.length());
        new AlertDialog.Builder(this)
            .setTitle(getResources().getString(R.string.settings_change_name))
            .setView(et)
            .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                String new_username = et.getText().toString().trim();
                if (Utils.isValidName(new_username)) {
                    settings.setUsername(new_username);
                    MainService.instance.saveDatabase();
                    initViews();
                } else {
                    Toast.makeText(this, getResources().getString(R.string.invalid_name), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(getResources().getText(R.string.cancel), null)
            .show();
    }

    private void showChangePasswordDialog() {
        String password = MainService.instance.getDatabasePassword();
        EditText et = new EditText(this);
        et.setText(password);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        et.setSelection(password.length());
        new AlertDialog.Builder(this)
            .setTitle(getResources().getString(R.string.settings_change_password))
            .setView(et)
            .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                String new_password = et.getText().toString();
                MainService.instance.setDatabasePassword(new_password);
                MainService.instance.saveDatabase();
                initViews();
            })
            .setNegativeButton(getResources().getText(R.string.cancel), null)
            .show();
    }

    private void showChangeIceServersDialog() {
        Settings settings = MainService.instance.getSettings();

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_set_ice_server);

        TextView iceServersTextView = dialog.findViewById(R.id.iceServersEditText);
        Button saveButton = dialog.findViewById(R.id.SaveButton);
        Button abortButton = dialog.findViewById(R.id.AbortButton);

        iceServersTextView.setText(Utils.join(settings.getIceServers()));

        saveButton.setOnClickListener((View v) -> {
            List<String> iceServers = Utils.split(iceServersTextView.getText().toString());
            settings.setIceServers(iceServers);

            // done
            Toast.makeText(SettingsActivity.this, R.string.done, Toast.LENGTH_SHORT).show();

            dialog.cancel();
        });

        abortButton.setOnClickListener((View v) -> {
            dialog.cancel();
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initViews();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);
    }
}
