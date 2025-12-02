package api_oager;

 import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.nio.file.Path;



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import kong.unirest.JsonNode;


public class ApiCall {
	
	 static LoggerService logger=new LoggerService();
	 
	 public static String extraerToken(String jsonResponse) {
	        JSONObject obj = new JSONObject(jsonResponse);
	        return obj.getString("Token");
	    }
	 
    public static String gettokken() {
    	//pre
    	// HttpResponse<String> response = Unirest.post("https://www.oager.com/ApiRestPreOager/api/Login")
    	
    	/*HttpResponse<String> response = Unirest.post("https://www.oager.com/ApiRestPreOager/api/Login")
    			  .header("Content-Type", "application/json")
    			  .body("{\r\n  \"Username\": \"fddsfa\",\r\n  \"Password\": \"fsdfa.123\"\r\n}")
    			  .asString();*/
    	//real
    		//	HttpResponse<String> response = Unirest.post("https://www.oager.com/ApiRest/api/Login")
    	HttpResponse<String> response = Unirest.post("https://www.oager.com/ApiRest/api/Login")
  			  .header("Content-Type", "application/json")
  			  .body("{\r\n  \"Username\": \"feo\",\r\n  \"Password\": \"FEo==\"\r\n}")
  			  .asString();
  	 logger.log("URL: https://www.oager.com/ApiRest/api/Login");
   	     
    	

        if (response.isSuccess()) {
        	
        	String token = extraerToken(response.getBody());
        	logger.log("Bearer " + token);
            return token;
        } else {
            return "Error al llamar a la API: " + response.getStatus();
        }
    }
    
