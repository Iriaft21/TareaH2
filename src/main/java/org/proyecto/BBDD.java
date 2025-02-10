package org.proyecto;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BBDD {

    //Datos de la base de datos
    private static final String nombreBd = "DBH2";
    private static final Path path = Path.of(nombreBd);
    private static final String url = "jdbc:h2:" + path.toAbsolutePath();
    private static final String usuario = "admin";
    private static final String password = "abc123.";

    public static void crearTabla(){
        // Establecemos la conexión con la base de datos
        try (Connection conn = DriverManager.getConnection(url, usuario, password)) {
            // Eliminamos la tabla Clima si ya existe
            try (PreparedStatement dropTabla = conn.prepareStatement("DROP TABLE IF EXISTS Clima")) {
                dropTabla.execute();
            }
            // Creamos la nueva tabla Clima con sus respectivas columnas y tipos de datos
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
            // Lanzamos una excepción en caso de error en la base de datos
            throw new RuntimeException(e);
        }
    }

    public static void insertarDatos(Prediccion prediccion) {
        // Establecemos la conexión con la base de datos
        try(Connection conn = DriverManager.getConnection(url, usuario, password)){
            // Insertamos datos en la tabla Clima
            try (PreparedStatement insertarDatos = conn.prepareStatement(
                    "INSERT INTO Clima (lugar, fecha, estadoCielo, temperaturaMax, temperaturaMin, precipitacionTotal, viento, coberturaNubosa, humedad)\n" +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                insertarDatos.setString(1, prediccion.getLugar());
                insertarDatos.setString(2, prediccion.getFecha());
                // Convertimos la lista de estado del cielo a una cadena de texto separada por comas
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
            // Lanzamos una excepción en caso de error en la base de datos
            throw new RuntimeException(e);
        }
    }

    public static List<Prediccion> select() {
        //Creamos un ArrayList para almacenar las predicciones
        List<Prediccion> prediccions = new ArrayList<>();
        // Establecemos la conexión con la base de datos
        try(Connection conn = DriverManager.getConnection(url, usuario, password)) {
            // Obtenemos todos los datos de la tabla Clima
            try (PreparedStatement select = conn.prepareStatement("SELECT * FROM Clima")) {
                ResultSet rs = select.executeQuery();
                while(rs.next()) {
                    int id = rs.getInt("id");
                    String lugar = rs.getString("lugar");
                    String fecha = rs.getString("fecha");
                    String estadoCielo = rs.getString("estadoCielo");
                    // Convertimos la cadena de estado del cielo a una lista
                    List<String> cielo = List.of(estadoCielo.split(","));
                    double temperaturaMax = rs.getDouble("temperaturaMax");
                    double temperaturaMin = rs.getDouble("temperaturaMin");
                    double precipitacionTotal = rs.getDouble("precipitacionTotal");
                    double viento = rs.getDouble("viento");
                    double coberturaNubosa = rs.getDouble("coberturaNubosa");
                    double humedad = rs.getDouble("humedad");
                    // Creamos una instancia de Prediccion con los datos obtenidos
                    Prediccion prediccion = new Prediccion(String.valueOf(id), lugar, fecha, cielo, temperaturaMax, temperaturaMin, precipitacionTotal, viento, coberturaNubosa, humedad);
                    //Añadimos dicha prediccion al ArrayList
                    prediccions.add(prediccion);
                }
            }
            //Devolvemos el ArrayList
            return prediccions;
        } catch (SQLException e) {
            // Lanzamos una excepción en caso de error en la base de datos
            throw new RuntimeException(e);
        }
    }


    public static void modificarDatos(Prediccion prediccion){
        // Establecemos la conexión con la base de datos
        try (Connection conn = DriverManager.getConnection(url, usuario, password)) {
            // Actualizamos los datos de la tabla Clima basados en el id
            String sql = "UPDATE Clima SET lugar = ?, fecha = ?, estadoCielo = ?, temperaturaMax = ?, temperaturaMin = ?, precipitacionTotal = ?, viento = ?, coberturaNubosa = ?, humedad = ? " +
                    "WHERE id = ?";
            try (PreparedStatement actualizarDatos = conn.prepareStatement(sql)) {
                actualizarDatos.setString(1, prediccion.getLugar());
                actualizarDatos.setString(2, prediccion.getFecha());
                // Convertimos la lista de estado del cielo a una cadena de texto separada por comas
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
            // Lanzamos una excepción en caso de error en la base de datos
            throw new RuntimeException(e);
        }
    }

    public static void eliminarPrediccion(int id) {
        // Establecemos la conexión con la base de datos
        try (Connection conn = DriverManager.getConnection(url, usuario, password)) {
            // Eliminamos una predicción de la tabla Clima basada en el id
            String sql = "DELETE FROM Clima WHERE id = ?";
            try (PreparedStatement borrarDatos = conn.prepareStatement(sql)) {
                borrarDatos.setInt(1, id);
                int filasAfectadas = borrarDatos.executeUpdate();
                //Mostramos un aviso mostrando los elementos eliminados
                if (filasAfectadas > 0) {
                    System.out.println("Elemento con id " + id + " eliminado.");
                } else {
                    //Tambien mostramos otro aviso en caso de no encontrar el elemento
                    System.out.println("No se encontró el elemento con id " + id + ".");
                }
            }
        } catch (SQLException e) {
            // Lanzamos una excepción en caso de error en la base de datos
            throw new RuntimeException(e);
        }
    }
}