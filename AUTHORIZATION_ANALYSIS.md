# Análisis de Autorización - Bot vs Backend

## 1. El Problema: Mismatch Arquitectónico

### ¿Cómo ve grupos el usuario?
```
Bot Menu: "Select a group"
          ↓
fnListGroups() → taskGroupService.findAll()
          ↓
Devuelve: TODOS los grupos en la BD
          ↓
Usuario ve: [Equipo1, Equipo52, Equipo99, MiGrupo, OtroGrupo, ...]
```

### ¿Por qué no puede acceder?
```
Usuario hace click en "Equipo52"
          ↓
fnListGroupTasks() → renderGroupTasksMenu(groupId=52)
          ↓
taskService.getTasksByGroupId(52, currentUser)
          ↓
validateGroupAccess(currentUser, groupId=52)
          ↓
belongsToGroup(user, 52)
  → groupMemberRepository.existsByGroupIdAndUserId(52, userId)
          ↓
¿Existe GroupMember(group=52, user=currentUser)?
  - NO → ❌ "You do not have access to this group"
  - SÍ → ✅ Acceso permitido
```

## 2. Cómo Funciona la Autorización Actual

### Tabla de Relaciones en la BD

```
┌─────────────────────┐
│      USERS          │
├─────────────────────┤
│ ID (PK)             │ ← 8261084667 (Miguel, telegramUserId)
│ NAME                │
│ EMAIL               │
│ ROLE (FK → ROLES)   │ ← SUPERADMIN, ADMIN, o USUARIO
│ TELEGRAM_USER_ID    │
└─────────────────────┘

┌──────────────────────────┐
│      TASKGROUPS          │
├──────────────────────────┤
│ ID (PK)                  │
│ NAME                     │ ← "Equipo52", "MiGrupo", etc.
│ CREATED_BY (FK → USERS)  │ ← Quién creó el grupo
│ CREATED_AT               │
└──────────────────────────┘

┌──────────────────────────────────────┐
│      GROUP_MEMBERS                   │
├──────────────────────────────────────┤
│ ID (PK)                              │
│ GROUP_ID (FK → TASKGROUPS)           │ ← grupo al que pertenece
│ USER_ID (FK → USERS)                 │ ← usuario que es miembro
│ ROLE_ID (FK → ROLES)                 │ ← rol del usuario en el grupo
│ JOINED_AT                            │
├──────────────────────────────────────┤
│ UNIQUE(GROUP_ID, USER_ID)            │ ← Un usuario solo 1 vez por grupo
└──────────────────────────────────────┘
```

### Reglas de Acceso (TaskService.validateGroupAccess)

```
IF user.isSuperAdmin():
    ✅ ACCESO TOTAL a todos los grupos
ELSE IF groupMemberRepository.existsByGroupIdAndUserId(groupId, user.id):
    ✅ ACCESO al grupo (es miembro)
ELSE:
    ❌ ACCESO DENEGADO ("You do not have access to this group")
```

**NOTA:** La regla NO contempla que el usuario sea el creador del grupo.

## 3. Comparación: API vs Bot

### ✅ API (GroupController) - CORRECTO

```java
List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
// Grupo 1: Usuario es miembro

List<TaskGroup> createdGroups = taskGroupRepository.findByCreatedById(userId);
// Grupo 2: Usuario lo creó

List<Task> createdTasks = taskRepository.findByCreatedById(userId);
// Grupo 3: Usuario creaturas tareas en este grupo

// Resultado: Se muestran solo grupos accesibles
```

### ❌ Bot (BotActions) - INCORRECTO

```java
List<TaskGroup> groups = taskGroupService.findAll();
// Devuelve: TODOS los grupos, sin filtrar por usuario

// Resultado: Se muestran grupos que el usuario no puede ver
```

## 4. ¿Qué tiene realmente el usuario Miguel?

### Consulta en la BD

```sql
-- Grupos donde Miguel es miembro
SELECT tg.id, tg.name, 'MEMBER' as relation
FROM taskgroups tg
INNER JOIN group_members gm ON tg.id = gm.group_id
WHERE gm.user_id = (SELECT id FROM users WHERE telegram_user_id = 8261084667)

UNION

-- Grupos que Miguel creó
SELECT id, name, 'CREATED_BY' as relation
FROM taskgroups
WHERE created_by = (SELECT id FROM users WHERE telegram_user_id = 8261084667)

UNION

-- Grupos donde Miguel creó tareas
SELECT DISTINCT tg.id, tg.name, 'HAS_TASKS' as relation
FROM taskgroups tg
INNER JOIN todolists tl ON tg.id = tl.group_id
INNER JOIN tasks t ON tl.id = t.todolist_id
WHERE t.created_by = (SELECT id FROM users WHERE telegram_user_id = 8261084667);
```

**Resultado esperado:** Estos son los ÚNICOS grupos a los que Miguel debería poder acceder.

## 5. Soluciones Posibles

### Opción 1: MÍNIMA (Bot-only) - 15 min
**Cambio:** Filtrar en fnListGroups() sin tocar backend

