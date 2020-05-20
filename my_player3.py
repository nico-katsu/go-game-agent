
from copy import deepcopy
import sys
import json
from functools import partial
import time

init_board = [[0]*5 for _ in range(5)]


class GO:
    def __init__(self):
        self.size = 5
        self.died_stones = []  # Intialize died stones to be empty
        self.n_move = 0  # Trace the number of moves
        self.max_move = 24  # The max movement of a Go game
        self.komi = 2.5  # Komi rule

    def compare_board(self, board1, board2):
        for i in range(self.size):
            for j in range(self.size):
                if board1[i][j] != board2[i][j]:
                    return False
        return True

    def board_to_sets(self, board):
        sets = {0: set(), 1: set(), 2: set()}
        for i in range(5):
            for j in range(5):
                sets[board[i][j]].add((i, j))
        return sets

    def set_board(self, stone_type, previous_board, board):
        for i in range(self.size):
            for j in range(self.size):
                if previous_board[i][j] == stone_type and board[i][j] != stone_type:
                    self.died_stones.append((i, j))
        self.previous_board = previous_board
        self.board = board
        self.action = 'PASS' if self.compare_board(previous_board, board) and not (
            self.compare_board(board, init_board) and stone_type == 1) else 'MOVE'
        self.stone_sets = self.board_to_sets(board)

    def copy_board(self):
        return deepcopy(self)

    def next_go(self):
        next_board = self.copy_board()
        next_board.n_move += 1
        next_board.action = 'PASS'
        return next_board

    def detect_neighbor(self, i, j):
        board = self.board
        neighbors = []
        if i > 0:
            neighbors.append((i-1, j))
        if i < len(board) - 1:
            neighbors.append((i+1, j))
        if j > 0:
            neighbors.append((i, j-1))
        if j < len(board) - 1:
            neighbors.append((i, j+1))
        return neighbors

    def detect_neighbor_ally(self, i, j):
        board = self.board
        neighbors = self.detect_neighbor(i, j)  # Detect neighbors
        group_allies = []
        for stone in neighbors:
            if board[stone[0]][stone[1]] == board[i][j]:
                group_allies.append(stone)
        return group_allies

    def ally_dfs(self, i, j):
        stack = [(i, j)]  # stack for DFS serach
        ally_members = []  # record allies positions during the search
        while stack:
            stone = stack.pop()
            ally_members.append(stone)
            neighbor_allies = self.detect_neighbor_ally(stone[0], stone[1])
            for ally in neighbor_allies:
                if ally not in stack and ally not in ally_members:
                    stack.append(ally)
        return ally_members

    def find_liberty(self, i, j):
        # board = self.board
        ally_members = self.ally_dfs(i, j)
        return self.find_allies_liberty(ally_members)
        # for member in ally_members:
        #     neighbors = self.detect_neighbor(member[0], member[1])
        #     for stone in neighbors:
        #         if board[stone[0]][stone[1]] == 0:
        #             return True
        # return False

    def find_allies_liberty(self, ally_members):
        board = self.board
        for member in ally_members:
            neighbors = self.detect_neighbor(member[0], member[1])
            for stone in neighbors:
                if board[stone[0]][stone[1]] == 0:
                    return True
        return False

    def find_died_stones(self, stone_type):
        board = self.board
        died_stones = set()
        closed = set()
        for (i, j) in self.stone_sets[stone_type]:
            if (i, j) not in closed:
                ally_members = self.ally_dfs(i, j)
                closed.update(ally_members)
                if (i, j) not in died_stones and not self.find_allies_liberty(ally_members):
                    died_stones.update(ally_members)
        return list(died_stones)

    def remove_died_stone(self, stone_type):
        died_stones = self.find_died_stones(stone_type)
        if not died_stones:
            return []
        self.remove_certain_stones(died_stones)
        return died_stones

    def remove_certain_stones(self, positions):
        board = self.board
        for (i, j) in positions:
            self.stone_sets[board[i][j]].remove((i, j))
            board[i][j] = 0
            self.stone_sets[0].add((i, j))
        self.update_board(board)

    def place_stone(self, i, j, stone_type):
        board = self.board

        valid_place = self.valid_place_check(i, j, stone_type)
        if not valid_place:
            return False
        self.previous_board = deepcopy(board)
        self.stone_sets[0].remove((i, j))
        board[i][j] = stone_type
        self.stone_sets[stone_type].add((i, j))
        self.update_board(board)
        self.remove_died_stone(3-stone_type)
        self.action = 'MOVE'
        return True

    def valid_place_check(self, i, j, stone_type):
        board = self.board

        # Check if the place is in the board range
        if not (i >= 0 and i < len(board)):
            return False
        if not (j >= 0 and j < len(board)):
            return False

        # Check if the place already has a stone
        if board[i][j] != 0:
            return False

        # Copy the board for testing
        test_go = self.copy_board()
        test_board = test_go.board

        # Check if the place has liberty
        test_go.stone_sets[0].remove((i, j))
        test_board[i][j] = stone_type
        test_go.stone_sets[stone_type].add((i, j))
        test_go.update_board(test_board)
        if test_go.find_liberty(i, j):
            return True

        # If not, remove the died stones of opponent and check again
        test_go.remove_died_stone(3 - stone_type)
        if not test_go.find_liberty(i, j):
            return False

        # Check special case: repeat placement causing the repeat board state (KO rule)
        else:
            if self.died_stones and self.compare_board(self.previous_board, test_go.board):
                return False
        return True

    def update_board(self, new_board):
        self.board = new_board

    def game_end(self, stone_type, action="MOVE"):
        # Case 1: max move reached
        if self.n_move >= self.max_move:
            return True
        # Case 2: two players all pass the move.
        if self.action == 'PASS' and action == "PASS":
            return True
        return False

    def score(self, stone_type):

        return len(self.stone_sets[stone_type])

    def judge_winner(self):
        cnt_1 = self.score(1)
        cnt_2 = self.score(2)
        if cnt_1 > cnt_2 + self.komi:
            return 1
        elif cnt_1 < cnt_2 + self.komi:
            return 2
        else:
            return 0

    def score_difference(self, stone_type):
        scores = {
            1: self.score(1),
            2: self.score(2)+self.komi
        }
        return scores[stone_type]-scores[3-stone_type]

    def liberty(self, stone_type):
        return sum([self.board[ni][nj] == 0 for (i, j) in self.stone_sets[stone_type] for (ni, nj) in self.detect_neighbor(i, j)])
        #rst = 0
        # for (i, j) in self.stone_sets[0]:
        #    for (ni, nj) in self.detect_neighbor(i, j):
        #        if self.board[ni][nj] == stone_type:
        #            rst += 1
        #            break
        # return rst

    def liberty_difference(self, stone_type):
        return self.liberty(stone_type)-self.liberty(3-stone_type)


