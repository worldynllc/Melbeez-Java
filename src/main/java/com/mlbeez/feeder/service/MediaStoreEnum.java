package com.mlbeez.feeder.service;

/**
 * Enum to represents different types of supported media storage
 */
public enum MediaStoreEnum {
    AWSS3("awss3"),
    GCP("gcp"),
    CEPH("ceph"),
    SOLIDIFIER("solidifier");
    String name;
    MediaStoreEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static MediaStoreEnum getStore(String name){
        for(MediaStoreEnum mediaStoreEnum : MediaStoreEnum.values()){
            if(mediaStoreEnum.getName().equalsIgnoreCase(name)){
                return mediaStoreEnum;
            }
        }
        return null;
    }
}
