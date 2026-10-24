package com.gathin.tvhome;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecentAppsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_APP = 1;
    private static final int VIEW_TYPE_CLOSE_ALL = 2;

    public interface OnRecentAppInteractionListener {
        void onAppClicked(RecentAppItem item);
        void onAppClosed(RecentAppItem item, int position, View itemView);
        void onCloseAllClicked();
    }

    private final List<RecentAppItem> items = new ArrayList<>();
    private final OnRecentAppInteractionListener listener;

    public RecentAppsAdapter(OnRecentAppInteractionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<RecentAppItem> newItems) {
        items.clear();
        if (newItems != null && !newItems.isEmpty()) {
            items.addAll(newItems);
            // Append the Close All card at the end of the carousel
            items.add(RecentAppItem.createCloseAll());
        }
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            // If only Close All remains, clear everything
            if (items.size() == 1 && items.get(0).isCloseAll) {
                items.clear();
                notifyDataSetChanged();
            }
        }
    }

    public void clearAll() {
        items.clear();
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<RecentAppItem> getItems() {
        return items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isCloseAll ? VIEW_TYPE_CLOSE_ALL : VIEW_TYPE_APP;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_CLOSE_ALL) {
            View view = inflater.inflate(R.layout.item_recent_close_all, parent, false);
            return new CloseAllViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_recent_app, parent, false);
            return new AppViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RecentAppItem item = items.get(position);
        if (holder instanceof AppViewHolder) {
            ((AppViewHolder) holder).bind(item, listener, position);
        } else if (holder instanceof CloseAllViewHolder) {
            ((CloseAllViewHolder) holder).bind(listener, position);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout topPill;
        private final ImageView ivSmallIcon;
        private final TextView tvAppLabel;
        private final FrameLayout cardContainer;
        private final FrameLayout squircleContainer;
        private final ImageView ivCardIcon;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            topPill = itemView.findViewById(R.id.topPill);
            ivSmallIcon = itemView.findViewById(R.id.ivSmallIcon);
            tvAppLabel = itemView.findViewById(R.id.tvAppLabel);
            cardContainer = itemView.findViewById(R.id.cardContainer);
            squircleContainer = itemView.findViewById(R.id.squircleContainer);
            ivCardIcon = itemView.findViewById(R.id.ivCardIcon);
        }

        public void bind(RecentAppItem item, OnRecentAppInteractionListener listener, int position) {
            tvAppLabel.setText(item.label);
            if (item.icon != null) {
                ivSmallIcon.setImageDrawable(item.icon);
                ivCardIcon.setImageDrawable(item.icon);
            }

            // CoverFlow stack elevation: earlier cards sit on top of later cards when unfocused
            itemView.setElevation(Math.max(4f, 32f - position * 2f));

            // Realistic card colors matching reference photo
            int cardColor = 0xFFE2EDF9; // clean ice silver default (like Settings/Nuvio)
            boolean isDarkCard = false;
            String p = item.packageName.toLowerCase(Locale.US);
            if (p.contains(".tv") || p.contains("livetv")) {
                cardColor = 0xFFE53935; // Vibrant Red (TV)
                isDarkCard = true;
            } else if (p.contains("file") || p.contains("explorer")) {
                cardColor = 0xFFF57C00; // Warm Orange (File Manager)
                isDarkCard = true;
            } else if (p.contains("netflix") || p.contains("prime")) {
                cardColor = 0xFF141822; // Deep Slate/Black
                isDarkCard = true;
            } else if (p.contains("youtube")) {
                cardColor = 0xFFCC0000; // Red (YouTube)
                isDarkCard = true;
            } else if (p.contains("settings")) {
                cardColor = 0xFFE2EDF9; // Ice Silver (Settings)
                isDarkCard = false;
            }

            float density = itemView.getResources().getDisplayMetrics().density;
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(cardColor);
            cardBg.setCornerRadius(26 * density);
            cardContainer.setBackground(cardBg);

            // Squircle style adaptation
            GradientDrawable squircleBg = new GradientDrawable();
            squircleBg.setCornerRadius(22 * density);
            if (isDarkCard) {
                squircleBg.setColor(0x38FFFFFF);
                squircleBg.setStroke(Math.round(1.5f * density), 0x55FFFFFF);
            } else {
                squircleBg.setColor(0xFFFFFFFF);
                squircleBg.setStroke(Math.round(1f * density), 0x1A000000);
            }
            squircleContainer.setBackground(squircleBg);

            // Deep realistic drop shadows on API 28+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cardContainer.setOutlineAmbientShadowColor(0x99000000);
                cardContainer.setOutlineSpotShadowColor(0xCC000000);
            }

            cardContainer.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    itemView.setTranslationZ(60f);
                    v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(160).start();
                    topPill.setBackgroundResource(R.drawable.bg_app_pill_focused);
                    tvAppLabel.setTextColor(0xFF14161D);
                } else {
                    itemView.setTranslationZ(0f);
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(160).start();
                    topPill.setBackgroundResource(R.drawable.bg_app_pill_unfocused);
                    tvAppLabel.setTextColor(0xFFFFFFFF);
                }
            });

            cardContainer.setOnClickListener(v -> {
                if (listener != null) listener.onAppClicked(item);
            });

            cardContainer.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (listener != null) {
                        int pos = getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            listener.onAppClosed(item, pos, itemView);
                        }
                    }
                    return true;
                }
                return false;
            });
        }
    }

    static class CloseAllViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout btnCircle;

        public CloseAllViewHolder(@NonNull View itemView) {
            super(itemView);
            btnCircle = itemView.findViewById(R.id.btnCircle);
        }

        public void bind(OnRecentAppInteractionListener listener, int position) {
            itemView.setElevation(Math.max(4f, 32f - position * 2f));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                btnCircle.setOutlineAmbientShadowColor(0x80000000);
                btnCircle.setOutlineSpotShadowColor(0xB3000000);
            }

            btnCircle.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    itemView.setTranslationZ(60f);
                    btnCircle.animate().scaleX(1.16f).scaleY(1.16f).setDuration(160).start();
                } else {
                    itemView.setTranslationZ(0f);
                    btnCircle.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start();
                }
            });

            btnCircle.setOnClickListener(v -> {
                if (listener != null) listener.onCloseAllClicked();
            });

            btnCircle.setOnKeyListener((v, keyCode, event) -> {
                if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                        && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (listener != null) listener.onCloseAllClicked();
                    return true;
                }
                return false;
            });
        }
    }
}
