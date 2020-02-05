package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the version-control system.
 *  @author Kelvin Pang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            if (args == null || args.length == 0) {
                throw Utils.error("Please enter a command.");
            }
            Command com = new Command();
            switch (args[0]) {
            case "init":
                com.init();
                break;
            case "add":
                if (args.length != 2) {
                    throw Utils.error("Invalid length.");
                }
                com.add(args[1]);
                break;
            case "commit":
                if (args.length != 2) {
                    throw Utils.error("Invalid length.");
                }
                com.commit(args[1]);
                break;
            case "log":
                com.log();
                break;
            case "checkout":
                if (args.length == 3 && args[1].equals("--")) {
                    com.checkout(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    com.checkout2(args[1], args[3]);
                } else if (args.length == 2) {
                    com.checkout3(args[1]);
                } else {
                    throw Utils.error("Incorrect operands.");
                }
                break;
            case "global-log":
                com.globallog();
                break;
            case "rm":
                if (args.length != 2) {
                    throw Utils.error("Invalid length.");
                }
                com.remove(args[1]);
                break;
            case "find":
                if (args.length != 2) {
                    throw Utils.error("Invalid length.");
                }
                com.find(args[1]);
                break;
            default: main2(args);
            }
        } catch (GitletException | IOException e) {
            Utils.message(e.getMessage());
        }
    }

    /** Extended main class.
     * @param args arguments.
     * */
    public static void main2(String... args) {
        try {
            if (args == null || args.length == 0) {
                throw Utils.error("Please enter a command.");
            }
            Command com = new Command();
            switch (args[0]) {
            case "branch":
                if (args.length != 2) {
                    throw Utils.error("Invalid length.");
                }
                com.branch(args[1]);
                break;
            case "rm-branch":
                if (args.length != 2) {
                    throw Utils.error("Invalid length.");
                }
                com.rmbranch(args[1]);
                break;
            case "reset":
                if (args.length != 2) {
                    throw Utils.error("Invalid length.");
                }
                com.reset(args[1]);
                break;
            case "status":
                com.status();
                break;
            case "merge":
                if (args.length != 2) {
                    throw Utils.error("Invalid length.");
                }
                com.merge(args[1]);
                break;
            default: break;
            }
        } catch (GitletException | IOException e) {
            Utils.message(e.getMessage());
        }
    }
}
