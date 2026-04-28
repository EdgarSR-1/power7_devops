# Guía: Cómo Arreglar el Acceso a Grupos en el Bot

## 1. Verificar Qué Grupos Tiene Miguel en la BD

### Opción A: Desde Oracle SQL Developer / SQLPlus

```sql
-- 1. Encontrar el ID del usuario Miguel
SELECT ID, NAME, TELEGRAM_USER_ID, EMAIL 
FROM USERS 
WHERE TELEGRAM_USER_ID = 8261084667;
-- Resultado esperado: algo como ID=123, NAME='Miguel'

-- 2. Guardar ese ID en una variable (ejemplo: 123)

-- 3. Ver TODOS los grupos en el sistema
SELECT ID, NAME, CREATED_BY, CREATED_AT 
FROM TASKGROUPS 
ORDER BY ID;
-- Resultado esperado: lista de todos los grupos

-- 4. Ver grupos a los que Miguel tiene acceso (membresía explícita)
SELECT tg.ID, tg.NAME, gm.ROLE_ID, gm.JOINED_AT
FROM TASKGROUPS tg
INNER JOIN GROUP_MEMBERS gm ON tg.ID = gm.GROUP_ID
WHERE gm.USER_ID = 123;  -- Reemplazar 123 con el ID real

-- 5. Ver grupos que Miguel creó
SELECT ID, NAME, CREATED_AT
FROM TASKGROUPS
WHERE CREATED_BY = 123;  -- Reemplazar 123 con el ID real

-- 6. Ver grupos donde Miguel tiene tareas
SELECT DISTINCT tg.ID, tg.NAME
FROM TASKGROUPS tg
INNER JOIN TODOLISTS tl ON tg.ID = tl.GROUP_ID
INNER JOIN TASKS t ON tl.ID = t.TODOLIST_ID
WHERE t.CREATED_BY = 123;  -- Reemplazar 123 con el ID real

-- TOTAL: Grupos a los que Miguel PUEDE acceder:
SELECT DISTINCT tg.ID, tg.NAME, 'MIEMBRO' as TIPO
FROM TASKGROUPS tg
INNER JOIN GROUP_MEMBERS gm ON tg.ID = gm.GROUP_ID
WHERE gm.USER_ID = 123

UNION

SELECT ID, NAME, 'CREADOR'
FROM TASKGROUPS WHERE CREATED_BY = 123

UNION

SELECT DISTINCT tg.ID, tg.NAME, 'CREADOR_TAREAS'
FROM TASKGROUPS tg
INNER JOIN TODOLISTS tl ON tg.ID = tl.GROUP_ID
INNER JOIN TASKS t ON tl.ID = t.TODOLIST_ID
WHERE t.CREATED_BY = 123

ORDER BY ID;
```

### Opción B: Desde Postman (usando API)

```
GET /api/groups/user
Authorization: Bearer <JWT_TOKEN>
```

Respuesta mostrará todos los grupos a los que el usuario tiene acceso.

### Opción C: Ejecutar SQL desde Java

Crear un endpoint temporal en GroupController:

```java
@GetMapping("/debug/user-groups/{telegramUserId}")
public ResponseEntity<?> debugUserGroups(@PathVariable Long telegramUserId) {
    User user = userRepository.findByTelegramUserId(telegramUserId);
    if (user == null) {
        return ResponseEntity.notFound().build();
    }
    
    // Membresías
    List<GroupMember> memberships = groupMemberRepository.findByUserId(user.getId());
    List<TaskGroup> membershipGroups = memberships.stream()
        .map(GroupMember::getGroup)
        .collect(Collectors.toList());
    
    // Grupos creados
    List<TaskGroup> createdGroups = taskGroupRepository.findByCreatedById(user.getId());
    
    // Grupos con tareas
    List<Task> createdTasks = taskRepository.findByCreatedById(user.getId());
    Set<Long> groupIdsFromTasks = createdTasks.stream()
        .map(t -> t.getTodoList().getGroup().getId())
        .collect(Collectors.toSet());
    
    return ResponseEntity.ok(Map.of(
        "user", user.getName(),
        "membershipGroups", membershipGroups,
        "createdGroups", createdGroups,
        "groupIdsWithTasks", groupIdsFromTasks,
        "totalAccessibleGroups", 
            membershipGroups.size() + createdGroups.size() + groupIdsFromTasks.size()
    ));
}
```

