package offlcersam.weaponfoundry.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON value tree, with no reliance on external dependency. This should mean that other ship mods would not need their own JSON parser.
 */
public final class JsonValue {
    public enum Type { OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL }

    private final Type type;
    private final Map<String, JsonValue> object;
    private final List<JsonValue> array;
    private final String stringValue;
    private final double numberValue;
    private final boolean boolValue;

    private JsonValue(Type type, Map<String, JsonValue> object, List<JsonValue> array,
                      String stringValue, double numberValue, boolean boolValue) {
        this.type = type;
        this.object = object;
        this.array = array;
        this.stringValue = stringValue;
        this.numberValue = numberValue;
        this.boolValue = boolValue;
    }

    static JsonValue ofObject(Map<String, JsonValue> map) {
        return new JsonValue(Type.OBJECT, map, null, null, 0, false);
    }

    static JsonValue ofArray(List<JsonValue> list) {
        return new JsonValue(Type.ARRAY, null, list, null, 0, false);
    }

    static JsonValue ofString(String s) {
        return new JsonValue(Type.STRING, null, null, s, 0, false);
    }

    static JsonValue ofNumber(double d) {
        return new JsonValue(Type.NUMBER, null, null, null, d, false);
    }

    static JsonValue ofBoolean(boolean b) {
        return new JsonValue(Type.BOOLEAN, null, null, null, 0, b);
    }

    static JsonValue ofNull() {
        return new JsonValue(Type.NULL, null, null, null, 0, false);
    }

    public Type type() {
        return type;
    }

    public boolean isNull() {
        return type == Type.NULL;
    }

    public boolean has(String key) {
        return type == Type.OBJECT && object.containsKey(key);
    }

    public JsonValue get(String key) {
        if (type != Type.OBJECT) {
            throw new JsonException("Not an object, cannot read key \"" + key + "\"");
        }
        JsonValue v = object.get(key);
        if (v == null) {
            throw new JsonException("Missing required field \"" + key + "\"");
        }
        return v;
    }

    public JsonValue getOrNull(String key) {
        return type == Type.OBJECT ? object.get(key) : null;
    }

    public List<JsonValue> asArray() {
        if (type != Type.ARRAY) {
            throw new JsonException("Not an array");
        }
        return array;
    }

    public String asString() {
        if (type != Type.STRING) {
            throw new JsonException("Not a string");
        }
        return stringValue;
    }

    public int asInt() {
        if (type != Type.NUMBER) {
            throw new JsonException("Not a number");
        }
        return (int) numberValue;
    }

    public double asDouble() {
        if (type != Type.NUMBER) {
            throw new JsonException("Not a number");
        }
        return numberValue;
    }

    public float asFloat() {
        return (float) asDouble();
    }

    public boolean asBoolean() {
        if (type != Type.BOOLEAN) {
            throw new JsonException("Not a boolean");
        }
        return boolValue;
    }

    public String getString(String key, String fallback) {
        JsonValue v = getOrNull(key);
        return v == null || v.isNull() ? fallback : v.asString();
    }

    public int getInt(String key, int fallback) {
        JsonValue v = getOrNull(key);
        return v == null || v.isNull() ? fallback : v.asInt();
    }

    public float getFloat(String key, float fallback) {
        JsonValue v = getOrNull(key);
        return v == null || v.isNull() ? fallback : v.asFloat();
    }

    public boolean getBoolean(String key, boolean fallback) {
        JsonValue v = getOrNull(key);
        return v == null || v.isNull() ? fallback : v.asBoolean();
    }

    public List<JsonValue> getArray(String key) {
        JsonValue v = getOrNull(key);
        return v == null || v.isNull() ? new ArrayList<>() : v.asArray();
    }

    public static final class JsonException extends RuntimeException {
        public JsonException(String message) {
            super(message);
        }
    }

    // package-private mutable builders used only by JsonParser
    static Map<String, JsonValue> newObjectMap() {
        return new LinkedHashMap<>();
    }

    static List<JsonValue> newArrayList() {
        return new ArrayList<>();
    }
}