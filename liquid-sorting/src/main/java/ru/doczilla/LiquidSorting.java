package ru.doczilla;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class LiquidSorting {
    private final Flow flow;

    public LiquidSorting(Flow flow) {
        this.flow = flow;
    }

    public Flow solve() {
        LinkedList<Flow> queue = new LinkedList<>();
        queue.add(flow);

        Set<String> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            Flow currentState = queue.removeLast();
            String hash = hashBottles(currentState.bottles());
            if (visited.contains(hash)) continue;

            visited.add(hash);

            if (isDone(currentState.bottles()))
                return currentState;

            for (int i = 0; i < currentState.bottles().size(); i++) {
                List<Integer> bottle1 = currentState.bottles().get(i);
                if (isSorted(bottle1) || bottle1.get(0) == -1) {
                    continue;
                }
                for (int j = 0; j < currentState.bottles().size(); j++) {
                    if (i == j) continue;

                    List<Integer> bottle2 = currentState.bottles().get(j);
                    if (!bottle2.isEmpty() && bottle2.get(bottle2.size() - 1) != -1) {
                        continue;
                    }
                    if (canPoured(bottle1, bottle2)) {
                        List<List<Integer>> newBottles = copyBottles(currentState.bottles());
                        pour(newBottles, i, j);
                        String newHash = hashBottles(newBottles);
                        if (!isVisited(queue, newHash)) {
                            List<String> newMoves = new ArrayList<>(currentState.moves());
                            newMoves.add("(" + i + ", " + j + ")");
                            Flow newFlow = new Flow(newBottles, newMoves);
                            queue.add(newFlow);
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isVisited(LinkedList<Flow> queue, String hash) {
        for (Flow flow : queue) {
            if (hashBottles(flow.bottles()).equals(hash)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSorted(List<Integer> bottle) {
        int color = bottle.isEmpty() ? -1 : bottle.get(0);
        if (color == -1) {
            return false;
        }
        for (int c : bottle) {
            if (c != color) {
                return false;
            }
        }
        return true;
    }

    private List<List<Integer>> copyBottles(List<List<Integer>> bottles) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> bottle : bottles) {
            List<Integer> newBottle = new ArrayList<>(bottle);
            copy.add(newBottle);
        }
        return copy;
    }

    private String hashBottles(List<List<Integer>> bottles) {
        StringBuilder sb = new StringBuilder();
        for (List<Integer> bottle : bottles) {
            for (int color : bottle) {
                sb.append(color).append("|");
            }
            sb.append("=>");
        }
        return sb.toString();
    }

    private void pour(List<List<Integer>> bottles, int i, int j) {
        List<Integer> from = bottles.get(i);
        List<Integer> to = bottles.get(j);
        while (canPoured(from, to)) {
            int color = getTopLiquid(from);
            int index = getTopIndex(from);
            int emptyIndex = getEmptyIndex(to);
            from.set(index, -1);
            to.set(emptyIndex, color);
        }
    }

    private boolean canPoured(List<Integer> from, List<Integer> to) {
        int topLiquid1 = getTopLiquid(from);
        int topLiquid2 = getTopLiquid(to);

        int empty = 0;
        for (int color : to) {
            if (color == -1)
                empty++;
        }

        int liquids = 0;
        for (int color : from) {
            if (color == topLiquid1)
                liquids++;
        }

        if (liquids > empty) return false;
        if (to.get(to.size() - 1) != -1) return false;

        return topLiquid1 == topLiquid2 || topLiquid2 == -1;
    }

    private int getEmptyIndex(List<Integer> bottle) {
        for (int i = 0; i < bottle.size(); i++) {
            if (bottle.get(i) == -1)
                return i;
        }
        return -1;
    }

    private int getTopLiquid(List<Integer> bottle) {
        for (int i = bottle.size() - 1; i >= 0; i--) {
            if (bottle.get(i) != -1)
                return bottle.get(i);
        }
        return -1;
    }

    private int getTopIndex(List<Integer> bottle) {
        for (int i = bottle.size() - 1; i >= 0; i--) {
            if (bottle.get(i) != -1)
                return i;
        }
        return -1;
    }

    private boolean isDone(List<List<Integer>> bottles) {
        for (List<Integer> bottle : bottles) {
            int currentLiquid = -1;
            for (int color : bottle) {
                if (currentLiquid == -1)
                    currentLiquid = color;
                else if (color != currentLiquid)
                    return false;
            }
        }
        return true;
    }
}
