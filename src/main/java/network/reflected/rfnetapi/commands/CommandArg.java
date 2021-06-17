package network.reflected.rfnetapi.commands;

import lombok.*;

@RequiredArgsConstructor
public abstract class CommandArg {
    public static CommandArg[] parse(String[] args) {
        CommandArg[] commandArgs = new CommandArg[args.length];
        for (int i = 0; i < args.length; i++) {
            commandArgs[i] = parse(args[i]);
        }
        return commandArgs;
    }

    public static CommandArg parse(String arg) {
        if (PlayerArg.is(arg)) {
            return new PlayerArg(arg);
        } else if (IntArg.is(arg)) {
            return new IntArg(arg);
        } else if (DoubleArg.is(arg)) {
            return new DoubleArg(arg);
        } else {
            return new StringArg(arg);
        }
    }

    private final String originalArgument;

    abstract public boolean isValid();
    abstract public Object get();
}
