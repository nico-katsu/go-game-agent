
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


class point_type implements Cloneable {
    /**
     * Creates a new pair
     *
     * @param key   The key for this pair
     * @param value The value to use for this pair
     */
    int i;
    int j;

    public point_type(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public int getI() {
        return i;
    }

    public int getJ() {
        return j;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof point_type)) return false;
        point_type that = (point_type) o;
        return getI() == that.getI() &&
                getJ() == that.getJ();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getI(), getJ());
    }

    @Override
    public String toString() {
        return "" + i + "," + j;
    }
}

class Search implements Serializable {
    HashMap<String, Search> map = new HashMap<>();
    String[] children;
}

class board_set extends HashSet<point_type> implements Cloneable {
}

class board_type implements Cloneable {
    int[][] board = {
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0}
    };

    public board_type() {
        super();
    }

    public board_type(int[][] b) {
        super();
        this.set(b);
    }


    public int get(point_type point) {
        return board[point.getI()][point.getJ()];
    }

    public int get(int i, int j) {
        return board[i][j];
    }

    public void set(int[][] b) {
        board = new int[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                board[i][j] = b[i][j];
            }
        }
    }

    public void set(point_type point, int stone_type) {
        board[point.getI()][point.getJ()] = stone_type;
    }

    public void set(int i, int j, int stone_type) {
        board[i][j] = stone_type;
    }

    @Override
    protected board_type clone() throws CloneNotSupportedException {
        board_type board_type = new board_type();
        board_type.set(board);
        return board_type;
    }
}

class stone_list extends ArrayList<point_type> implements Cloneable {
    public stone_list() {
    }

    public stone_list(Collection<? extends point_type> c) {
        super(c);
    }
}

class stone_set_map extends HashMap<Integer, board_set> implements Cloneable {
    @Override
    public stone_set_map clone() {
        stone_set_map t = this;
        return new stone_set_map() {{
            for (int i : t.keySet()) {
                put(i, (board_set) t.get(i).clone());
            }
        }};
    }
}


class GO {
    int size = 5;
    stone_list died_stones = new stone_list();
    int n_move = 0;
    int max_move = 24;
    double komi = 2.5;
    String action;
    board_type board, previous_board;
    stone_set_map stone_sets;
    point_type last_move;


    @Override
    protected GO clone() throws CloneNotSupportedException {
        GO go = new GO();
        go.size = size;
        go.n_move = n_move;
        go.max_move = max_move;
        go.komi = komi;
        go.action = action;
        go.died_stones = (stone_list) died_stones.clone();
        go.board = (board_type) board.clone();
        go.previous_board = (board_type) previous_board.clone();
        go.stone_sets = (stone_set_map) stone_sets.clone();
        return go;
    }

    public boolean compare_board(board_type board1, board_type board2) {
        return Arrays.deepEquals(board1.board, board2.board);
//        for (int i = 0; i < 5; i++) {
//            for (int j = 0; j < 5; j++) {
//                if (board1.get(i).get(j) != board2.get(i).get(j)) {
//                    return false;
//                }
//            }
//        }
//        return true;
    }

