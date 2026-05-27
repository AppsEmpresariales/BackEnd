# ☁️ DocuCloud - Backend (API REST)

¡Bienvenido al backend de **DocuCloud**! Este proyecto es una API REST empresarial robusta construida con **Spring Boot**, diseñada para la gestión documental electrónica estructurada, flujos de trabajo con asignación dinámica de roles y auditoría completa de acciones.

---

## 🚀 Arquitectura y Tecnologías
El backend de DocuCloud está estructurado siguiendo los mejores patrones de diseño arquitectónico (Capas: Controller, Service, DAO, Repository) y utiliza las siguientes tecnologías clave:

*   **Framework Principal**: Spring Boot 3.5.5
*   **Gestor de Dependencias y Construcción**: Gradle
*   **Persistencia de Datos**: Spring Data JPA & Hibernate
*   **Base de Datos**: MySQL 8.0+
*   **Seguridad**: Spring Security con autenticación basada en tokens JWT
*   **Mapeo de Entidades/DTOs**: MapStruct & Lombok
*   **Documentación**: Springdoc OpenAPI / Swagger UI
*   **Versión de Java**: JDK 17+ (Desarrollado y testeado bajo JDK 25.0.2)

---

## 🛠️ Requisitos Previos

Antes de configurar y ejecutar el servidor, asegúrate de tener instalado en tu máquina:

1.  **Java Development Kit (JDK)**: Versión 17 o superior.
    *   *Verifica tu instalación ejecutando*: `java -version`
2.  **MySQL Server**: Versión 8.0 o posterior.
    *   *Asegúrate de tener el servicio activo en el puerto estándar `3306`.*
3.  **Gradle**: Opcional (el proyecto incluye el wrapper `./gradlew` para autodescarga e instalación local).

---

## 📦 Configuración e Instalación Paso a Paso

### 1. Preparar la Base de Datos en MySQL
Abre tu consola de MySQL, Workbench o cliente de preferencia (como phpMyAdmin o DBeaver) y ejecuta la sentencia para crear el esquema de base de datos necesario:

```sql
CREATE DATABASE docucloud CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> [!WARNING]  
> Por defecto, la configuración de desarrollo busca el usuario `root` sin contraseña. Si tu base de datos tiene contraseña, actualízala en los archivos de propiedades como se detalla en el siguiente paso.

### 2. Configurar Propiedades de Entorno
El proyecto utiliza perfiles de configuración dinámicos. Las configuraciones principales se encuentran en:
*   [application.properties](file:///C:/Users/JUAN%20DAVID/Documents/AprendizajeAPPS/Proyecto%20de%20APPS%20Empresariales/BackEnd/proyecto/src/main/resources/application.properties) (Configuración general e internacionalización)
*   [application-dev.properties](file:///C:/Users/JUAN%20DAVID/Documents/AprendizajeAPPS/Proyecto%20de%20APPS%20Empresariales/BackEnd/proyecto/src/main/resources/application-dev.properties) (Configuración para el entorno de desarrollo)

Si requieres modificar las credenciales de base de datos, edita [application-dev.properties](file:///C:/Users/JUAN%20DAVID/Documents/AprendizajeAPPS/Proyecto%20de%20APPS%20Empresariales/BackEnd/proyecto/src/main/resources/application-dev.properties):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/docucloud
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_CONTRASEÑA
```

---

## 🏃 Cómo Ejecutar el Servidor

Para compilar y arrancar la aplicación en tu entorno local, ubícate en la raíz del directorio backend (`proyecto/`) y ejecuta el siguiente comando:

### En Windows (PowerShell o CMD):
```powershell
.\gradlew.bat bootRun
```

### En Linux / macOS:
```bash
./gradlew bootRun
```

Una vez ejecutado, el servidor se iniciará. Podrás confirmar que está listo cuando visualices el siguiente log en consola:
```text
Tomcat initialized with port 8080 (http)
Active profiles: dev
Context path: /docucloud
```

> [!IMPORTANT]  
> **Context Path**: El proyecto está configurado bajo el contexto `/docucloud`. Esto significa que todas tus solicitudes HTTP y llamadas de API deben llevar este prefijo en la URL (ejemplo: `http://localhost:8080/docucloud/...`).

---

## 👥 Datos Semilla para Pruebas (Seed Data)

Para facilitar la evaluación inmediata de los flujos de trabajo sin configuraciones manuales previas, el sistema **inicializa automáticamente** los siguientes datos en la base de datos la primera vez que arranca:

*   **Organización Principal de Prueba**:
    *   **NIT**: `900123456`
    *   **Nombre**: *Organización Demo*

### Credenciales de Usuarios de Prueba:

| Cédula | Nombre Completo | Rol en Sistema | Correo Electrónico | Contraseña |
| :--- | :--- | :--- | :--- | :--- |
| `999999999` | Admin DocuCloud | **ADMIN_ORG** *(Admin General)* | `admin@docucloud.local` | `Admin12345` |
| `1001001001` | Carlos Mendoza | **REVISOR** | `carlos@docucloud.local` | `Carlos123` |
| `1002002002` | Laura Gutierrez | **APROBADOR** | `laura@docucloud.local` | `Laura123` |
| `1003003003` | Andres Ramirez | **EMPLEADO** *(Editor/Creador)* | `andres@docucloud.local` | `Andres123` |

### Flujos de Trabajo Semilla Autocreados:
El sistema crea automáticamente un flujo activo para contratos de la organización demo (`900123456`):
1.  **Paso 1: Revisión de Contrato** -> Asignado al rol `REVISOR` (Carlos Mendoza).
2.  **Paso 2: Aprobación de Contrato** -> Asignado al rol `APROBADOR` (Laura Gutierrez).

