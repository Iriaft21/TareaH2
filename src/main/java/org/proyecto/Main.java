package org.proyecto;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Main {

    private static Scanner scL = new Scanner(System.in);
    private static Scanner scN = new Scanner(System.in);

    public static void main(String[] args) {
        int respuesta;
        BBDD.crearTabla();

        //ruta del archivo json
        String json = "./prediccion.json";
        //direccion donde se va a crear el .csv
        Path direccionArchivo = Paths.get("D:\\Predicciones\\25-11-2023-galicia.csv");
        //araylist donde se va a guardar las predicciones
        List<Prediccion> predicciones = new ArrayList<>();

        obtenerPredicciones(json, predicciones);
        do{
            //mostramos las opciones al usuario
            System.out.println("Que accion deseas realizar? \n1.Mostrar datos en pantalla \n2.Generar archivo .csv con datos de la ciudad elegida " +
                    "\n3.Mostrar las predicciones almacenadas \n4.Modificar una prediccion \n5.Eliminar una prediccion \n6.Buscar la prediccion de otra ciudad \n7.Salir");
            //obtenemos su respuesta
            respuesta = scN.nextInt();

            switch(respuesta){
                case 1:
                    if (predicciones.isEmpty()) {
                        //se avisa si no hay ninguna
                        System.out.println("No hay predicciones disponibles para mostrar.");
                    } else {
                        //se imprimen las predicciones
                        for (Prediccion p : predicciones) {
                            System.out.println(p);
                        }
                    }
                    break;
                case 2:
                    //llamamos al metodo que escribe el csv
                    escribirCSV(direccionArchivo, predicciones);
                    break;
                case 3:
                    List<Prediccion> prediccionesBD = BBDD.select();
                    for(Prediccion prediccion : prediccionesBD){
                        System.out.println(prediccion.toString());
                    }
                    System.out.println("Patata");
                    break;
                case 4:
                    System.out.println("Cal es la prediccion que deseas modificar?");
                    int i = 1;
                    List<Prediccion> prediccionesBDModif = BBDD.select();
                    for(Prediccion prediccion : prediccionesBDModif){
                        System.out.println(i + "." + prediccion);
                        i++;
                    }
                    int prediccionModif =  scN.nextInt();
                    //TODO preguntar que se desea modificar
                    BBDD.modificarDatos(prediccionesBDModif.get(prediccionModif));
                    break;
                case 5:
                    System.out.println("Cual es la prediccion que deses eliminar?");
                    int j = 1;
                    List<Prediccion> prediccionesBDElim = BBDD.select();
                    for(Prediccion prediccion : prediccionesBDElim){
                        System.out.println(j + "." + prediccion);
                        j++;
                    }
                    int prediccionElim = scN.nextInt();
                    BBDD.eliminarPrediccion(prediccionElim);
                    break;
                case 6:
                    predicciones.clear();
                    obtenerPredicciones(json, predicciones);
                    break;
            }
        }while(respuesta != 7); //repetir hasta que el usuario seleccione el numero 3
    }

    private static void obtenerPredicciones(String json, List<Prediccion> predicciones) {
        String idLugar = buscarLugar();
        if (idLugar == null) {
            System.out.println("No se pudo encontrar la ubicación. Inténtalo de nuevo.");
            return;
        }

        String ApiUrl ="https://servizos.meteogalicia.gal/apiv4/getNumericForecastInfo?locationIds=" + idLugar +"&variables=temperature,wind,sky_state,precipitation_amount,relative_humidity,cloud_area_fraction&API_KEY=4hk91p9mQV1qysT4PE1YJndSRCebJhd5E1uOf07nU1bcqiR0GN1qLy3SfkRp6f4B";

        conexionApi(ApiUrl, json);
        Parsear.parsearPredicciones(json, predicciones);
        if (predicciones.isEmpty()) {
            System.out.println("No se encontraron predicciones para este lugar.");
        }
    }

    private  static String buscarLugar(){
        HashMap<String, String> lugares = new HashMap<>();
        System.out.println("Dime el nombre del lugar de dónde deseas obtener la predicción");
        String nombreLugar  = scL.nextLine().trim();
        //TODO comprobacion de que no vaya en vacio el nombre y mirar de que no pete con las tildes
        String urlFindPlace = "https://servizos.meteogalicia.gal/apiv4/findPlaces?location="+ nombreLugar +"&API_KEY=4hk91p9mQV1qysT4PE1YJndSRCebJhd5E1uOf07nU1bcqiR0GN1qLy3SfkRp6f4B";
        findPlace(urlFindPlace, lugares);
        System.out.println("Cuál de los siguientes lugares es?");
        String idSeleccionado = "";

        if (!lugares.isEmpty()) {
            Map<Integer, String> lugaresMap = new HashMap<>();
            int[] i = {1}; // Usa un array para que pueda ser modificado dentro de la lambda
            lugares.forEach((key, value) -> {
                lugaresMap.put(i[0], key);
                System.out.println(i[0] + ". " + value);
                i[0]++;
            });
            System.out.print("Seleccione un número: ");
            int opcion = scN.nextInt();

            idSeleccionado = lugaresMap.get(opcion);
            return idSeleccionado;
        }
        return null;
    }

    private static void findPlace(String urlFinPlace, HashMap<String,String> lugares){
        try {
            // Configurar la URL y la conexión HTTP
            URL url = new URL(urlFinPlace);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            // Leer la respuesta de la API utilizando un BufferedReader
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder stringBuilder = new StringBuilder();
                String linea;
                // Leer cada línea de la respuesta y agregarla al StringBuilder
                while ((linea = reader.readLine()) != null) {
                    stringBuilder.append(linea);
                }
                Parsear. parsearLugar(lugares, stringBuilder);
            }
        } catch (MalformedURLException e) {
            // Manejar excepciones de URL mal formadas
            System.out.println("La URL es incorrecta: " + e.getMessage());
        } catch (IOException e) {
            // Manejar excepciones de entrada/salida
            System.out.println("Error al leer o escribir el archivo: " + e.getMessage());
        }
    }

    private static void conexionApi(String ApiUrl, String json) {
        try {
            URL url = new URL(ApiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder stringBuilder = new StringBuilder();
                String linea;
                while ((linea = reader.readLine()) != null) {
                    stringBuilder.append(linea);
                }

                Files.write(Paths.get(json), stringBuilder.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Archivo JSON guardado correctamente.");
            }
        } catch (IOException e) {
            System.out.println("Error al conectar con la API: " + e.getMessage());
        }
    }

    public static void escribirCSV(Path direccionArchivo, List<Prediccion> predicciones){
        // Mensaje indicando que se seleccionó crear un archivo CSV
        System.out.println("Has seleccionado crear un archivo .csv con los resultados de la ciudad que has elegido");
        try {
            // Se obtiene la ruta del directorio que contiene el archivo .csv
            Path pathDirectorio = direccionArchivo.getParent();
            // Si no existe dicho directorio, se crea, así como el archivo csv
            if (pathDirectorio != null && !Files.exists(pathDirectorio)) {
                Files.createDirectories(pathDirectorio);
            }
            if (!Files.exists(direccionArchivo)) {
                Files.createFile(direccionArchivo);
            }
            // Escribir encabezados y datos
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(direccionArchivo.toFile(), false))) { // false para sobrescribir
                // Escribir encabezados
                bw.write("Lugar,Fecha,EstadoCielo,TemperaturaMax,TemperaturaMin,Precipitacion,Viento,CoberturaNubosa,Humedad");
                bw.newLine();
                // Verificar si hay datos
                if (predicciones.isEmpty()) {
                    System.out.println("No hay datos para escribir en el archivo CSV.");
                    return;
                }
                // Escribir datos de las predicciones en el archivo .csv
                for (Prediccion prediccion : predicciones) {
                    String datos = prediccion.getLugar() + "," + prediccion.getFecha() + "," + prediccion.getEstadoCielo() + ","
                            + prediccion.getTemperaturaMax() + "," + prediccion.getTemperaturaMin() + ","
                            + prediccion.getPrecipitacionTotal() + "," + prediccion.getViento() + ","
                            + prediccion.getCoberturaNubosa() + "," + prediccion.getHumedad();
                    bw.write(datos);
                    bw.newLine();
                }
                // Mensaje indicando que el archivo CSV fue creado y los datos fueron escritos exitosamente
                System.out.println("Archivo CSV creado y datos escritos exitosamente.");
            }
        } catch (IOException e) {
            // Manejar excepciones de entrada/salida y generar una excepción en tiempo de ejecución
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}