Luego ir a: `http://localhost:8080/api/groups/debug/user-groups/8261084667`

---

## 2. Implementar la Solución

### A. Crear método en TaskGroupService

Archivo: `backend/src/main/java/com/springboot/MyTodoList/service/TaskGroupService.java`

```java
package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.GroupMember;
import com.springboot.MyTodoList.model.Role;
import com.springboot.MyTodoList.model.RoleName;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskGroup;
import com.springboot.MyTodoList.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskGroupService {
    
    @Autowired
    private TaskGroupRepository taskGroupRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    // ... métodos existentes ...
    
    /**
     * Obtiene grupos a los que el usuario tiene acceso
     * 
     * Si es SUPERADMIN: todos los grupos
     * Si no: 
     *   - Grupos donde es miembro (GroupMember)
     *   - Grupos que creó
     *   - Grupos donde creó tareas
     */
    public List<TaskGroup> findAccessibleGroups(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        
        // SUPERADMIN tiene acceso a todo
        if (isSuperAdmin(user)) {
            return findAll();
        }
        
        Set<Long> accessibleGroupIds = new HashSet<>();
        
        // 1. Grupos donde el usuario es miembro
        List<GroupMember> memberships = groupMemberRepository.findByUserId(user.getId());
        memberships.forEach(m -> {
            if (m.getGroup() != null && m.getGroup().getId() != null) {
                accessibleGroupIds.add(m.getGroup().getId());
            }
        });
        
        // 2. Grupos que el usuario creó
        List<TaskGroup> createdGroups = taskGroupRepository.findByCreatedById(user.getId());
        createdGroups.forEach(g -> {
            if (g.getId() != null) {
                accessibleGroupIds.add(g.getId());
            }
        });
        
        // 3. Grupos donde el usuario creó tareas
        List<Task> createdTasks = taskRepository.findByCreatedById(user.getId());
        createdTasks.forEach(task -> {
            if (task.getTodoList() != null 
                && task.getTodoList().getGroup() != null 
                && task.getTodoList().getGroup().getId() != null) {
                accessibleGroupIds.add(task.getTodoList().getGroup().getId());
            }
        });
        
        // Si no tiene grupos accesibles, retornar lista vacía
        if (accessibleGroupIds.isEmpty()) {
            return List.of();
        }
        
        // Buscar todos los grupos por IDs
        return taskGroupRepository.findAllById(new ArrayList<>(accessibleGroupIds));
    }
    
    private boolean isSuperAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getName() == RoleName.SUPERADMIN;
    }
}
```

**NOTA:** Si el método `findAllById` no existe en TaskGroupRepository, añadirlo:

```java
@Repository
public interface TaskGroupRepository extends JpaRepository<TaskGroup, Long> {
    List<TaskGroup> findByCreatedById(Long userId);
    // Ya existe en JpaRepository: findAllById(Iterable<ID>)
}
```

### B. Actualizar BotActions.fnListGroups()

Archivo: `backend/src/main/java/com/springboot/MyTodoList/util/BotActions.java`

**Cambio:**

