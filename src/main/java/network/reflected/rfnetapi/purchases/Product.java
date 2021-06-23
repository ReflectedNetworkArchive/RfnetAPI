package network.reflected.rfnetapi.purchases;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Product {
    @Getter private final String name;
    @Getter private final int price;
    @Getter private final boolean oneTimePurchase;
}