package io.matte;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.HashMap;

public class Matte {
    private final Map<String, EntityController<?>> controllers = new HashMap<>();
    private final Map<String, Repository<?>> repositories = new HashMap<>();
    private HttpServer server;
    private final int port;

    public Matte() {
        this(8080);
    }

    public Matte(int port) {
        this.port = port;
    }

    public <T extends Entity> Matte register(String resourceName, EntityFactory<T> factory) {
        // Create repository and controller for this entity
        Repository<T> repository = new Repository<>(resourceName);
        EntityController<T> controller = new EntityController<>(repository, resourceName, factory);
        
        // Store them
        controllers.put(resourceName, controller);
        repositories.put(resourceName, repository);
        
        System.out.println("✅ Registered entity: " + resourceName);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Repository<T> getRepository(String resourceName) {
        return (Repository<T>) repositories.get(resourceName);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> EntityController<T> getController(String resourceName) {
        return (EntityController<T>) controllers.get(resourceName);
    }

    public Matte start() throws IOException {
        if (controllers.isEmpty()) {
            System.out.println("⚠️  No entities registered. Register entities before starting the server.");
            return this;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Create UI context for root path
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                
                // Only handle root path, let other contexts handle their paths
                if (path.equals("/")) {
                    String html = generateUI();
                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, html.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(html.getBytes());
                    os.close();
                } else {
                    // Pass through to other handlers
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                }
            }
        });

        // Create context for each registered entity
        for (Map.Entry<String, EntityController<?>> entry : controllers.entrySet()) {
            String resourceName = entry.getKey();
            EntityController<?> controller = entry.getValue();
            
            server.createContext("/api/" + resourceName, new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String method = exchange.getRequestMethod();
                    String path = exchange.getRequestURI().getPath();
                    
                    String body = "";
                    if (method.equals("POST") || method.equals("PUT")) {
                        body = new String(exchange.getRequestBody().readAllBytes());
                    }
                    
                    String response = controller.handleRequest(method, path, body);
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            });
        }

        server.setExecutor(null);
        server.start();

        System.out.println("\n🚀 Server started on http://localhost:" + port);
        printEndpoints();
        
