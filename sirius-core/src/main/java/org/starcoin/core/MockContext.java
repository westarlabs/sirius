package org.starcoin.core;

import java.util.HashMap;
import java.util.Map;

public class MockContext {

    public static final MockContext DEFAULT = new MockContext();

    private Map<String, Object> properites = new HashMap<>();

    public boolean containsKey(String key) {
        return properites.containsKey(key);
    }

    public Object get(String key) {
        return properites.get(key);
    }

    public boolean getAsBoolean(String key) {
        Object v = this.get(key);
        if (v == null) {
            return false;
        }
        return (Boolean) v;
    }

    public int getAsInt(String key) {
        Object v = this.get(key);
        if (v == null) {
            return 0;
        }
        return (Integer) v;
    }

    public MockContext put(String key, Object value) {
        properites.put(key, value);
        return this;
    }

    public <V> V getOrDefault(String key, V defaultValue) {
        return (V) properites.getOrDefault(key, defaultValue);
    }

    public <V> V getOrSet(String key, V setValue) {
        Object v = this.get(key);
        if (v == null) {
            this.properites.put(key, setValue);
            v = setValue;
        }
        return (V) v;
    }

    public static <T extends Mockable> T mock(Class<T> clazz, MockContext context) {
        try {
            T t = clazz.newInstance();
            t.mock(context);
            return t;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
