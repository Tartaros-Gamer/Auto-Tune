package unprotesting.com.github.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import unprotesting.com.github.Main;
import unprotesting.com.github.util.Config;
import unprotesting.com.github.util.EnchantmentAlgorithm;
import unprotesting.com.github.util.EnchantmentSetting;

public class AutoTuneBuyCommand implements CommandExecutor {

    public static List<String> shopTypes = new ArrayList<String>();

    @Deprecated
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player){
            Player player = (Player) sender;
            if (args.length == 0){
                player.sendMessage(ChatColor.YELLOW + "Command Usage: ");
                player.sendMessage(ChatColor.YELLOW + "/buy: <shop-type> <shop>");
                for (String str : shopTypes){
                    player.sendMessage(ChatColor.YELLOW + "Available shop: '" + str + "'");
                    return true;
                }
            }
            if (args.length == 1){
                if (args[0].contains("enchantments")){
                    ConcurrentHashMap<String, EnchantmentSetting> inputMap = Main.enchMap.get("Auto-Tune");
                    for (String str : Main.enchMap.get("Auto-Tune").keySet()){
                        EnchantmentSetting setting = inputMap.get(str);
                        player.sendMessage(ChatColor.WHITE + "Enchantment: " + ChatColor.AQUA + str + ChatColor.YELLOW +
                        " | Price : "+ Config.getCurrencySymbol() + AutoTuneGUIShopUserCommand.df2.format(setting.price) + " | Item Multiplier: " + setting.ratio + "x");
                    }
                    return true;
                }
                else{
                    player.sendMessage(ChatColor.RED + "Shop " + args[0] + " not found!");
                    return false;
                }
            }
            if (args.length == 2){
                if (args[0].contains("enchantments")){
                    ConcurrentHashMap<String, EnchantmentSetting> inputMap = Main.enchMap.get("Auto-Tune");
                    EnchantmentSetting setting = inputMap.get((args[1].toUpperCase()));
                    ItemStack is = player.getInventory().getItemInMainHand();
                    Material mat = is.getType();
                    boolean enchantExists = false;
                    Map<Enchantment, Integer> map = is.getEnchantments();
                    Enchantment ench = Enchantment.getByName(setting.name);
                    if (map.get(ench) != null){
                        enchantExists = true;
                    }
                    if (is != null){
                        try{
                            if (!enchantExists){
                                is.addEnchantment(ench, 1);
                            }
                            else{
                                int level = is.getEnchantmentLevel(ench);
                                is.addEnchantment(ench, level+1);
                            }
                        }
                        catch(IllegalArgumentException ex){
                            player.sendMessage(ChatColor.RED + "Cannot enchant item: " + is.getType().toString() + " with enchantment " + setting.name);
                            ex.printStackTrace();
                            return true;
                        }
                        ItemStack test = new ItemStack(mat);
                        test.addEnchantment(ench, 1);
                        double price = EnchantmentAlgorithm.calculatePriceWithEnch(new ItemStack(mat), true);
                        Main.getEconomy().withdrawPlayer(player, Double.parseDouble(AutoTuneGUIShopUserCommand.df1.format(price)));
                        player.sendMessage(ChatColor.GOLD + "Purchased " + setting.name + " for "
                         + ChatColor.GREEN + Config.getCurrencySymbol() + AutoTuneGUIShopUserCommand.df2.format(price));
                        ConcurrentHashMap<Integer, Double[]> buySellMap = setting.buySellData;
                        Double[] arr = buySellMap.get(buySellMap.size()-1);
                        if (arr == null){
                            arr = new Double[]{setting.price, 0.0, 0.0};
                        }
                        if (arr[1] == null){
                            arr[1] = 0.0;
                        }
                        if (arr[2] == null){
                            arr[2] = 0.0;
                        }
                        arr[1] = arr[1]+1; 
                        buySellMap.put(buySellMap.size()-1, arr);
                        setting.buySellData = buySellMap;
                        inputMap.put(setting.name, setting);
                        Main.enchMap.put("Auto-Tune", inputMap);
                        return true;
                    }
                    player.sendMessage(ChatColor.RED + "Hold the item you want to enchant in your main hand!");
                    return true;
                }
                else{
                    player.sendMessage("Shop " + args[0] + "not found!");
                    return false;
                }
            }
        }
        else{
            Main.sendMessage(sender, "&cPlayers only.");
            return true;
        }
        return false;
    }
    
}