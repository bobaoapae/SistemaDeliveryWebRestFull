/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package utils;

import adapters.*;
import com.google.gson.GsonBuilder;
import info.debatty.java.stringsimilarity.JaroWinkler;
import restFul.controle.ControleSistema;
import sistemaDelivery.modelo.HorarioFuncionamento;
import sistemaDelivery.modelo.ItemPedido;
import sistemaDelivery.modelo.Produto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.logging.Level;

/**
 * @author BOV-INOS
 */
public class Utilitarios {

    public static GsonBuilder getDefaultGsonBuilder(Type type) {
        GsonBuilder builder = new GsonBuilder().disableHtmlEscaping().
                registerTypeAdapter(java.sql.Date.class, new DateAdapterSerialize()).
                registerTypeAdapter(java.sql.Date.class, new DateAdapterDeserialize()).
                registerTypeAdapter(Timestamp.class, new TimestampAdapterSerialize()).
                registerTypeAdapter(Timestamp.class, new TimestampAdapterDeserialize()).
                registerTypeAdapter(Time.class, new TimeAdapter()).
                registerTypeAdapter(Time.class, new TimeAdapterDeserialize());
        HashMap<Type, Object> adapters = new HashMap<>();
        adapters.put(ItemPedido.class, new UseGetterAdapterSerialize<>());
        adapters.put(Produto.class, new UseGetterAdapterSerialize<>());
        adapters.put(HorarioFuncionamento.class, new UseGetterAdapterSerialize<>());
        for (Map.Entry<Type, Object> entry : adapters.entrySet()) {
            if (entry.getKey().equals(type)) {
                continue;
            }
            builder.registerTypeAdapter(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    public static void atualizarObjeto(Object x, Object y) {
        ArrayList<Field[]> listaFields = new ArrayList<>();
        Class cla = x.getClass();
        int nrHeranca = 0;
        while (Object.class != cla) {
            nrHeranca++;
            cla = cla.getSuperclass();
        }
        cla = x.getClass();
        for (int xx = 0; xx <= nrHeranca; xx++) {
            if (xx == 0) {
                Field[] f = cla.getDeclaredFields();
                listaFields.add(f);
            } else {
                Field[] f = cla.getDeclaredFields();
                listaFields.add(f);
            }
            cla = cla.getSuperclass();
        }
        cla = x.getClass();
        try {
            for (Field[] listaField : listaFields) {
                for (Field listaField1 : listaField) {
                    if (listaField1.isAnnotationPresent(Ignorar.class)) {
                        continue;
                    }
                    Method metodoGet;
                    if (listaField1.getType().toString().equals("boolean")) {
                        metodoGet = cla.getMethod("is" + primeiraMaiuscula(listaField1.getName()));
                    } else {
                        metodoGet = cla.getMethod("get" + primeiraMaiuscula(listaField1.getName()));
                    }
                    Method metodoSet = cla.getMethod("set" + primeiraMaiuscula(listaField1.getName()), (Class<?>) listaField1.getType());
                    if (metodoGet.invoke(y) == null) {
                        metodoSet.invoke(x, new Object[]{null});
                        continue;
                    }
                    if (listaField1.getType().isPrimitive() || listaField1.getType().isEnum() || listaField1.getType().isArray() || listaField1.getType().equals(String.class) || listaField1.getType().equals(Date.class) || listaField1.getType().equals(Time.class) || listaField1.getType().equals(java.sql.Date.class) || listaField1.getType().equals(Timestamp.class)) {
                        metodoSet.invoke(x, metodoGet.invoke(y));
                    } else {
                        if (metodoGet.invoke(x) == null) {
                            metodoSet.invoke(x, metodoGet.invoke(y));
                            continue;
                        }
                        atualizarObjeto(metodoGet.invoke(x), metodoGet.invoke(y));
                    }
                }
                cla = cla.getSuperclass();
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private static String primeiraMaiuscula(String palavra) {
        return palavra.replaceFirst(palavra.substring(0, 1), palavra.substring(0, 1).toUpperCase());
    }

    public static String fileToBase64(File file) throws IOException {
        String contentType = Files.probeContentType(file.toPath());

        // read data as byte[]
        byte[] data = Files.readAllBytes(file.toPath());

        // convert byte[] to base64(java7)
        //String base64str = DatatypeConverter.printBase64Binary(data);
        // convert byte[] to base64(java8)
        String base64str = Base64.getEncoder().encodeToString(data);

        // cretate "data URI"
        StringBuilder sb = new StringBuilder();
        sb.append("data:");
        sb.append(contentType);
        sb.append(";base64,");
        sb.append(base64str);
        return sb.toString();
    }

    public static String generate(int keyLen) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keyLen);
        SecretKey secretKey = keyGen.generateKey();
        byte[] encoded = secretKey.getEncoded();
        return DatatypeConverter.printHexBinary(encoded).toLowerCase();
    }

    public static String getText(URL website) throws Exception {
        URLConnection connection = website.openConnection();
        connection.addRequestProperty("User-Agent",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        return response.toString();
    }

    public static String getMonth(int mes) {
        return Month.of(mes).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
    }

    public static String getDayOfWeekName(int diaSemana) {
        return DayOfWeek.of(diaSemana).getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("pt-BR"));
    }

    public static String retornarApenasNumeros(String input) {
        return input.replaceAll("[^0-9]", "");
    }

    public static String retornarApenasLetras(String input) {
        return removerAcentos(input).replaceAll("[^a-zA-Z]", "");
    }

    public static String convertToString(Date d, String formato) {
        SimpleDateFormat formatador = new SimpleDateFormat(formato);
        return formatador.format(d);
    }

    public static String removerAcentos(String str) {
        return str.replaceAll("[ãâàáä]", "a").replaceAll("[êèéë]", "e").replaceAll("[îìíï]", "i").replaceAll("[õôòóö]", "o").replaceAll("[ûúùü]", "u").replaceAll("[ÃÂÀÁÄ]", "A").replaceAll("[ÊÈÉË]", "E").replaceAll("[ÎÌÍÏ]", "I").replaceAll("[ÕÔÒÓÖ]", "O").replaceAll("[ÛÙÚÜ]", "U").replace('ç', 'c').replace('Ç', 'C').replace('ñ', 'n').replace('Ñ', 'N').replaceAll("!", "").replaceAll("\\[\\´\\`\\?!\\@\\#\\$\\%\\¨\\*", " ").replaceAll("\\(\\)\\=\\{\\}\\[\\]\\~\\^\\]", " ").replaceAll("[\\;\\-\\_\\+\\'\\ª\\º\\:\\;\\/]", " ").replaceAll("§", " ");
    }

    public static boolean verificarSeSaoStringParecidas(String a, String b) {
        JaroWinkler jw = new JaroWinkler();
        double igualdade = jw.similarity(retornarApenasLetras(a).toUpperCase(), retornarApenasLetras(b).toUpperCase());
        return igualdade >= 0.9d;
    }

    public static boolean verificarFrasePossuiPalavraIgualOuParecida(String frase, String palavra) {
        String[] palavras = frase.split("\\s");
        for (String palavraAtual : palavras) {
            if (retornarApenasLetras(palavraAtual).equalsIgnoreCase(retornarApenasLetras(palavra)) || verificarSeSaoStringParecidas(palavraAtual.trim(), palavra.trim())) {
                return true;
            }
        }
        if (retornarApenasLetras(frase).equalsIgnoreCase(retornarApenasLetras(palavra)) || verificarSeSaoStringParecidas(frase, palavra)) {
            return true;
        }
        return false;
    }

    public static String corrigirStringComBaseEmListaDeStringsValidas(List<String> stringsValidas, String corrigir) {
        JaroWinkler jw = new JaroWinkler();
        double maiorIgualdade = 0d;
        String stringValida = "";
        for (String string : stringsValidas) {
            String[] palavras = corrigir.trim().split("\\s");
            double igualdade = 0d;
            for (String palavraAtual : palavras) {
                double igualdadeAtual = 0d;
                String[] palavrasValidas = string.trim().split("\\s");
                int totalValidas = 0;
                for (String pa : palavrasValidas) {
                    double igualdadeTeste = jw.similarity(retornarApenasLetras(pa.toUpperCase()), retornarApenasLetras(palavraAtual.toUpperCase()));
                    if (igualdadeTeste > 0.7) {
                        igualdadeAtual += igualdadeTeste;
                        totalValidas++;
                    }
                }
                if (totalValidas > 0) {
                    igualdadeAtual /= totalValidas;
                    igualdade += igualdadeAtual;
                }
            }
            igualdade /= palavras.length;
            if (igualdade > maiorIgualdade) {
                maiorIgualdade = igualdade;
                stringValida = string;
            }
        }
        for (String string : stringsValidas) {
            double igualdade = jw.similarity(retornarApenasLetras(string.toUpperCase()), retornarApenasLetras(corrigir.toUpperCase()));
            if (igualdade > 0.7d && igualdade > maiorIgualdade) {
                maiorIgualdade = igualdade;
                stringValida = string;
            }
        }
        return stringValida;
    }

}
