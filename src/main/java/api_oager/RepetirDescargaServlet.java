package api_oager;

import okhttp3.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@WebServlet("/repetir_descarga")
public class RepetirDescargaServlet extends HttpServlet {
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/repetir_descarga.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Parámetros de formulario
        int ejercicio   = Integer.parseInt(req.getParameter("year"));
        int codigo      = Integer.parseInt(req.getParameter("codigo"));
        int facturacion = Integer.parseInt(req.getParameter("facturacion"));
        int tipoDeuda   = Integer.parseInt(req.getParameter("tipo_deuda"));

        ApiCall api_llama     = new ApiCall();
        ProcesaZip procesazip = new ProcesaZip();
        String token = api_llama.gettokken();

        // Construye el payload JSON
        String json = String.format(
          "{ \"Codigo\": %d, \"Ejercicio\": %d, \"Entidad\": 3, \"TipoDeuda\": %d, \"TipoFacturacionContabilidad\": %d }",
          codigo, ejercicio, tipoDeuda, facturacion
        );
        RequestBody body = RequestBody.create(
            json,
            MediaType.get("application/json; charset=utf-8")
        );

        // Prepara y ejecuta la llamada HTTP a la API
        Request apiReq = new Request.Builder()
            .url("https://www.oager.com/ApiRest/Contabilidad/GetContabilidad")
            .addHeader("Authorization", "Bearer " + token)
            .post(body)
            .build();

        try (Response apiResp = client.newCall(apiReq).execute()) {
            if (!apiResp.isSuccessful()) {
                resp.sendError(apiResp.code(), "Error API: " + apiResp.message());
                return;
            }

            // 1) Lee todo el ZIP en memoria
            ResponseBody respBody = apiResp.body();
            if (respBody == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Sin cuerpo de respuesta");
                return;
            }
            byte[] fileBytes = respBody.bytes();

            // 2) Extrae el nombre de fichero de la cabecera Content-Disposition
            String cd = apiResp.header("Content-Disposition", "");
            String fileName = "descarga.zip";
            for (String part : cd.split(";")) {
                part = part.trim();
                if (part.startsWith("filename*=")) {
                    String enc = part.split("=", 2)[1];
                    if (enc.toLowerCase().startsWith("utf-8''")) {
                        enc = enc.substring(7);
                    }
                    fileName = URLDecoder.decode(enc, StandardCharsets.UTF_8.name());
                    break;
                } else if (part.startsWith("filename=")) {
                    fileName = part.split("=", 2)[1]
                                   .replace("\"", "")
                                   .trim();
                    break;
                }
            }

            // 3) Envía el ZIP al cliente usando un ByteArrayInputStream
            resp.setContentType("application/zip");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            try (InputStream in = new ByteArrayInputStream(fileBytes);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }

            // 4) Guarda en la base de datos y procesa el ZIP
            if (fileName != null && !fileName.isEmpty()) {
                String usuarioWindows = "pgarcia";
                try (Connection conn = conexion_bd.conexion()) {
                    int idFichero = GuardaBBDD.guardarEnBaseDatos(
                        fileName,
                        usuarioWindows,
                        fileBytes,
                        conn,
                        facturacion,
                        tipoDeuda
                    );
                    procesazip.procesarZip(fileBytes, idFichero, conn);
                   // conn.commit();
                } catch (SQLException e) {
                    throw new ServletException("Error al guardar/procesar en BBDD", e);
                }
            }
        }
    }
}
