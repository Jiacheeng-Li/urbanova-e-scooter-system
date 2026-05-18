package com.lcyhz.urbanova.domain;

import java.util.HashMap;
import java.util.Map;

public class ScooterPhoneID {

    private static final Map<String, String> DEVICE_TO_SCOOTER = new HashMap<>();

    static {
        DEVICE_TO_SCOOTER.put("8072d281-821d-495a-9ffa-dc3cfb5d9a55", "SCO-0001");
        DEVICE_TO_SCOOTER.put("0a2a92de-7892-45df-8e4a-46284d615845", "SCO-0002");
        DEVICE_TO_SCOOTER.put("1a8a6725-cc09-4571-aae2-efd339894240", "SCO-0003");
    }

    public static String getScooterId(String phoneDeviceId) {
        return DEVICE_TO_SCOOTER.get(phoneDeviceId);
    }
}