        return this;
    }

    private void printEndpoints() {
        for (String resourceName : controllers.keySet()) {
            System.out.println("\n📋 " + capitalize(resourceName) + " endpoints:");
            System.out.println("  GET    /api/" + resourceName + "          - Get all " + resourceName);
            System.out.println("  GET    /api/" + resourceName + "/{id}     - Get " + resourceName.substring(0, resourceName.length() - 1) + " by ID");
            System.out.println("  POST   /api/" + resourceName + "          - Create new " + resourceName.substring(0, resourceName.length() - 1));
            System.out.println("  PUT    /api/" + resourceName + "/{id}     - Update " + resourceName.substring(0, resourceName.length() - 1));
            System.out.println("  DELETE /api/" + resourceName + "/{id}     - Delete " + resourceName.substring(0, resourceName.length() - 1) + " by ID");
        }
        
        System.out.println("\n📝 Example commands:");
        String firstResource = controllers.keySet().iterator().next();
        System.out.println("  curl http://localhost:" + port + "/api/" + firstResource);
    }

    private String generateUI() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Matte Admin</title>\n");
        html.append("    <style>\n");
        html.append("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }\n");
        html.append("        .container { display: flex; height: 100vh; }\n");
        html.append("        .sidebar { width: 250px; background: #2c3e50; color: white; padding: 20px; }\n");
        html.append("        .sidebar h1 { font-size: 24px; margin-bottom: 30px; }\n");
        html.append("        .sidebar ul { list-style: none; }\n");
        html.append("        .sidebar li { padding: 12px; margin: 5px 0; cursor: pointer; border-radius: 5px; transition: background 0.2s; }\n");
        html.append("        .sidebar li:hover { background: #34495e; }\n");
        html.append("        .sidebar li.active { background: #3498db; }\n");
        html.append("        .main { flex: 1; padding: 30px; background: #ecf0f1; overflow-y: auto; }\n");
        html.append("        .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n");
        html.append("        .header h2 { font-size: 28px; color: #2c3e50; }\n");
        html.append("        .btn { padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; font-size: 14px; transition: all 0.2s; }\n");
        html.append("        .btn-primary { background: #3498db; color: white; }\n");
        html.append("        .btn-primary:hover { background: #2980b9; }\n");
        html.append("        .btn-success { background: #27ae60; color: white; }\n");
        html.append("        .btn-success:hover { background: #229954; }\n");
        html.append("        .btn-danger { background: #e74c3c; color: white; }\n");
        html.append("        .btn-danger:hover { background: #c0392b; }\n");
        html.append("        .btn-secondary { background: #95a5a6; color: white; }\n");
        html.append("        .btn-secondary:hover { background: #7f8c8d; }\n");
        html.append("        table { width: 100%; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("        th, td { padding: 15px; text-align: left; border-bottom: 1px solid #ecf0f1; }\n");
        html.append("        th { background: #34495e; color: white; font-weight: 600; }\n");
        html.append("        tr:hover { background: #f8f9fa; }\n");
        html.append("        .actions { display: flex; gap: 10px; }\n");
        html.append("        .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 1000; }\n");
        html.append("        .modal.show { display: flex; justify-content: center; align-items: center; }\n");
        html.append("        .modal-content { background: white; padding: 30px; border-radius: 8px; width: 500px; max-width: 90%; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }\n");
        html.append("        .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n");
        html.append("        .modal-header h3 { font-size: 22px; color: #2c3e50; }\n");
        html.append("        .close { font-size: 28px; cursor: pointer; color: #95a5a6; }\n");
        html.append("        .close:hover { color: #2c3e50; }\n");
        html.append("        .form-group { margin-bottom: 15px; }\n");
        html.append("        .form-group label { display: block; margin-bottom: 5px; color: #2c3e50; font-weight: 500; }\n");
        html.append("        .form-group input { width: 100%; padding: 10px; border: 1px solid #bdc3c7; border-radius: 5px; font-size: 14px; }\n");
        html.append("        .form-group input:focus { outline: none; border-color: #3498db; }\n");
        html.append("        .form-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }\n");
        html.append("        .empty-state { text-align: center; padding: 60px 20px; color: #95a5a6; }\n");
        html.append("        .empty-state h3 { font-size: 20px; margin-bottom: 10px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        html.append("        <div class=\"sidebar\">\n");
        html.append("            <h1>🎯 Matte Admin</h1>\n");
        html.append("            <ul id=\"entityList\">\n");
        
        // Generate sidebar items for each entity
        for (String resourceName : controllers.keySet()) {
            html.append("                <li data-entity=\"").append(resourceName).append("\">")
                .append(capitalize(resourceName)).append("</li>\n");
        }
        
        html.append("            </ul>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"main\">\n");
        html.append("            <div class=\"header\">\n");
        html.append("                <h2 id=\"pageTitle\">Select an entity</h2>\n");
        html.append("                <button class=\"btn btn-primary\" id=\"createBtn\" style=\"display:none;\">+ Create New</button>\n");
        html.append("            </div>\n");
        html.append("            <div id=\"content\"></div>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("\n");
        html.append("    <!-- Modal for Create/Edit -->\n");
        html.append("    <div class=\"modal\" id=\"modal\">\n");
        html.append("        <div class=\"modal-content\">\n");
        html.append("            <div class=\"modal-header\">\n");
        html.append("                <h3 id=\"modalTitle\">Create Item</h3>\n");
        html.append("                <span class=\"close\" onclick=\"closeModal()\">&times;</span>\n");
        html.append("            </div>\n");
        html.append("            <form id=\"entityForm\">\n");
        html.append("                <div id=\"formFields\"></div>\n");
        html.append("                <div class=\"form-actions\">\n");
        html.append("                    <button type=\"button\" class=\"btn btn-secondary\" onclick=\"closeModal()\">Cancel</button>\n");
        html.append("                    <button type=\"submit\" class=\"btn btn-success\">Save</button>\n");
        html.append("                </div>\n");
        html.append("            </form>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("\n");
        html.append("    <script>\n");
        html.append("        let currentEntity = null;\n");
        html.append("        let currentId = null;\n");
        html.append("        let entityFields = {};\n");
        html.append("\n");
        html.append("        // Initialize\n");
        html.append("        document.addEventListener('DOMContentLoaded', () => {\n");
        html.append("            const firstEntity = document.querySelector('.sidebar li');\n");
        html.append("            if (firstEntity) {\n");
        html.append("                firstEntity.click();\n");
        html.append("            }\n");
        html.append("        });\n");
        html.append("\n");
        html.append("        // Entity selection\n");
        html.append("        document.querySelectorAll('.sidebar li').forEach(li => {\n");
        html.append("            li.addEventListener('click', () => {\n");
        html.append("                document.querySelectorAll('.sidebar li').forEach(l => l.classList.remove('active'));\n");
        html.append("                li.classList.add('active');\n");
        html.append("                currentEntity = li.dataset.entity;\n");
        html.append("                loadEntities();\n");
        html.append("            });\n");
        html.append("        });\n");
        html.append("\n");
        html.append("        // Create button\n");
        html.append("        document.getElementById('createBtn').addEventListener('click', () => {\n");
        html.append("            currentId = null;\n");
        html.append("            openModal('Create');\n");
        html.append("        });\n");
        html.append("\n");
        html.append("        // Load entities\n");
        html.append("        async function loadEntities() {\n");
        html.append("            const response = await fetch(`/api/${currentEntity}`);\n");
        html.append("            const entities = await response.json();\n");
        html.append("            \n");
        html.append("            document.getElementById('pageTitle').textContent = capitalize(currentEntity);\n");
        html.append("            document.getElementById('createBtn').style.display = 'block';\n");
        html.append("            \n");
        html.append("            if (entities.length === 0) {\n");
        html.append("                document.getElementById('content').innerHTML = `\n");
        html.append("                    <div class=\"empty-state\">\n");
        html.append("                        <h3>No ${currentEntity} yet</h3>\n");
        html.append("                        <p>Click the \"Create New\" button to add your first item.</p>\n");
        html.append("                    </div>\n");
        html.append("                `;\n");
        html.append("                entityFields = {};\n");
        html.append("                return;\n");
        html.append("            }\n");
        html.append("            \n");
        html.append("            // Extract field names from first entity\n");
        html.append("            const fields = Object.keys(entities[0]);\n");
        html.append("            entityFields = fields.filter(f => f !== 'id');\n");
        html.append("            \n");
        html.append("            let html = '<table><thead><tr>';\n");
        html.append("            fields.forEach(field => {\n");
        html.append("                html += `<th>${capitalize(field)}</th>`;\n");
        html.append("            });\n");
        html.append("            html += '<th>Actions</th></tr></thead><tbody>';\n");
        html.append("            \n");
        html.append("            entities.forEach(entity => {\n");
        html.append("                html += '<tr>';\n");
        html.append("                fields.forEach(field => {\n");
        html.append("                    html += `<td>${entity[field] !== null ? entity[field] : ''}</td>`;\n");
        html.append("                });\n");
        html.append("                html += `\n");
        html.append("                    <td class=\"actions\">\n");
        html.append("                        <button class=\"btn btn-primary\" onclick=\"editEntity(${entity.id})\">Edit</button>\n");
        html.append("                        <button class=\"btn btn-danger\" onclick=\"deleteEntity(${entity.id})\">Delete</button>\n");
        html.append("                    </td>\n");
        html.append("                `;\n");
        html.append("                html += '</tr>';\n");
        html.append("            });\n");
        html.append("            \n");
        html.append("            html += '</tbody></table>';\n");
        html.append("            document.getElementById('content').innerHTML = html;\n");
        html.append("        }\n");
        html.append("\n");
        html.append("        // Open modal\n");
        html.append("        async function openModal(mode, id = null) {\n");
        html.append("            currentId = id;\n");
        html.append("            document.getElementById('modalTitle').textContent = `${mode} ${capitalize(currentEntity).slice(0, -1)}`;\n");
        html.append("            \n");
        html.append("            let entity = {};\n");
        html.append("            if (id) {\n");
        html.append("                const response = await fetch(`/api/${currentEntity}/${id}`);\n");
        html.append("                entity = await response.json();\n");
        html.append("            }\n");
        html.append("            \n");
        html.append("            let fieldsHtml = '';\n");
        html.append("            entityFields.forEach(field => {\n");
        html.append("                const value = entity[field] || '';\n");
        html.append("                fieldsHtml += `\n");
        html.append("                    <div class=\"form-group\">\n");
        html.append("                        <label>${capitalize(field)}</label>\n");
        html.append("                        <input type=\"text\" name=\"${field}\" value=\"${value}\" required>\n");
        html.append("                    </div>\n");
        html.append("                `;\n");
        html.append("            });\n");
        html.append("            \n");
        html.append("            document.getElementById('formFields').innerHTML = fieldsHtml;\n");
        html.append("            document.getElementById('modal').classList.add('show');\n");
        html.append("        }\n");
        html.append("\n");
        html.append("        // Close modal\n");
        html.append("        function closeModal() {\n");
        html.append("            document.getElementById('modal').classList.remove('show');\n");
        html.append("            document.getElementById('entityForm').reset();\n");
        html.append("        }\n");
        html.append("\n");
        html.append("        // Edit entity\n");
        html.append("        async function editEntity(id) {\n");
        html.append("            await openModal('Edit', id);\n");
        html.append("        }\n");
        html.append("\n");
        html.append("        // Delete entity\n");
        html.append("        async function deleteEntity(id) {\n");
        html.append("            if (!confirm('Are you sure you want to delete this item?')) return;\n");
        html.append("            \n");
        html.append("            await fetch(`/api/${currentEntity}/${id}`, { method: 'DELETE' });\n");
        html.append("            loadEntities();\n");
        html.append("        }\n");
        html.append("\n");
        html.append("        // Form submission\n");
        html.append("        document.getElementById('entityForm').addEventListener('submit', async (e) => {\n");
        html.append("            e.preventDefault();\n");
        html.append("            \n");
        html.append("            const formData = new FormData(e.target);\n");
        html.append("            const data = {};\n");
        html.append("            formData.forEach((value, key) => {\n");
        html.append("                data[key] = value;\n");
        html.append("            });\n");
        html.append("            \n");
        html.append("            const url = currentId ? `/api/${currentEntity}/${currentId}` : `/api/${currentEntity}`;\n");
        html.append("            const method = currentId ? 'PUT' : 'POST';\n");
        html.append("            \n");
        html.append("            await fetch(url, {\n");
        html.append("                method: method,\n");
        html.append("                headers: { 'Content-Type': 'application/json' },\n");
        html.append("                body: JSON.stringify(data)\n");
        html.append("            });\n");
        html.append("            \n");
        html.append("            closeModal();\n");
        html.append("            loadEntities();\n");
        html.append("        });\n");
        html.append("\n");
        html.append("        // Utility function\n");
        html.append("        function capitalize(str) {\n");
        html.append("            return str.charAt(0).toUpperCase() + str.slice(1);\n");
        html.append("        }\n");
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 Server stopped");
        }
    }
}
