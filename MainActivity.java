package com.aswin.reelsblocker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;
    private TextView counter;
    private Button openSettings;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(ReelsBlockerService.PREFS, Context.MODE_PRIVATE);

        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad * 2, pad, pad);

        TextView title = new TextView(this);
        title.setText("Reels Blocker");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(title);

        status = new TextView(this);
        status.setTextSize(18);
        status.setPadding(0, pad, 0, pad / 2);
        box.addView(status);

        counter = new TextView(this);
        counter.setTextSize(15);
        counter.setPadding(0, 0, 0, pad);
        box.addView(counter);

        openSettings = new Button(this);
        openSettings.setText("Open Accessibility settings");
        openSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        box.addView(openSettings);

        Switch enabled = new Switch(this);
        enabled.setText("Block Reels");
        enabled.setTextSize(18);
        enabled.setPadding(0, pad, 0, pad);
        enabled.setChecked(prefs.getBoolean(ReelsBlockerService.KEY_ENABLED, true));
        enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean on) {
                prefs.edit().putBoolean(ReelsBlockerService.KEY_ENABLED, on).apply();
                refresh();
            }
        });
        box.addView(enabled);

        TextView help = new TextView(this);
        help.setTextSize(14);
        help.setText("Setup (once):\n"
                + "1. Tap \"Open Accessibility settings\".\n"
                + "2. Open \"Installed apps\" / \"Downloaded apps\" and choose Reels Blocker.\n"
                + "3. Turn it on and allow.\n\n"
                + "If the switch is greyed out (Android 13+): open Settings > Apps > "
                + "Reels Blocker, tap the three dots at the top right and choose "
                + "\"Allow restricted settings\", then try again.\n\n"
                + "After that there is nothing to start. The blocker wakes up whenever "
                + "Instagram is opened and sleeps when you leave it.");
        box.addView(help);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean serviceOn = isServiceOn();
        boolean blocking = prefs.getBoolean(ReelsBlockerService.KEY_ENABLED, true);
        if (!serviceOn) {
            status.setText("Not active yet: turn on the accessibility service below.");
            status.setTextColor(Color.rgb(198, 40, 40));
        } else if (!blocking) {
            status.setText("Paused: Reels are allowed.");
            status.setTextColor(Color.rgb(230, 120, 0));
        } else {
            status.setText("Active: Reels are blocked whenever Instagram is open.");
            status.setTextColor(Color.rgb(46, 125, 50));
        }
        openSettings.setVisibility(serviceOn ? View.GONE : View.VISIBLE);
        counter.setText("Reels blocked so far: "
                + prefs.getInt(ReelsBlockerService.KEY_BLOCKED_COUNT, 0));
    }

    private boolean isServiceOn() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) return false;
        ComponentName me = new ComponentName(this, ReelsBlockerService.class);
        for (String s : enabledServices.split(":")) {
            ComponentName c = ComponentName.unflattenFromString(s);
            if (me.equals(c)) return true;
        }
        return false;
    }
}
