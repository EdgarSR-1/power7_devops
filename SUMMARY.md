# RESUMEN EJECUTIVO: Autorización Bot - Problemas y Soluciones

## El Problema en 1 Minuto

**¿Qué pasa?**
- El bot muestra TODOS los grupos del sistema al usuario
- Cuando el usuario intenta abrir un grupo, el backend lo rechaza con "You do not have access to this group"
- Result: Usuario ve grupo → hace clic → error

**¿Por qué?**
- `BotActions.fnListGroups()` usa `taskGroupService.findAll()` (sin filtro)
- `TaskService.validateGroupAccess()` es muy estricto (solo GroupMember)
- Mismatch: UI muestra todo, Backend rechaza mayoría

**¿Cuál es el impacto?**
- Bot no funciona correctamente para la mayoría de usuarios
- Solo funciona si el usuario es miembro explícito de un grupo
- Ignora grupos creados por el usuario
- Ignora grupos donde el usuario tiene tareas

---

## Soluciones (del más rápido al más completo)

### 🟢 OPCIÓN 1: Bot-Only Quick Fix (15 min)

**Qué hacer:** Cambiar 1 línea en BotActions

```java
// ANTES:
List<TaskGroup> groups = taskGroupService.findAll();

// DESPUÉS:
List<TaskGroup> groups = taskGroupService.findAccessibleGroups(requesterUser);
```

**Ventaja:** Rápido, sin cambios en BD, sin cambios en validación

**Desventaja:** Requiere crear el método `findAccessibleGroups()` en TaskGroupService

---

### 🟡 OPCIÓN 2: Mediana - RECOMENDADA (30 min)

**Qué hacer:**
1. Crear método `findAccessibleGroups(User)` en TaskGroupService (40 líneas)
2. Usar ese método en BotActions.fnListGroups() (1 línea)

**Ventaja:**
- Soluciona problema de raíz
- Centraliza lógica de autorización
- Consistente con API (GroupController ya hace esto)
- Sin riesgo de romper código

**Desventaja:** Requiere 30 minutos de trabajo

**Resultado:**
```
Antes: Usuario ve 50 grupos, puede acceder a 3
Después: Usuario ve 3 grupos, puede acceder a 3 ✅
```

---

### 🔴 OPCIÓN 3: Completa - Refactorización (2 horas)

**Qué hacer:**
1. Crear servicio `AuthorizationService` centralizado
2. Refactorizar TaskService, BotActions, GroupController
3. Unificar reglas de autorización en un solo lugar

**Ventaja:**
- Arquitectura limpia
- Fácil auditoría
- Fácil cambiar reglas globalmente

**Desventaja:**
- Cambios grandes
- Riesgo de regresiones
- Lleva más tiempo

---

## Mi Recomendación: Opción 2

**Por qué?**
1. Balanza perfecta entre velocidad y calidad
2. Soluciona el problema de raíz (no parches)
3. Sin riesgo significativo
4. 30 minutos es razonable
5. Mejora la arquitectura (centraliza autorización)

**Plan de acción:**
```
1. Crear TaskGroupService.findAccessibleGroups(User) ...................... 10 min
2. Cambiar BotActions.fnListGroups() ...................................... 2 min
3. Testear localmente ..................................................... 10 min
4. Testear en Docker ..................................................... 5 min
5. Commit & push ......................................................... 3 min
```

---

## Entendiendo la Autorización Actual

### ¿Qué grupos tiene un usuario?

Un usuario tiene acceso a un grupo si:

```
IF usuario es SUPERADMIN:
    ✅ Todos los grupos

ELSE IF existe GroupMember(grupo, usuario):
    ✅ El grupo (es miembro explícito)

ELSE:
    ❌ Sin acceso
```

### ¿Qué DEBERÍA ser?

```
IF usuario es SUPERADMIN:
    ✅ Todos los grupos

ELSE IF:
    existe GroupMember(grupo, usuario) OR
    usuario.id == grupo.createdBy OR
    existe Task.createdBy=usuario en el grupo
    
THEN:
    ✅ El grupo

ELSE:
    ❌ Sin acceso
```

---

## Archivos Relacionados

Dentro del repo, encontrarás:

1. **AUTHORIZATION_ANALYSIS.md** 
   - Análisis detallado del problema
   - Explicación de cada opción
   - Queries SQL para verificar datos
   
2. **IMPLEMENTATION_GUIDE.md**
   - Pasos específicos de implementación
   - Código Java completo
   - Checklist de QA

3. **ARCHITECTURE_DIAGRAMS.md**
   - Diagramas visuales antes/después
   - Flujos de ejecución
   - Impacto en BD

---

## Pasos Para Verificar Qué Grupos Tiene Miguel

### Opción A: SQL en Oracle

```sql
SELECT * FROM USERS WHERE TELEGRAM_USER_ID = 8261084667;
-- Resultado: encontrar su ID (ej: 123)

SELECT DISTINCT tg.ID, tg.NAME, 'MEMBER' as TYPE
FROM TASKGROUPS tg
INNER JOIN GROUP_MEMBERS gm ON tg.ID = gm.GROUP_ID
WHERE gm.USER_ID = 123

UNION

SELECT ID, NAME, 'CREATED'
FROM TASKGROUPS WHERE CREATED_BY = 123

UNION

SELECT DISTINCT tg.ID, tg.NAME, 'HAS_TASKS'
FROM TASKGROUPS tg
INNER JOIN TODOLISTS tl ON tg.ID = tl.GROUP_ID
INNER JOIN TASKS t ON tl.ID = t.TODOLIST_ID
WHERE t.CREATED_BY = 123
ORDER BY ID;
```

### Opción B: API REST

```
GET /api/groups/user
Authorization: Bearer <JWT_TOKEN>
```

---

## Próximos Pasos

1. **Leer los 3 documentos de análisis** (15 min)
2. **Ejecutar la query SQL** para ver datos actuales (5 min)
3. **Implementar la Opción 2** (30 min)
4. **Testear** (15 min)
5. **Commit & push** (5 min)

**Tiempo total: ~70 minutos**

---

## Questions?

Todo está documentado en los 3 archivos .md. Cualquier duda, revisa:

- ¿Qué es belongsToGroup()? → AUTHORIZATION_ANALYSIS.md, sección 2
- ¿Cómo implemento? → IMPLEMENTATION_GUIDE.md
- ¿Cómo es el flujo? → ARCHITECTURE_DIAGRAMS.md

