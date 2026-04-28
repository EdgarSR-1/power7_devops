# Testing Guide: Debug Endpoints para Autorización

## Endpoints Disponibles

### 1. Ver TODOS los Grupos del Sistema
```
GET /api/debug/all-groups
```

**Respuesta esperada:**
```json
{
  "totalGroups": 50,
  "groups": [
    {
      "id": 1,
      "name": "MiGrupo",
      "createdBy": "Miguel",
      "createdAt": "2026-04-20T10:30:00"
    },
    {
      "id": 2,
      "name": "Equipo52",
      "createdBy": "OtroUsuario",
      "createdAt": "2026-04-15T08:00:00"
    }
    // ... más grupos
  ]
}
```

---

### 2. Ver Grupos Accesibles para un Usuario (RECOMENDADO)
```
GET /api/debug/user-groups/{telegramUserId}
```

**Ejemplo:**
```
GET /api/debug/user-groups/8261084667
```

**Respuesta esperada:**
```json
{
  "user": {
    "id": 123,
    "name": "Miguel",
    "email": "miguel@example.com",
    "telegramUserId": 8261084667,
    "telegramChatId": 98765432,
    "role": "USUARIO"
  },
  "summary": {
    "totalGroupsInSystem": 50,
    "totalAccessibleGroups": 3,
    "membershipGroups": 1,
    "createdGroups": 1,
    "groupsWithTasks": 2,
    "totalTasks": 5
  },
  "accessibleGroups": [
    {
      "id": 1,
      "name": "MiGrupo",
      "createdBy": "Miguel",
      "accessTypes": [
        "CREATOR",
        "HAS_TASKS"
      ]
    },
    {
      "id": 2,
      "name": "GrupoMiembro",
      "createdBy": "OtroUsuario",
      "accessTypes": [
        "MEMBER"
      ]
    },
    {
      "id": 5,
      "name": "GrupoConMisTareas",
      "createdBy": "OtroUsuario",
      "accessTypes": [
        "HAS_TASKS"
      ]
    }
  ],
  "membershipDetails": [
    {
      "id": 2,
      "name": "GrupoMiembro",
      "role": "USUARIO",
      "joinedAt": "2026-04-18T14:20:00"
    }
  ],
  "createdGroupsDetails": [
    {
      "id": 1,
      "name": "MiGrupo",
      "createdAt": "2026-04-10T09:15:00"
    }
  ],
  "tasksDetails": [
    {
      "taskId": 10,
      "taskName": "Implementar autenticación",
      "groupId": 1,
      "groupName": "MiGrupo",
      "status": "in_progress"
    },
    {
      "taskId": 11,
      "taskName": "Revisar código",
      "groupId": 5,
      "groupName": "GrupoConMisTareas",
      "status": "pending"
    }
  ]
}
```

---

## Interpretación de la Respuesta

### `summary` Section
- **totalGroupsInSystem**: Total de grupos en la BD (50)
- **totalAccessibleGroups**: Grupos que el usuario PUEDE ver (3)
- **membershipGroups**: Grupos donde es miembro explícito (1)
- **createdGroups**: Grupos que el usuario creó (1)
- **groupsWithTasks**: Grupos donde creó tareas (2)
- **totalTasks**: Total de tareas creadas por el usuario (5)

### `accessibleGroups` Section
Lista de grupos a los que tiene acceso con tipo de acceso:
- **MEMBER**: Es miembro explícito (GroupMember record)
- **CREATOR**: Creó el grupo
- **HAS_TASKS**: Tiene tareas en ese grupo

### El Problema Visual
```
Todos los grupos en BD:     50 grupos
Grupos accesibles:          3 grupos
├─ MEMBER:                  1 grupo
├─ CREATOR:                 1 grupo
└─ HAS_TASKS:               2 grupos (algunos solapados)
```

---

## Cómo Testear desde Postman

### Paso 1: Compilar y ejecutar el backend
```bash
cd MtdrSpring/backend
mvn clean package -DskipTests
# o con el contenedor
docker-compose up
```

### Paso 2: Crear request en Postman

**Request 1: Ver todos los grupos**
```
GET http://localhost:8080/api/debug/all-groups
```

**Request 2: Ver grupos del usuario Miguel**
```
GET http://localhost:8080/api/debug/user-groups/8261084667
```

### Paso 3: Comparar resultados

```
Todos los grupos: 50
Grupos accesibles para Miguel: 3
├─ MiGrupo (CREATOR, HAS_TASKS)
├─ GrupoMiembro (MEMBER)
└─ GrupoConMisTareas (HAS_TASKS)

Conclusión: ✅ Miguel solo debería ver estos 3 grupos en el bot
```

---

## Cómo Testear desde Terminal

### Opción A: curl

