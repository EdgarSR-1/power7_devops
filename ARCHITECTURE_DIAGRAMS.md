# Diagrama Visual: Autorización Bot vs API

## 1. Estado Actual (INCORRECTO)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    TELEGRAM BOT - fnListGroups()                    │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ↓
                    taskGroupService.findAll()
                         (SIN FILTRADO)
                                   │
                                   ↓
                    ┌──────────────────────────┐
                    │   Base de Datos: BD      │
                    │   SELECT * FROM          │
                    │   TASKGROUPS             │
                    └──────────────────────────┘
                                   │
                                   ↓
                    ┌──────────────────────────┐
                    │   Lista de TODOS         │
                    │   los grupos:            │
                    │   [Equipo1]              │
                    │   [Equipo52]   ← Acceso NO permitido
                    │   [Equipo99]   ← Acceso NO permitido
                    │   [MiGrupo]    ← Acceso SÍ permitido
                    │   [OtroGrupo]  ← Acceso NO permitido
                    └──────────────────────────┘
                                   │
                                   ↓
                    Usuario selecciona "Equipo52"
                                   │
                                   ↓
                    fnListGroupTasks()
                    taskService.getTasksByGroupId(52, user)
                                   │
                                   ↓
                    validateGroupAccess()
                    belongsToGroup(user, 52)
                                   │
                                   ↓
                    ❌ GroupMember NOT FOUND
                    "You do not have access to this group"
```

**Problema:** El usuario ve el grupo en el menú pero no puede abrirlo.

---

## 2. Solución: Filtrado en BotActions (CORRECTO)

```
┌─────────────────────────────────────────────────────────────────────┐
│           TELEGRAM BOT - fnListGroups()                             │
│           (CON FILTRADO POR USUARIO)                                │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ↓
              taskGroupService.findAccessibleGroups(user)
                         (CON FILTRADO POR USUARIO)
                                   │
           ┌───────────────────────┼───────────────────────┐
           │                       │                       │
           ↓                       ↓                       ↓
    Qué grupos              Qué grupos              Qué grupos
    es miembro              creó                   tiene tareas
         │                       │                       │
         ↓                       ↓                       ↓
    GROUP_MEMBERS         TASKGROUPS             TASKS.CREATED_BY
    WHERE user_id=123     WHERE created_by=123  GROUP BY group_id
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                                 ↓
              ┌────────────────────────────────┐
              │  Merge & Deduplicar IDs        │
              │  [1, 5, 8]                     │
              └────────────────────────────────┘
                                 │
                                 ↓
              ┌────────────────────────────────┐
              │  findAllById([1, 5, 8])        │
              │  FROM TASKGROUPS               │
              │  WHERE ID IN (1, 5, 8)         │
              └────────────────────────────────┘
                                 │
                                 ↓
              ┌────────────────────────────────┐
              │  Lista FILTRADA de grupos:     │
              │  [MiGrupo]      ✅ Acceso SÍ  │
              │  [GrupoMiembro] ✅ Acceso SÍ  │
              │  [GrupoTareas]  ✅ Acceso SÍ  │
              └────────────────────────────────┘
                                 │
                                 ↓
                    Usuario selecciona "MiGrupo"
                                 │
                                 ↓
                    fnListGroupTasks()
                    taskService.getTasksByGroupId(1, user)
                                 │
                                 ↓
                    validateGroupAccess()
                    belongsToGroup(user, 1)
                                 │
                                 ↓
                    ✅ GroupMember FOUND O
                    ✅ Creado por usuario
                    "Tareas del grupo:"
```

**Resultado:** El usuario solo ve grupos a los que tiene acceso.

---

## 3. Comparación: Antes vs Después

### ANTES
```
┌─────────────────────┐
│ Bot Menu            │
├─────────────────────┤
│ ▶ Equipo1           │  ← Todas las opciones
│ ▶ Equipo52          │
│ ▶ Equipo99          │
│ ▶ MiGrupo           │
│ ▶ OtroGrupo         │
│ ▶ GrupoRandom       │
└─────────────────────┘
         ↓
    Usuario hace clic
         ↓
    "Error: You do not
     have access"      ← La mayoría fallan
```

### DESPUÉS
```
┌─────────────────────┐
│ Bot Menu            │
├─────────────────────┤
│ ▶ MiGrupo           │  ← Solo las accesibles
│ ▶ GrupoMiembro      │
│ ▶ GrupoTareas       │
└─────────────────────┘
         ↓
    Usuario hace clic
         ↓
    "Tareas del grupo: │
     ▶ Tarea 1        │
     ▶ Tarea 2"       │  ← Todas funcionan
```

---

## 4. Arquitectura: Dónde Va la Lógica

### Opción 1: ACTUAL (Broken)
```
                    TaskGroupService
                         │
        ┌────────────────┼────────────────┐
        │                │                │
    findAll()    validateGroupAccess()  [otros]
    (Bot)        (TaskService)
        │                │
        └────────────────┴────────────────┐
                                          │
                        ❌ Mismatch: diferente lógica
```

### Opción 2: PROPUESTA (Fixed)
```
                    TaskGroupService
                         │
        ┌────────────────┼────────────────┬────────────────┐
        │                │                │                │
    findAll()    findAccessibleGroups()  validateGroup...  [otros]
    (Admin UI)   (Bot + API)             (TaskService)
                            │
                    ✅ Lógica consistente
                    ✅ Un solo lugar
                    ✅ Fácil de auditar
