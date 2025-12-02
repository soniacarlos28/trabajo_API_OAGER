<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" language="java" %>
<%@ page import="java.sql.*, java.util.*" %>
<%@ include file="menu.jspf" %>
<%@ include file="verificar_permisos.jspf" %>

<%
    // ---------------------- LÓGICA SERVIDOR: CONSULTA + CONTADORES ----------------------
    String codigo = request.getParameter("codigo");
    String tipo   = request.getParameter("tipo");
    String tipo2  = request.getParameter("tipo2");
    String descargadoParam = request.getParameter("descargado");

    StringBuilder where = new StringBuilder();
    if (codigo != null && !codigo.trim().isEmpty())
        where.append(" AND codigo LIKE '%").append(codigo).append("%'");
    if (tipo != null && !tipo.trim().isEmpty())
        where.append(" AND id_tipo LIKE '%").append(tipo).append("%'");
    if (tipo2 != null && !tipo2.trim().isEmpty())
        where.append(" AND id_tipo2 LIKE '%").append(tipo2).append("%'");
    if (descargadoParam != null && !descargadoParam.trim().isEmpty())
        where.append(" AND descargado = ").append(descargadoParam);

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM ( ");
    sql.append("SELECT DISTINCT d.descargado, ");
    sql.append("       d.identificador_fichero, d.codigo, ");
    sql.append("       z.descripcion AS tipo2, z.id AS id_tipo2, ");
    sql.append("       t.id AS id_tipo, t.descripcion AS tipo, ");
    sql.append("       NVL(SUM(e.importe + NVL(e.importe_iva,0)), NVL(SUM(p.importe_total), 0)) AS importe, ");
    sql.append("       CASE WHEN e.id_fichero IS NOT NULL THEN 'excel' ");
    sql.append("            WHEN p.id_fichero IS NOT NULL THEN 'pdf' ");
    sql.append("            ELSE 'Descargar' END AS tipo_fichero, ");
    sql.append("       NVL(a.id,0) AS id_fichero ");
    sql.append("  FROM atm_fichero_descarga d ");
    sql.append("  LEFT JOIN atm_fichero_ingresos a ON a.identificador_fichero = d.identificador_fichero ");
    sql.append("  LEFT JOIN atm_fichero_ing_excel e ON e.id_fichero = a.id  ");
    sql.append("  LEFT JOIN atm_fichero_ing_pdf p ON p.id_fichero = a.id ");
    sql.append("  LEFT JOIN atm_tr_tipo_deuda t ON d.tipo_deuda = t.id ");
    sql.append("  LEFT JOIN atm_tr_tipo_fact_cont z ON d.tipo_facturacion_contab = z.id ");
    sql.append(" GROUP BY d.descargado, d.identificador_fichero, d.codigo, ");
    sql.append("          z.descripcion, z.id, t.id, t.descripcion, a.id, ");
    sql.append("          CASE WHEN e.id_fichero IS NOT NULL THEN 'excel' ");
    sql.append("               WHEN p.id_fichero IS NOT NULL THEN 'pdf' ");
    sql.append("               ELSE 'Descargar' END ");
    sql.append(") WHERE 1=1 ").append(where);
    sql.append(" ORDER BY tipo2, tipo, TO_NUMBER(codigo)");

    // Lista de filas para luego pintar la tabla
    List<Map<String, Object>> filas = new ArrayList<Map<String, Object>>();

    // Contadores por estado
    int totalEstado0 = 0; // pendiente
    int totalEstado1 = 0; // descargado
    int totalEstado2 = 0; // cargado en GEMA
    int totalEstado3 = 0; // fiscalizado

    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;

    try {
        conn = DriverManager.getConnection(MM_RRHH_STRING, MM_RRHH_USERNAME, MM_RRHH_PASSWORD);
        stmt = conn.createStatement();
        rs = stmt.executeQuery(sql.toString());

        while (rs.next()) {
            int estado = rs.getInt("descargado");

            // Contar por estado
            switch (estado) {
                case 0: totalEstado0++; break;
                case 1: totalEstado1++; break;
                case 2: totalEstado2++; break;
                case 3: totalEstado3++; break;
            }

            Map<String, Object> fila = new HashMap<String, Object>();
            fila.put("estado", estado);
            fila.put("idFichero", rs.getString("id_fichero"));
            fila.put("identificador", rs.getString("identificador_fichero"));
            fila.put("codigo", rs.getString("codigo"));
            fila.put("tipo2", rs.getString("tipo2"));
            fila.put("tipo", rs.getString("tipo"));
            fila.put("importe", rs.getDouble("importe"));
            fila.put("tipo_fichero", rs.getString("tipo_fichero"));

            filas.add(fila);
        }
    } finally {
        if (rs != null) try { rs.close(); } catch (Exception e) {}
        if (stmt != null) try { stmt.close(); } catch (Exception e) {}
        if (conn != null) try { conn.close(); } catch (Exception e) {}
    }

    // Dividir entre 2 los totales (división entera)
    int totalEstado0Div = totalEstado0 / 2;
    int totalEstado1Div = totalEstado1 / 2;
    int totalEstado2Div = totalEstado2 / 2;
    int totalEstado3Div = totalEstado3 / 2;
%>

<!DOCTYPE html>
<html>
<head>
<title>Listado de Ficheros Ingresos</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<style>
  .estado-cell { text-align:center; width:48px; }
  .estado-icon { display:inline-flex; align-items:center; justify-content:center; width:22px; height:22px; }
  .estado-0 { color:#2563eb; }   /* pendiente: azul descarga */
  .estado-1 { color:#ea580c; }   /* descargado: naranja */
  .estado-2 { color:#d6b85a; }   /* cargado en GEMA: beige */
  .estado-3 { color:#16a34a; }   /* fiscalizado: verde */
  .estado-btn{ background:none;border:0;cursor:pointer;padding:0; }
  .estado-btn:disabled{ cursor:not-allowed; opacity:.6; }
  .estado-btn svg { pointer-events:none; }
  .legend { display:flex; gap:3rem; align-items:center; margin:.5rem 0 1rem; font-size:.9rem; }
  .legend span { display:inline-flex; align-items:center; gap:.35rem; white-space:nowrap; }
</style>
</head>
<body>
<div class="main-content">
<h2>Listado de Ficheros Ingresos</h2>

<!-- FORMULARIO DE BÚSQUEDA -->
<div class="actions">
<form method="get" style="display:inline-block;">
    Código:
    <input type="text" name="codigo" value="<%= request.getParameter("codigo") != null ? request.getParameter("codigo") : "" %>">

    Facturación:
    <select name="tipo2">
        <option value="">-- Todas --</option>
        <option value="1" <%= "1".equals(request.getParameter("tipo2"))?"selected":"" %>>Cargo</option>
        <option value="2" <%= "2".equals(request.getParameter("tipo2"))?"selected":"" %>>FacturaVoluntaria</option>
        <option value="3" <%= "3".equals(request.getParameter("tipo2"))?"selected":"" %>>FacturaEjecutiva</option>
        <option value="4" <%= "4".equals(request.getParameter("tipo2"))?"selected":"" %>>DataImprocedenteVoluntaria</option>
        <option value="5" <%= "5".equals(request.getParameter("tipo2"))?"selected":"" %>>DataImprocedenteEjecutiva</option>
        <option value="6" <%= "6".equals(request.getParameter("tipo2"))?"selected":"" %>>DataFallido</option>
    </select>

    Tipo Deuda:
    <select name="tipo">
        <option value="">-- Todas --</option>
        <option value="0" <%= "0".equals(request.getParameter("tipo"))?"selected":"" %>>Liquidacion</option>
        <option value="1" <%= "1".equals(request.getParameter("tipo"))?"selected":"" %>>Autoliquidacion</option>
        <option value="2" <%= "2".equals(request.getParameter("tipo"))?"selected":"" %>>Recibo</option>
        <option value="3" <%= "3".equals(request.getParameter("tipo"))?"selected":"" %>>Devolucion</option>
        <option value="4" <%= "4".equals(request.getParameter("tipo"))?"selected":"" %>>Multa</option>
        <option value="5" <%= "5".equals(request.getParameter("tipo"))?"selected":"" %>>IngresoDirecto</option>
    </select>

    Estado:
    <select name="descargado">
        <option value="">-- Todos --</option>
        <option value="0" <%= "0".equals(request.getParameter("descargado"))?"selected":"" %>>0 - Pendiente</option>
        <option value="1" <%= "1".equals(request.getParameter("descargado"))?"selected":"" %>>1 - Descargado</option>
        <option value="2" <%= "2".equals(request.getParameter("descargado"))?"selected":"" %>>2 - Cargado en GEMA</option>
        <option value="3" <%= "3".equals(request.getParameter("descargado"))?"selected":"" %>>3 - Fiscalizado</option>
    </select>

    <input type="submit" value="Buscar">
</form>
<button onclick="window.print()">Imprimir</button>
<button onclick="exportarCSV()">Exportar a Excel (CSV)</button>
</div>

<!-- LEYENDA ARRIBA, CON TOTALES DIVIDIDOS ENTRE 2 -->
<div class="legend">
  <span>
    <svg width="20" height="20" viewBox="0 0 24 24" class="estado-icon estado-0" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M12 3v12m0 0-4-4m4 4 4-4M4 21h16"/>
    </svg>
    Pdte descarga GTI (<%= totalEstado0Div %>) --
  </span>

  <span>
    <svg width="20" height="20" viewBox="0 0 24 24" class="estado-icon estado-1" fill="currentColor">
      <path d="M4 3h16v18H4z"/>
      <path d="M8 3v4h8V3z" fill="#fff" opacity="0.4"/>
    </svg>
    Descargado de GTI (<%= totalEstado1Div %>) --
  </span>

  <span>
    <svg width="22" height="22" viewBox="0 0 24 24" class="estado-icon estado-2">
      <text x="6" y="17" font-size="14" font-weight="bold" fill="#d6b85a">G</text>
    </svg>
    Cargado en GEMA (<%= totalEstado2Div %>) --
  </span>

  <span>
    <svg width="20" height="20" viewBox="0 0 24 24" class="estado-icon estado-3" fill="none" stroke="currentColor" stroke-width="2">
      <circle cx="12" cy="12" r="9"/>
      <path d="M9 12l2 2 4-4"/>
    </svg>
    Fiscalizado GEMA (<%= totalEstado3Div %>) 
  </span>
</div>


<table id="tablaFicheros">
<thead>
<tr>
  <th>Estado</th>
  <th>Identificador</th>
  <th>Código</th>
  <th>Facturación</th>
  <th>Tipo Deuda</th>
  <th>Importe</th>
  <th>Tipo Fichero</th>
  <th class="no-exportar">Acciones</th>
</tr>
</thead>
<tbody>
<%
    for (Map<String, Object> fila : filas) {
        int estado = (Integer) fila.get("estado");
        String idFichero = (String) fila.get("idFichero");
        String identificador = (String) fila.get("identificador");
        String codigoRow = (String) fila.get("codigo");
        String tipo2Row = (String) fila.get("tipo2");
        String tipoRow = (String) fila.get("tipo");
        double importe = (Double) fila.get("importe");
        String tipoFichero = (String) fila.get("tipo_fichero");

        String iconoSvg = "";
        switch (estado) {
            case 0:
                iconoSvg = "<svg width='20' height='20' viewBox='0 0 24 24' class='estado-icon estado-0' fill='none' stroke='currentColor' stroke-width='2'><path d='M12 3v12m0 0-4-4m4 4 4-4M4 21h16'/></svg>";
                break;
            case 1:
                iconoSvg = "<svg width='20' height='20' viewBox='0 0 24 24' class='estado-icon estado-1' fill='currentColor'><path d='M4 3h16v18H4z'/><path d='M8 3v4h8V3z' fill='#fff' opacity='0.4'/></svg>";
                break;
            case 2:
                iconoSvg = "<svg width='22' height='22' viewBox='0 0 24 24' class='estado-icon estado-2'><text x='6' y='17' font-size='14' font-weight='bold' fill='#d6b85a'>G</text></svg>";
                break;
            case 3:
                iconoSvg = "<svg width='20' height='20' viewBox='0 0 24 24' class='estado-icon estado-3' fill='none' stroke='currentColor' stroke-width='2'><circle cx='12' cy='12' r='9'/><path d='M9 12l2 2 4-4'/></svg>";
                break;
        }
%>
<tr>
  <td class="estado-cell">
    <button type="button" class="estado-btn" data-identificador="<%= identificador %>" data-estado="<%= estado %>" onclick="intentCambiarEstado(this)">
      <%= iconoSvg %>
    </button>
  </td>
  <td><%= identificador %></td>
  <td><%= codigoRow %></td>
  <td><%= tipo2Row %></td>
  <td><%= tipoRow %></td>
  <td style="text-align:right;"><%= importe %></td>
  <td><%= tipoFichero %></td>
  <td class="no-exportar">
    <a href="descargar_blob.jsp?id_fichero=<%= idFichero %>">Descargar</a>
  </td>
</tr>
<%
    }
%>
</tbody>
</table>

<script>
function intentCambiarEstado(btn){
  var id = btn.getAttribute('data-identificador');
  var estado = parseInt(btn.getAttribute('data-estado'), 10);

  // Estado 3 = final, no se puede cambiar
  if (estado === 3) {
    alert('No se puede cambiar el estado de un fichero ya Fiscalizado en GEMA');
    return;
  }

  // Calcular el siguiente estado automáticamente
  var nuevo = (estado === 1) ? 2 : 3;
  var txt = (nuevo === 2 ? 'Cargado en GEMA' : 'Fiscalizado en GEMA');

  if (!confirm('¿Confirmas cambiar el estado a "' + txt + '"?')) return;

  // Crear y enviar formulario POST a update_descargado.jsp
  var f = document.createElement('form');
  f.method = 'POST';
  f.action = 'procesar_ficheros_accion.jsp';

  var i1 = document.createElement('input');
  i1.type = 'hidden';
  i1.name = 'identificador';
  i1.value = id;

  var i2 = document.createElement('input');
  i2.type = 'hidden';
  i2.name = 'nuevo';
  i2.value = String(nuevo);

  var i3 = document.createElement('input');
  i3.type = 'hidden';
  i3.name = 'returnUrl';
  i3.value = location.search.substring(1);

  f.appendChild(i1);
  f.appendChild(i2);
  f.appendChild(i3);
  document.body.appendChild(f);
  f.submit();
}
</script>

</div>
</body>
</html>

