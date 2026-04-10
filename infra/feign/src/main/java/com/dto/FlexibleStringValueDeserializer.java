package com.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Iterator;

public class FlexibleStringValueDeserializer extends JsonDeserializer<String> {

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    JsonToken token = parser.currentToken();

    if (token == JsonToken.VALUE_STRING) {
      return parser.getValueAsString();
    }
    if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT
        || token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
      return parser.getValueAsString();
    }
    if (token == JsonToken.START_ARRAY) {
      JsonNode node = parser.readValueAsTree();
      return flattenArray(node);
    }
    if (token == JsonToken.VALUE_NULL) {
      return null;
    }

    JsonNode node = parser.readValueAsTree();
    return node == null || node.isNull() ? null : node.asText();
  }

  private String flattenArray(JsonNode arrayNode) {
    if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
      return null;
    }

    StringBuilder builder = new StringBuilder();
    Iterator<JsonNode> iterator = arrayNode.elements();
    while (iterator.hasNext()) {
      JsonNode element = iterator.next();
      if (element == null || element.isNull()) {
        continue;
      }

      String value = element.isValueNode() ? element.asText() : element.toString();
      if (value == null || value.isBlank()) {
        continue;
      }

      if (!builder.isEmpty()) {
        builder.append(",");
      }
      builder.append(value);
    }

    return builder.isEmpty() ? null : builder.toString();
  }
}
