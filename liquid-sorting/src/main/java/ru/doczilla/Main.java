package ru.doczilla;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Flow flow = new Flow(initFlow(), new ArrayList<>());

        LiquidSorting sorting = new LiquidSorting(flow);

        Flow result = sorting.solve();

        if (result == null)
            System.out.println("Not solved.");
        else
            print(result);
    }

    private static void print(Flow result) {
        System.out.println("Result: ");
        System.out.println("Move count: " + result.moves().size());

        List<String> moves = result.moves();

        for (int i = 0; i < moves.size(); i++) {
            System.out.print(moves.get(i) + " ");
            if ((i + 1) % 8 == 0)
                System.out.println();
        }

        System.out.println();
        for (List<Integer> bottle : result.bottles())
            System.out.println(Arrays.toString(bottle.toArray()));
    }

    private static List<List<Integer>> initFlow() {
        int[] inputData = input();
        int amount = inputData[0];
        int bottleSize = inputData[1];
        int liquidQuantity = inputData[2];

        List<Integer> numsList = new ArrayList<>();

        for (int i = 1; i <= liquidQuantity; i++) {
            for (int j = 0; j < bottleSize; j++)
                numsList.add(i);
        }

        Collections.shuffle(numsList);

        List<List<Integer>> bottles = new ArrayList<>(amount);
        int empty = amount - liquidQuantity;

        for (int p = 0, i = 0; i < liquidQuantity; i++) {
            List<Integer> bottle = new ArrayList<>(bottleSize);
            for (int j = 0; j < bottleSize; j++) {
                bottle.add(numsList.get(p++));
            }
            bottles.add(bottle);
        }

        for (int i = 0; i < empty; i++) {
            List<Integer> bottle = new ArrayList<>(bottleSize);
            for (int j = 0; j < bottleSize; j++)
                bottle.add(-1);
            bottles.add(bottle);
        }

        for (List<Integer> bottle : bottles)
            System.out.println(Arrays.toString(bottle.toArray()));

        return bottles;
    }

    private static int[] input() {
        int[] inputData = new int[3];
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter N: ");
            inputData[0] = scanner.nextInt();

            System.out.print("Enter V: ");
            inputData[1] = scanner.nextInt();

            do {
                System.out.print("Enter M: ");
                inputData[2] = scanner.nextInt();
                if (inputData[0] <= inputData[2])
                    System.out.println("Must to be N > M");
            } while (inputData[0] <= inputData[2]);
        }
        return inputData;
    }
}