    public static String descargarYGuardarEnBD(String token, String ejercicio,Connection conn,String usuarioWindows) throws SQLException, IOException {
    	//pendientes PRE
        //String url = "https://www.oager.com/ApiRestPreOager/Contabilidad/GetContabilidadPendiente?" + ejercicio;
    	
     //pendientes REAL	  
     String url = "https://www.oager.com/ApiRest/Contabilidad/GetContabilidadPendiente?ejercicio=" + ejercicio;
    //https://www.oager.com/ApiRestPreOager/Contabilidad/GetContabilidad
    //String url = "https://www.oager.com/ApiRestPreOager/Contabilidad/GetContabilidad";
    
    //String usuarioWindows = (String) session.getAttribute("user"); // o "USER" en Linux
    String nombreFichero ="";
	 
     ProcesaZip procesazip=new ProcesaZip();
	 Map<Integer, String> tiposDeuda = null;
	 
	 logger.log("token descarga y guarda" + token); 
	try {
		tiposDeuda = cargarTiposDeudaDesdeBD(conn);
	} catch (SQLException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} // por ejemplo {0:"Liquidacion", 1:"Autoliquidacion", ...}

	 int tipoDeuda = determinarTipoDeuda(nombreFichero, tiposDeuda);
	 int tipoFacturacion = 2; 
	  
    if (usuarioWindows == null) usuarioWindows = "desconocido";

      GuardaBBDD guardabbdd = new GuardaBBDD();       
  try {
    	
         	 HttpResponse<byte[]> response = Unirest.post(url)
    			    .header("Authorization", "Bearer " + token)
    			    .header("Accept", "application/octet-stream")
    			    .header("Content-Type", "application/json")
    			    //.body("{\r\n  \"Codigo\": 4,\r\n  \"Ejercicio\": 2024,\r\n  \"Entidad\": 3,\r\n  \"TipoDeuda\": 0,\r\n  \"TipoFacturacionContabilidad\": 2\r\n}")
    			   // .body("{\r\n  \"Codigo\": 23,\r\n  \"Ejercicio\": 2024,\r\n  \"Entidad\": 3,\r\n  \"TipoDeuda\": 1,\r\n  \"TipoFacturacionContabilidad\": 2\r\n}")
    			    .asBytes();
    	 

       if (response.getStatus() == 200) {
      //  if (1 ==1) {	 
        	  byte[] fileBytes = response.getBody();
        	  
        	
        	  String respuestaAPI = new String(fileBytes, StandardCharsets.UTF_8);
              if (respuestaAPI.contains("No existen ficheros pendientes")) {
                  nombreFichero = "No existen ficheros pendientes de exportación";
                  // No insertamos en BD y devolvemos el mensaje
                  return nombreFichero;
              }
        	  
        	  
        	 // String filePath = "C:\\Users\\carlos\\Downloads\\Contabilidad_Pendientes10.zip";
             //   byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
                  InputStream in = new ByteArrayInputStream(fileBytes);

                  
                  // Cierra el InputStream si no se va a usar más
                  

            

        	    // Aquí puedes procesar los bytes como un InputStream si lo necesitas
        	     //InputStream in = new ByteArrayInputStream(fileBytes);

        	    logger.log("Fichero descargado correctamente, tamaño: " + fileBytes.length);
        	    
        	 String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        	
        	// String contentDisposition = filePath;
        	 //nombreFichero ="Contabilidad_Pendientes_10_JULIO_2025.zip";
        	 if (contentDisposition != null && contentDisposition.contains("filename*=")) {
        	     String[] partes = contentDisposition.split("filename\\*=");
        	     if (partes.length > 1) {
        	         String codificado = partes[1].trim();

        	         // Eliminar prefijo 'utf-8'' si existe
        	         if (codificado.toLowerCase().startsWith("utf-8''")) {
        	             codificado = codificado.substring(7);
        	         }

        	         // Decodificar URL (espacios, tildes, etc.)
        	         //nombreFichero = codificado;
        	         LocalDate hoy = LocalDate.now();
        	         String fecha = hoy.format(DateTimeFormatter.ofPattern("yyyyMMdd"));  // e.g. 20250716
        	         nombreFichero = "Contabilidad_Pendiente_" + fecha + ".zip";
        	         logger.log("Nombre de fichero forzado: " + nombreFichero);
        	        // nombreFichero ="Contabilidad_Pendientes_10_JULIO_2025.zip";
        	         logger.log("Nombre:" + nombreFichero);
        	     }
        	 }
        	 
                	 
        	 
             // Guardar en disco
        	// logger.log("antes de leer fichero");
            if  (nombreFichero != null || nombreFichero.equalsIgnoreCase("")) 
            {
                 // Guardar en Oracle
                int idfichero= guardabbdd.guardarEnBaseDatos(nombreFichero, usuarioWindows,fileBytes, conn,tipoFacturacion,tipoDeuda);
                 logger.log("Fichero descargado y almacenado correctamente."  + idfichero);
                 logger.log("Fichero para processar"  + idfichero);
                 procesazip.procesarZip(fileBytes, idfichero, conn);
                 logger.log("Ficheros procesados.");
            }    
              //   guardarEnBaseDatos(nombreFichero, usuarioWindows, fileBytes);
                 //logger.log("Fichero descargado y almacenado correctamente.");
                 return nombreFichero;
           	
           
        } else {
        	logger.log("ERROR" );
            return "Error al descargar: código " ;
        	  //logger.log("ERROR" + response.getStatus());
              //return "Error al descargar: código " + response.getStatus();
        }

   } catch (Exception e) {
        return "Error general: " + e.getMessage();
    }
    }
    public static int determinarTipoDeuda(String nombreFichero, Map<Integer, String> tiposDeuda) {
        nombreFichero = nombreFichero.toLowerCase();

        int mejorCoincidencia = -1;
        int mejorId = -1;

        for (Map.Entry<Integer, String> entry : tiposDeuda.entrySet()) {
            String valor = entry.getValue().toLowerCase();
            if (nombreFichero.contains(valor)) {
                if (valor.length() > mejorCoincidencia) {
                    mejorCoincidencia = valor.length();
                    mejorId = entry.getKey();
                }
            }
        }

        return mejorId != -1 ? mejorId : 0; // por defecto: Liquidación (ID = 0)
    }



        public static Map<Integer, String> cargarTiposDeudaDesdeBD(Connection conn) throws SQLException {
            Map<Integer, String> tipos = new HashMap<>();

            String sql = "SELECT ID, DESCRIPCION FROM ATM_TR_TIPO_DEUDA";

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("ID");
                    String descripcion = rs.getString("DESCRIPCION");
                    tipos.put(id, descripcion);
                }
            }

            return tipos;
        }
    
    
}
