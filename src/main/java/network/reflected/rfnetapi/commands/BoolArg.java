package network.reflected.rfnetapi.commands;

public class BoolArg extends CommandArg {
    boolean argument;
    boolean isValid = false;

    public BoolArg(String originalArgument) {
        super(originalArgument);
        if (originalArgument.equalsIgnoreCase("false")) {
            argument = false;
            isValid = true;
        } else if (originalArgument.equalsIgnoreCase("true")) {
            argument = true;
            isValid = true;
        }
    }

    public static boolean is(String boolArg) {
        return new BoolArg(boolArg).isValid();
    }

    public boolean isValid() {
        return isValid;
    }

    public Boolean get() {
        return argument;
    }
}
