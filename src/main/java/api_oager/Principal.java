package api_oager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Principal {
	private static final String DB_URL = "fadfa";
    private static final String DB_USER = "XXXXX";
    private static final String DB_PASSWORD = "XXXX";
	public static void main(String[] args) throws IOException {
		String usuario="";
        try {
			Descarga(usuario);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
    public static String Descarga(String usuario) throws SQLException, IOException {
        // Aquí puedes invocar ZipDownloader.descargarYGuardar(...) y ZipProcessor.procesarZip(...)
        
    	String tokken="";
    	String ejercicio="2025";
    	String resultado ="";
  	   final String LOG_PATH = "C:\\temp\\OAGER_FICHERO.txt";
  	  conexion_bd con=new conexion_bd();
  	   LoggerService logger=new LoggerService();
  	  logger.log("Inicio del proceso");
  	
  	  
  	    //Descarga fichero y guarda fichero ATM_FICHERO_CARGA
    	ApiCall api_llama=new ApiCall();
    	
    	//APIfichero api_fichero=new APIfichero();
    	
    	//Procesa el zip
    	 ProcesaZip procesazip=new ProcesaZip();
    	
    	 //obtenemos tokken
    	tokken=api_llama.gettokken();
    	 //logger.log("Tokken:"  + tokken);
    	
    		 try( Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD))
    		 { logger.log("Conexion abierta");
    	 
    	 if (conn == null && conn.isClosed())  logger.log("Conexion cerrada 2");
     	//descarga fichero y guardarlos.
    	 
    	 
     	resultado =ApiCall.descargarYGuardarEnBD(tokken, ejercicio,conn,usuario);
     
    	/* procesazip.procesarZip(LOG_PATH, 0, conn);
     	 try {
     		conn.close();
     	} catch (SQLException e) {
     		// TODO Auto-generated catch block
     		e.printStackTrace();
     	}*/
            
    		 }
    catch (SQLException e) {
        e.printStackTrace();
        logger.log("Error general: " + e.getMessage());
        return e.getMessage();
    }
    		 
    
  	  logger.log("Fin del proceso");
  	return resultado;
  	 
    }
    
    
    
}
