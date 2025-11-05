package io.matte;

import java.util.Map;
import java.util.List;

public class EntityController<T extends Entity> {
    private final Repository<T> repository;
    private final String resourceName;
    private final String basePath;
    private final EntityFactory<T> entityFactory;

    public EntityController(Repository<T> repository, String resourceName, EntityFactory<T> entityFactory) {
        this.repository = repository;
        this.resourceName = resourceName;
        this.basePath = "/api/" + resourceName;
        this.entityFactory = entityFactory;
    }

    public String handleRequest(String method, String path, String body) {
        try {
            if (method.equals("GET") && path.equals(basePath)) {
                return getAll();
            } else if (method.equals("GET") && path.startsWith(basePath + "/")) {
                String idStr = path.substring(basePath.length() + 1);
                return getById(Long.parseLong(idStr));
            } else if (method.equals("POST") && path.equals(basePath)) {
                return create(body);
            } else if (method.equals("PUT") && path.startsWith(basePath + "/")) {
                String idStr = path.substring(basePath.length() + 1);
                return update(Long.parseLong(idStr), body);
            } else if (method.equals("DELETE") && path.startsWith(basePath + "/")) {
                String idStr = path.substring(basePath.length() + 1);
                return delete(Long.parseLong(idStr));
            } else {
                return errorResponse("Not Found", 404);
            }
        } catch (NumberFormatException e) {
            return errorResponse("Invalid ID format", 400);
        } catch (Exception e) {
            return errorResponse("Internal Server Error: " + e.getMessage(), 500);
        }
    }

    private String getAll() {
        List<T> entities = repository.findAll();
        StringBuilder json = new StringBuilder();
        json.append("[");
        boolean first = true;
        for (T entity : entities) {
            if (!first) json.append(",");
            first = false;
            json.append(JsonSerializer.toJson(entity));
        }
        json.append("]");
        return json.toString();
    }

    private String getById(Long id) {
        T entity = repository.findById(id);
        if (entity == null) {
            return errorResponse(capitalize(resourceName) + " not found", 404);
        }
        return JsonSerializer.toJson(entity);
    }

    private String create(String body) {
        T entity = entityFactory.create();
        populateEntityFromJson(entity, body);
        repository.save(entity);
        return JsonSerializer.toJson(entity);
    }

    private String update(Long id, String body) {
        T entity = repository.findById(id);
        if (entity == null) {
            return errorResponse(capitalize(resourceName) + " not found", 404);
        }
        populateEntityFromJson(entity, body);
        repository.save(entity);
        return JsonSerializer.toJson(entity);
    }

    private String delete(Long id) {
        T entity = repository.findById(id);
        if (entity == null) {
            return errorResponse(capitalize(resourceName) + " not found", 404);
        }
        repository.deleteById(id);
        return "{\"message\":\"" + capitalize(resourceName) + " deleted successfully\"}";
    }

    private void populateEntityFromJson(T entity, String json) {
        for (Map.Entry<Object, Field<?>> entry : entity.data.entrySet()) {
            Field field = entry.getValue();
            String fieldName = field.fieldName().toString();
            
            // Skip the id field for creation
            if (fieldName.equals("id")) continue;
            
            if (field.type() == String.class) {
                String value = extractJsonValue(json, fieldName);
                if (value != null) {
                    ((Field<String>) field).set(value);
                }
            } else if (field.type() == Integer.class) {
                String value = extractJsonValue(json, fieldName);
                if (value != null) {
                    try {
                        ((Field<Integer>) field).set(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // Skip invalid integers
                    }
                }
            } else if (field.type() == Long.class && !fieldName.equals("id")) {
                String value = extractJsonValue(json, fieldName);
                if (value != null) {
                    try {
                        ((Field<Long>) field).set(Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        // Skip invalid longs
                    }
                }
            } else if (field.type() == Boolean.class) {
                String value = extractJsonValue(json, fieldName);
                if (value != null) {
                    ((Field<Boolean>) field).set(Boolean.parseBoolean(value));
                }
            }
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        int valueStart = json.indexOf("\"", colonIndex);
        if (valueStart == -1) return null;
        valueStart++;
        
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd == -1) return null;
        
        return json.substring(valueStart, valueEnd);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String errorResponse(String message, int statusCode) {
        return "{\"error\":\"" + message + "\",\"status\":" + statusCode + "}";
    }
}
