package com.gathin.tvhome;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/** Bing's captions, split into what the shelf shows: the thing's name and its country. */
public class PlaceWallpapersTest {

    private static void check(String caption, String name, String country) {
        assertArrayEquals(caption, new String[]{name, country}, PlaceWallpapers.nameAndCountry(caption));
    }

    @Test
    public void namesTheThingAndItsCountry() {
        check("Brown-throated sloth, Puerto Viejo de Talamanca, Costa Rica", "Brown-Throated Sloth", "Costa Rica");
        check("Grand Canal in Venice, Italy", "Grand Canal", "Italy");
        check("Dolbadarn Castle, Llanberis, Eryri National Park (Snowdonia), Wales", "Dolbadarn Castle", "Wales");
        check("Life-size replica of Chauvet  cave art in Vallon-Pont-d'Arc, France", "Chauvet Cave Art", "France");
        check("Mount Sneffels in the San Juan Mountains near Ridgway, Colorado", "Mount Sneffels", "United States");
        check("View of the Wroclaw Market Square from the Witches' Bridge in Wroclaw, Poland", "Wroclaw Market Square", "Poland");
        check("Ammonite fossils from the Jurassic Period", "Ammonite Fossils", "");
    }
}
