import java.util.HashMap;
import java.util.Random;

public class Map {
    int width;
    int num_mine;
    int[][] mineMap;
    int[][] displayMap;
    HashMap<Integer, Integer> minePosition;

    public Map(int width, int num_mine) {
        this.width = width;
        this.num_mine = num_mine;

        // create map
        System.out.println("Create " + width + " X " + width + " map");
        mineMap = new int[width][width];
        displayMap = new int[width][width];
        minePosition = new HashMap<>();

        Random rand = new Random();
        for (int i = 0; i < num_mine; i++) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(width);
            if (mineMap[x][y] == -1) {
                i--;
                continue;
            }
            mineMap[x][y] = -1;
            minePosition.put(x * width + y, -1);
        }

        for (int i = 0; i < width * width; i++) {
            int x = i / width;
            int y = i % width;
            if (mineMap[x][y] == -1)
                continue;
            int count = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx >= 0 && nx < width && ny >= 0 && ny < width && mineMap[nx][ny] == -1)
                        count++;
                }
            }
            mineMap[x][y] = count;
        }

        printMap(mineMap);
    }

    public int checkMine(int x, int y) {
        int pos = x * width + y;

        if (minePosition.containsKey(pos)) {
            System.out.println("Find mine at (" + x + ", " + y + ")");
            return pos;
        } else {
            System.out.println("No mine at (" + x + ", " + y + ")");
            return -1;
        }
    }

    public void printMap(int[][] a) {
        System.out.println();
        for (int i = 0; i < a.length; i++) {
            System.out.print(a[i][0]);
            for (int j = 1; j < a[0].length; j++)
                System.out.print(" " + a[i][j]);
            System.out.println();
        }
    }

    public void updateMap(int x, int y) {
        displayMap[x][y] = 1;
        printMap(displayMap);
    }
}