```bash
# Ver todos los grupos
curl -X GET http://localhost:8080/api/debug/all-groups | jq

# Ver grupos de Miguel
curl -X GET http://localhost:8080/api/debug/user-groups/8261084667 | jq
```

### Opción B: Docker exec

```bash
# Si la aplicación está en contenedor
docker exec agilecontainer curl -X GET http://localhost:8080/api/debug/all-groups | jq
docker exec agilecontainer curl -X GET http://localhost:8080/api/debug/user-groups/8261084667 | jq
```

---

## Interpretación de Resultados

### Caso 1: Usuario NO tiene grupos accesibles
```json
{
  "summary": {
    "totalGroupsInSystem": 50,
    "totalAccessibleGroups": 0,
    "membershipGroups": 0,
    "createdGroups": 0,
    "groupsWithTasks": 0,
    "totalTasks": 0
  },
  "accessibleGroups": []
}
```
**Problema:** El usuario no puede acceder a ningún grupo. El bot no tendría nada que mostrar.

### Caso 2: Usuario tiene acceso pero bot muestra error
```json
{
  "summary": {
    "totalGroupsInSystem": 50,
    "totalAccessibleGroups": 3,
    "membershipGroups": 1,
    "createdGroups": 1,
    "groupsWithTasks": 1,
    "totalTasks": 2
  },
  "accessibleGroups": [
    // ... 3 grupos
  ]
}
```
**Análisis:** Usuario TIENE 3 grupos accesibles, pero el bot muestra 50.
**Solución:** Implementar `findAccessibleGroups()` en TaskGroupService.

### Caso 3: Usuario es SUPERADMIN
```json
{
  "user": {
    "role": "SUPERADMIN"
  },
  "summary": {
    "totalGroupsInSystem": 50,
    "totalAccessibleGroups": 50
  }
}
```
**Análisis:** SUPERADMIN tiene acceso a todos los grupos. Esto es correcto.

---

## Script de Prueba Completo

Crear archivo: `test_auth.sh`

```bash
#!/bin/bash

# Variables
API_URL="http://localhost:8080"
TELEGRAM_USER_ID=8261084667

echo "=========================================="
echo "TESTING AUTHORIZATION ENDPOINTS"
echo "=========================================="

echo ""
echo "[1/2] Obteniendo TODOS los grupos..."
ALL_GROUPS=$(curl -s -X GET "$API_URL/api/debug/all-groups")
TOTAL_GROUPS=$(echo $ALL_GROUPS | jq '.totalGroups')
echo "Total de grupos en el sistema: $TOTAL_GROUPS"

echo ""
echo "[2/2] Obteniendo grupos accesibles para usuario $TELEGRAM_USER_ID..."
USER_GROUPS=$(curl -s -X GET "$API_URL/api/debug/user-groups/$TELEGRAM_USER_ID")

# Extraer información
USER_NAME=$(echo $USER_GROUPS | jq -r '.user.name')
ACCESSIBLE=$(echo $USER_GROUPS | jq '.summary.totalAccessibleGroups')
MEMBER=$(echo $USER_GROUPS | jq '.summary.membershipGroups')
CREATED=$(echo $USER_GROUPS | jq '.summary.createdGroups')
WITH_TASKS=$(echo $USER_GROUPS | jq '.summary.groupsWithTasks')

echo "=========================================="
echo "RESULTADOS"
echo "=========================================="
echo "Usuario: $USER_NAME"
echo "Grupos en el sistema: $TOTAL_GROUPS"
echo "Grupos accesibles: $ACCESSIBLE"
echo "  ├─ Como miembro: $MEMBER"
echo "  ├─ Creados por ti: $CREATED"
echo "  └─ Con tus tareas: $WITH_TASKS"

echo ""
echo "GRUPOS ACCESIBLES:"
echo $USER_GROUPS | jq '.accessibleGroups | .[] | {id, name, accessTypes}'

echo ""
echo "RECOMENDACIÓN:"
if [ "$TOTAL_GROUPS" -gt "$ACCESSIBLE" ]; then
    echo "⚠️  El bot muestra $TOTAL_GROUPS grupos pero solo $ACCESSIBLE son accesibles"
    echo "✅ Solución: Implementar findAccessibleGroups() en TaskGroupService"
else
    echo "✅ OK: El bot muestra exactamente los grupos accesibles"
fi
```

Ejecutar:
```bash
chmod +x test_auth.sh
./test_auth.sh
```

---

## Verificación Final

Después de implementar la solución, ejecutar:

```bash
# Ver grupos antes de la solución
./test_auth.sh

# Implementar findAccessibleGroups()
# ...

# Ver grupos después de la solución
./test_auth.sh

# Resultados esperados:
# Antes:  Grupos accesibles: 3, Pero bot muestra: 50
# Después: Grupos accesibles: 3, Bot muestra: 3 ✅
```

