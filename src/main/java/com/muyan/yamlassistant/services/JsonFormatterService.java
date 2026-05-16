package com.muyan.yamlassistant.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonFormatterService {

    private final JsonValidatorService validatorService = new JsonValidatorService();
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public String beautify(String text) {
        String validation = validatorService.validate(text);
        if (validation != null) {
            throw new IllegalArgumentException(validation);
        }

        if (text == null || text.trim().isEmpty()) {
            return text != null ? text : "";
        }

        JsonElement jsonElement = JsonParser.parseString(text);
        return gson.toJson(jsonElement);
    }
}
