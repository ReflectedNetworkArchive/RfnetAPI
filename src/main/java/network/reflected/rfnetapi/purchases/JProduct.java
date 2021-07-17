package network.reflected.rfnetapi.purchases;

public class JProduct {
    private final int price;
    private final String name;
    private final boolean otp;

    JProduct(int price, String name, boolean otp) {
        this.price = price;
        this.name = name;
        this.otp = otp;
    }

    public String  getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public boolean isOneTimePurchase() {
        return otp;
    }
}
