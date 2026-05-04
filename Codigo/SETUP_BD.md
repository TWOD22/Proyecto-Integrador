# SGP — Configuración de Base de Datos Oracle

## 1. Requisitos
- Oracle Database XE (Express Edition) instalado y corriendo
- Usuario y esquema creado con el script `Base_de_Datos_Proyecto_Integrador.txt`

## 2. Agregar el driver JDBC de Oracle al proyecto

### Descargar ojdbc11.jar
https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html
→ Descargar: ojdbc11.jar (compatible con Java 11+)

### En IntelliJ IDEA
1. Crea la carpeta `lib/` dentro del proyecto
2. Copia `ojdbc11.jar` a `lib/`
3. `File → Project Structure → Libraries → + → Java`
4. Selecciona `lib/ojdbc11.jar` → OK → Apply

## 3. Configurar la conexión
Edita `src/sigep/db/Conexion.java`:
```java
private static final String HOST = "localhost";   // IP del servidor Oracle
private static final String PORT = "1521";
private static final String SID  = "XE";          // o tu ServiceName
private static final String USER = "ADMI_PRACTICAS";
private static final String PASS = "admi";
```

## 4. Ejecutar el script SQL
En SQL*Plus o SQL Developer, ejecuta:
```
Base_de_Datos_Proyecto_Integrador.txt
```
Esto crea las tablas T01-T06 y los datos semilla.

## 5. Credenciales de prueba (de los datos semilla)
| Cédula | Contraseña | Rol |
|--------|-----------|-----|
| 12345  | admin123  | Director |
| 1010   | docente123| Docente  |
| 2020   | asesor123 | Asesor   |

## 6. Modo Demo (sin BD)
Si Oracle no está disponible, el sistema pregunta si deseas iniciar en
**Modo Demo** con datos de prueba locales. En ese modo:
- Login: cédula `director` → Director, `docente` → Evaluador, cualquier otra → Estudiante
- Los cambios NO se persisten en BD

## 7. Compilar
```bash
javac -cp lib/ojdbc11.jar -d out src/sigep/*.java src/sigep/db/*.java
java  -cp out:lib/ojdbc11.jar sigep.SGPApp
```
En Windows usar `;` en lugar de `:` en el classpath.
