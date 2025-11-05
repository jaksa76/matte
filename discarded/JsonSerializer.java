// Example of what a generated JSON serializer would look like

public class JsonSerializer {
    
    public static String example() {
        return """
            
            === Generated JSON Serializer Pattern ===
            
            Instead of:
              // Reflection-based (traditional)
              for (Field field : obj.getClass().getDeclaredFields()) {
                  field.setAccessible(true);  // REFLECTION!
                  Object value = field.get(obj);  // REFLECTION!
                  json.append(field.getName() + ":" + value);
              }
            
            Use:
              // Generated code (no reflection)
              public static String toJson(User user) {
                  return "{"
                      + "\\"id\\":" + user.getId() + ","
                      + "\\"name\\":\\"" + user.getName() + "\\","
                      + "\\"email\\":\\"" + user.getEmail() + "\\""
                      + "}";
              }
            
            Benefits:
            ✅ No reflection = Native image compatible
            ✅ Faster (no reflection overhead)
            ✅ Type-safe at compile time
            ✅ Smaller binary size
            """;
    }
}
