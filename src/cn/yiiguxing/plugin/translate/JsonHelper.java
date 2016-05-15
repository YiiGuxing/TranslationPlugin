package cn.yiiguxing.plugin.translate;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public final class JsonHelper {

    private JsonHelper() {
    }

    public static BasicExplain getBasicExplainEntity(Gson gson, JsonObject json) {
        return gson.fromJson(json.get("basic"), BasicExplain.class);
    }

    public static WebExplain[] getWebExplainEntities(Gson gson, JsonObject json) {
        TypeToken<WebExplain[]> type = new TypeToken<WebExplain[]>() {
        };
        return gson.fromJson(json.get("web"), type.getType());
    }

    public static QueryResult getYouDaoResultEntity(String jsonString) {
        if (jsonString == null || jsonString.trim().length() == 0)
            return null;

        JsonParser jsonParser = new JsonParser();
        JsonElement rootElement = jsonParser.parse(jsonString);
        JsonObject rootJsonObject = rootElement.getAsJsonObject();

        Gson gson = new Gson();

        QueryResult result = gson.fromJson(rootElement, QueryResult.class);
        result.setBasicExplain(getBasicExplainEntity(gson, rootJsonObject));
        result.setWebExplains(getWebExplainEntities(gson, rootJsonObject));

        return result;
    }

}
