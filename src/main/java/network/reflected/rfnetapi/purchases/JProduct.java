package network.reflected.rfnetapi.purchases;

public class JProduct {
    private final int price;
    private final String name;
    private final boolean otp;
    private final String niceName;

    JProduct(int price, String name, boolean otp, String niceName) {
        this.price = price;
        this.name = name;
        this.otp = otp;
        this.niceName = niceName;
    }

    public String  getName() { return name; }

    public String getNiceName() { return niceName; }

    public int getPrice() { return price; }

    public boolean isOneTimePurchase() { return otp; }
}
