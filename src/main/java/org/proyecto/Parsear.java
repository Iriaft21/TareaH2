package org.proyecto;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Parsear {

    public static void parsearLugar(HashMap<String, String> lugares, StringBuilder stringBuilder) {
        try {
            // Parseamos el contenido del StringBuilder a un objeto JSON
            JSONObject jsonResponse = (JSONObject) new JSONParser().parse(stringBuilder.toString());
            // Extraemos el array de features del JSON
            JSONArray features = (JSONArray) jsonResponse.get("features");
            // Verificamos si hay elementos en features
            if (features.size() > 0) {
                // Iteramos sobre cada feature
                for (Object obj : features) {
                    JSONObject feature = (JSONObject) obj;
                    // Extraemos las propiedades de cada feature
                    JSONObject properties = (JSONObject) feature.get("properties");

                    String id = (String) properties.get("id");
                    String nombre = (String) properties.get("name");
                    String municipio = (String) properties.get("municipality");
                    String provincia = (String) properties.get("province");
                    // Construimos la información del lugar
                    String infoLugar = "Nombre: " + nombre + " | Municipio: " + municipio + " | Provincia: " + provincia;
                    // Guardamos la información del lugar en el HashMap
                    lugares.put(id, infoLugar);
                }
            } else {
                System.out.println("No se encontraron resultados.");
            }
        } catch (ParseException e) {
            // En caso de fallo al parsear JSON, lanzamos una excepcion
            throw new RuntimeException("Error al parsear JSON: " + e.getMessage(), e);
        }
    }

    public static void parsearPredicciones(String jsonPath, List<Prediccion> predicciones) {
        try {
            // Leemos y parseamos el archivo JSON
            JSONObject objeto = (JSONObject) new JSONParser().parse(new FileReader(jsonPath));
            // Comprobamos si el JSON contiene la clave "features"
            if (!objeto.containsKey("features")) {
                //Avisamos en caso de que el Json no contenga "features"
                System.out.println("Error: El JSON no contiene la clave 'features'.");
                return;
            }
            // Extraemos el arreglo de features del JSON
            JSONArray features = (JSONArray) objeto.get("features");
            // Verificamos si la lista de features es nula o vacía
            if (features == null || features.isEmpty()) {
                System.out.println("Error: No se encontraron datos en 'features'.");
                return;
            }
            // Iteramos sobre cada feature
            for (Object featureObj : features) {
                JSONObject feature = (JSONObject) featureObj;
                // Extraemos las propiedades de cada feature
                JSONObject propiedades = (JSONObject) feature.get("properties");
                String lugar = (String) propiedades.get("name");
                // Extraemos la lista de días
                JSONArray dias = (JSONArray) propiedades.get("days");
                // Iteramos sobre cada día
                for (Object dayObj : dias) {
                    JSONObject dia = (JSONObject) dayObj;
                    // Extraemos el periodo de tiempo
                    JSONObject periodoTiempo = (JSONObject) dia.get("timePeriod");
                    String fechaCompleta = (String) ((JSONObject) periodoTiempo.get("begin")).get("timeInstant");
                    String fechaDia = fechaCompleta.split("T")[0];
                    // Extraemos la predicción
                    Prediccion prediccion = extraerPrediccion(dia, lugar, fechaDia);
                    //Añadimos la prediccion al ArrayList de predicciones
                    predicciones.add(prediccion);
                    // Insertamos los datos en la base de datos
                    BBDD.insertarDatos(prediccion);
                }
            }
        } catch (IOException | ParseException e) {
            // Avisamos al usuario en caso de fallo al leer o parsear el archivo JSON
            System.out.println("Error al leer o parsear el archivo JSON: " + e.getMessage());
        }
    }

    private static Prediccion extraerPrediccion(JSONObject dia, String lugar, String fechaDia) {
        double temperaturaMaxima = 0, temperaturaMinima = Double.MAX_VALUE;
        double viento = 0, precipitacion = 0, coberturaNubosa = 0, humedad = 0;
        List<String> cielo = new ArrayList<>();
        // Extraemos la lista de variables del día
        JSONArray variables = (JSONArray) dia.get("variables");
        // Iteramos sobre cada variable
        for (Object variableObj : variables) {
            JSONObject variable = (JSONObject) variableObj;
            String nombreVariable = (String) variable.get("name");
            JSONArray valores = (JSONArray) variable.get("values");
            // Procesamos cada variable según su nombre
            switch (nombreVariable) {
                case "temperature":
                    // Procesamos las temperaturas para encontrar la máxima y mínima del día
                    for (Object valorObj : valores) {
                        // Extraemos el valor de la temperatura y lo convertimos a doble
                        double temp = ((Number) ((JSONObject) valorObj).get("value")).doubleValue();
                        // Actualizamos la temperatura máxima si el valor actual es mayor
                        temperaturaMaxima = Math.max(temperaturaMaxima, temp);
                        // Actualizamos la temperatura mínima si el valor actual es menor
                        temperaturaMinima = Math.min(temperaturaMinima, temp);
                    }
                    break;
                case "wind":
                    // Sumamos los valores del viento para calcular el total del día
                    for (Object valorObj : valores) {
                        // Extraemos el valor del viento y lo convertimos a doble
                        viento += ((Number) ((JSONObject) valorObj).get("moduleValue")).doubleValue();
                    }
                    break;
                case "precipitation_amount":
                    // Sumamos los valores de la precipitación para calcular el total del día
                    for (Object valorObj : valores) {
                        // Extraemos el valor de la precipitación y lo convertimos a doble
                        precipitacion += ((Number) ((JSONObject) valorObj).get("value")).doubleValue();
                    }
                    break;
                case "cloud_area_fraction":
                    // Sumamos los valores de la cobertura nubosa para calcular el total del día
                    for (Object valorObj : valores) {
                        // Extraemos el valor de la cobertura nubosa y lo convertimos a doble
                        coberturaNubosa += ((Number) ((JSONObject) valorObj).get("value")).doubleValue();
                    }
                    break;
                case "relative_humidity":
                    // Sumamos los valores de la humedad relativa para calcular el total del día
                    for (Object valorObj : valores) {
                        // Extraemos el valor de la humedad y lo convertimos a doble
                        humedad += ((Number) ((JSONObject) valorObj).get("value")).doubleValue();
                    }
                    break;
                case "sky_state":
                    // Procesamos los estados del cielo y los traducimos
                    for (Object valorObj : valores) {
                        // Extraemos el valor del estado del cielo y lo traducimos
                        String estadoCielo = traducirEstadoCielo((String) ((JSONObject) valorObj).get("value"));
                        if (!cielo.contains(estadoCielo)) {
                            // Añadimos el estado del cielo a la lista si no está ya presente
                            cielo.add(estadoCielo);
                        }
                    }
                    break;
            }
        }

        int horas = ((JSONArray) ((JSONObject) variables.get(0)).get("values")).size();
        // Calculamos promedios para viento, cobertura nubosa y humedad
        viento = roundDouble(viento / horas, 2);
        coberturaNubosa = roundDouble(coberturaNubosa / horas, 2);
        humedad = roundDouble(humedad / horas, 2);
        // Creamos y devolvemos una instancia de Prediccion
        return new Prediccion(lugar, fechaDia, cielo, temperaturaMaxima, temperaturaMinima, precipitacion, viento, coberturaNubosa, humedad);
    }

    private static String traducirEstadoCielo(String estado) {
        // Traducimos el estado del cielo del inglés al español
        switch (estado) {
            case "SUNNY": return "Soleado";
            case "HIGH_CLOUDS": return "Nubes altas";
            case "PARTLY_CLOUDY": return "Parcialmente nuboso";
            case "OVERCAST": return "Nublado";
            case "CLOUDY": return "Nuboso";
            case "FOG": return "Niebla";
            case "SHOWERS": return "Chubascos";
            case "OVERCAST_AND_SHOWERS": return "Nublado con chubascos";
            case "DRIZZLE": return "Llovizna";
            case "RAIN": return "Lluvia";
            case "SNOW": return "Nieve";
            case "STORMS": return "Tormentas";
            case "MIST": return "Neblina";
            case "FOG_BANK": return "Banco de niebla";
            case "MID_CLOUDS": return "Nubes medias";
            case "WEAK_RAIN": return "Lluvia débil";
            case "WEAK_SHOWERS": return "Chubascos débiles";
            case "STORM_THEN_CLOUDY": return "Tormenta y luego nuboso";
            case "MELTED_SNOW": return "Nieve derretida";
            case "RAIN_HayL": return "Granizo";
            default: return estado;
        }
    }

    private static double roundDouble(double value, int numDecimals) {
        // Redondeamos un valor doble a un número específico de decimales
        return new BigDecimal("" + value).setScale(numDecimals, RoundingMode.HALF_UP).doubleValue();
    }
}