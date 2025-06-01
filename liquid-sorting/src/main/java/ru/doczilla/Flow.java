package ru.doczilla;

import java.util.List;

public record Flow
        (
                List<List<Integer>> bottles,
                List<String> moves
        ) {
}
