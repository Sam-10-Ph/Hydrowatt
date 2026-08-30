package ga.iai.hydrowatt.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonMapper {
    private static final ObjectMapper INSTANCE = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // Sans ceci, les LocalDate sont sérialisées en tableau [année,mois,jour]
            // au lieu d'une chaîne ISO "yyyy-MM-dd" attendue par l'API Django.
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            // Omet les champs Java null du JSON envoyé, au lieu d'écrire "champ": null.
            // Django REST Framework accepte qu'un champ optionnel soit absent de la
            // requête, mais refuse qu'il soit explicitement null (ex: first_name, password).
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static ObjectMapper get() { return INSTANCE; }
}