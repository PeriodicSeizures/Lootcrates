package com.crazicrafter1.lootcrates.cmd;

import com.crazicrafter1.crutils.*;
import com.crazicrafter1.lootcrates.*;
import com.crazicrafter1.lootcrates.LCMain;
import com.crazicrafter1.lootcrates.crate.CrateInstance;
import com.crazicrafter1.lootcrates.crate.CrateSettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CmdArg {

    private static final LCMain plugin = LCMain.get();

    private static void arg(@Nonnull String arg, @Nonnull TriFunction<CommandSender, String[], Set<String>, Boolean> executor) {
        arg(arg, executor, null);
    }

    private static void arg(@Nonnull String arg, @Nonnull TriFunction<CommandSender, String[], Set<String>, Boolean> executor,
                            @Nullable BiFunction<CommandSender, String[], List<String>> tabCompleter) {
        args.put(arg, new Pair<>(executor, tabCompleter));
    }

    /**
     * Command executor and tab-completer map
     */
    static Map<String,
            Pair<TriFunction<CommandSender, String[], Set<String>, Boolean>,
                    BiFunction<CommandSender, String[], List<String>>>> args = new HashMap<>();

    static {
        if (plugin.debug) {
            arg("class", (sender, args, flags) -> {
                try {
                    Class<?> clazz = Class.forName(args[0]);
                    try {
                        plugin.notifier.info("Package: " + clazz.getPackage().getName());
                    } catch (Exception e) {
                        plugin.notifier.info("In default package?");
                    }
                    return info(sender, "Found: " + clazz.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                    return severe(sender, e.getMessage());
                }
            });

            arg("opened", (sender, args, flags) -> {
                return info(sender, "Open crates: " + String.join(", ",
                        CrateInstance.CRATES.values().stream().map(e -> e.getPlayer().getName()).toArray(String[]::new)
                ));
                //return info(sender, "Updated");
            });

            arg("method", (sender, args, flags) -> {
                try {
                    Class<?> clazz = Class.forName(args[0]);
                    try {
                        plugin.notifier.info("Package: " + clazz.getPackage().getName());
                    } catch (Exception e) {
                        plugin.notifier.info("In default package?");
                    }

                    plugin.notifier.info("Methods:");
                    for (Method method : clazz.getMethods()) {
                        if (method.getName().startsWith(args[1]))
                            plugin.notifier.info(" - " + method.getName());
                    }

                    return info(sender, "Found: " + clazz.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                    return severe(sender, e.getMessage());
                }
            });

            arg("throw", (sender, args, flags) -> {
                throw new RuntimeException("Test exception");
            });
        }

        arg("colors", (sender, args, flags) -> {
            if (args.length == 0)
                return severe(sender, "Usage: crates colors <#883388>Inertia is a property of matter</#1144FF>");

            return info(sender, ColorUtil.renderAll(String.join(" ", args)));
        }, (sender, args) -> {

            String s = String.join(" ", args);

            MutableString m = new MutableString(s);

            int lastOpen = m.lastIndexOf('<');
            if (lastOpen != -1) {
                MutableString color = MutableString.mutable(m).subRight('<', lastOpen);

                // #RRGGBB
                // /#RRGGBB
                if (!color.startsWith('#')) {

                    // GR
                    int lastClose = color.indexOf('>');
                    if (lastClose == -1) {

                        int slashIndex = m.indexOf('/', lastOpen);

                        // determine whether this is contained within a closing gradient (either implicitly or explicitly)
                        // closing bracket detection is difficult
                        // without scanning the entire string prior for

                        // then get matches for the unclosed
                        return new ArrayList<>();
                    }
                }
            }

            return new ArrayList<>();
        });

        arg("crate", (sender, args, flags) -> {
            CrateSettings crate = Lootcrates.getCrate(args[0]);

            // the best way to represent this command structure would be with a treemap
            // crates -> display plugin info
            //  - crate
            //      - <crate>

            // STATIC and WILDCARD are more like specifiers
            //  STATIC is an expected arg among predetermined args
            //  WILDCARD is an unpredictable arg

            // flags:
            // flags could take a class type to construct
            //  - NUMBER_OR_STRING
            //  - NUMBER strict
            //  - STRING
            //  - PLAYER (online)
            //  - ANY_PLAYER (online/offline)

            if (crate == null)
                return severe(sender, Lang.COMMAND_ERROR_CRATE);

            // /crates crate common
            if (args.length == 1) {
                if (!(sender instanceof Player))
                    return severe(sender, Lang.CRATE_ERROR_PLAYER0);
                Util.give((Player) sender, crate.itemStack((Player) sender));
                return info(sender, String.format(Lang.MESSAGE_RECEIVE_CRATE, crate.id));
            }

            // crates crate common *
            if (args[1].equals("*")) {
                int given = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Util.give(p, crate.itemStack(p));
                    if (p != sender && !(flags.contains("s") || flags.contains("silent"))) {
                        info(p, String.format(Lang.MESSAGE_RECEIVE_CRATE, crate.id));
                        given++;
                    }
                }

                if (given == 0)
                    return severe(sender, Lang.COMMAND_ERROR_PLAYERS);

                return info(sender, String.format(Lang.COMMAND_GIVE_ALL, crate.id, given));
            }

            // crates crate common crazicrafter1
            Player p = Bukkit.getServer().getOnlinePlayers().stream().filter(player -> player.getName().equals(args[1])).findFirst().orElse(null);
            if (p == null)
                return severe(sender, Lang.CRATE_ERROR_PLAYER1);

            Util.give(p, crate.itemStack(p));

            // now test quantity with args

            // Redundant spam
            if (p != sender) {
                if (!(flags.contains("s") || flags.contains("silent")))
                    info(p, String.format(Lang.MESSAGE_RECEIVE_CRATE, crate.id));
                return info(sender, String.format(Lang.COMMAND_GIVE, crate.id, p.getDisplayName()));
            }

            return info(sender, String.format(Lang.MESSAGE_RECEIVE_CRATE, crate.id));
        }, (sender, args) -> {
            if (args.length == 1) {
                return getMatches(args[0], plugin.rewardSettings.crates.keySet());
            }
            if (args.length == 2) {
                return getMatches(args[1],
                        Stream.concat(
                                        Stream.concat(
                                                Bukkit.getServer().getOnlinePlayers().stream().filter(p -> p != sender).map(Player::getName),
                                                (Bukkit.getServer().getOnlinePlayers().size() > 1) ? // Removing redundant identifiers for discrete
                                                        Stream.of("*") : Stream.empty()),
                                        sender instanceof Player ? Stream.of("-s", "-silent") : Stream.empty()
                                )
                                .collect(Collectors.toList()));
            }
            if (args.length == 3 && !args[args.length - 1].startsWith("-s")) {
                return getMatches(args[2], Stream.of("-s", "-silent")
                        .collect(Collectors.toList()));
            }
            return new ArrayList<>();
        });

        arg("editor", (sender, args, flags) -> {
            if (sender instanceof Player) {
                Player p = (Player) sender;

                new Editor().open(p);

                return true;
            }

            return severe(sender, Lang.COMMAND_ERROR_PLAYER);
        });

        arg("identify", (sender, args, flags) -> {
            if (!(sender instanceof Player))
                return severe(sender, Lang.COMMAND_ERROR_PLAYER);

            Player p = (Player) sender;

            ItemStack itemStack = p.getInventory().getItemInMainHand();
            if (itemStack.getType() != Material.AIR) {
                CrateSettings crate = Lootcrates.getCrate(itemStack);
                if (crate != null) {
                    return info(sender, String.format(Lang.COMMAND_IDENTIFY, crate.id));
                } else {
                    return info(sender, Lang.MESSAGE_NOT_CRATE);
                }
            }
            return severe(sender, Lang.MESSAGE_REQUIRE_ITEM);
        });

        arg("lang", (sender, args, flags) -> {
            if (Lang.load(sender, args[0]))
                plugin.language = args[0];
            return true;
        }, (sender, args) -> {
            try {
                return Files.list(Lang.PATH.toPath()).filter(f -> f.endsWith(".yml")).map(f -> f.getFileName().toString()).collect(Collectors.toList());
            } catch (IOException e) {
                //return severe(sender, e.getMessage());
                //throw new RuntimeException(e);
                return null;
            }
        });

        arg("reload", (sender, args, flags) -> {
            plugin.reloadConfig(sender);
            plugin.reloadData(sender);
            return info(sender, Lang.CONFIG_LOADED_DISK);
        });

        arg("reset", (sender, args, flags) -> {
            plugin.saveDefaultConfig(sender, true);
            plugin.reloadConfig(sender);
            return info(sender, Lang.CONFIG_LOADED_DEFAULT);
        });

        arg("save", (sender, args, flags) -> {
            plugin.saveConfig(sender);
            plugin.saveOtherConfigs(sender);
            return info(sender, Lang.CONFIG_SAVED);
        });
    }

    static boolean info(CommandSender sender, String message) {
        return plugin.notifier.commandInfo(sender, message);
    }

    static boolean warn(CommandSender sender, String message) {
        return plugin.notifier.commandWarn(sender, message);
    }

    static boolean severe(CommandSender sender, String message) {
        return plugin.notifier.commandSevere(sender, message);
    }

    //static List<String> getNonDestructiveMatches(String arg, Collection<String> samples, String)

    static List<String> getMatches(String arg, Collection<String> samples) {
        return getMatches(arg, samples, "%s");
    }

    static List<String> getMatches(String arg, Collection<String> samples, String format) {
        // todo this doesnt work as expected on vanilla client because the autocomplete is alphabetical
        //Stream<SimilarString> similar = samples.stream().map(s -> new SimilarString(s, arg));

        return samples.parallelStream().filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                .limit(10).map(s -> String.format(format, s)).collect(Collectors.toList());

        //return similar.filter(s -> s.s.toLowerCase().replace(" ", "").contains(arg.toLowerCase()))
        //        .sorted().limit(10).map(s -> String.format(format, s)).collect(Collectors.toList());
    }
}
