package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class OnlineWallpapersTest {

    @Test
    public void describeReadsCompanionTextFile() throws Exception {
        File dir = File.createTempFile("online_test", "");
        dir.delete();
        dir.mkdirs();

        File jpg = new File(dir, "photo1.jpg");
        jpg.createNewFile();

        File txt = new File(dir, "photo1.txt");
        try (FileOutputStream os = new FileOutputStream(txt)) {
            os.write("Moody Alpine Mountains\nDark 4K Landscape\nUnsplash\n".getBytes(StandardCharsets.UTF_8));
        }

        String[] about = OnlineWallpapers.describe(jpg);
        assertNotNull(about);
        assertEquals(3, about.length);
        assertEquals("Moody Alpine Mountains", about[0]);
        assertEquals("Dark 4K Landscape", about[1]);
        assertEquals("Unsplash", about[2]);

        jpg.delete();
        txt.delete();
        dir.delete();
    }
}
