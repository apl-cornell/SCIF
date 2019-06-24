import java.util.ArrayList;
import java.util.HashMap;

public class LookupMaps {
    ArrayList<HashMap<String, String>> maps;
    public LookupMaps(HashMap<String, varInfo> varMap) {
        maps = new ArrayList<>();
        HashMap<String, String> initMap = new HashMap<>();
        for (String s : varMap.keySet()) {
            initMap.put(s, s);
        }
        maps.add(initMap);
    }
    public String get(String k) {
        for (int i = maps.size() - 1; i >= 0; --i) {
            if (maps.get(i).containsKey(k))
                return maps.get(i).get(k);
        }
        return null;
    }
    public void incLayer() {
        HashMap<String, String> newMap = new HashMap<>();
        maps.add(newMap);
    }
    public void decLayer() {
        maps.remove(maps.size() - 1);
    }
    public boolean exists_curlevel(String k) {
        return maps.get(maps.size() - 1).containsKey(k);
    }
    public boolean exists(String k) {
        for (HashMap<String, String> map : maps) {
            if (map.containsKey(k))
                return true;
        }
        return false;
    }
    public void add(String k, String v) {
        maps.get(maps.size() - 1).put(k, v);
    }
}
