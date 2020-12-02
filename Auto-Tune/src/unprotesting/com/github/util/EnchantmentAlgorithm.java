package unprotesting.com.github.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.simple.parser.ParseException;

import unprotesting.com.github.Main;

public class EnchantmentAlgorithm {

    public static void loadEnchantmentSettings() {
        ConfigurationSection config = Main.getEnchantmentConfig().getConfigurationSection("enchantments");
        ConcurrentHashMap<String, EnchantmentSetting> newMap = new ConcurrentHashMap<String, EnchantmentSetting>();
        if (Main.enchMap.containsKey("Auto-Tune")) {
            newMap = Main.enchMap.get("Auto-Tune");
            for (String str : config.getKeys(false)) {
                if (!newMap.containsKey(str)) {
                    Main.debugLog("Loaded new enchantment: " + str + ".");
                    EnchantmentSetting setting = new EnchantmentSetting(str);
                    newMap.put(str, setting);
                }
            }
        } else {
            for (String str : config.getKeys(false)) {
                if (!newMap.containsKey(str)) {
                    Main.debugLog("Loaded new enchantment: " + str + ".");
                    EnchantmentSetting setting = new EnchantmentSetting(str);
                    newMap.put(str, setting);
                }
            }
        }
        Main.enchMap.put("Auto-Tune", newMap);
    }

    public static double calculatePriceWithEnch(ItemStack is) {
        ItemMeta iMeta = is.getItemMeta();
        Map<Enchantment, Integer> enchants = iMeta.getEnchants();
        ConcurrentHashMap<Integer, Double[]> inMap = Main.map.get(is.getType().toString());
        double price = 0.0;
        if (inMap != null) {
            price = inMap.get(inMap.size() - 1)[0];
        }
        double cachePrice = 0;
        for (Map.Entry<Enchantment, Integer> ench : enchants.entrySet()) {
            String enchName = ench.getKey().getName();
            EnchantmentSetting setting = Main.enchMap.get("Auto-Tune").get(enchName);
            Double enchPrice = setting.price;
            Double ratio = setting.ratio;
            cachePrice = cachePrice + (price * ratio) + enchPrice;
        }
        if (cachePrice == 0) {
            return price;
        } else {
            return cachePrice;
        }
    }

    public static ConcurrentHashMap<Integer, Double[]> loadAverageBuyAndSellValue(ConcurrentHashMap<Integer, Double[]> map, EnchantmentSetting setting)
            throws ParseException {
        map.put(map.size(), new Double[]{setting.price, 0.0, 0.0});
        for (Integer i : map.keySet()){
            Double[] arr = map.get(i);
            Integer tempSize = map.keySet().size();
            Integer x = 0;
            Double price = arr[0];
            Integer expvalues = 0;
            Double tempbuys = 0.0;
            Double tempsells = 0.0;
            Double buys = 0.0;
            Double sells = 0.0;
            for (; x < 100000;) {
                Double y = Config.getDataSelectionM() * (Math.pow(x, Config.getDataSelectionZ()))
                    + Config.getDataSelectionC();
                Integer inty = (int) Math.round(y) - 1;
                if (inty > tempSize - 1) {
                  expvalues = inty + 1;
                  break;
                }
                Double[] key = map.get((tempSize - 1) - inty);
                tempbuys = key[1];
                tempbuys = tempbuys * key[0];
                if (tempbuys == 0) {
                  tempbuys = key[0];
                }
                buys = buys + tempbuys;
                tempsells = key[2];
                tempsells = tempsells * key[0];
                if (tempsells == 0) {
                  tempsells = key[0];
                }
                sells = sells + tempsells;
                x++;
            }
            if (Config.isInflationEnabled()){
                buys = buys + buys * 0.01 * Config.getInflationValue();
            }
            Double avBuy = buys / (expvalues);
            Double avSells = sells / (expvalues);
            if (avBuy > avSells){
                Main.debugLog("AvBuyValue > AvSellValue for " + setting.name);
            }
            if (avBuy < avSells){
                Main.debugLog("AvBuyValue < AvSellValue for " + setting.name);
            }
            if (avBuy == avSells){
                Main.debugLog("AvBuyValue = AvSellValue for " + setting.name);
            }
            Double newSpotPrice = HttpPostRequestor.sendRequestForPrice("Variable", "Exponential", Config.getApiKey(),
                Config.getEmail(), setting.name, price, avBuy, avSells, Config.getBasicMaxFixedVolatility(),
                Config.getBasicMinFixedVolatility());
            Double[] temporary = { newSpotPrice, 0.0, 0.0 };
            map.put(map.size(), temporary);
        }
        return map;
    }
}
