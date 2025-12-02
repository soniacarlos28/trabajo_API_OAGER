package api_oager;


	import okhttp3.*;

	import java.io.*;
	import java.net.URLDecoder;
	import java.nio.charset.StandardCharsets;

	public class APIfichero{

	    private static final OkHttpClient client = new OkHttpClient.Builder()
	            .build();

	    public static String descargarZip(String token, String ejercicio) {
	        String url = "https://www.oager.com/ApiRest/Contabilidad/GetContabilidadPendiente?ejercicio=" + ejercicio;
	        LoggerService logger=new LoggerService();
	        
	        MediaType mediaType = MediaType.parse("text/plain");
	        RequestBody body = RequestBody.create(mediaType, "");
	        Request request = new Request.Builder()
	                .url(url)
	                .addHeader("Authorization", "Bearer " + token)
	                .method("POST", body)
	                .build();
	        
	          logger.log("URL: " + url);
	    	  logger.log("Bearer " + token);

	        try (Response response = client.newCall(request).execute()) {

	            if (!response.isSuccessful()) {
	            	System.out.println("Error HTTP: " + response.code() + " - " + response.message());
	            	logger.log("Error HTTP: " + response.code() + " - " + response.message());
	                return "Error HTTP: " + response.code() + " - " + response.message();
	            }

	            // Obtener nombre desde Content-Disposition
	            String contentDisposition = response.header("Content-Disposition", "attachment; filename=\"descarga.zip\"");
	            String nombreFichero = "descarga.zip";

	            if (contentDisposition != null && contentDisposition.contains("filename*=")) {
	                String codificado = contentDisposition.split("filename\\*=")[1].trim();
	                if (codificado.toLowerCase().startsWith("utf-8''")) {
	                    codificado = codificado.substring(7);
	                }
	                nombreFichero = codificado;
	            } else if (contentDisposition.contains("filename=")) {
	                nombreFichero = contentDisposition.split("filename=")[1].replaceAll("\"", "").trim();
	            }

	           
	          
	      	     logger.log("ZIP descargado correctamente");
	            return "ZIP descargado correctamente en: " ;
	            

	        } catch (IOException e) {
	        	logger.log("Error al descargar el ZIP: " + e.getMessage());
	      	   
	            return "Error al descargar el ZIP: " + e.getMessage();
	        }
	    }
	}


