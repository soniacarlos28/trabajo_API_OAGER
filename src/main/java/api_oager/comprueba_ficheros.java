package api_oager;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;
import java.util.Objects;

public class comprueba_ficheros {

	private static final String DB_URL = "";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";
	
	  private static final String URL_LOGIN_REAL =
	            "https://www.oager.com/ApiRest/api/Login";

	    // API de datos contables. Usa POST con body vacío.
	    private static final String URL_DATOS_CONTABILIDAD =
	            "https://www.oager.com/ApiRest/Contabilidad/GetDatosContabilidadEjercicio?Ejercicio=%s";

	    static LoggerService logger = new LoggerService();

	    /** Extrae el campo "Token" de la respuesta del login */
	    public static String extraerToken(String jsonResponse) {
	        JSONObject obj = new JSONObject(jsonResponse);
	        return obj.getString("Token");
	    }

	    /** Obtiene el token (entorno real). */
	    public static String gettokken() {
	        HttpResponse<String> response = Unirest.post(URL_LOGIN_REAL)
	                .header("Content-Type", "application/json")
	                .body("{\n  \"Username\": \"Userconta\",\n  \"Password\": \"zPvLuZquVW2AZ+HISAtEUw==\"\n}")
	                .asString();

	        logger.log("URL: " + URL_LOGIN_REAL);

	        if (response.isSuccess()) {
	            String token = extraerToken(response.getBody());
	            logger.log("Bearer " + token);
	            return token;
	        } else {
	            return "Error al llamar a la API: " + response.getStatus();
	        }
	    }

	    /**
	     * Descarga del API el JSON de ficheros para el ejercicio indicado.
	     * Devuelve el JSONArray para ser procesado.
	     */
	    private static JSONArray fetchDatosContabilidad(String token, String ejercicio) {
	        String url = String.format(URL_DATOS_CONTABILIDAD, Objects.requireNonNull(ejercicio, "Ejercicio nulo"));
	        HttpResponse<String> resp = Unirest.post(url)
	                .header("Authorization", "Bearer " + Objects.requireNonNull(token, "Token nulo"))
	                .header("Content-Type", "text/plain")
	                .body("") // el endpoint requiere POST con body vacío
	                .asString();

	        if (!resp.isSuccess()) {
	            throw new RuntimeException("HTTP " + resp.getStatus() + " - " + resp.getStatusText() + " al llamar " + url);
	        }
	        // El API devuelve un array JSON
	        return new JSONArray(resp.getBody());
	    }