def makeBoard(input_board): return list(
    map(lambda row: list(map(int, list(row.strip()))), input_board))


class MyPlayer():

    def check_terminate(self, go: GO, stone_type, previous_action, accumulate):
        if go.game_end(stone_type, previous_action):
            winner = go.judge_winner()
            if winner == self.stone_type:
                return True, (100, 0, 0)
            elif winner == 3-self.stone_type:
                return True, (-100, 0, 0)
            else:
                return True, (0, 0, 0)
        if accumulate * go.score(0) > 10000:
            return True, (go.score_difference(self.stone_type), 0 if self.stone_type == 2 else -go.liberty(3-self.stone_type), 0 if self.stone_type == 2 else go.liberty(self.stone_type))
            # return True, (go.score_difference(self.stone_type), go.liberty_difference(self.stone_type), go.liberty(self.stone_type))
            # return True, (go.score_difference(self.stone_type),  go.liberty(self.stone_type), go.liberty_difference(self.stone_type))
            # return True, (go.score_difference(self.stone_type),  0, 0)
        valid_actions = list(self.get_valid_actions(go, stone_type))
        return False, valid_actions

    def get_valid_actions(self, go: GO, stone_type):
        for (i, j) in go.stone_sets[0]:
            new_go = go.next_go()
            if new_go.place_stone(i, j, stone_type):
                yield new_go, (i, j)
        new_go = go.next_go()
        yield new_go, 'PASS'

    def max_value(self, go: GO, stone_type, previous_action, accumulate, alpha, beta):
        terminate, utility = self.check_terminate(
            go, stone_type, previous_action, accumulate)
        if terminate:
            # print('max', go.board, utility)
            return utility
        valid_actions = utility
        v = (-sys.maxsize, 0, 0)
        for go_action in utility:
            new_go, action = go_action
            v_bak = v
            v = max(v, self.min_value(new_go, 3-stone_type,
                                      go.action, accumulate*len(valid_actions), alpha, beta))
            if accumulate == 1 and v_bak != v:
                self.action = action
            if v > beta:
                return v
            alpha = max(alpha, v)
        # print('max', go.board, v)
        return v

    def min_value(self, go: GO, stone_type,  previous_action, accumulate, alpha, beta):
        terminate, utility = self.check_terminate(
            go, stone_type, previous_action, accumulate)
        if terminate:
            # print('min', go.board, utility)
            return utility
        valid_actions = utility
        v = (sys.maxsize, 0, 0)
        for go_action in self.get_valid_actions(go, stone_type):
            new_go, action = go_action
            v = min(v, self.max_value(new_go, 3-stone_type,
                                      go.action, accumulate*len(valid_actions), alpha, beta))
            if v < alpha:
                return v
            beta = min(beta, v)
        # print('min', go.board, v)
        return v

    def get_action(self, go: GO, stone_type):
        # n_free = len(go.stone_sets[0])
        self.stone_type = stone_type
        # if n_free <= 5:
        #     self.deep = 5
        # elif n_free < 15:
        #     self.deep = 4
        # else:
        #     self.deep = 3
        self.max_value(go, stone_type, 'MOVE', 1,
                       (-sys.maxsize, 0, 0), (sys.maxsize, 0, 0))
        return self.action


def writeNextInput(piece_type, previous_board, board, path="input.txt"):
    res = ""
    res += str(piece_type) + "\n"
    for item in previous_board:
        res += "".join([str(x) for x in item])
        res += "\n"

    for item in board:
        res += "".join([str(x) for x in item])
        res += "\n"

    with open(path, 'w') as f:
        f.write(res[:-1])


if __name__ == "__main__":
    with open('input.txt') as input, open('output.txt', 'w')as output:
        t = time.time()
        lines = input.readlines()
        stone_type = int(lines[0])
        previous_board, board = makeBoard(lines[1:6]), makeBoard(lines[6:11])
        go = GO()
        go.set_board(stone_type, previous_board, board)
        if go.compare_board(previous_board, init_board):
            go.n_move = stone_type-1
        else:
            with open('help.json') as help_json:
                go.n_move = int(json.load(help_json))
        # print(go.liberties(1), go.liberties(2))
        # print(go.valid_place_check(4, 4, 2))

        player = MyPlayer()
        action = player.get_action(go, stone_type)
        print(action if action == 'PASS' else '{},{}'.format(*action), file=output)
        with open('help.json', 'w') as help_json:
            json.dump(go.n_move+2, help_json)
        #     json.dump(go.n_move+1, help_json)
        # if action != 'PASS':
        #     go.place_stone(*action, stone_type)
        # writeNextInput(3-stone_type, go.previous_board, go.board)
        print(time.time()-t)
