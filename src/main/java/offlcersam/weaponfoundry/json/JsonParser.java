package offlcersam.weaponfoundry.json;

import java.util.List;
import java.util.Map;

/**
 * JSon parser that covers the JSon ship definitions.
 * Missing some things but otherwise fine for testing.
 */
public final class JsonParser {
    private final String src;
    private int pos;

    private JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static JsonValue parse(String text) {
        JsonParser parser = new JsonParser(text);
        parser.skipWhitespace();
        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos != parser.src.length()) {
            throw new JsonValue.JsonException("Unexpected trailing content at position " + parser.pos);
        }
        return value;
    }

    private JsonValue parseValue() {
        char c = peek();
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> JsonValue.ofString(parseString());
            case 't' -> {
                expect("true");
                yield JsonValue.ofBoolean(true);
            }
            case 'f' -> {
                expect("false");
                yield JsonValue.ofBoolean(false);
            }
            case 'n' -> {
                expect("null");
                yield JsonValue.ofNull();
            }
            default -> parseNumber();
        };
    }

    private JsonValue parseObject() {
        Map<String, JsonValue> map = JsonValue.newObjectMap();
        expectChar('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return JsonValue.ofObject(map);
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expectChar(':');
            skipWhitespace();
            JsonValue value = parseValue();
            map.put(key, value);
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == '}') {
                pos++;
                break;
            } else {
                throw new JsonValue.JsonException("Expected ',' or '}' at position " + pos);
            }
        }
        return JsonValue.ofObject(map);
    }

    private JsonValue parseArray() {
        List<JsonValue> list = JsonValue.newArrayList();
        expectChar('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return JsonValue.ofArray(list);
        }
        while (true) {
            skipWhitespace();
            list.add(parseValue());
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == ']') {
                pos++;
                break;
            } else {
                throw new JsonValue.JsonException("Expected ',' or ']' at position " + pos);
            }
        }
        return JsonValue.ofArray(list);
    }

    private String parseString() {
        expectChar('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = src.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        String hex = src.substring(pos, pos + 4);
                        pos += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                        break;
                    default:
                        throw new JsonValue.JsonException("Unknown escape \\" + esc + " at position " + pos);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private JsonValue parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.'
                || src.charAt(pos) == 'e' || src.charAt(pos) == 'E' || src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
            pos++;
        }
        String numStr = src.substring(start, pos);
        try {
            return JsonValue.ofNumber(Double.parseDouble(numStr));
        } catch (NumberFormatException e) {
            throw new JsonValue.JsonException("Invalid number \"" + numStr + "\" at position " + start);
        }
    }

    private void expect(String literal) {
        if (pos + literal.length() > src.length() || !src.startsWith(literal, pos)) {
            throw new JsonValue.JsonException("Expected \"" + literal + "\" at position " + pos);
        }
        pos += literal.length();
    }

    private void expectChar(char c) {
        if (peek() != c) {
            throw new JsonValue.JsonException("Expected '" + c + "' at position " + pos);
        }
        pos++;
    }

    private char peek() {
        if (pos >= src.length()) {
            throw new JsonValue.JsonException("Unexpected end of input");
        }
        return src.charAt(pos);
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }
}