    public stone_set_map board_to_sets(board_type board) {
        stone_set_map rst = new stone_set_map() {
            {
                put(0, new board_set());
                put(1, new board_set());
                put(2, new board_set());
            }
        };
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                rst.get(board.get(i, j)).add(new point_type(i, j));
            }
        }
        return rst;

    }

    public void set_board(int stone_type, board_type previous_board, board_type board) throws CloneNotSupportedException {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (previous_board.get(i, j) == stone_type && board.get(i, j) != stone_type) {
                    died_stones.add(new point_type(i, j));
                }
                if (previous_board.get(i, j) == 0 && board.get(i, j) == 3 - stone_type) {
                    last_move = new point_type(i, j);
                }
            }
        }
        this.previous_board = previous_board.clone();
        this.board = board.clone();
        if (compare_board(this.previous_board, this.board) && !(compare_board(board, my_player.init_board) && stone_type == 1)) {
            action = "PASS";
        } else {
            action = "MOVE";
        }
        stone_sets = board_to_sets(board);

    }

    public GO copy_board() throws CloneNotSupportedException {
        return this.clone();
    }

    public GO next_go() throws CloneNotSupportedException {
        GO next = copy_board();
        next.n_move += 1;
        next.action = "PASS";
        return next;
    }

    public stone_list detect_neighbor(point_type point) {
//        List<point_type> neighbors = new ArrayList<>();
        stone_list neighbors = new stone_list();
        int i = point.getI(), j = point.getJ();
        if (i > 0) {
            neighbors.add(new point_type(i - 1, j));
        }
        if (i < 4) {
            neighbors.add(new point_type(i + 1, j));
        }
        if (j > 0) {
            neighbors.add(new point_type(i, j - 1));
        }
        if (j < 4) {
            neighbors.add(new point_type(i, j + 1));
        }
        return neighbors;
    }

    public stone_list detect_neighbor_ally(point_type point) {
        stone_list neighbors = detect_neighbor(point);
        stone_list group_allies = new stone_list();
        for (point_type stone : neighbors) {
            if (board.get(stone) == board.get(point)) {
                group_allies.add(stone);
            }
        }
        return group_allies;
    }

    public stone_list ally_dfs(point_type point) {
        Stack<point_type> stack = new Stack<>();
        stack.push(point);
        stone_list ally_members = new stone_list();
        while (!stack.empty()) {
            point_type stone = stack.pop();
            ally_members.add(stone);
            List<point_type> neighbor_allies = detect_neighbor_ally(stone);
            for (point_type ally : neighbor_allies) {
                if (!stack.contains(ally) && !ally_members.contains(ally)) {
                    stack.push(ally);
                }
            }
        }
        return ally_members;

    }

    public boolean find_liberty(point_type point) {
        stone_list ally_members = ally_dfs(point);
        return find_liberty(ally_members);
//        for (point_type member : ally_members) {
//            List<point_type> neighbors = detect_neighbor(member);
//            for (point_type stone : neighbors) {
//                if (board.get(stone) == 0) {
//                    return true;
//                }
//            }
//        }
//        return false;
    }

    public boolean find_liberty(stone_list ally_members) {
        for (point_type member : ally_members) {
            List<point_type> neighbors = detect_neighbor(member);
            for (point_type stone : neighbors) {
                if (board.get(stone) == 0) {
                    return true;
                }
            }
        }
        return false;

    }

    public stone_list find_died_stones(int stone_type) {
        board_set died_stones = new board_set();
        board_set closed = new board_set();
        for (point_type stone : stone_sets.get(stone_type)) {
            if (!closed.contains(stone)) {
                stone_list ally_members = ally_dfs(stone);
                closed.addAll(ally_members);
                if (!died_stones.contains(stone) && !find_liberty(ally_members)) {
                    died_stones.addAll(ally_members);
                }
            }
        }
        return new stone_list(died_stones);
    }

    public void remove_certain_stones(stone_list positions) {
        for (point_type point : positions) {
            stone_sets.get(board.get(point)).remove(point);
            board.set(point, 0);
            stone_sets.get(0).add(point);
        }
    }

    public stone_list remove_died_stone(int stone_type) {
        stone_list died_stones = find_died_stones(stone_type);
        if (!died_stones.isEmpty()) {
            remove_certain_stones(died_stones);
        }
        return died_stones;
    }

    public boolean valid_place_check(point_type position, int stone_type) throws CloneNotSupportedException {
        if (!(position.getI() >= 0 && position.getI() < 5 && position.getJ() >= 0 && position.getJ() < 5)) {
            return false;
        }
        GO test_go = copy_board();
        test_go.stone_sets.get(0).remove(position);
        test_go.board.set(position, stone_type);
        test_go.stone_sets.get(stone_type).add(position);
        if (test_go.find_liberty(position)) {
            return true;
        }
        test_go.remove_died_stone(3 - stone_type);
        if (!test_go.find_liberty(position)) {
            return false;
        } else {
            if (!died_stones.isEmpty() && compare_board(previous_board, test_go.board)) {
                return false;
            }
        }
        return true;

    }

    public boolean place_stone(point_type position, int stone_type) throws CloneNotSupportedException {
        boolean valid_place = valid_place_check(position, stone_type);
        if (!valid_place) {
            return false;
        }
        previous_board = (board_type) board.clone();
        stone_sets.get(0).remove(position);
        board.set(position, stone_type);
        stone_sets.get(stone_type).add(position);
        remove_died_stone(3 - stone_type);
        action = "MOVE";
        return true;
    }

    public boolean game_end(int stone_type, String action) {
        if (n_move >= max_move) {
            return true;
        }
        if (this.action.equals("PASS") && action.equals("PASS")) {
            return true;
        }
        return false;
    }

    public int score(int stone_type) {
        return stone_sets.get(stone_type).size();
    }

    public int judge_winner() {
        int cnt_1 = score(1), cnt_2 = score(2);
        if (cnt_1 > cnt_2 + komi) {
            return 1;
        } else if (cnt_1 < cnt_2 + komi) {
            return 2;
        } else {
            return 0;
        }
    }

    public double score_difference(int stone_type) {
        Map<Integer, Double> scores = new HashMap<Integer, Double>() {
            {
                put(1, (double) score(1));
                put(2, komi + score(2));
            }
        };
        return scores.get(stone_type) - scores.get(3 - stone_type);
    }

    public int liberty(int stone_type) {
        int rst = 0;
        for (point_type position : stone_sets.get(0)) {
            for (point_type neighbor : detect_neighbor(position)) {
                if (board.get(neighbor) == stone_type) {
                    rst++;
                    break;
                }
            }
        }
        return rst;
    }

    public int liberty_difference(int stone_type) {
        return liberty(stone_type) - liberty(3 - stone_type);
    }

    board_type make_board(String[] input_board) {
        int[][] board = new int[5][];
        for (int i = 0; i < 5; i++) {
            board[i] = Arrays.stream(input_board[i].split("")).mapToInt(s -> Integer.parseInt(s)).toArray();
        }
        return new board_type(board);

    }

}

