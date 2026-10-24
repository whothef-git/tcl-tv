package com.gathin.tvhome;

import java.util.Calendar;

/**
 * Daily Quranic Ayah with Arabic script, Surah reference, and translation.
 * Rotates daily to provide uplifting spiritual reflection on the home screen.
 */
final class DailyAyat {

    static class Ayat {
        final String reference;
        final String arabic;
        final String translation;

        Ayat(String reference, String arabic, String translation) {
            this.reference = reference;
            this.arabic = arabic;
            this.translation = translation;
        }
    }

    private static final Ayat[] AYATS = new Ayat[]{
            new Ayat("AL-BAQARAH 2:152",
                    "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
                    "So remember Me; I will remember you. And be grateful to Me and do not deny Me."),

            new Ayat("ASH-SHARH 94:5-6",
                    "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا • إِنَّ مَعَ الْعُسْرِ يُسْرًا",
                    "For indeed, with hardship will be ease. Indeed, with hardship will be ease."),

            new Ayat("AR-RA'D 13:28",
                    "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                    "Unquestionably, by the remembrance of Allah hearts find rest."),

            new Ayat("AT-TALAQ 65:3",
                    "وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
                    "And whoever relies upon Allah - then He is sufficient for him."),

            new Ayat("AL-BAQARAH 2:286",
                    "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
                    "Allah does not burden a soul beyond that it can bear."),

            new Ayat("IBRAHIM 14:7",
                    "لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ",
                    "If you are grateful, I will surely increase you in favor."),

            new Ayat("AZ-ZUMAR 39:53",
                    "لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ ۚ إِنَّ اللَّهَ يَغْفِرُ الذُّنُوبَ جَمِيعًا",
                    "Do not despair of the mercy of Allah. Indeed, Allah forgives all sins."),

            new Ayat("AL-IMRAN 3:139",
                    "وَلَا تَهِنُوا وَلَا تَحْزَنُوا وَأَنتُمُ الْأَعْلَوْنَ إِن كُنتُم مُّؤْمِنِينَ",
                    "Do not weaken and do not grieve, and you will be superior if you are believers."),

            new Ayat("AL-BAQARAH 2:186",
                    "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ",
                    "When My servants ask you concerning Me, indeed I am near. I respond to the call of the caller."),

            new Ayat("AL-ANBIYA 21:87",
                    "لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
                    "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers."),

            new Ayat("AL-MUDDATHTHIR 74:56",
                    "هُوَ أَهْلُ التَّقْوَىٰ وَأَهْلُ الْمَغْفِرَةِ",
                    "He is worthy of fear and worthy of granting forgiveness."),

            new Ayat("AL-FURQAN 25:74",
                    "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ",
                    "Our Lord, grant us from among our spouses and offspring comfort to our eyes."),

            new Ayat("AL-ISRA 17:24",
                    "رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
                    "My Lord, have mercy upon them as they brought me up when I was small."),

            new Ayat("TA-HA 20:114",
                    "رَّبِّ زِدْنِي عِلْمًا",
                    "My Lord, increase me in knowledge.")
    };

    private DailyAyat() {
    }

    /** Returns today's Ayat based on day of year. */
    static Ayat getToday() {
        Calendar cal = Calendar.getInstance();
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        return AYATS[dayOfYear % AYATS.length];
    }
}
