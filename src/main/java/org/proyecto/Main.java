package org.proyecto;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;

public class Main {
    //Se crean los correspondientes scanners para Strigns y para números
    private static Scanner scL = new Scanner(System.in);
    private static Scanner scN = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        int respuesta;
        BBDD.crearTabla();
        //ruta del archivo json
        String json = "./prediccion.json";
        //direccion donde se va a crear el .csv
        int fechaActual = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        Path direccionArchivo = Paths.get("D:\\Predicciones\\Predicciones_dias_" + fechaActual + "-" + (fechaActual + 4) +"-galicia.csv");
        //araylist donde se va a guardar las predicciones
        List<Prediccion> predicciones = new ArrayList<>();
        //Llamamos al método que buscar el lugar y sus respectivas predicciones
        obtenerPredicciones(json, predicciones);
        do{
            //mostramos las opciones al usuario
            System.out.println("Que accion deseas realizar? \n1.Mostrar las predicciones almacenadas  " +
                    "\n2.Modificar una prediccion \n3.Generar archivo .csv con las predicciones almacenadas" +
                    "\n4.Eliminar una prediccion \n5.Obtener  la prediccion de otra ciudad \n6.Salir");
            //obtenemos su respuesta
            respuesta = scN.nextInt();

            switch(respuesta){
                case 1:
                    //Metemos en un List todas las predicciones que hay en H2
                    List<Prediccion> prediccionesBD = BBDD.select();
                    //se recorrre el List
                    for(Prediccion prediccion : prediccionesBD){
                        //MOstramos en consola las predicciones
                        System.out.println(prediccion.toString());
                    }
                    break;
                case 2:
                    //llamamos al método que modifica los datos de las predicciones
                    modificarDatos();
                    break;
                case 3:
                    //llamamos al metodo que escribe el csv
                    escribirCSV(direccionArchivo, predicciones);
                    break;
                case 4:
                    //Preguntamos por la prediccion que se desea eliminar
                    System.out.println("Cual es la prediccion que deses eliminar?");
                    int j = 1;
                    //Traemos de la base de datos todas las predicciones
                    List<Prediccion> prediccionesBDElim = BBDD.select();
                    for(Prediccion prediccion : prediccionesBDElim){
                        //Se las mostramos al usuario
                        System.out.println(j + "." + prediccion);
                        j++;
                    }
                    //Se obtiene la respuesta
                    int prediccionElim = scN.nextInt();
                    //LLamamos al método que borrará la predicción seleccionada
                    BBDD.eliminarPrediccion(prediccionElim);
                    break;
                case 5:
                    //Limpiamos el ArrayList que almacena las predicciones de un lugar
                    predicciones.clear();
                    //Volvemos a preguntar al usuario por un lugar y obtenemos las predicciones del mismo
                    obtenerPredicciones(json, predicciones);
                    break;
            }
        }while(respuesta != 6);
    }

    private static void obtenerPredicciones(String json, List<Prediccion> predicciones) throws Exception {
        //Llamamos al método que le preguntara al usuario por el lugar del que desea obtener la prediccion y nos devuelve el id del lugar
        String idLugar = buscarLugar();
        //si dicho dato esta vacio
        if (idLugar == null) {
            //Avisamos al usuario
            System.out.println("No se pudo encontrar la ubicación. Inténtalo de nuevo.");
            return;
        }
        //Creamos la url para obtener las predicciones
        String ApiUrl ="https://servizos.meteogalicia.gal/apiv4/getNumericForecastInfo?locationIds=" + idLugar +"&variables=temperature,wind,sky_state,precipitation_amount,relative_humidity,cloud_area_fraction&API_KEY=4hk91p9mQV1qysT4PE1YJndSRCebJhd5E1uOf07nU1bcqiR0GN1qLy3SfkRp6f4B";
        //Se llama al método que realiza una conexion a la API de MeteoGalicia
        conexionApi(ApiUrl, json);
        //Llamamos a la clase que parsea el json que se ha obtenido de la conexion
        Parsear.parsearPredicciones(json, predicciones);
        //En caso de que el ArrayList que contiene las predicciones este vacio
        if (predicciones.isEmpty()) {
            //Avisamos al usuario
            System.out.println("No se encontraron predicciones para este lugar.");
        }
    }

    private  static String buscarLugar() throws Exception {
        //Creamos un HashMap para almacenar el id de un lugar y el nombre del mismo
        HashMap<String, String> lugares = new HashMap<>();
        //Preguntamos al usuario por el lugar
        System.out.println("Dime el nombre del lugar de dónde deseas obtener la predicción");
        //Obtenemos la respuesta
        String nombreLugar  = scL.nextLine().trim();
        //Si la respuesta esta vacia, se lanza una excepcion
        if (nombreLugar.isEmpty()){
            throw new Exception("Se ha pasado un dato vacío");
        }
        //Creamos un patrón para detectar las tildes
        Pattern pattern = Pattern.compile("[áéíóúÁÉÍÓÚ]");
        //si el nombre del lugar contiene una tilde, lanzamosuna excepcion
        if (pattern.matcher(nombreLugar).find()) {
            throw new Exception("El nombre del lugar contiene una tilde");
        }
        //creamos la url para preguntar por la id del lugar
        String urlFindPlace = "https://servizos.meteogalicia.gal/apiv4/findPlaces?location="+ nombreLugar +"&API_KEY=4hk91p9mQV1qysT4PE1YJndSRCebJhd5E1uOf07nU1bcqiR0GN1qLy3SfkRp6f4B";
        //llamamos al metodo que va a obtener esos datos
        findPlace(urlFindPlace, lugares);
        //Mostramos las distintas opciones
        System.out.println("Cuál de los siguientes lugares es?");
        String idSeleccionado = "";

        //si el HashMap no se encuentra vacío...
        if (!lugares.isEmpty()) {
            //Creamos un Map que va a almacenar la posicion y el id del lugar
            Map<Integer, String> lugaresMap = new HashMap<>();
            // Se usa un array para que pueda ser modificado dentro de la lambda
            int[] i = {1};
            //Realizamos un for each en el HashMap con los datos de los lugares
            lugares.forEach((key, value) -> {
                //Añadimos en el map las posiciones y los ids
                lugaresMap.put(i[0], key);
                //Imprimimos los datos
                System.out.println(i[0] + ". " + value);
                i[0]++;
            });
            //Le pedimos al usuario que escoja uno
            System.out.print("Seleccione un número: ");
            //Se guarda el dato
            int opcion = scN.nextInt();
            //Obtenemos el id del lugar que se corresponde a esa posicion
            idSeleccionado = lugaresMap.get(opcion);
            //lo devolvemos al método obtenerPredicciones();
            return idSeleccionado;
        }
        //Si el HashMap esta vacio, devuelve un null
        return null;
    }

    private static void findPlace(String urlFinPlace, HashMap<String,String> lugares){
        try {
            // Configuramos la URL y la conexión HTTP
            URL url = new URL(urlFinPlace);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            // Leemos la respuesta de la API utilizando un BufferedReader
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder stringBuilder = new StringBuilder();
                String linea;
                // Leemos cada línea de la respuesta y agregarla al StringBuilder
                while ((linea = reader.readLine()) != null) {
                    stringBuilder.append(linea);
                }
                //Llamamos al método que se encargará de parsear el json con los datos de los lugares que se asemejen al proporcionado por el usuario
                Parsear. parsearLugar(lugares, stringBuilder);
            }
        } catch (MalformedURLException e) {
            // Salta un mensaje en caso de URL mal formada
            System.out.println("La URL es incorrecta: " + e.getMessage());
        } catch (IOException e) {
            // Salta un mensaje en caso de excepciones de entrada/salida
            System.out.println("Error al leer o escribir el archivo: " + e.getMessage());
        }
    }

    private static void conexionApi(String ApiUrl, String json) {
        try {
            // Configuramos la URL y la conexión HTTP
            URL url = new URL(ApiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            // Leemos la respuesta de la API utilizando un BufferedReader
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder stringBuilder = new StringBuilder();
                String linea;
                // Leemos cada línea de la respuesta y agregarla al StringBuilder
                while ((linea = reader.readLine()) != null) {
                    stringBuilder.append(linea);
                }
                //Escribimos un archivo con lo que nos devuelve la API, cada vez que se llama al metodo que sobreescribe el documento
                Files.write(Paths.get(json), stringBuilder.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                //Avisamos de que se termino de crear el archivo
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
                bw.write("Lugar;Fecha;EstadoCielo;TemperaturaMax;TemperaturaMin;Precipitacion;Viento;CoberturaNubosa;Humedad");
                bw.newLine();
                List<Prediccion> prediccionesBD = BBDD.select();
                // Verificar si hay datos
                if (prediccionesBD.isEmpty()) {
                    System.out.println("No hay datos para escribir en el archivo CSV.");
                    return;
                }
                // Escribir datos de las predicciones en el archivo .csv
                for (Prediccion prediccion : prediccionesBD) {
                    String datos = prediccion.getLugar() + ";" + prediccion.getFecha() + ";" + prediccion.getEstadoCielo() + ";"
                            + prediccion.getTemperaturaMax() + ";" + prediccion.getTemperaturaMin() + ";"
                            + prediccion.getPrecipitacionTotal() + ";" + prediccion.getViento() + ";"
                            + prediccion.getCoberturaNubosa() + ";" + prediccion.getHumedad();
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

    public static void modificarDatos() throws Exception {
        //Preguntamos por la prediccion que se desea modificar
        System.out.println("Cal es la prediccion que deseas modificar?");
        int i = 1;
        //Obtenemos las predicciones que hay en la base de datos y las guardamos en un List
        List<Prediccion> prediccionesBDModif = BBDD.select();
        //Recorremos dicho List
        for(Prediccion prediccion : prediccionesBDModif){
            //Imprimimos las predicciones
            System.out.println(i + "." + prediccion);
            i++;
        }
        //Obtenemos la respuesta del usuario
        int prediccionModif =  scN.nextInt();

        String respModifBis = " ";
        do{
            //Le preguntamos al usuario que dato desea modificar
            System.out.println("Que dato deseas modificar? \n1. El nombre del lugar \n2. Fecha \n3. El estado del cielo \n4. Temperatura máxima \n5. Temperatura mínima" +
                    "\n6. Precipitación total \n7. Viento \n8. Cobertura nubosa \n9. Humedad");
            //Obtenemos la respuesta
            int respuestaModif = scN.nextInt();

            switch (respuestaModif) {
                case 1:
                    //Preguntamos por el nuevo valor del nombre del lugar
                    System.out.println("Dime el nuevo valor para el nombre del lugar");
                    //Obtenemos dicho valor
                    String nuevoNombre = scL.nextLine().trim();
                    //En caso de que los datos esten vacios
                    if (nuevoNombre.isEmpty()) {
                        //lanzamos una excepcion
                        throw new Exception("Se ha pasado un dato vacío");
                    }
                    //Modificamos el nombre de la prediccion deseada
                    prediccionesBDModif.get(prediccionModif -1).setLugar(nuevoNombre);
                    break;
                case 2:
                    //Preguntamos por el nuevo valor de la fecha
                    System.out.println("Dime el nuevo valor para la fecha");
                    //Obtenemos dicho valor
                    String nuevaFecha = scL.nextLine().trim();
                    //En caso de que los datos esten vacios
                    if (nuevaFecha.isEmpty()) {
                        //lanzamos una excepcion
                        throw new Exception("Se ha pasado un dato vacío");
                    }
                    //Modificamos la fecha de la prediccion deseada
                    prediccionesBDModif.get(prediccionModif -1).setFecha(nuevaFecha);
                    break;
                case 3:
                    //Preguntamos por el nuevo valor del estado del cielo
                    System.out.println("Dime el nuevo valor para el estado del cielo");
                    //Obtenemos dicho valor
                    String estadoCieloNuevo = scL.nextLine().trim();
                    //En caso de que los datos esten vacios
                    if (estadoCieloNuevo.isEmpty()) {
                        //lanzamos una excepcion
                        throw new Exception("Se ha pasado un dato vacío");
                    }
                    // Divide el String por comas y lo convierte en una lista
                    List<String> nuevoEstadoCielo = Arrays.asList(estadoCieloNuevo.split(","));
                    //Modificamos el estado del cielo de la prediccion deseada
                    prediccionesBDModif.get(prediccionModif -1).setEstadoCielo(nuevoEstadoCielo);
                    break;
                case 4:
                    //Preguntamos por el nuevo valor de la temperatura maxima
                    System.out.println("Dime el nuevo valor para la temperatura máxima");
                    //Obtenemos dicho valor
                    double nuevaTempMax = scN.nextDouble();
                    //Modificamos la temperatura maxima de la prediccion deseada
                    prediccionesBDModif.get(prediccionModif -1).setTemperaturaMax(nuevaTempMax);
                    break;
                case 5:
                    //Preguntamos por el nuevo valor de la temperatura minima
                    System.out.println("Dime el nuevo valor para la temperatura mínima");
                    //Obtenemos dicho valor
                    double nuevaTempMin = scN.nextDouble();
                    //Modificamos la temperatura minima de la prediccion deseada
                    prediccionesBDModif.get(prediccionModif -1).setTemperaturaMin(nuevaTempMin);
                    break;
                case 6:
                    //Preguntamos por el nuevo valor de la precipitación total
                    System.out.println("Dime el nuevo valor para la precipitación total");
                    //Obtenemos dicho valor
                    double nuevaPrecipitacionTotal = scN.nextDouble();
                    //Modificamos la precipitación total de la prediccion deseada
                    prediccionesBDModif.get(prediccionModif -1).setPrecipitacionTotal(nuevaPrecipitacionTotal);
                    break;
                case 7:
                    //Preguntamos por el nuevo valor de la velocidad del viento
                    System.out.println("Dime el nuevo valor para el viento");
                    //Obtenemos dicho valor
                    double nuevoViento = scN.nextDouble();
                    //Modificamos la precipitación total de la velocidad del viento
                    prediccionesBDModif.get(prediccionModif -1).setViento(nuevoViento);
                    break;
                case 8:
                    //Preguntamos por el nuevo valor de la cobertura nubosa
                    System.out.println("Dime el nuevo valor para la cobertura nubosa");
                    //Obtenemos dicho valor
                    double nuevaCoberturaNubosa = scN.nextDouble();
                    //Modificamos la precipitación total de la cobertura nubosa
                    prediccionesBDModif.get(prediccionModif -1).setCoberturaNubosa(nuevaCoberturaNubosa);
                    break;
                case 9:
                    //Preguntamos por el nuevo valor de la humedad
                    System.out.println("Dime el nuevo valor para la humedad");
                    //Obtenemos dicho valor
                    double nuevaHumedad = scN.nextDouble();
                    //Modificamos la precipitación total de la humedad
                    prediccionesBDModif.get(prediccionModif -1).setHumedad(nuevaHumedad);
                    break;
                default:
                    System.out.println("Opción no válida");
                    break;
            }
            //Preguntamos al usuario si desea modificar algún dato más
            System.out.println("Quieres modificar algún dato más?");
            //Obtenemos la respuesta y pasamos todo a minúsculas
            respModifBis = scL.nextLine().toLowerCase();
            //Si la respuesta es no, acaba el do while
        }while(!respModifBis.equals("no"));
        //Llamamos al método que va a modificar la prediccion y le pasamos a la susodicha
        BBDD.modificarDatos(prediccionesBDModif.get(prediccionModif -1));
    }
}