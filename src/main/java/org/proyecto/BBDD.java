package org.proyecto;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BBDD {

    private static final String nombreBd = "DBH2";
    private static final Path path = Path.of(nombreBd);
    private static final String url = "jdbc:h2:" + path.toAbsolutePath();
    private static final String usuario = "admin";
    private static final String password = "abc123.";

    public static void crearTabla(){
        try (Connection conn = DriverManager.getConnection(url, usuario, password)) {
            // Eliminar la tabla si ya existe
            try (PreparedStatement dropTabla = conn.prepareStatement("DROP TABLE IF EXISTS Clima")) {
                dropTabla.execute();
            }

            // Crear la tabla
            try (PreparedStatement crearTabla = conn.prepareStatement(
                    "CREATE TABLE Clima (\n" +
                            " id INT AUTO_INCREMENT NOT NULL,\n" +
                            " lugar VARCHAR(255) NOT NULL,\n" +
                            " fecha VARCHAR(255) NOT NULL,\n" +
                            " estadoCielo TEXT NOT NULL,\n" +
                            " temperaturaMax DECIMAL(5, 2) NOT NULL,\n" +
                            " temperaturaMin DECIMAL(5, 2) NOT NULL,\n" +
                            " precipitacionTotal DECIMAL(5, 2) NOT NULL,\n" +
                            " viento DECIMAL(5, 2) NOT NULL,\n" +
                            " coberturaNubosa DECIMAL(5, 2) NOT NULL,\n" +
                            " humedad DECIMAL(5, 2) NOT NULL,\n" +
                            " PRIMARY KEY (id)\n" +
                            ");")) {
                crearTabla.execute();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void insertarDatos(Prediccion prediccion) {
        try(Connection conn = DriverManager.getConnection(url, usuario, password)){
            try (PreparedStatement insertarDatos = conn.prepareStatement(
                    "INSERT INTO Clima (lugar, fecha, estadoCielo, temperaturaMax, temperaturaMin, precipitacionTotal, viento, coberturaNubosa, humedad)\n" +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                insertarDatos.setString(1, prediccion.getLugar());
                insertarDatos.setString(2, prediccion.getFecha());
                List<String> estadoCieloList = prediccion.getEstadoCielo();
                String estadoCielo = String.join(",", estadoCieloList);
                insertarDatos.setString(3, estadoCielo);
                insertarDatos.setDouble(4, prediccion.getTemperaturaMax());
                insertarDatos.setDouble(5, prediccion.getTemperaturaMin());
                insertarDatos.setDouble(6, prediccion.getPrecipitacionTotal());
                insertarDatos.setDouble(7, prediccion.getViento());
                insertarDatos.setDouble(8, prediccion.getCoberturaNubosa());
                insertarDatos.setDouble(9, prediccion.getHumedad());
                insertarDatos.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Prediccion> select() {
        List<Prediccion> prediccions = new ArrayList<>();
        try(Connection conn = DriverManager.getConnection(url, usuario, password)) {
            try (PreparedStatement select = conn.prepareStatement("SELECT * FROM Clima")) {
                ResultSet rs = select.executeQuery();
                while(rs.next()) {
                    int id = rs.getInt("id");
                    String lugar = rs.getString("lugar");
                    String fecha = rs.getString("fecha");
                    String estadoCielo = rs.getString("estadoCielo");
                    List<String> cielo = List.of(estadoCielo.split(","));
                    double temperaturaMax = rs.getDouble("temperaturaMax");
                    double temperaturaMin = rs.getDouble("temperaturaMin");
                    double precipitacionTotal = rs.getDouble("precipitacionTotal");
                    double viento = rs.getDouble("viento");
                    double coberturaNubosa = rs.getDouble("coberturaNubosa");
                    double humedad = rs.getDouble("humedad");

                    Prediccion prediccion = new Prediccion(String.valueOf(id), lugar, fecha, cielo, temperaturaMax, temperaturaMin, precipitacionTotal, viento, coberturaNubosa, humedad);
                    prediccions.add(prediccion);
                }
            }
            return prediccions;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static void modificarDatos(Prediccion prediccion){
        try (Connection conn = DriverManager.getConnection(url, usuario, password)) {
            String sql = "UPDATE Clima SET lugar = ?, fecha = ?, estadoCielo = ?, temperaturaMax = ?, temperaturaMin = ?, precipitacionTotal = ?, viento = ?, coberturaNubosa = ?, humedad = ? " +
                    "WHERE id = ?";
            try (PreparedStatement actualizarDatos = conn.prepareStatement(sql)) {
                actualizarDatos.setString(1, prediccion.getLugar());
                actualizarDatos.setString(2, prediccion.getFecha());
                List<String> estadoCieloList = prediccion.getEstadoCielo();
                String estadoCielo = String.join(",", estadoCieloList);
                actualizarDatos.setString(3, estadoCielo);
                actualizarDatos.setDouble(4, prediccion.getTemperaturaMax());
                actualizarDatos.setDouble(5, prediccion.getTemperaturaMin());
                actualizarDatos.setDouble(6, prediccion.getPrecipitacionTotal());
                actualizarDatos.setDouble(7, prediccion.getViento());
                actualizarDatos.setDouble(8, prediccion.getCoberturaNubosa());
                actualizarDatos.setDouble(9, prediccion.getHumedad());
                actualizarDatos.setInt(10, Integer.parseInt(prediccion.getId()));
                actualizarDatos.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void eliminarPrediccion(int id) {
        try (Connection conn = DriverManager.getConnection(url, usuario, password)) {
            String sql = "DELETE FROM Clima WHERE id = ?";
            try (PreparedStatement borrarDatos = conn.prepareStatement(sql)) {
                borrarDatos.setInt(1, id);
                int filasAfectadas = borrarDatos.executeUpdate();

                // Mensaje de depuración
                if (filasAfectadas > 0) {
                    System.out.println("Elemento con id " + id + " eliminado.");
                } else {
                    System.out.println("No se encontró el elemento con id " + id + ".");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}