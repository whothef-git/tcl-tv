package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentAppsActivity extends Activity implements RecentAppsAdapter.OnRecentAppInteractionListener {
    private static RecentAppsActivity sForegroundInstance = null;

    public static final String ACTION_CLOSE_ALL = "com.gathin.tvhome.ACTION_CLOSE_ALL";

    public static boolean isForeground() {
        return sForegroundInstance != null;
    }

    public static void triggerCloseAllFromGlobal() {
        if (sForegroundInstance != null) {
            sForegroundInstance.onCloseAllClicked();
        }
    }

    public static void dismissFromGlobal() {
        if (sForegroundInstance != null) {
            sForegroundInstance.finish();
        }
    }

    private TextView tvRamText;
    private ProgressBar pbRam;
    private RecyclerView rvRecentApps;
    private TextView tvEmptyRecents;
    private TextView tvBottomHint;
    private RecentAppsAdapter adapter;
    private long lastHomePressTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        setContentView(R.layout.activity_recent_apps);

        tvRamText = findViewById(R.id.tvRamText);
        pbRam = findViewById(R.id.pbRam);
        rvRecentApps = findViewById(R.id.rvRecentApps);
        tvEmptyRecents = findViewById(R.id.tvEmptyRecents);
        tvBottomHint = findViewById(R.id.tvBottomHint);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvRecentApps.setLayoutManager(layoutManager);

        // Add 3D card deck overlapping decoration (-120dp overlap for CoverFlow stack)
        float density = getResources().getDisplayMetrics().density;
        final int overlapPx = Math.round(120 * density);
        final int closeAllGapPx = Math.round(32 * density);

        rvRecentApps.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int pos = parent.getChildAdapterPosition(view);
                int total = state.getItemCount();
                if (pos > 0) {
                    if (pos == total - 1 && adapter != null && !adapter.isEmpty() && adapter.getItems().get(pos).isCloseAll) {
                        outRect.left = closeAllGapPx;
                    } else {
                        outRect.left = -overlapPx;
                    }
                } else {
                    outRect.left = 0;
                }
            }
        });

        adapter = new RecentAppsAdapter(this);
        rvRecentApps.setAdapter(adapter);

        updateRamStats();
        loadRecentApps();
        if (getIntent() != null && ACTION_CLOSE_ALL.equals(getIntent().getAction())) {
            rvRecentApps.post(this::onCloseAllClicked);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sForegroundInstance = this;
        updateRamStats();
        loadRecentApps();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && ACTION_CLOSE_ALL.equals(intent.getAction())) {
            onCloseAllClicked();
            return;
        }
        updateRamStats();
        loadRecentApps();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sForegroundInstance == this) {
            sForegroundInstance = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sForegroundInstance == this) {
            sForegroundInstance = null;
        }
    }

    private void updateRamStats() {
        RecentAppsManager.MemoryStats stats = RecentAppsManager.getMemoryStats(this);
        tvRamText.setText(stats.formattedText);
        pbRam.setProgress(stats.percentUsed);
    }

    private void loadRecentApps() {
        List<RecentAppItem> apps = RecentAppsManager.getRecentApps(this);
        if (apps.isEmpty()) {
            showEmptyState();
        } else {
            tvEmptyRecents.setVisibility(View.GONE);
            rvRecentApps.setVisibility(View.VISIBLE);
            tvBottomHint.setVisibility(View.VISIBLE);
            adapter.setItems(apps);

            // Give initial focus to first card
            rvRecentApps.postDelayed(() -> {
                RecyclerView.ViewHolder vh = rvRecentApps.findViewHolderForAdapterPosition(0);
                if (vh != null) {
                    View focusTarget = vh.itemView.findViewById(R.id.cardContainer);
                    if (focusTarget != null) {
                        focusTarget.requestFocus();
                    }
                }
            }, 100);
        }
    }

    private void showEmptyState() {
        rvRecentApps.setVisibility(View.GONE);
        tvBottomHint.setVisibility(View.GONE);
        tvEmptyRecents.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAppClicked(RecentAppItem item) {
        if (item.launchIntent != null) {
            SoundEffects.playSelect();
            try {
                item.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(item.launchIntent);
                finish();
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Cannot open app", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onAppClosed(RecentAppItem item, int position, View itemView) {
        SoundEffects.playSelect();
        itemView.animate()
                .translationY(-380f)
                .alpha(0f)
                .setDuration(220)
                .withEndAction(() -> {
                    itemView.setTranslationY(0f);
                    itemView.setAlpha(1f);
                    RecentAppsManager.killApp(this, item.packageName);
                    adapter.removeItem(position);
                    updateRamStats();
                    if (adapter.isEmpty()) {
                        showEmptyState();
                        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 600);
                    }
                })
                .start();
    }

    @Override
    public void onCloseAllClicked() {
        SoundEffects.playSelect();
        rvRecentApps.animate()
                .translationY(-320f)
                .alpha(0f)
                .setDuration(260)
                .withEndAction(() -> {
                    RecentAppsManager.closeAll(this, adapter.getItems());
                    adapter.clearAll();
                    updateRamStats();
                    showEmptyState();
                    Toast.makeText(this, "RAM cleared! All apps closed", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 650);
                })
                .start();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_HOME || code == KeyEvent.KEYCODE_ALL_APPS) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                long now = SystemClock.uptimeMillis();
                if (now - lastHomePressTime < 450) {
                    // Double press Home in Recent Apps -> Trigger Close All!
                    lastHomePressTime = 0;
                    onCloseAllClicked();
                    return true;
                } else {
                    lastHomePressTime = now;
                    return true;
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }
}
