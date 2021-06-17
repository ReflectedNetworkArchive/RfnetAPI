package network.reflected.rfnetapi.commands;

public class DoubleArg extends CommandArg{
    double argument;
    boolean isValid = false;

    public DoubleArg(String originalArgument) {
        super(originalArgument);
        try {
            argument = Double.parseDouble(originalArgument);
            isValid = true;
        } catch (NumberFormatException e) {
            isValid = false;
        }
    }

    public static boolean is(String doubleArg) {
        return new DoubleArg(doubleArg).isValid();
    }

    public boolean isValid() {
        return isValid;
    }

    public Double get() {
        return argument;
    }
}
