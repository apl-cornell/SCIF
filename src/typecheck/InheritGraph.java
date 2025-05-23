package typecheck;

import ast.Node;

import java.util.*;

public class InheritGraph {
    // maintain the inheritance relationship, built it as a directed graph
    HashMap<String, HashSet<String>> edges;

    public InheritGraph() {
        edges = new HashMap<>();
    }

    private void checkExist(String x) {
        if (!edges.containsKey(x)) {
            edges.put(x, new HashSet<>());
        }
    }

    public void addEdge(String from, String to) {
        checkExist(from);
        checkExist(to);
        edges.get(from).add(to);
    }

    public Set<String> getAllNodes() {
        return edges.keySet();
    }

    public List<String> getTopologicalQueue() {
        // return null if there is any circle
        Map<String, Integer> ind = new HashMap<>();
        for (String x : getAllNodes()) {
            ind.put(x, 0);
        }
        for (String from : edges.keySet()) {
            for (String to : edges.get(from)) {
                ind.put(to, ind.get(to) + 1);
            }
        }

        List<String> q = new ArrayList<>();
        for (String x : getAllNodes()) {
            if (ind.get(x) == 0)
                q.add(x);
        }

        int index = 0;
        while (index < q.size()) {
            String cur = q.get(index);
            for (String to : edges.get(cur)) {
                ind.put(to, ind.get(to) - 1);
                if (ind.get(to) == 0) {
                    q.add(to);
                }
            }
            ++index;
        }
        assert index == getAllNodes().size();
        return q;
    }
}
