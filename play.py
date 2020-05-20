from host import GO
from random_player import RandomPlayer
from copy import deepcopy
from collections import defaultdict

go = GO(5)
init_board = [
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
]
go.set_board(1, deepcopy(init_board), deepcopy(init_board))
player = RandomPlayer()
piece_type = 1
previous_action = 'MOVE'
actions = defaultdict(list)
board_int = defaultdict(list)
while not go.game_end(piece_type, previous_action):
    action = player.get_input(go, piece_type)
    actions[piece_type].append(action)
    if action != 'PASS':
        go.place_chess(*action, piece_type)
        go.remove_died_pieces(piece_type)
        previous_action = 'MOVE'
    else:
        previous_action = 'PASS'
    go.n_move += 1
    piece_type = 3-piece_type
print(actions)
print(go.judge_winner())
