package com.divinefreeze.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    private static final int VPN_REQUEST_CODE = 1001;
    private static final String PREFS_NAME = "FreezeAppPrefs";
    private static final String KEY_FROZEN_APPS = "frozen_apps";
    private static final String KEY_VPN_ACTIVE = "vpn_active";

    private ListView appListView;
    private EditText searchBox;
    private Button masterFreezeBtn;
    private TextView statusText;
    private SharedPreferences prefs;

    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> filteredApps = new ArrayList<>();
    private Set<String> frozenPackages = new HashSet<>();
    private boolean vpnActive = false;

    private AppListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load saved frozen apps
        Set<String> saved = prefs.getStringSet(KEY_FROZEN_APPS, new HashSet<>());
        frozenPackages = new HashSet<>(saved);
        vpnActive = prefs.getBoolean(KEY_VPN_ACTIVE, false);

        appListView = findViewById(R.id.appListView);
        searchBox = findViewById(R.id.searchBox);
        masterFreezeBtn = findViewById(R.id.masterFreezeBtn);
        statusText = findViewById(R.id.statusText);

        loadInstalledApps();

        adapter = new AppListAdapter();
        appListView.setAdapter(adapter);

        searchBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });

        appListView.setOnItemClickListener((parent, view, position, id) -> {
            AppInfo app = filteredApps.get(position);
            if (frozenPackages.contains(app.packageName)) {
                frozenPackages.remove(app.packageName);
            } else {
                frozenPackages.add(app.packageName);
            }
            saveFrozenApps();
            adapter.notifyDataSetChanged();
            updateStatus();
        });

        masterFreezeBtn.setOnClickListener(v -> {
            if (vpnActive) {
                stopFreeze();
            } else {
                if (frozenPackages.isEmpty()) {
                    Toast.makeText(this, "Select at least one app to freeze first!", Toast.LENGTH_SHORT).show();
                } else {
                    requestVpnPermission();
                }
            }
        });

        updateStatus();
        updateButton();
    }

    private void loadInstalledApps() {
        allApps.clear();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo info : packages) {
            // Only show user-installed apps + popular system apps
            boolean isUserApp = (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
            if (isUserApp) {
                AppInfo app = new AppInfo();
                app.name = pm.getApplicationLabel(info).toString();
                app.packageName = info.packageName;
                app.icon = pm.getApplicationIcon(info);
                allApps.add(app);
            }
        }

        // Sort alphabetically
        allApps.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        filteredApps = new ArrayList<>(allApps);
    }

    private void filterApps(String query) {
        filteredApps.clear();
        for (AppInfo app : allApps) {
            if (app.name.toLowerCase().contains(query.toLowerCase())) {
                filteredApps.add(app);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void requestVpnPermission() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            startFreeze();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startFreeze();
        } else {
            Toast.makeText(this, "VPN permission denied. Cannot freeze apps.", Toast.LENGTH_LONG).show();
        }
    }

    private void startFreeze() {
        Intent intent = new Intent(this, FreezeVpnService.class);
        intent.setAction(FreezeVpnService.ACTION_START);
        intent.putStringArrayListExtra(FreezeVpnService.EXTRA_PACKAGES,
                new ArrayList<>(frozenPackages));
        startService(intent);
        vpnActive = true;
        prefs.edit().putBoolean(KEY_VPN_ACTIVE, true).apply();
        updateButton();
        updateStatus();
        Toast.makeText(this, "❄️ Freeze activated!", Toast.LENGTH_SHORT).show();
    }

    private void stopFreeze() {
        Intent intent = new Intent(this, FreezeVpnService.class);
        intent.setAction(FreezeVpnService.ACTION_STOP);
        startService(intent);
        vpnActive = false;
        prefs.edit().putBoolean(KEY_VPN_ACTIVE, false).apply();
        updateButton();
        updateStatus();
        Toast.makeText(this, "✅ Apps unfrozen!", Toast.LENGTH_SHORT).show();
    }

    private void saveFrozenApps() {
        prefs.edit().putStringSet(KEY_FROZEN_APPS, frozenPackages).apply();
    }

    private void updateStatus() {
        int count = frozenPackages.size();
        if (vpnActive) {
            statusText.setText("❄️ FREEZING " + count + " app" + (count != 1 ? "s" : "")
                    + " — you appear OFFLINE to them");
            statusText.setBackgroundColor(0xFF1565C0);
        } else if (count > 0) {
            statusText.setText("✅ " + count + " app" + (count != 1 ? "s" : "")
                    + " selected — tap FREEZE to activate");
            statusText.setBackgroundColor(0xFF2E7D32);
        } else {
            statusText.setText("Tap apps below to select which ones to freeze");
            statusText.setBackgroundColor(0xFF424242);
        }
    }

    private void updateButton() {
        if (vpnActive) {
            masterFreezeBtn.setText("⚡ UNFREEZE ALL");
            masterFreezeBtn.setBackgroundColor(0xFFE53935);
        } else {
            masterFreezeBtn.setText("❄️ FREEZE SELECTED APPS");
            masterFreezeBtn.setBackgroundColor(0xFF1565C0);
        }
    }

    // ─── Inner classes ──────────────────────────────────────────────────────────

    static class AppInfo {
        String name;
        String packageName;
        android.graphics.drawable.Drawable icon;
    }

    class AppListAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredApps.size(); }
        @Override public Object getItem(int pos) { return filteredApps.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_app, parent, false);
            }
            AppInfo app = filteredApps.get(position);
            ImageView icon = convertView.findViewById(R.id.appIcon);
            TextView name = convertView.findViewById(R.id.appName);
            TextView pkg = convertView.findViewById(R.id.appPackage);
            CheckBox checkBox = convertView.findViewById(R.id.appCheckbox);

            icon.setImageDrawable(app.icon);
            name.setText(app.name);
            pkg.setText(app.packageName);
            checkBox.setChecked(frozenPackages.contains(app.packageName));

            boolean frozen = vpnActive && frozenPackages.contains(app.packageName);
            convertView.setBackgroundColor(frozen ? 0xFF0D2137 : 0xFF1A1A2E);

            return convertView;
        }
    }
}
