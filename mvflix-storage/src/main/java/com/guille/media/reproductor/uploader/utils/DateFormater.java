package com.guille.media.reproductor.uploader.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormater {

    public static String getSimpleDate(Date dateNow) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy:MM:dd");

        return simpleDateFormat.format(dateNow);
    }
}
