package common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapLayout {
    public static int[][] map = {{1,1,1,1,2,1,1,1,1,2,1,1,3,1,1,1,1,1,1,1},
                           {1,1,1,1,1,1,1,1,1,1,1,1,1,1,4,4,4,4,4,1},
                           {1,1,1,1,1,1,1,1,3,1,1,1,1,2,5,5,6,5,5,1},
                           {1,1,1,1,1,1,1,1,1,1,1,1,1,1,6,6,6,5,5,1},
                           {1,1,1,1,1,1,1,1,1,1,1,1,1,3,1,1,3,1,1,1},
                           {1,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
                           {1,1,1,3,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
                           {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
                           {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,3,1},
                           {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
                           {1,1,1,1,3,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
                           {1,1,4,4,4,4,1,1,1,1,2,1,1,1,1,1,1,1,1,1},
                           {1,1,5,6,5,5,1,1,1,1,1,1,3,1,1,1,1,1,1,1},
                           {2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}};

    public static void loadFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            List<int[]> rows = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] tokens = line.split(",");
                int[] row = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    row[i] = Integer.parseInt(tokens[i].trim());
                }
                rows.add(row);
            }
            map = rows.toArray(new int[0][]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
