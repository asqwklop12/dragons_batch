package com.dto;

import com.dto.MarketPriceDailyResponse.MarketPriceDailyItem;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class MarketPriceDailyItemDeserializer extends JsonDeserializer<MarketPriceDailyItem> {

  private final FlexibleStringValueDeserializer flexibleStringValueDeserializer = new FlexibleStringValueDeserializer();

  @Override
  public MarketPriceDailyItem deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    JsonNode node = parser.getCodec().readTree(parser);

    return new MarketPriceDailyItem(
        readString(node, "productno", parser, context),
        readString(node, "item_name", parser, context),
        readString(node, "product_cls_code", parser, context),
        readString(node, "product_cls_name", parser, context),
        readString(node, "category_code", parser, context),
        readString(node, "category_name", parser, context),
        readString(node, "productName", parser, context),
        readString(node, "unit", parser, context),
        readString(node, "day1", parser, context),
        readString(node, "dpr1", parser, context),
        readString(node, "day2", parser, context),
        readString(node, "dpr2", parser, context),
        readString(node, "day3", parser, context),
        readString(node, "dpr3", parser, context),
        readString(node, "day4", parser, context),
        readString(node, "dpr4", parser, context),
        readString(node, "direction", parser, context),
        readString(node, "value", parser, context)
    );
  }

  private String readString(JsonNode node, String fieldName, JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode field = node.get(fieldName);
    if (field == null || field.isNull()) {
      return null;
    }

    JsonParser fieldParser = field.traverse(parser.getCodec());
    fieldParser.nextToken();
    return flexibleStringValueDeserializer.deserialize(fieldParser, context);
  }
}
