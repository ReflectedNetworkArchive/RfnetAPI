package network.reflected.rfnetapi.commands;

public class StringArg extends CommandArg {
    String argument;

    public StringArg(String originalArgument) {
        super(originalArgument);
        argument = originalArgument;
    }

    public static boolean is(String stringArg) {
        return new StringArg(stringArg).isValid();
    }

    @Override
    public boolean isValid() {
        if (PlayerArg.is(argument)) {
            return false;
        } else if (IntArg.is(argument)) {
            return false;
        } else return !DoubleArg.is(argument);
    }

    @Override
    public String get() {
        return argument;
    }
}