```

---

## 5. Flujo de Ejecución

### BEFORE (Current)
```
fnListGroups()
  │
  ├─→ taskGroupService.findAll()           [1 query]
  │   └─→ SELECT * FROM TASKGROUPS
  │       Result: 50 grupos
  │
  └─→ Show all 50 in menu
      User clicks "Equipo52"
      │
      └─→ fnListGroupTasks(52)
          │
          └─→ taskService.getTasksByGroupId(52, user)
              │
              └─→ validateGroupAccess(user, 52)
                  │
                  └─→ belongsToGroup(user, 52)
                      │
                      └─→ SELECT * FROM GROUP_MEMBERS
                          WHERE group_id=52 AND user_id=123
                          Result: NOT FOUND
                      │
                      └─→ ❌ THROW RuntimeException
```

### AFTER (Proposed)
```
fnListGroups()
  │
  ├─→ taskGroupService.findAccessibleGroups(user)
  │   │
  │   ├─→ Query 1: SELECT FROM GROUP_MEMBERS WHERE user_id=123
  │   │   Result: groupIds=[1, 5]
  │   │
  │   ├─→ Query 2: SELECT FROM TASKGROUPS WHERE created_by=123
  │   │   Result: groupIds=[8]
  │   │
  │   ├─→ Query 3: SELECT FROM TASKS WHERE created_by=123
  │   │   Result: groupIds=[1, 5, 8] (merged)
  │   │
  │   └─→ Query 4: SELECT * FROM TASKGROUPS WHERE ID IN (1,5,8)
  │       Result: 3 grupos
  │
  └─→ Show 3 grupos in menu (only accessible)
      User clicks "MiGrupo"
      │
      └─→ fnListGroupTasks(1)
          │
          └─→ taskService.getTasksByGroupId(1, user)
              │
              └─→ validateGroupAccess(user, 1)
                  │
                  └─→ belongsToGroup(user, 1)
                      │
                      └─→ SELECT * FROM GROUP_MEMBERS
                          WHERE group_id=1 AND user_id=123
                          Result: FOUND
                      │
                      └─→ ✅ Return tareas del grupo
```

---

## 6. Impacto en Base de Datos

### Queries Antes
```
1. SELECT * FROM TASKGROUPS                         (50 filas)
2. SELECT * FROM TODOLISTS WHERE group_id = ?       (10 filas)
3. SELECT * FROM TASKS WHERE todolist_id IN (...)   (100 filas)
   ❌ PERO: Error de autorización arriba

Total queries: 2 (la 3ª nunca se ejecuta)
Datos: 50 + 10 = 60 filas innecesarias
```

### Queries Después (Versión Optimizada)
```
1. SELECT GROUP_ID FROM GROUP_MEMBERS WHERE user_id = ?           (2 filas)
2. SELECT ID FROM TASKGROUPS WHERE created_by = ?                 (1 fila)
3. SELECT GROUP_ID FROM TASKS WHERE created_by = ? GROUP BY ...   (1 fila)
4. SELECT * FROM TASKGROUPS WHERE ID IN (1,5,8)                   (3 filas)
5. SELECT * FROM TODOLISTS WHERE group_id IN (1,5,8)              (3 filas)
6. SELECT * FROM TASKS WHERE todolist_id IN (...)                 (9 filas)
   ✅ TODO FUNCIONA

Total queries: 6 (más queries, pero datos relevantes)
Datos: 3 + 3 + 9 = 15 filas (75% menos datos)
Performance: MEJOR (menos datos, solo los accesibles)
```

---

## 7. Resumen: Qué Cambia

| Aspecto | Antes | Después |
|---------|-------|---------|
| Grupos mostrados | TODOS (50) | Solo accesibles (3-5) |
| Errors al hacer clic | "You do not have access" | Funciona ✅ |
| Queries al listar | 2 | 6 (pero más eficientes) |
| Datos mostrados | 60 filas innecesarias | 15 filas relevantes |
| Código modificado | BotActions: 1 línea | BotActions: 1 línea + 40 líneas en Service |
| Riesgo | Bajo (solo lectura) | Bajo (sin cambiar validación existente) |
| Tiempo | N/A | 30 min |

---

## 8. Pseudo-código de la Solución

```
FUNCTION findAccessibleGroups(user):
    IF user is SUPERADMIN:
        RETURN findAll()
    
    accessibleIds = EMPTY SET
    
    // 1. Obtener IDs de grupos donde es miembro
    FOR EACH membership IN groupMemberRepository.findByUserId(user.id):
        accessibleIds.ADD(membership.group.id)
    
    // 2. Obtener IDs de grupos que creó
    FOR EACH group IN taskGroupRepository.findByCreatedById(user.id):
        accessibleIds.ADD(group.id)
    
    // 3. Obtener IDs de grupos donde creó tareas
    FOR EACH task IN taskRepository.findByCreatedById(user.id):
        accessibleIds.ADD(task.todoList.group.id)
    
    // 4. Retornar todos los grupos con esos IDs
    RETURN taskGroupRepository.findAllById(accessibleIds)

FUNCTION fnListGroups():
    groups = taskGroupService.findAccessibleGroups(requesterUser)
    // Mostrar groups en el menú
```

