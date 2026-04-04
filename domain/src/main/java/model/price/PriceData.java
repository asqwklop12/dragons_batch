package model.price;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "price_data")
public class PriceData {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "item_code", nullable = false, length = 20)
  private String itemCode;

  @Column(name = "item_name", nullable = false, length = 100)
  private String itemName;

  @Column(name = "kind_code", nullable = false, length = 20)
  private String kindCode;

  @Column(name = "kind_name", nullable = false, length = 100)
  private String kindName;

  @Column(name = "market_code", nullable = false, length = 20)
  private String marketCode;

  @Column(name = "market_name", nullable = false, length = 100)
  private String marketName;

  @Column(name = "rank_code", nullable = false, length = 20)
  private String rankCode;

  @Column(name = "rank_name", nullable = false, length = 100)
  private String rankName;

  @Column(nullable = false)
  private int price;

  @Column(nullable = false, length = 20)
  private String unit;

  @Column(name = "reg_day", nullable = false)
  private LocalDate regDay;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;


  public static PriceData create(
      String itemCode,
      String itemName,
      String kindCode,
      String kindName,
      String marketCode,
      String marketName,
      String rankCode,
      String rankName,
      int price,
      String unit,
      LocalDate regDay,
      LocalDateTime createdAt
  ) {
    return new PriceData(
        null,
        itemCode,
        itemName,
        kindCode,
        kindName,
        marketCode,
        marketName,
        rankCode,
        rankName,
        price,
        unit,
        regDay,
        createdAt
    );
  }

  public boolean matchesRegDay(String targetRegDay) {
    return regDay.toString().equals(targetRegDay);
  }

  public boolean containsItemName(String keyword) {
    return itemName.toLowerCase().contains(keyword.toLowerCase());
  }
}
