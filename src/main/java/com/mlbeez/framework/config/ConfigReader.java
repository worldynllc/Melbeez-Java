package com.mlbeez.framework.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Load properties from remote config server
 */
public class ConfigReader {
    private static final Map<String, Object> properties = new HashMap<>();
    private static ConfigReader configReader;

    private ConfigReader() {
        init();
    }

    public static ConfigReader getInstance() {
        if(configReader == null) {
            configReader = new ConfigReader();
        }
        return configReader;
    }

    private void init() {
        String url = System.getenv("config_path");
        if (url == null) {
            url = System.getProperty("config_path");
        }
        if (url == null) {
            throw new RuntimeException("config_path not set");
        }
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String jsonResponse = response.body().string();
            Type listType = new TypeToken<ArrayList<Property>>(){}.getType();
            List<Property> propertyList = new Gson().fromJson(jsonResponse, listType);
            propertyList.forEach(prop -> getProperties().put(prop.getKey(), prop.getValue()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getProperty(String key) {
        return String.valueOf(properties.get(key));
    }
}
