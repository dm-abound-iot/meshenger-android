package d.d.meshenger;

import com.codekidlabs.storagechooser.StorageChooser;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.File;


public class BackupActivity extends MeshengerActivity implements
        StorageChooser.OnSelectListener, StorageChooser.OnCancelListener {
    private static final String TAG = "BackupActivity";
    private static final int REQUEST_PERMISSION = 0x01;
    private AlertDialog.Builder builder;
    private Button exportButton;
    private Button importButton;
    private ImageButton selectButton;
    private TextView pathEditText;
    private TextView passwordEditText;

    private void showErrorMessage(String title, String message) {
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        initViews();
    }

    private void initViews() {
        builder = new AlertDialog.Builder(this);
        importButton = findViewById(R.id.ImportButton);
        exportButton = findViewById(R.id.ExportButton);
        selectButton = findViewById(R.id.SelectButton);
        pathEditText = findViewById(R.id.PathEditText);
        passwordEditText = findViewById(R.id.PasswordEditText);

        importButton.setOnClickListener((View v) -> {
            if (Utils.hasReadPermission(BackupActivity.this)) {
                importDatabase();
            } else {
                Utils.requestReadPermission(BackupActivity.this, REQUEST_PERMISSION);
            }
        });

        exportButton.setOnClickListener((View v) -> {
            if (Utils.hasReadPermission(BackupActivity.this) && Utils.hasWritePermission(BackupActivity.this)) {
                exportDatabase();
            } else {
                Utils.requestWritePermission(BackupActivity.this, REQUEST_PERMISSION);
                Utils.requestReadPermission(BackupActivity.this, REQUEST_PERMISSION);
            }
        });

        selectButton.setOnClickListener((View v) -> {
            if (Utils.hasReadPermission(BackupActivity.this)) {
                StorageChooser chooser = new StorageChooser.Builder()
                    .withActivity(this)
                    .withFragmentManager(getFragmentManager())
                    .allowCustomPath(true)
                    .setType(StorageChooser.DIRECTORY_CHOOSER)
                    .build();
                chooser.show();

                // get path that the user has chosen
                chooser.setOnSelectListener(this);
                chooser.setOnCancelListener(this);
            } else {
                Utils.requestReadPermission(BackupActivity.this, REQUEST_PERMISSION);
            }
        });
    }

    private void exportDatabase() {
        String password = passwordEditText.getText().toString();
        String path = pathEditText.getText().toString();

        if (path == null || path.isEmpty()) {
            showErrorMessage(getResources().getString(R.string.error), getResources().getString(R.string.no_path_selected));
            return;
        }

        if ((new File(path)).isDirectory() || path.endsWith("/")) {
            showErrorMessage(getResources().getString(R.string.error), getResources().getString(R.string.no_file_name));
            return;
        }

        if ((new File(path)).exists()) {
            showErrorMessage(getResources().getString(R.string.error), getResources().getString(R.string.file_already_exists));
            return;
        }

        try {
            Database db = MainService.instance.getDatabase();
            Database.store(path, db, password);
            Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            showErrorMessage(getResources().getString(R.string.error), e.getMessage());
        }
    }

    private void importDatabase() {
        String password = passwordEditText.getText().toString();
        String path = pathEditText.getText().toString();

        if (path == null || path.isEmpty()) {
            showErrorMessage(getResources().getString(R.string.error), getResources().getString(R.string.no_path_selected));
            return;
        }

        if ((new File(path)).isDirectory() || path.endsWith("/")) {
            showErrorMessage(getResources().getString(R.string.error), getResources().getString(R.string.no_file_name));
            return;
        }

        if (!(new File(path)).exists()) {
            showErrorMessage(getResources().getString(R.string.error), getResources().getString(R.string.file_does_not_exist));
            return;
        }

        try {
            Database db = Database.load(path, password);
            MainService.instance.replaceDatabase(db);
            Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            showErrorMessage(getResources().getString(R.string.error), e.toString());
        }
    }

    // for StorageChooser
    @Override
    public void onSelect(String path) {
        if ((new File(path)).isDirectory()) {
            // append slash
            if (!path.endsWith("/")) {
                path += "/";
            }
            path += "meshenger-backup.json";
        }
        pathEditText.setText(path);
    }

    // for StorageChooser
    @Override
    public void onCancel() {
        // nothing to do
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (Utils.allGranted(grantResults)) {
                    // permissions granted
                    Toast.makeText(getApplicationContext(), "Permissions granted - please try again.", Toast.LENGTH_SHORT).show();
                } else {
                    showErrorMessage("Permissions Required", "Action cannot be performed.");
                }
                break;
        }
    }
}
