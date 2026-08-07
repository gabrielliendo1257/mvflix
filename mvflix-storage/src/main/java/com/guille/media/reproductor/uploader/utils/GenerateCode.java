package com.guille.media.reproductor.uploader.utils;

import java.security.SecureRandom;

public class GenerateCode {

    public GenerateCode() {
    }

    public static String generateCode() {
        String code;

        try {
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            int c = secureRandom.nextInt(9000) + 1000;
            code = String.valueOf(c);
        } catch (Exception e) {
            throw new RuntimeException("Problema con la generacion de codigo seguro.");
        }

        return code;
    }
}
