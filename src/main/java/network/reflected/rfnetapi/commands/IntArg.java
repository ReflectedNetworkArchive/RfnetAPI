package network.reflected.rfnetapi.commands;

public class IntArg extends CommandArg {
    int argument;
    boolean isValid = false;

    public IntArg(String originalArgument) {
        super(originalArgument);
        if (!originalArgument.contains(".")) {
            try {
                argument = Integer.parseInt(originalArgument);
                isValid = true;
            } catch (NumberFormatException e) {
                isValid = false;
            }
        }
    }

    public static boolean is(String intArg) {
        return new IntArg(intArg).isValid();
    }

    public boolean isValid() {
        return isValid;
    }

    public Integer get() {
        return argument;
    }
}
