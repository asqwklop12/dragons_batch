package config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

  @Primary
  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // Java Time Module 등록
    mapper.registerModule(new JavaTimeModule());

    // Serialization 설정
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Deserialization 설정
    mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    mapper.disable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
    mapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    return mapper;
  }
}