class Help implements Serializable {
    int n_move;

    public Help(int n_move) {
        this.n_move = n_move;
    }
}

public class my_player {
    int stone_type;
    point_type action;
    public static board_type init_board = new board_type();

    public static String actionToString(point_type action) {
        if (action == null) {
            return "PASS";
        } else {
            return action.toString();
        }
    }

    public static point_type stringToAction(String s) {
        if (s.equals("PASS")) {
            return null;
        } else {
            String[] sp = s.split(",");
            int i = Integer.parseInt(sp[0]), j = Integer.parseInt(sp[1]);
            return new point_type(i, j);
        }
    }


    public static byte[] objToByte(Help help) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(help);
        return byteArrayOutputStream.toByteArray();
    }

    public static Help byteToObj(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objStream = new ObjectInputStream(byteStream);

        return (Help) objStream.readObject();
    }

    class utility_type extends ArrayList<Double> {
        public utility_type(Collection<? extends Double> c) {
            super(c);
        }

        public utility_type(Double... params) {
            super(Arrays.asList(params));
        }
    }

    utility_type max(utility_type u1, utility_type u2) {
        for (int i = 0; i < u1.size(); i++) {
            if (u1.get(i) > u2.get(i)) {
                return u1;
            } else if (u2.get(i) > u1.get(i)) {
                return u2;
            }
        }
        return u1;
    }

    utility_type min(utility_type u1, utility_type u2) {
        for (int i = 0; i < u1.size(); i++) {
            if (u1.get(i) < u2.get(i)) {
                return u1;
            } else if (u2.get(i) < u1.get(i)) {
                return u2;
            }
        }
        return u1;
    }

    boolean lt(utility_type u1, utility_type u2) {
        for (int i = 0; i < u1.size(); i++) {
            if (u1.get(i) < u2.get(i)) {
                return true;
            } else if (u2.get(i) < u1.get(i)) {
                return false;
            }

        }
        return false;

    }


    class terminate_result {
        boolean terminate;
        utility_type utility;

        public terminate_result(boolean terminate, utility_type utility) {
            this.terminate = terminate;
            this.utility = utility;
        }

        public boolean isTerminate() {
            return terminate;
        }

        public utility_type getUtility() {
            return utility;
        }
    }

    terminate_result check_terminate(GO go, int stone_type, String previous_action, int accumulate) {
        if (go.game_end(stone_type, previous_action)) {
            int winner = go.judge_winner();
            if (winner == this.stone_type) {
                return new terminate_result(true, new utility_type(go.score_difference(this.stone_type) + 100.0, 0.0, 0.0));
            } else if (winner == 3 - this.stone_type) {
                return new terminate_result(true, new utility_type(go.score_difference(this.stone_type) - 100.0, 0.0, 0.0));
            } else {
                return new terminate_result(true, new utility_type(0.0, 0.0, 0.0));
            }
        }
        if (accumulate * go.score(0) > 1000000) {
            double score_difference = go.score_difference(this.stone_type);
            if (stone_type == 1) {
                if (score_difference > 0) {
                    return new terminate_result(true, new utility_type(go.score_difference(this.stone_type), (double) go.liberty(this.stone_type), (double) -go.liberty(3 - this.stone_type)));
                } else {
                    return new terminate_result(true, new utility_type(go.score_difference(this.stone_type), (double) -go.liberty(3 - this.stone_type), (double) go.liberty(this.stone_type)));
                }
            } else {
                return new terminate_result(true, new utility_type(go.score_difference(this.stone_type), 0.0, 0.0));
            }
//            return new terminate_result(true, new utility_type(go.score_difference(this.stone_type), (double) go.liberty(this.stone_type), (double) -go.liberty(3 - this.stone_type)));
        }
        return new terminate_result(false, null);
    }

    class go_point {
        GO go;
        point_type action;

        public go_point(GO go, point_type action) {
            this.go = go;
            this.action = action;
        }

        public GO getGo() {
            return go;
        }

        public point_type getAction() {
            return action;
        }
    }

    List<go_point> get_valid_actions(GO go, int stone_type) throws CloneNotSupportedException {
        List<go_point> rst = new ArrayList();
        for (point_type position : go.stone_sets.get(0)) {
            GO new_go = go.next_go();
            if (new_go.place_stone(position, stone_type)) {
                rst.add(new go_point(new_go, position));
            }
        }
        rst.add(new go_point(go.next_go(), null));
        return rst;
    }

    stone_list valid_actions(GO go, int stone_type) throws CloneNotSupportedException {
        stone_list rst = new stone_list();
        for (point_type position : go.stone_sets.get(0)) {
            if (go.valid_place_check(position, stone_type)) {
                rst.add(position);
            }
        }
        rst.add(null);
        return rst;
    }

    utility_type max_value(GO go, int stone_type, String previous_action, int accumulate, utility_type alpha, utility_type beta) throws CloneNotSupportedException {
        terminate_result terminate_result = check_terminate(go, stone_type, previous_action, accumulate);
        if (terminate_result.isTerminate()) {
//            System.out.println(terminate_result.getValue());
            return terminate_result.getUtility();
        }
//        List<go_point> valid_actions = get_valid_actions(go, stone_type);
        stone_list valid_actions;
        valid_actions = valid_actions(go, stone_type);
        utility_type v = new utility_type((double) -(1 << 20), 0.0, 0.0);
//        if (accumulate == 1) {
//            for (go_point gp : valid_actions) {
//                if (gp.action != null) {
//                    System.out.println(gp.action.toString());
//                    System.out.println(go.valid_place_check(gp.action, stone_type));
//                }
//            }
//        }
//        for (go_point go_action : valid_actions) {
        for (point_type action : valid_actions) {
            GO new_go = go.next_go();
            if (action != null) {
                new_go.place_stone(action, stone_type);
            }
//            GO new_go = go_action.getGo();
//            point_type action = go_action.getAction();
            utility_type v_bak = v;
            String actionStr = actionToString(action);
            v = max(v, min_value(new_go, 3 - stone_type, go.action, accumulate * valid_actions.size(), alpha, beta));
//            v = max(v, min_value(new_go, 3 - stone_type, go.action, accumulate << 1, alpha, beta));
            if (accumulate == 1 && !v_bak.equals(v)) {
                this.action = action;
            }
            if (lt(beta, v)) {
                return v;
            }
            alpha = max(alpha, v);
        }
//        System.out.println(v);
        return v;
    }

    utility_type min_value(GO go, int stone_type, String previous_action, int accumulate, utility_type alpha, utility_type beta) throws CloneNotSupportedException {
        terminate_result terminate_result = check_terminate(go, stone_type, previous_action, accumulate);
        if (terminate_result.isTerminate()) {
            return terminate_result.getUtility();
        }
//        List<go_point> valid_actions = get_valid_actions(go, stone_type);
//        search.children = valid_actions.stream().map(ac -> actionToString(ac.getAction())).toArray(String[]::new);
//        stone_list valid_actions = valid_actions(go, stone_type);
//        search.children = valid_actions.stream().map(a -> actionToString(a)).toArray(String[]::new);
        stone_list valid_actions;
        valid_actions = valid_actions(go, stone_type);
        utility_type v = new utility_type((double) (1 << 20), 0.0, 0.0);
//        for (go_point go_action : valid_actions) {
        for (point_type action : valid_actions) {
            GO new_go = go.next_go();
            if (action != null) {
                new_go.place_stone(action, stone_type);
            }
//            GO new_go = go_action.getGo();
//            point_type action = go_action.getAction();
            String actionStr = actionToString(action);
            v = min(v, max_value(new_go, 3 - stone_type, go.action, accumulate * valid_actions.size(), alpha, beta));
//            v = min(v, max_value(new_go, 3 - stone_type, go.action, accumulate << 1, alpha, beta));
            if (lt(v, alpha)) {
                return v;
            }
            beta = min(beta, v);
        }
        return v;
    }

    public point_type get_action(GO go, int stone_type) throws CloneNotSupportedException {
        this.stone_type = stone_type;
        utility_type v = max_value(go, stone_type, "MOVE", 1, new utility_type((double) -(1 << 20), 0.0, 0.0), new utility_type((double) (1 << 20), 0.0, 0.0));
//        System.out.println(v);
        return action;
    }

    public static void main(String[] args) throws IOException, CloneNotSupportedException, ClassNotFoundException {
        File input = new File("input.txt");
        BufferedReader br = new BufferedReader(new FileReader(input));
        int stone_type = Integer.parseInt(br.readLine());
        GO go = new GO();
        board_type previous_board = go.make_board(new String[]{
                br.readLine(),
                br.readLine(),
                br.readLine(),
                br.readLine(),
                br.readLine()
        }), board = go.make_board(new String[]{
                br.readLine(),
                br.readLine(),
                br.readLine(),
                br.readLine(),
                br.readLine()
        });
        go.set_board(stone_type, previous_board, board);
        my_player player = new my_player();
        if (go.compare_board(previous_board, my_player.init_board)) {
            go.n_move = stone_type - 1;
        } else {
//            File help_json = new File("help.json");
//            BufferedReader hbr = new BufferedReader(new FileReader(help_json));
//            go.n_move = Integer.parseInt(hbr.readLine());
//            hbr.close();
            byte[] bytes = Files.readAllBytes(Paths.get("help.txt"));
            Help h = byteToObj(bytes);
//            player.search = h.search;
            go.n_move = h.n_move;
            System.out.println(h.n_move);
//            player.search = player.search.map.get(actionToString(go.last_move));
//
//            if (!go.died_stones.isEmpty()) {
//                player.search = player.search.map.get(actionToString(go.died_stones.get(0)));
//            } else {
//                player.search = player.search.map.get(actionToString(null));
//
//            }
        }
        br.close();
        point_type action = player.get_action(go, stone_type);
        FileWriter outputFileWriter = new FileWriter("output.txt");
//        System.out.println(stone_type);
//        for (point_type aciton : go.stone_sets.get(0)) {
//            if (go.valid_place_check(aciton, stone_type)) {
//                System.out.print("(" + aciton.getI() + "," + aciton.getJ() + ")");
//            }
//        }
//        System.out.println();

        if (action == null) {
            outputFileWriter.write("PASS");
        } else {
//            go.place_stone(action, stone_type);
            outputFileWriter.write("" + action.getI() + "," + action.getJ());
        }
        outputFileWriter.close();
//        FileWriter helpJson = new FileWriter("help.json");
////        helpJson.write("" + (go.n_move + 1));
//        helpJson.write("" + (go.n_move + 2));
//        helpJson.close();
//        Help oh = new Help(go.n_move + 2, player.search.map.get(actionToString(action)));
        Help oh = new Help(go.n_move + 2);
        byte[] ob = objToByte(oh);
        Files.write(Paths.get("help.txt"), ob);
//        FileWriter outputWriter = new FileWriter("input.txt");
//        outputWriter.write("" + (3 - stone_type) + "\n");
//        for (int[] row : go.previous_board.board) {
//            for (int i : row) {
//                outputWriter.write("" + i);
//            }
//            outputWriter.write('\n');
//        }
//        for (int[] row : go.board.board) {
//            for (int i : row) {
//                outputWriter.write("" + i);
//            }
//            outputWriter.write('\n');
//        }
//        outputWriter.close();
//        System.out.println(go.score(1));
//        System.out.println(go.score(2));
//        System.out.println(go.n_move);


    }
}