```java
public void fnListGroups() {
    if (!(requestText.equals(BotLabels.LIST_GROUP_TASKS.getLabel())
            || requestText.equals(BotLabels.SELECT_GROUP.getLabel())) || exit)
        return;

    // ANTES:
    // List<TaskGroup> groups = taskGroupService.findAll();
    
    // DESPUÉS:
    List<TaskGroup> groups = taskGroupService.findAccessibleGroups(requesterUser);
    
    ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
        .resizeKeyboard(true)
        .oneTimeKeyboard(false)
        .selective(true)
        .build();

    List<KeyboardRow> keyboard = new ArrayList<>();
    clearGroupSelectionButtons();
    KeyboardRow topRow = new KeyboardRow();
    topRow.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
    keyboard.add(topRow);

    KeyboardRow titleRow = new KeyboardRow();
    titleRow.add(BotLabels.SELECT_GROUP.getLabel());
    keyboard.add(titleRow);

    for (TaskGroup group : groups) {
        KeyboardRow row = new KeyboardRow();
        row.add(registerGroupSelectionButton(group.getName(), GROUP_SELECTION_PREFIX + group.getId()));
        keyboard.add(row);
    }

    keyboardMarkup.setKeyboard(keyboard);
    BotHelper.sendMessageToTelegram(chatId, "Select a group", telegramClient, keyboardMarkup);
    exit = true;
}
```

---

## 3. Validar la Solución

### Test 1: Verificar que solo ve grupos accesibles

```bash
# 1. Hacer login en Telegram bot como Miguel
/start

# 2. Hacer clic en "List Group"
# Resultado esperado: Ver SOLO los grupos a los que tiene acceso

# 3. Seleccionar un grupo
# Resultado esperado: DEBE funcionar ahora

# 4. Crear una tarea
# Resultado esperado: DEBE funcionar sin error
```

### Test 2: Verificar que NO ve grupos de otros

```bash
# Crear un grupo nuevo como otro usuario
# Hacer login como Miguel
# Hacer clic en "List Group"
# Resultado esperado: El nuevo grupo NO aparece en la lista
```

### Test 3: SQL - Comparación

Antes y después de la solución:

```sql
-- ANTES (fnListGroups devuelve todo):
SELECT COUNT(*) FROM TASKGROUPS;
-- Resultado: 50 grupos

-- DESPUÉS (fnListGroups filtra):
-- El bot solo muestra los grupos accesibles a Miguel
-- Resultado esperado en el bot: 3-5 grupos (no 50)
```

---

## 4. Cambios Adicionales Opcionales

### Opción: Actualizar TaskService.validateGroupAccess()

Actualmente: Solo valida si es miembro (GroupMember)
Debería: También permitir si creó el grupo o las tareas

```java
private boolean belongsToGroup(User user, Long groupId) {
    if (user == null || user.getId() == null || groupId == null) {
        return false;
    }

    // Verificar membresía explícita
    if (groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
        return true;
    }
    
    // Verificar si el usuario creó el grupo
    TaskGroup group = taskGroupRepository.findById(groupId).orElse(null);
    if (group != null && group.getCreatedBy() != null 
        && group.getCreatedBy().getId().equals(user.getId())) {
        return true;
    }
    
    return false;
}
```

Esto sería un "bonus" que haría que la autorización sea más flexible.

---

## 5. Resumen de Cambios

| Archivo | Cambio | Complejidad |
|---------|--------|------------|
| TaskGroupService.java | + método `findAccessibleGroups()` | 🟢 Bajo |
| BotActions.java | Cambiar `findAll()` a `findAccessibleGroups()` | 🟢 Bajo |
| TaskService.java (opcional) | Mejorar `belongsToGroup()` | 🟡 Medio |

**Tiempo total:** 30-45 minutos

---

## 6. Checklist de Implementación

- [ ] Leer el análisis en AUTHORIZATION_ANALYSIS.md
- [ ] Ejecutar query SQL para ver qué grupos tiene Miguel
- [ ] Crear método `findAccessibleGroups()` en TaskGroupService
- [ ] Cambiar BotActions.fnListGroups() a usar el nuevo método
- [ ] Compilar y testear en local
- [ ] Testear en contenedor Docker
- [ ] Verificar que otros usuarios no se ven afectados
- [ ] (Opcional) Mejorar `belongsToGroup()` en TaskService
- [ ] Hacer commit y push