```java
// En BotActions.fnListGroups()
// Antes:
List<TaskGroup> groups = taskGroupService.findAll();

// Después:
List<TaskGroup> groups = taskGroupService.findAccessibleGroups(requesterUser);
// O implementar filtrado local:
List<TaskGroup> allGroups = taskGroupService.findAll();
List<TaskGroup> groups = allGroups.stream()
    .filter(group -> canAccessGroup(requesterUser, group.getId()))
    .collect(Collectors.toList());
```

**Ventajas:**
- No toca base de datos
- No toca validación de acceso
- Rápido de implementar

**Desventajas:**
- Duplica lógica de autorización
- No soluciona el problema de fondo

---

### Opción 2: MEDIANA (TaskGroupService) - 30 min
**Cambio:** Añadir método filtrado en TaskGroupService

```java
// En TaskGroupService
public List<TaskGroup> findAccessibleGroups(User user) {
    if (isSuperAdmin(user)) {
        return findAll();
    }
    
    Set<Long> groupIds = new HashSet<>();
    
    // Grupos donde es miembro
    List<GroupMember> memberships = groupMemberRepository.findByUserId(user.getId());
    memberships.forEach(m -> groupIds.add(m.getGroup().getId()));
    
    // Grupos que creó
    List<TaskGroup> createdGroups = findByCreatedById(user.getId());
    createdGroups.forEach(g -> groupIds.add(g.getId()));
    
    // Grupos donde creó tareas
    List<Task> createdTasks = taskRepository.findByCreatedById(user.getId());
    createdTasks.forEach(t -> groupIds.add(t.getTodoList().getGroup().getId()));
    
    return findByIds(new ArrayList<>(groupIds));
}
```

Luego en BotActions:
```java
List<TaskGroup> groups = taskGroupService.findAccessibleGroups(requesterUser);
```

**Ventajas:**
- Centraliza lógica de autorización
- Consistente con API (GroupController)
- Reutilizable en otros lugares

**Desventajas:**
- Requiere añadir un repositorio.findByIds()
- Una query SQL más compleja

---

### Opción 3: COMPLETA (Refactorización) - 2 horas
**Cambio:** Refactorizar autorización a un servicio dedicado

```java
// Nuevo: AuthorizationService
public class AuthorizationService {
    
    public List<TaskGroup> getAccessibleGroups(User user) {
        // Implementa lógica centralizada
    }
    
    public boolean canAccessGroup(User user, Long groupId) {
        // Reutilizable en todos lados
    }
    
    public boolean canEditTask(User user, Task task) {
        // Validación completa
    }
    
    // ... etc
}
```

Luego:
- BotActions usa AuthorizationService
- TaskService usa AuthorizationService
- GroupController usa AuthorizationService
- Todos consistentes

**Ventajas:**
- Autorización centralizada
- Fácil de auditar
- Fácil de cambiar reglas globalmente
- Testeable

**Desventajas:**
- Requiere cambios en múltiples archivos
- Potencial de romper código existente

---

## 6. Recomendación

**Para producción:**
- **Opción 2 (Mediana)** es el balance perfecto
- Soluciona el problema de raíz
- Sin riesgo de romper código
- Toma 30 minutos

**Pasos:**
1. Crear `findAccessibleGroups(User)` en TaskGroupService
2. Cambiar BotActions.fnListGroups() a usar ese método
3. Verificar que GroupController ya usa la lógica equivalente
4. Testear con usuario real

---

## 7. Query para Verificar Datos Actuales

```sql
-- Ejecutar en SQLPlus o SQL Developer con cuenta admin

-- Usuario Miguel
SELECT * FROM users WHERE telegram_user_id = 8261084667;

-- Grupos a los que puede acceder
SELECT 
    tg.ID,
    tg.NAME,
    'MEMBER' as ACCESS_TYPE,
    gm.ROLE_ID,
    SYSDATE as JOINED_AT
FROM taskgroups tg
INNER JOIN group_members gm ON tg.id = gm.group_id
WHERE gm.user_id = (SELECT id FROM users WHERE telegram_user_id = 8261084667)

UNION ALL

SELECT 
    ID,
    NAME,
    'CREATED',
    NULL,
    CREATED_AT
FROM taskgroups
WHERE created_by = (SELECT id FROM users WHERE telegram_user_id = 8261084667)

UNION ALL

SELECT DISTINCT
    tg.ID,
    tg.NAME,
    'HAS_TASKS',
    NULL,
    tg.CREATED_AT
FROM taskgroups tg
INNER JOIN todolists tl ON tg.id = tl.group_id
INNER JOIN tasks t ON tl.id = t.todolist_id
WHERE t.created_by = (SELECT id FROM users WHERE telegram_user_id = 8261084667)
ORDER BY ID;

-- Todos los grupos
SELECT ID, NAME, CREATED_BY FROM taskgroups ORDER BY ID;

-- Miembros del grupo "Equipo52" (ejemplo)
SELECT u.NAME, gm.ROLE_ID
FROM group_members gm
INNER JOIN users u ON gm.user_id = u.id
WHERE gm.group_id = (SELECT id FROM taskgroups WHERE name = 'Equipo52')
ORDER BY u.name;
```