	    /**
	     * Inserta en RRHH.ATM_FICHERO_DESCARGA los registros del JSON que NO existan por IDENTIFICADOR_FICHERO.
	     * - Usa transacción.
	     * - Deja que Oracle ponga DESCARGADO=1 y FECHA_ALTA=SYSTIMESTAMP (valores por defecto).
	     * - ENTIDAD viene en el JSON (en tus ejemplos es 3).
	     *
	     * @param token           token Bearer ya obtenido
	     * @param ejercicio       ejercicio numérico en String (e.g., "2025")
	     * @param conn            conexión Oracle (se cierra fuera)
	     * @param usuarioWindows  para logging / auditoría (si quieres guardarlo en otra tabla, aquí lo tienes)
	     */
	    public static String descargarYcompruebaEnBD(String token, String ejercicio, Connection conn, String usuarioWindows)
	            throws SQLException, IOException {

	        logger.log("[descargarYcompruebaEnBD] Inicio. Usuario: " + usuarioWindows + ", Ejercicio=" + ejercicio);

	        JSONArray arr = fetchDatosContabilidad(token, ejercicio);
	        logger.log("[descargarYcompruebaEnBD] Registros recibidos: " + arr.length());

	        // SQLs
	        final String SQL_EXISTE =
	                "SELECT 1 FROM RRHH.ATM_FICHERO_DESCARGA WHERE IDENTIFICADOR_FICHERO = ?";

	        final String SQL_INSERT =
	                "INSERT INTO RRHH.ATM_FICHERO_DESCARGA " +
	                " (CODIGO, EJERCICIO, ENTIDAD, IDENTIFICADOR_FICHERO, TIPO_DEUDA, TIPO_FACTURACION_CONTAB) " +
	                "VALUES (?, ?, ?, ?, ?, ?)";

	        boolean prevAuto = conn.getAutoCommit();
	        conn.setAutoCommit(false);

	        int yaExistentes = 0;
	        int insertados = 0;

	        try (PreparedStatement psExiste = conn.prepareStatement(SQL_EXISTE);
	             PreparedStatement psInsert = conn.prepareStatement(SQL_INSERT)) {

	            for (int i = 0; i < arr.length(); i++) {
	                JSONObject o = arr.getJSONObject(i);

	                // Campos del JSON
	                int codigo = o.getInt("Codigo");
	                int ej     = o.getInt("Ejercicio");
	                int entidad= o.getInt("Entidad"); // en tus ejemplos, siempre 3
	                String identificador = o.getString("IdentificadorFichero");
	                int tipoDeuda        = o.getInt("TipoDeuda");
	                int tipoFactCont     = o.getInt("TipoFacturacionContabilidad");

	                // 1) ¿Existe ya por IDENTIFICADOR_FICHERO?
	                psExiste.clearParameters();
	                psExiste.setString(1, identificador);
	                try (ResultSet rs = psExiste.executeQuery()) {
	                    if (rs.next()) {
	                        yaExistentes++;
	                        continue; // saltamos insert
	                    }
	                }

	                // 2) Insertar nuevo (DESCARGADO y FECHA_ALTA quedan por defecto: 1 y SYSTIMESTAMP)
	                psInsert.clearParameters();
	                psInsert.setInt(1, codigo);
	                psInsert.setInt(2, ej);
	                psInsert.setInt(3, entidad);
	                psInsert.setString(4, identificador);
	                psInsert.setInt(5, tipoDeuda);
	                psInsert.setInt(6, tipoFactCont);
	                psInsert.executeUpdate();
	                insertados++;
	            }

	            conn.commit();
	        } catch (SQLException ex) {
	            conn.rollback();
	            logger.log("[descargarYcompruebaEnBD] ERROR SQL: " + ex.getMessage());
	            throw ex;
	        } finally {
	            conn.setAutoCommit(prevAuto);
	        }

	        String resumen = String.format("Ejercicio %s -> Insertados: %d, Ya existentes: %d", ejercicio, insertados, yaExistentes);
	        logger.log("[descargarYcompruebaEnBD] Fin. " + resumen);
	        
	        return resumen;
	        
	    }

	    public static String descarga() throws Exception 
	    {
	        // (Ejemplo) Obtención del token y conexión simplificada.
	        // En tu aplicación real, ya llamas gettokken() y pasas 'conn' desde fuera.
	        String token = gettokken();
	        String resultado="";
	        
	        if (token == null || token.startsWith("Error")) {
	            System.err.println("No se pudo obtener token: " + token);
	            return "error";
	        }

	        String ejercicio = "2025";
	        try( Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD))
   		      { 
	        	logger.log("Conexion abierta");
   	 
   	            if (conn == null && conn.isClosed())  
   	            	logger.log("Conexion cerrada 2");
   	            
   	             resultado=descargarYcompruebaEnBD(token, ejercicio, conn, ejercicio);
		      }
		   catch (SQLException e) 
	          {
		       e.printStackTrace();
		       logger.log("Error general: " + e.getMessage()); 
		      }
   		 
	        logger.log("Fin del proceso");
	        
	        return resultado;
 	    }

	    
	    // ===== ejemplo de uso =====
	    public static void main(String[] args) throws Exception 
	    {
	        // (Ejemplo) Obtención del token y conexión simplificada.
	        // En tu aplicación real, ya llamas gettokken() y pasas 'conn' desde fuera.
	        String token = gettokken();
	        if (token == null || token.startsWith("Error")) {
	            System.err.println("No se pudo obtener token: " + token);
	            return;
	        }

	        String ejercicio = "2025";
	        try( Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD))
   		      { 
	        	logger.log("Conexion abierta");
   	 
   	            if (conn == null && conn.isClosed())  
   	            	logger.log("Conexion cerrada 2");
   	            
   	         //token, String ejercicio, Connection conn, String usuarioWindows
   	         
   	            descargarYcompruebaEnBD(token, ejercicio, conn, ejercicio);
		      }
		   catch (SQLException e) 
	          {
		       e.printStackTrace();
		       logger.log("Error general: " + e.getMessage()); 
		      }
   		 
	        logger.log("Fin del proceso");
 	    }
	}

