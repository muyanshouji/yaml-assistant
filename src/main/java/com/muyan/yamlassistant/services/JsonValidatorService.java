package com.muyan.yamlassistant.services;

import com.google.gson.JsonParser;

public class JsonValidatorService {

    public String validate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            JsonParser.parseString(text);
            return null;
        } catch (Exception e) {
            String message = e.getMessage();
            return message != null ? message : e.getClass().getSimpleName();
        }
    }
}
