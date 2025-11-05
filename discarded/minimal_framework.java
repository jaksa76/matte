///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+

// Simple entity - NO ANNOTATIONS!
class User {
    private Long id;
    private String name;
    private String email;
    
    public User() {}
    
    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

// Manual mapper - explicit, no magic, no reflection
class UserMapper {
    public static String toJson(User user) {
        return String.format(
            "{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\"}",
            user.getId(),
            escape(user.getName()),
            escape(user.getEmail())
        );
    }
    
    public static User fromJson(String json) {
        // Simple manual parsing (use a real JSON library in production)
        User user = new User();
        String content = json.substring(1, json.length() - 1);
        for (String pair : content.split(",")) {
            String[] kv = pair.split(":", 2);
            String key = kv[0].replaceAll("\"", "").trim();
            String value = kv[1].replaceAll("\"", "").trim();
            
            switch (key) {
                case "id" -> user.setId(Long.parseLong(value));
                case "name" -> user.setName(value);
                case "email" -> user.setEmail(value);
            }
        }
        return user;
    }
    
    public static String toInsertSQL(User user) {
        return String.format(
            "INSERT INTO users (id, name, email) VALUES (%d, '%s', '%s')",
            user.getId(),
            escape(user.getName()),
            escape(user.getEmail())
        );
    }
    
    private static String escape(String str) {
        return str == null ? "" : str.replace("'", "''");
    }
}

public class minimal_framework {
    public static void main(String[] args) {
        System.out.println("=== Minimalist Framework (No Annotations) ===\n");
        
        User user = new User(1L, "Jane Smith", "jane@example.com");
        
        // JSON
        String json = UserMapper.toJson(user);
        System.out.println("JSON: " + json);
        
        // Parse back
        User parsed = UserMapper.fromJson(json);
        System.out.println("Parsed: " + parsed.getName() + " (" + parsed.getEmail() + ")");
        
        // SQL
        System.out.println("SQL: " + UserMapper.toInsertSQL(user));
        
        System.out.println("\n✅ No annotations needed!");
        System.out.println("✅ No reflection!");
        System.out.println("✅ No code generation!");
        System.out.println("✅ Just plain Java!");
        
        System.out.println("\n--- When to use each approach ---");
        System.out.println("Manual Mappers: Small projects, few entities, want simplicity");
        System.out.println("Annotations + Generation: Many entities, want consistency, DRY principle");
    }
}
