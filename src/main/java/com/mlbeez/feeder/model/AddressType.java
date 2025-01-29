package com.mlbeez.feeder.model;

public enum AddressType {
    RESIDENTIAL(1),
    BILLING(2);

    private final int value;

    AddressType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AddressType fromValue(int value) {
        for (AddressType type : AddressType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
}
