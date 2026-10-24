package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Manages Arsenal FC fixtures, live 365Scores API integration, countdowns, and UI card data.
 */
public final class ArsenalMatch {

    private static final String TAG = "ArsenalMatch";
    private static final String API_URL =
            "https://webws.365scores.com/web/games/current/?appTypeId=5&langId=1&timezoneName=Asia/Kuala_Lumpur&competitors=104";

    public static final class Match {
        public final String opponent;
        public final String competition;
        public final String venue; // "Home" or "Away"
        public final long kickoffUtcMs;
        public final String liveScore;

        public Match(String opponent, String competition, String venue, long kickoffUtcMs, String liveScore) {
            this.opponent = opponent;
            this.competition = competition;
            this.venue = venue;
            this.kickoffUtcMs = kickoffUtcMs;
            this.liveScore = liveScore;
        }
    }

    public static final class MatchCardData {
        public final String header;
        public final String opponent;
        public final String score;
        public final boolean hasScore;
        public final String details;
        public final String timeStr;
        public final String countdownStr;
        public final boolean isLive;

        public MatchCardData(String header, String opponent, String score, String details, String timeStr, String countdownStr, boolean isLive) {
            this.header = header;
            this.opponent = opponent;
            this.score = score;
            this.hasScore = (score != null && !score.isEmpty());
            this.details = details;
            this.timeStr = timeStr;
            this.countdownStr = countdownStr;
            this.isLive = isLive;
        }
    }

    private static volatile MatchCardData liveData = null;
    private static long lastFetchTime = 0;
    private static boolean isFetching = false;

    private static final List<Match> FIXTURES = new ArrayList<>();

