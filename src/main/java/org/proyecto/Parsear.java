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
            JSONObject jsonResponse = (JSONObject) new JSONParser().parse(stringBuilder.toString());
            JSONArray features = (JSONArray) jsonResponse.get("features");

            if (features.size() > 0) {
                for (Object obj : features) {
                    JSONObject feature = (JSONObject) obj;
                    JSONObject properties = (JSONObject) feature.get("properties");

                    String id = (String) properties.get("id");
                    String nombre = (String) properties.get("name");
                    String municipio = (String) properties.get("municipality");
                    String provincia = (String) properties.get("province");

                    String infoLugar = "Nombre: " + nombre + " | Municipio: " + municipio + " | Provincia: " + provincia;
                    lugares.put(id, infoLugar);
                }
            } else {
                System.out.println("No se encontraron resultados.");
            }
        } catch (ParseException e) {
            throw new RuntimeException("Error al parsear JSON: " + e.getMessage(), e);
        }
    }

    public static void parsearPredicciones(String jsonPath, List<Prediccion> predicciones) {
        try {
            JSONObject objeto = (JSONObject) new JSONParser().parse(new FileReader(jsonPath));
            // Comprobar si el JSON contiene la clave "features"
            if (!objeto.containsKey("features")) {
                System.out.println("Error: El JSON no contiene la clave 'features'.");
                return;
            }

            JSONArray features = (JSONArray) objeto.get("features");
            // Verificar si la lista de features es nula o vacía
            if (features == null || features.isEmpty()) {
                System.out.println("Error: No se encontraron datos en 'features'.");
                return;
            }

            for (Object featureObj : features) {
                JSONObject feature = (JSONObject) featureObj;
                JSONObject propiedades = (JSONObject) feature.get("properties");
                String lugar = (String) propiedades.get("name");
                JSONArray dias = (JSONArray) propiedades.get("days");

                for (Object dayObj : dias) {
                    JSONObject day = (JSONObject) dayObj;
                    JSONObject timePeriod = (JSONObject) day.get("timePeriod");
                    String fechaCompleta = (String) ((JSONObject) timePeriod.get("begin")).get("timeInstant");
                    String dia = fechaCompleta.split("T")[0];

                    Prediccion prediccion = extraerPrediccion(day, lugar, dia);
                    predicciones.add(prediccion);
                    BBDD.insertarDatos(prediccion);
                }
            }
        } catch (IOException | ParseException e) {
            System.out.println("Error al leer o parsear el archivo JSON: " + e.getMessage());
        }
    }

    private static Prediccion extraerPrediccion(JSONObject day, String lugar, String dia) {
        double temperaturaMaxima = 0, temperaturaMinima = Double.MAX_VALUE;
        double viento = 0, precipitacion = 0, coberturaNubosa = 0, humedad = 0;
        List<String> cielo = new ArrayList<>();

        JSONArray variables = (JSONArray) day.get("variables");
        for (Object variableObj : variables) {
            JSONObject variable = (JSONObject) variableObj;
            String nombreVariable = (String) variable.get("name");
            JSONArray valores = (JSONArray) variable.get("values");

            switch (nombreVariable) {
                case "temperature":
                    for (Object valorObj : valores) {
                        double temp = ((Number) ((JSONObject) valorObj).get("value")).doubleValue();
                        temperaturaMaxima = Math.max(temperaturaMaxima, temp);
                        temperaturaMinima = Math.min(temperaturaMinima, temp);
                    }
                    break;
                case "wind":
                    for (Object valorObj : valores) {
                        viento += ((Number) ((JSONObject) valorObj).get("moduleValue")).doubleValue();
                    }
                    break;
                case "precipitation_amount":
                    for (Object valorObj : valores) {
                        precipitacion += ((Number) ((JSONObject) valorObj).get("value")).doubleValue();
                    }
                    break;
                case "cloud_area_fraction":
                    for (Object valorObj : valores) {
                        coberturaNubosa += ((Number) ((JSONObject) valorObj).get("value")).doubleValue();
                    }
                    break;
                case "relative_humidity":
                    for (Object valorObj : valores) {
                        humedad += ((Number) ((JSONObject) valorObj).get("value")).doubleValue();
                    }
                    break;
                case "sky_state":
                    for (Object valorObj : valores) {
                        String estadoCielo = traducirEstadoCielo((String) ((JSONObject) valorObj).get("value"));
                        if (!cielo.contains(estadoCielo)) {
                            cielo.add(estadoCielo);
                        }
                    }
                    break;
            }
        }

        int horas = ((JSONArray) ((JSONObject) variables.get(0)).get("values")).size();
        viento = roundDouble(viento / horas, 2);
        coberturaNubosa = roundDouble(coberturaNubosa / horas, 2);
        humedad = roundDouble(humedad / horas, 2);

        return new Prediccion(lugar, dia, cielo, temperaturaMaxima, temperaturaMinima, precipitacion, viento, coberturaNubosa, humedad);
    }

    private static String traducirEstadoCielo(String estado) {
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
            default: return estado;
        }
    }

    private static double roundDouble(double value, int numDecimals) {
        return new BigDecimal("" + value).setScale(numDecimals, RoundingMode.HALF_UP).doubleValue();
    }
}