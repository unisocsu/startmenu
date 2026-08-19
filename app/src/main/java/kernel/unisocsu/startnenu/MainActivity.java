package kernel.unisocsu.startnenu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private View startMenuPanel;
    private EditText searchApps;
    private ListView appsList;
    private TextView trayClock;
    private TextView trayDate;

    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> filteredApps = new ArrayList<>();
    private AppAdapter appAdapter;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        Button btnMenu = findViewById(R.id.btn_menu);
        startMenuPanel = findViewById(R.id.start_menu_panel);
        searchApps = findViewById(R.id.search_apps);
        appsList = findViewById(R.id.apps_list);
        trayClock = findViewById(R.id.tray_clock);
        trayDate = findViewById(R.id.tray_date);
        View desktopArea = findViewById(R.id.desktop_area);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnCloseMenu = findViewById(R.id.btn_close_menu);

        // Start status notification service
        startService(new Intent(this, StatusNotificationService.class));

        // Toggle Start Menu Panel
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleStartMenu();
            }
        });

        // Close Menu when clicking desktop or close button
        desktopArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideStartMenu();
            }
        });

        btnCloseMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideStartMenu();
            }
        });

        // Open System Settings
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                hideStartMenu();
            }
        });

        // Load Apps
        loadInstalledApps();

        // Setup App Adapter
        appAdapter = new AppAdapter(this, filteredApps);
        appsList.setAdapter(appAdapter);

        // Launch selected app
        appsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppInfo selectedApp = filteredApps.get(position);
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(selectedApp.packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                    hideStartMenu();
                }
            }
        });

        // Live App Filtering via Search Bar
        searchApps.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Run Clock Update
        setupClock();
    }

    private void toggleStartMenu() {
        if (startMenuPanel.getVisibility() == View.VISIBLE) {
            hideStartMenu();
        } else {
            startMenuPanel.setVisibility(View.VISIBLE);
            searchApps.requestFocus();
        }
    }

    private void hideStartMenu() {
        startMenuPanel.setVisibility(View.GONE);
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        allApps.clear();

        for (ResolveInfo ri : resolveInfos) {
            String label = ri.loadLabel(pm).toString();
            String pkg = ri.activityInfo.packageName;
            Drawable icon = ri.loadIcon(pm);
            allApps.add(new AppInfo(label, pkg, icon));
        }

        // Sort applications alphabetically
        Collections.sort(allApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo o1, AppInfo o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });

        filteredApps.clear();
        filteredApps.addAll(allApps);
    }

    private void filterApps(String query) {
        filteredApps.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredApps.addAll(allApps);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (AppInfo app : allApps) {
                if (app.name.toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                        app.packageName.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                    filteredApps.add(app);
                }
            }
        }
        appAdapter.notifyDataSetChanged();
    }

    private void setupClock() {
        final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        clockRunnable = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                trayClock.setText(timeFormat.format(now));
                trayDate.setText(dateFormat.format(now));
                // Update every 10 seconds to keep clock fresh
                clockHandler.postDelayed(this, 10000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }

    // Static structures
    private static class AppInfo {
        String name;
        String packageName;
        Drawable icon;

        AppInfo(String name, String packageName, Drawable icon) {
            this.name = name;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    private static class AppAdapter extends BaseAdapter {
        private final Context context;
        private final List<AppInfo> apps;

        AppAdapter(Context context, List<AppInfo> apps) {
            this.context = context;
            this.apps = apps;
        }

        @Override
        public int getCount() {
            return apps.size();
        }

        @Override
        public Object getItem(int position) {
            return apps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
            }

            AppInfo app = apps.get(position);

            ImageView appIcon = convertView.findViewById(R.id.app_icon);
            TextView appName = convertView.findViewById(R.id.app_name);
            TextView appPackage = convertView.findViewById(R.id.app_package);

            appIcon.setImageDrawable(app.icon);
            appName.setText(app.name);
            appPackage.setText(app.packageName);

            return convertView;
        }
    }
}