    static {
        // 2026/2027 Season Fallback Schedule (UTC epoch timestamps)
        // 24 Oct 2026: BST is UTC+1. Kickoff 15:00 BST = 14:00 UTC = 10:00 PM Malaysia Time (MYT).
        addFixture("vs Everton", "Premier League", "Home", 2026, Calendar.OCTOBER, 24, 14, 0, "1 - 0");
        // 27 Oct 2026 19:45 UTC (28 Oct 03:45 AM MYT): vs Fleetwood Town (Carabao Cup, Away)
        addFixture("vs Fleetwood Town", "Carabao Cup", "Away", 2026, Calendar.OCTOBER, 27, 19, 45, null);
        // 01 Nov 2026 16:30 UTC (02 Nov 00:30 AM MYT): vs Liverpool (Premier League, Away)
        addFixture("vs Liverpool", "Premier League", "Away", 2026, Calendar.NOVEMBER, 1, 16, 30, null);
        // 04 Nov 2026 20:00 UTC (05 Nov 04:00 AM MYT): vs Slavia Prague (Champions League, Home)
        addFixture("vs Slavia Prague", "Champions League", "Home", 2026, Calendar.NOVEMBER, 4, 20, 0, null);
        // 08 Nov 2026 14:00 UTC (22:00 MYT): vs Sunderland (Premier League, Away)
        addFixture("vs Sunderland", "Premier League", "Away", 2026, Calendar.NOVEMBER, 8, 14, 0, null);
        // 21 Nov 2026 17:30 UTC (22 Nov 01:30 AM MYT): vs Tottenham Hotspur (Premier League, Home)
        addFixture("vs Tottenham Hotspur", "Premier League", "Home", 2026, Calendar.NOVEMBER, 21, 17, 30, null);
        // 25 Nov 2026 20:00 UTC (26 Nov 04:00 AM MYT): vs Bayern Munich (Champions League, Home)
        addFixture("vs Bayern Munich", "Champions League", "Home", 2026, Calendar.NOVEMBER, 25, 20, 0, null);
        // 28 Nov 2026 15:00 UTC (23:00 MYT): vs Chelsea (Premier League, Away)
        addFixture("vs Chelsea", "Premier League", "Away", 2026, Calendar.NOVEMBER, 28, 15, 0, null);
        // 02 Dec 2026 19:45 UTC: vs Brentford (Premier League, Home)
        addFixture("vs Brentford", "Premier League", "Home", 2026, Calendar.DECEMBER, 2, 19, 45, null);
        // 05 Dec 2026 15:00 UTC: vs Aston Villa (Premier League, Away)
        addFixture("vs Aston Villa", "Premier League", "Away", 2026, Calendar.DECEMBER, 5, 15, 0, null);
        // 09 Dec 2026 20:00 UTC: vs Club Brugge (Champions League, Away)
        addFixture("vs Club Brugge", "Champions League", "Away", 2026, Calendar.DECEMBER, 9, 20, 0, null);
        // 13 Dec 2026 14:00 UTC: vs Wolves (Premier League, Home)
        addFixture("vs Wolves", "Premier League", "Home", 2026, Calendar.DECEMBER, 13, 14, 0, null);
        // 19 Dec 2026 15:00 UTC: vs Everton (Premier League, Away)
        addFixture("vs Everton", "Premier League", "Away", 2026, Calendar.DECEMBER, 19, 15, 0, null);
        // 26 Dec 2026 15:00 UTC: vs Brighton (Premier League, Home)
        addFixture("vs Brighton", "Premier League", "Home", 2026, Calendar.DECEMBER, 26, 15, 0, null);
        // 29 Dec 2026 19:45 UTC: vs Aston Villa (Premier League, Home)
        addFixture("vs Aston Villa", "Premier League", "Home", 2026, Calendar.DECEMBER, 29, 19, 45, null);
        // 02 Jan 2027 15:00 UTC: vs Bournemouth (Premier League, Away)
        addFixture("vs Bournemouth", "Premier League", "Away", 2027, Calendar.JANUARY, 2, 15, 0, null);
        // 13 Jan 2027 20:00 UTC: vs Liverpool (Premier League, Home)
        addFixture("vs Liverpool", "Premier League", "Home", 2027, Calendar.JANUARY, 13, 20, 0, null);
        // 16 Jan 2027 15:00 UTC: vs Nottingham Forest (Premier League, Away)
        addFixture("vs Nottingham Forest", "Premier League", "Away", 2027, Calendar.JANUARY, 16, 15, 0, null);
        // 20 Jan 2027 20:00 UTC: vs Inter Milan (Champions League, Away)
        addFixture("vs Inter Milan", "Champions League", "Away", 2027, Calendar.JANUARY, 20, 20, 0, null);
        // 24 Jan 2027 16:30 UTC: vs Manchester United (Premier League, Home)
        addFixture("vs Manchester United", "Premier League", "Home", 2027, Calendar.JANUARY, 24, 16, 30, null);
        // 31 Jan 2027 14:00 UTC: vs Leeds United (Premier League, Away)
        addFixture("vs Leeds United", "Premier League", "Away", 2027, Calendar.JANUARY, 31, 14, 0, null);
        // 06 Feb 2027 15:00 UTC: vs Newcastle United (Premier League, Home)
        addFixture("vs Newcastle United", "Premier League", "Home", 2027, Calendar.FEBRUARY, 6, 15, 0, null);
        // 13 Feb 2027 15:00 UTC: vs Leicester City (Premier League, Away)
        addFixture("vs Leicester City", "Premier League", "Away", 2027, Calendar.FEBRUARY, 13, 15, 0, null);
        // 20 Feb 2027 15:00 UTC: vs West Ham United (Premier League, Home)
        addFixture("vs West Ham United", "Premier League", "Home", 2027, Calendar.FEBRUARY, 20, 15, 0, null);
        // 27 Feb 2027 17:30 UTC: vs Manchester City (Premier League, Away)
        addFixture("vs Manchester City", "Premier League", "Away", 2027, Calendar.FEBRUARY, 27, 17, 30, null);
    }

