// Example of what a generated database mapper would look like

public class DatabaseMapper {
    
    public static String example() {
        return """
            
            === Generated Database Mapper Pattern ===
            
            Instead of:
              // Reflection-based ORM
              String table = entity.getClass().getAnnotation(Table.class).value();
              for (Field field : entity.getClass().getDeclaredFields()) {
                  if (field.isAnnotationPresent(Column.class)) {
                      // Use reflection to get column name and value
                  }
              }
            
            Use:
              // Generated mapper (no reflection)
              public class UserMapper {
                  private static final String TABLE = "users";
                  private static final String[] COLUMNS = {"id", "name", "email"};
                  
                  public static String insert(User user) {
                      return "INSERT INTO " + TABLE + " (id, name, email) "
                           + "VALUES (" + user.getId() + ", "
                           + "'" + user.getName() + "', "
                           + "'" + user.getEmail() + "')";
                  }
                  
                  public static User fromResultSet(ResultSet rs) throws SQLException {
                      User user = new User();
                      user.setId(rs.getLong("id"));
                      user.setName(rs.getString("name"));
                      user.setEmail(rs.getString("email"));
                      return user;
                  }
              }
            
            Real frameworks using this:
            - Micronaut Data (compile-time queries)
            - jOOQ (type-safe SQL)
            - JDBI (with code generation)
            """;
    }
}
