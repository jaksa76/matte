let currentEntity = null;
let currentId = null;
let entityFields = {};

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
    await loadEntityList();
    const firstEntity = document.querySelector('.sidebar li');
    if (firstEntity) {
        firstEntity.click();
    }
});

// Load entity list from server
async function loadEntityList() {
    try {
        const response = await fetch('/api/entities');
        const entities = await response.json();
        
        const entityList = document.getElementById('entityList');
        entityList.innerHTML = '';
        
        entities.forEach(entityName => {
            const li = document.createElement('li');
            li.dataset.entity = entityName;
            li.textContent = capitalize(entityName);
            li.addEventListener('click', () => {
                document.querySelectorAll('.sidebar li').forEach(l => l.classList.remove('active'));
                li.classList.add('active');
                currentEntity = entityName;
                loadEntities();
            });
            entityList.appendChild(li);
        });
    } catch (error) {
        console.error('Failed to load entity list:', error);
    }
}

// Create button
document.getElementById('createBtn').addEventListener('click', () => {
    currentId = null;
    openModal('Create');
});

// Load entities
async function loadEntities() {
    const response = await fetch(`/api/${currentEntity}`);
    const entities = await response.json();
    
    document.getElementById('pageTitle').textContent = capitalize(currentEntity);
    document.getElementById('createBtn').style.display = 'block';
    
    if (entities.length === 0) {
        document.getElementById('content').innerHTML = `
            <div class="empty-state">
                <h3>No ${currentEntity} yet</h3>
                <p>Click the "Create New" button to add your first item.</p>
            </div>
        `;
        entityFields = {};
        return;
    }
    
    // Extract field names from first entity
    const fields = Object.keys(entities[0]);
    entityFields = fields.filter(f => f !== 'id');
    
    let html = '<table><thead><tr>';
    fields.forEach(field => {
        html += `<th>${capitalize(field)}</th>`;
    });
    html += '<th>Actions</th></tr></thead><tbody>';
    
    entities.forEach(entity => {
        html += '<tr>';
        fields.forEach(field => {
            html += `<td>${entity[field] !== null ? entity[field] : ''}</td>`;
        });
        html += `
            <td class="actions">
                <button class="btn btn-primary" onclick="editEntity(${entity.id})">Edit</button>
                <button class="btn btn-danger" onclick="deleteEntity(${entity.id})">Delete</button>
            </td>
        `;
        html += '</tr>';
    });
    
    html += '</tbody></table>';
    document.getElementById('content').innerHTML = html;
}

// Open modal
async function openModal(mode, id = null) {
    currentId = id;
    document.getElementById('modalTitle').textContent = `${mode} ${capitalize(currentEntity).slice(0, -1)}`;
    
    let entity = {};
    if (id) {
        const response = await fetch(`/api/${currentEntity}/${id}`);
        entity = await response.json();
    }
    
    let fieldsHtml = '';
    entityFields.forEach(field => {
        const value = entity[field] || '';
        fieldsHtml += `
            <div class="form-group">
                <label>${capitalize(field)}</label>
                <input type="text" name="${field}" value="${value}" required>
            </div>
        `;
    });
    
    document.getElementById('formFields').innerHTML = fieldsHtml;
    document.getElementById('modal').classList.add('show');
}

// Close modal
function closeModal() {
    document.getElementById('modal').classList.remove('show');
    document.getElementById('entityForm').reset();
}

// Edit entity
async function editEntity(id) {
    await openModal('Edit', id);
}

// Delete entity
async function deleteEntity(id) {
    if (!confirm('Are you sure you want to delete this item?')) return;
    
    await fetch(`/api/${currentEntity}/${id}`, { method: 'DELETE' });
    loadEntities();
}

// Form submission
document.getElementById('entityForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const data = {};
    formData.forEach((value, key) => {
        data[key] = value;
    });
    
    const url = currentId ? `/api/${currentEntity}/${currentId}` : `/api/${currentEntity}`;
    const method = currentId ? 'PUT' : 'POST';
    
    await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    
    closeModal();
    loadEntities();
});

// Utility function
function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}