---

## 📖 Documentación Interactiva (Swagger/OpenAPI)

El backend cuenta con integración completa de **Springdoc OpenAPI**. Con el servidor corriendo, puedes interactuar directamente con los endpoints y realizar peticiones reales a través de:

*   **Swagger UI**: [http://localhost:8080/docucloud/swagger-ui/index.html](http://localhost:8080/docucloud/swagger-ui/index.html)
*   **Especificación JSON**: [http://localhost:8080/docucloud/api-docs](http://localhost:8080/docucloud/api-docs)

---

## ⚡ Endpoints Principales para Pruebas (Insomnia / Postman)

A continuación se listan 5 endpoints estratégicos para probar y validar el comportamiento del backend:

### 1. Autenticación (Login)
*   **Método**: `POST`
*   **URL**: `http://localhost:8080/docucloud/api/auth/login`
*   **Cuerpo (JSON)**:
    ```json
    {
      "email": "admin@docucloud.local",
      "password": "Admin12345"
    }
    ```
*   **Retorno**: Devuelve el DTO del usuario junto con el token JWT (`token`) necesario para los demás endpoints.

### 2. Crear Registro de Nueva Organización y Administrador
*   **Método**: `POST`
*   **URL**: `http://localhost:8080/docucloud/api/auth/register`
*   **Cuerpo (JSON)**:
    ```json
    {
      "nit": "900999999",
      "nombreOrganizacion": "Nueva Empresa S.A.",
      "telefonoOrganizacion": "3001234567",
      "emailOrganizacion": "contacto@nuevaempresa.com",
      "dirCalle": "Calle Falsa",
      "dirNumero": "123",
      "dirComuna": "Comuna 4",
      "cedulaAdmin": "111222333",
      "nombreAdmin": "Juan Pérez",
      "emailAdmin": "juan.perez@nuevaempresa.com",
      "passwordAdmin": "Segura123",
      "telefonoAdmin": "3159876543"
    }
    ```

### 3. Crear Nuevo Documento (Requiere Token)
*   **Método**: `POST`
*   **URL**: `http://localhost:8080/docucloud/api/v1/documentos`
*   **Cabecera**: `Authorization: Bearer <TU_TOKEN_JWT>`
*   **Cuerpo (Multipart Form-Data)**:
    *   `documento` (JSON o campos clave correspondientes al DTO de creación del documento, como título, descripción, tipo documental id, etc.)
    *   `file` (Archivo adjunto PDF/Word opcional)

### 4. Transicionar Estado de un Documento (Requiere Token)
*   **Método**: `PATCH`
*   **URL**: `http://localhost:8080/docucloud/api/v1/documentos/{id}/estado/{estadoId}`
*   **Cabecera**: `Authorization: Bearer <TU_TOKEN_JWT>`
*   *Nota: Si la organización tiene un flujo configurado para el tipo de documento, esta transición generará de forma automática tareas asignadas y enviará notificaciones en tiempo real al responsable del siguiente paso.*

### 5. Obtener Notificaciones de un Usuario (Requiere Token)
*   **Método**: `GET`
*   **URL**: `http://localhost:8080/docucloud/api/v1/notificaciones/usuario/{cedula}`
*   **Cabecera**: `Authorization: Bearer <TU_TOKEN_JWT>`

---

## 💾 Almacenamiento de Archivos (Uploads)
Los archivos adjuntos de los documentos cargados en el sistema se guardan localmente en el servidor en la ruta:
`uploads/documentos/` (relativa a la raíz de ejecución del proyecto).

*   **Límite Máximo de Archivo**: `20 MB` (Configurable en `application.properties`).
*   **Soporte Nombres de Archivo Largos**: Las columnas de base de datos correspondientes tienen soporte optimizado de hasta `1024` caracteres para prevenir fallos por truncamiento.

---

## 🧪 Ejecutar Pruebas Unitarias e Integración
El backend contiene una suite de pruebas unitarias robusta que valida la lógica del negocio. Para ejecutarlas:

```bash
./gradlew test
```
Los reportes detallados en formato HTML se generan automáticamente tras finalizar la suite en: `build/reports/tests/test/index.html`.

---

## 💡 Resolución de Problemas Comunes (FAQ)

### ❌ Error: `UnexpectedRollbackException` al cambiar estado
*   **Causa**: Estás intentando cambiar de estado un documento de un tipo que no tiene un flujo de trabajo activo asignado en la organización.
*   **Solución**: El backend ha sido robustecido para evitar que esto provoque un rollback de transacción (RF30). Si ocurre en flujos manuales propios, asegúrate de que la organización tenga un Flujo de Trabajo configurado y activo para dicho Tipo Documental a través del módulo de flujos.

### ❌ Error: `Data truncation: Data too long for column 'archivo_ruta'`
*   **Causa**: Estás subiendo un archivo cuyo nombre original sumado a la ruta interna de almacenamiento supera los 255 caracteres en una versión de base de datos desactualizada.
*   **Solución**: La entidad y tabla física han sido actualizadas a un límite moderno de `VARCHAR(1024)`. Asegúrate de que las migraciones de tu base de datos se ejecuten correctamente mediante el flag `spring.jpa.hibernate.ddl-auto=update` habilitado en desarrollo.

### ❌ Error de CORS al conectar con el Frontend
*   **Causa**: El frontend corre en un puerto diferente (normalmente `4200`) y la política del backend bloquea los encabezados cruzados.
*   **Solución**: Se ha configurado CORS dinámico en `SecurityConfig.java` usando `setAllowedOriginPatterns("*")` para entornos de desarrollo local, permitiendo conexiones limpias sin bloqueos.
