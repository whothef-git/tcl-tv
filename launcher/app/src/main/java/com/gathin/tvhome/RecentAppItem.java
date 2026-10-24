package com.gathin.tvhome;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class RecentAppItem {
    public final String packageName;
    public final String label;
    public final Drawable icon;
    public final Intent launchIntent;
    public final boolean isCloseAll;

    public RecentAppItem(String packageName, String label, Drawable icon, Intent launchIntent) {
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
        this.launchIntent = launchIntent;
        this.isCloseAll = false;
    }

    public RecentAppItem(boolean isCloseAll) {
        this.packageName = "";
        this.label = "Close All";
        this.icon = null;
        this.launchIntent = null;
        this.isCloseAll = isCloseAll;
    }

    public static RecentAppItem createCloseAll() {
        return new RecentAppItem(true);
    }
}
