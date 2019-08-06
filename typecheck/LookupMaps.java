package typecheck;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

import java.util.ArrayList;
import java.util.HashMap;

public class LookupMaps {
    ArrayList<HashMap<String, String>> maps;
    HashMap<String, VarInfo> varMap;
    public LookupMaps() {
        maps = new ArrayList<>();
        varMap = new HashMap<>();
    }
    public LookupMaps(HashMap<String, VarInfo> varMap) {
        maps = new ArrayList<>();
        HashMap<String, String> initMap = new HashMap<>();
        for (String s : varMap.keySet()) {
            initMap.put(s, s);
        }
        maps.add(initMap);
        this.varMap = new HashMap<>(varMap);
    }
    public String getName(String k) {
        for (int i = maps.size() - 1; i >= 0; --i) {
            if (maps.get(i).containsKey(k))
                return maps.get(i).get(k);
        }
        return null;
    }
    public VarInfo getInfo(String k) {
        return varMap.get(getName(k));
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
    public void add(String k, String v, VarInfo vi) {
        maps.get(maps.size() - 1).put(k, v);
        varMap.put(v, vi);
    }

    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return genson.serialize(this);
    }
}