    private static void addFixture(String opponent, String comp, String venue, int y, int m, int d, int h, int min, String score) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(y, m, d, h, min, 0);
        c.set(Calendar.MILLISECOND, 0);
        FIXTURES.add(new Match(opponent, comp, venue, c.getTimeInMillis(), score));
    }

    public static void syncIfDue(Context context, Runnable onUpdated) {
        long now = System.currentTimeMillis();
        long interval = (liveData != null && liveData.isLive) ? (45 * 1000L) : (10 * 60 * 1000L);
        if (now - lastFetchTime < interval || isFetching) {
            return;
        }
        isFetching = true;

        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject root = new JSONObject(sb.toString());
                    JSONArray games = root.optJSONArray("games");
                    if (games != null && games.length() > 0) {
                        JSONObject targetGame = null;
                        // Priority 1: Game in progress (statusGroup == 3)
                        for (int i = 0; i < games.length(); i++) {
                            JSONObject g = games.getJSONObject(i);
                            if (g.optInt("statusGroup") == 3) {
                                targetGame = g;
                                break;
                            }
                        }
                        // Priority 2: Scheduled upcoming game (statusGroup == 2)
                        if (targetGame == null) {
                            for (int i = 0; i < games.length(); i++) {
                                JSONObject g = games.getJSONObject(i);
                                if (g.optInt("statusGroup") == 2) {
                                    targetGame = g;
                                    break;
                                }
                            }
                        }
                        // Priority 3: Fallback to the latest game
                        if (targetGame == null) {
                            targetGame = games.getJSONObject(0);
                        }

                        if (targetGame != null) {
                            JSONObject home = targetGame.optJSONObject("homeCompetitor");
                            JSONObject away = targetGame.optJSONObject("awayCompetitor");
                            boolean isArsenalHome = (home != null && home.optInt("id") == 104);
                            String oppName = "Opponent";
                            if (isArsenalHome && away != null) {
                                oppName = away.optString("name", "Opponent");
                            } else if (!isArsenalHome && home != null) {
                                oppName = home.optString("name", "Opponent");
                            }

                            String comp = targetGame.optString("competitionDisplayName", "Premier League");
                            String venue = isArsenalHome ? "Home" : "Away";
                            int statusGroup = targetGame.optInt("statusGroup", 2);
                            boolean isLive = (statusGroup == 3);

                            String header = isLive ? "ARSENAL FC · LIVE NOW" : "ARSENAL FC · NEXT MATCH";
                            String opponent = "vs " + oppName;
                            String details = comp + " · " + venue;

                            String score = null;
                            if (home != null && away != null && (isLive || statusGroup == 4)) {
                                int hScore = (int) home.optDouble("score", 0);
                                int aScore = (int) away.optDouble("score", 0);
                                score = isArsenalHome ? (hScore + " - " + aScore) : (aScore + " - " + hScore);
                            }

                            String timeStr;
                            String countdownStr;
                            String startTimeStr = targetGame.optString("startTime", "");
                            long gameUtcMs = parseIsoDate(startTimeStr);

                            if (isLive) {
                                SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
                                String tStr = (gameUtcMs > 0) ? timeFmt.format(new Date(gameUtcMs)).toLowerCase(Locale.ENGLISH) : "10:00 pm";
                                timeStr = "Today · " + tStr;

                                String statusText = targetGame.optString("statusText", "2nd Half");
                                String gameTimeDisplay = targetGame.optString("gameTimeDisplay", "");
                                if (!gameTimeDisplay.isEmpty()) {
                                    countdownStr = statusText + " · " + gameTimeDisplay;
                                } else {
                                    countdownStr = statusText;
                                }
                            } else {
                                SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
                                if (gameUtcMs > 0) {
                                    Calendar nowCal = Calendar.getInstance();
                                    Calendar matchCal = Calendar.getInstance();
                                    matchCal.setTimeInMillis(gameUtcMs);
                                    String formattedKickoff = timeFmt.format(new Date(gameUtcMs)).toLowerCase(Locale.ENGLISH);

                                    if (nowCal.get(Calendar.YEAR) == matchCal.get(Calendar.YEAR) &&
                                        nowCal.get(Calendar.DAY_OF_YEAR) == matchCal.get(Calendar.DAY_OF_YEAR)) {
                                        timeStr = "Today · " + formattedKickoff;
                                    } else {
                                        nowCal.add(Calendar.DAY_OF_YEAR, 1);
                                        if (nowCal.get(Calendar.YEAR) == matchCal.get(Calendar.YEAR) &&
                                            nowCal.get(Calendar.DAY_OF_YEAR) == matchCal.get(Calendar.DAY_OF_YEAR)) {
                                            timeStr = "Tomorrow · " + formattedKickoff;
                                        } else {
                                            SimpleDateFormat dayFmt = new SimpleDateFormat("EEE, d MMM", Locale.ENGLISH);
                                            timeStr = dayFmt.format(new Date(gameUtcMs)) + " · " + formattedKickoff;
                                        }
                                    }

                                    long diff = gameUtcMs - System.currentTimeMillis();
                                    long totalMinutes = diff / (60 * 1000L);
                                    if (totalMinutes < 60) {
                                        countdownStr = "in " + Math.max(1, totalMinutes) + "m";
                                    } else if (totalMinutes < 24 * 60) {
                                        long h = totalMinutes / 60;
                                        long m = totalMinutes % 60;
                                        countdownStr = (m > 0) ? ("in " + h + "h " + m + "m") : ("in " + h + "h");
                                    } else {
                                        long days = totalMinutes / (24 * 60);
                                        long remHours = (totalMinutes % (24 * 60)) / 60;
                                        countdownStr = (remHours > 0) ? ("in " + days + "d " + remHours + "h") : ("in " + days + "d");
                                    }
                                } else {
                                    timeStr = "Scheduled";
                                    countdownStr = "";
                                }
                            }

                            liveData = new MatchCardData(header, opponent, score, details, timeStr, countdownStr, isLive);
                            lastFetchTime = System.currentTimeMillis();

                            if (onUpdated != null) {
                                new Handler(Looper.getMainLooper()).post(onUpdated);
                            }
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.w(TAG, "Failed to sync 365scores live games: " + e.getMessage());
            } finally {
                isFetching = false;
            }
        }).start();
    }

    private static long parseIsoDate(String iso) {
        if (iso == null || iso.length() < 19) return 0;
        try {
            // e.g. 2026-10-24T22:00:00+08:00
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT" + iso.substring(19)));
            Date d = sdf.parse(iso.substring(0, 19));
            return d != null ? d.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static MatchCardData getNextMatch() {
        if (liveData != null) {
            return liveData;
        }

        // Local fallback calculation when offline
        long now = System.currentTimeMillis();
        Match current = null;

        for (Match m : FIXTURES) {
            if (m.kickoffUtcMs + (115L * 60 * 1000L) > now) {
                current = m;
                break;
            }
        }

        if (current == null) {
            return new MatchCardData(
                    "ARSENAL FC · NEXT MATCH",
                    "vs Opponent",
                    null,
                    "Premier League · Home",
                    "Schedule Pending",
                    "",
                    false
            );
        }

        long diff = current.kickoffUtcMs - now;
        boolean isLive = (diff <= 0);

        String header = isLive ? "ARSENAL FC · LIVE NOW" : "ARSENAL FC · NEXT MATCH";
        String opponent = current.opponent;
        String score = isLive ? current.liveScore : null;
        String details = current.competition + " · " + current.venue;

        Calendar nowCal = Calendar.getInstance();
        Calendar matchCal = Calendar.getInstance();
        matchCal.setTimeInMillis(current.kickoffUtcMs);

        SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
        String formattedKickoffTime = timeFmt.format(new Date(current.kickoffUtcMs)).toLowerCase(Locale.ENGLISH);

        String timeStr;
        if (nowCal.get(Calendar.YEAR) == matchCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == matchCal.get(Calendar.DAY_OF_YEAR)) {
            timeStr = "Today · " + formattedKickoffTime;
        } else {
            nowCal.add(Calendar.DAY_OF_YEAR, 1);
            if (nowCal.get(Calendar.YEAR) == matchCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == matchCal.get(Calendar.DAY_OF_YEAR)) {
                timeStr = "Tomorrow · " + formattedKickoffTime;
            } else {
                SimpleDateFormat dayFmt = new SimpleDateFormat("EEE, d MMM", Locale.ENGLISH);
                timeStr = dayFmt.format(new Date(current.kickoffUtcMs)) + " · " + formattedKickoffTime;
            }
        }

        String countdownStr;
        if (isLive) {
            long elapsedMinutes = (-diff) / (60 * 1000L);
            if (elapsedMinutes < 45) {
                countdownStr = "1st Half · " + Math.max(1, elapsedMinutes) + "'";
            } else if (elapsedMinutes < 60) {
                countdownStr = "Half Time";
            } else if (elapsedMinutes <= 105) {
                long secondHalfMin = 45 + (elapsedMinutes - 60);
                countdownStr = "2nd Half · " + Math.min(90, secondHalfMin) + "'";
            } else {
                countdownStr = "Full Time";
            }
        } else {
            long totalMinutes = diff / (60 * 1000L);
            if (totalMinutes < 60) {
                countdownStr = "in " + Math.max(1, totalMinutes) + "m";
            } else if (totalMinutes < 24 * 60) {
                long h = totalMinutes / 60;
                long m = totalMinutes % 60;
                countdownStr = (m > 0) ? ("in " + h + "h " + m + "m") : ("in " + h + "h");
            } else {
                long days = totalMinutes / (24 * 60);
                long remHours = (totalMinutes % (24 * 60)) / 60;
                countdownStr = (remHours > 0) ? ("in " + days + "d " + remHours + "h") : ("in " + days + "d");
            }
        }

        return new MatchCardData(header, opponent, score, details, timeStr, countdownStr, isLive);
    }

    public static void handleCardClick(Context context) {
        // Unclickable information display
    }
}
