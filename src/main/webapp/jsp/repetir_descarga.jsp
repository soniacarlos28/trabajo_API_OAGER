<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ page import="java.time.Year" %>
<%@ include file="menu.jspf" %>
<%@ include file="verificar_permisos.jspf" %>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Repetir descarga de Fichero</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
  <div class="main-content">
    <h2>Repetir descarga de Fichero desde el OAGER</h2>

    <form action="${pageContext.request.contextPath}/repetir_descarga" method="post">
      <div>
        <label for="year">Año:</label>
        <select id="year" name="year">
          <option value="2025" <%= Year.now().getValue()==2025?"selected":"" %>>2025</option>
          <option value="2026" <%= Year.now().getValue()==2026?"selected":"" %>>2026</option>
        </select>
      </div>

      <div>
        <label for="codigo">Código:</label>
        <input type="number" id="codigo" name="codigo" required />
      </div>

      <div>
        <label for="facturacion">Facturación:</label>
        <select id="facturacion" name="facturacion">
          <option value="1">Cargo</option>
          <option value="2">FacturaVoluntaria</option>
          <option value="3">FacturaEjecutiva</option>
          <option value="4">DataImprocedenteVoluntaria</option>
          <option value="5">DateImprocedenteEjecutiva</option>
          <option value="6">DataFallido</option>
        </select>
      </div>

      <div>
        <label for="tipo_deuda">Tipo de Deuda:</label>
        <select id="tipo_deuda" name="tipo_deuda">
          <option value="0">Liquidacion</option>
          <option value="1">Autoliquidacion</option>
          <option value="2">Recibo</option>
          <option value="3">Devolucion</option>
          <option value="4">Multa</option>
          <option value="5">IngresoDirecto</option>
        </select>
      </div>

      <div style="margin-top:20px;">
        <button type="submit">Descargar</button>
      </div>
    </form>
  </div>
</body>
</html>

