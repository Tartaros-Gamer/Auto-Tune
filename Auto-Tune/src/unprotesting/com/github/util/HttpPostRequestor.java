package unprotesting.com.github.util;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;

import unprotesting.com.github.Main;

public class HttpPostRequestor {

    public static void updatePricesforItems(JSONObject json) throws ClientProtocolException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpEntity entityResponse = sendPostRequest(json, client);
        if (entityResponse != null) {
            JsonParser parser = new JsonParser();
            String result = null;
            try {
                result = EntityUtils.toString(entityResponse);
            } catch (SocketException ex) {
                Main.debugLog("Socket Exception: Socket Closed. Reloading HttpClient..");
                client.close();
                client = HttpClients.createDefault();
                entityResponse = sendPostRequest(json, client);
                try {
                    result = EntityUtils.toString(entityResponse);
                } 
                catch (ConnectionClosedException | SocketException ex2) {
                    Main.log("Connection was closed! Please ensure your firewall isn't blocking our connections!");
                    return;
                }
            }
            catch (ConnectionClosedException ex){
                Main.debugLog("Connection Closed Exception: Socket Closed. Reloading HttpClient..");
                client.close();
                client = HttpClients.createDefault();
                entityResponse = sendPostRequest(json, client);
                try {
                    result = EntityUtils.toString(entityResponse);
                } 
                catch (ConnectionClosedException | SocketException ex2) {
                    Main.log("Connection was closed! Please ensure your firewall isn't blocking our connections!");
                    return;
                }
            }
            if (result == null) {
                return;
            }
            JsonElement jsonElement = parser.parse(result);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement jsonArrayElement = jsonObject.get("returnData");
            JsonArray jsonArray = jsonArrayElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();
                JsonElement priceElement = obj.get("p");
                JsonElement nameElement = obj.get("i");
                String priceString = priceElement.getAsString();
                String name = nameElement.getAsString();
                Double price = Double.parseDouble(priceString);
                ConcurrentHashMap<Integer, Double[]> map = Main.map.get(name);
                Double[] arr = { price, 0.0, 0.0 };
                map.put((map.size()), arr);
                Main.map.put(name, map);
            }
        }
    }

    public static HttpEntity sendPostRequest(JSONObject json, CloseableHttpClient client) throws IOException {
        HttpPost httpPost = new HttpPost("https://auto-tune-economy-api.herokuapp.com/");
        StringEntity entity = new StringEntity(json.toJSONString());
        httpPost.setEntity(entity);
        httpPost.setHeader("content-type", "application/json");
        httpPost.setHeader("apikey", Config.getApiKey());
        httpPost.setHeader("email", Config.getEmail());
        CloseableHttpResponse response = null;
        try{
            response = client.execute(httpPost);
        }
        catch (IllegalStateException ex){
            client = HttpClients.createDefault();
            response = client.execute(httpPost);
        }
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entityResponse = null;
        if (statusCode == 200) {
            entityResponse = response.getEntity();
            client.close();
        }
        if (statusCode != 200) {
            client.close();
            Main.log("Error on status code");
        }
        Main.debugLog(response.getStatusLine().getReasonPhrase());
        return entityResponse;
    }

    public static void updatePricesforEnchantments(JSONObject json) throws ClientProtocolException, IOException {
        HttpEntity entityResponse = sendPostRequest(json, HttpClients.createDefault());
        if (entityResponse != null) {
            JsonParser parser = new JsonParser();
            String result = EntityUtils.toString(entityResponse);
            JsonElement jsonElement = parser.parse(result);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement jsonArrayElement = jsonObject.get("returnData");
            JsonArray jsonArray = jsonArrayElement.getAsJsonArray();
            for (JsonElement element : jsonArray){
                JsonObject obj = element.getAsJsonObject();
                JsonElement priceElement = obj.get("p");
                JsonElement nameElement = obj.get("i");
                String priceString = priceElement.getAsString();
                String name = nameElement.getAsString();
                Double price = Double.parseDouble(priceString);
                ConcurrentHashMap<Integer, Double[]> map = Main.enchMap.get(name).buySellData;
                Double[] arr = {price, 0.0, 0.0};
                map.put((map.size()), arr);
                EnchantmentSetting setting = Main.enchMap.get(name);
                setting.buySellData.put(setting.buySellData.size(), arr);
                setting.price = price;
                Main.enchMap.put(name, setting);
            }
        }
    }

    public static boolean ghostCheckAPIKey() throws ClientProtocolException, IOException {
        if (Config.getApiKey() == "xyz"){
            Main.log("Please change your API key in the config.yml file. If you don't have an API key you can get one for free on our discord: https://discord.gg/bj2US6KuXW");
            return false;
        }
        if (Config.getEmail() == "xyz@gmail.com"){
            Main.log("Please change your email in the config.yml file to be the same email you used to sign up.");
            return false;
        }
        else{
            Main.debugLog("Api-Key has been changed in config");
            Main.log("Sending API key to database..");
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://safe-refuge-09383.herokuapp.com");
            httpPost.setHeader("content-type", "application/json");
            httpPost.setHeader("apikey", Config.getApiKey());
            httpPost.setHeader("email", Config.getEmail());
            JSONObject json = new JSONObject();
            json.put("apikey", Config.getApiKey());
            StringEntity entity = new StringEntity(json.toJSONString());
            httpPost.setEntity(entity);
            CloseableHttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                client.close();
                return true;
            }
            else if (statusCode != 200) {
                client.close();
                Main.log("Error on status code");
                return false;
            }
        }
        return false;
        
    }

    public static boolean checkAPIKey(){
        try {
            boolean validKey = ghostCheckAPIKey();
            if (validKey == true){
                return true;
            }
            else if (validKey != true){
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

            
}