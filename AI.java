package jump61;

import java.util.Random;

import static jump61.Side.*;

/** An automated Player.
 *  @author P. N. Hilfinger
 */
class AI extends Player {

    /** A new player of GAME initially COLOR that chooses moves automatically.
     *  SEED provides a random-number seed used for choosing moves.
     */
    AI(Game game, Side color, long seed) {
        super(game, color);
        _random = new Random(seed);
    }
    @Override
    String getMove() {
        Board board = getGame().getBoard();
        assert getSide() == board.whoseMove();
        int choice = searchForMove();
        getGame().reportMove(board.row(choice), board.col(choice));
        return String.format("%d %d", board.row(choice), board.col(choice));
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private int searchForMove() {
        Board work = new Board(getBoard());
        int value = 0;
        assert getSide() == work.whoseMove();
        _foundMove = -1;
        if (getSide() == RED) {
            value = minMax(work, 4, true, 1,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE);
        } else {
            value = minMax(work, 4, true, -1,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE);
        }
        return _foundMove;
    }

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board board, int depth, boolean saveMove,
                       int sense, int alpha, int beta) {
        if (depth == 0 || board.getWinner() != null) {
            return staticEval(board, 1000);
        }
        if (sense == 1) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 1; i <= board.size(); i++) {
                for (int j = 1; j <= board.size(); j++) {
                    if (board.isLegal(board.whoseMove(), i, j)
                            && board.isLegal(board.whoseMove())) {
                        board.addSpot(board.whoseMove(), i, j);
                        int eval = minMax(board, depth - 1,
                                false, -sense, alpha, beta);
                        board.undo();
                        if (maxEval < eval) {
                            alpha = Math.max(alpha, eval);
                            maxEval = Math.max(maxEval, eval);
                            if (saveMove) {
                                _foundMove = board.sqNum(i, j);
                            }
                        }
                        if (beta <= alpha) {
                            break;
                        }
                    }
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int i = 1; i <= board.size(); i++) {
                for (int j = 1; j <= board.size(); j++) {
                    if (board.isLegal(board.whoseMove(), i, j)
                            && board.isLegal(board.whoseMove())) {
                        board.addSpot(board.whoseMove(), i, j);
                        int eval = minMax(board, depth - 1,
                                false, -sense, alpha, beta);
                        board.undo();
                        if (minEval > eval) {
                            beta = Math.min(beta, eval);
                            minEval = Math.min(minEval, eval);
                            if (saveMove) {
                                _foundMove = board.sqNum(i, j);
                            }
                        }
                        if (beta <= alpha) {
                            break;
                        }
                    }
                }
            }
            return minEval;
        }
    }





    /** Return a heuristic estimate of the value of board position B.
     *  Use WINNINGVALUE to indicate a win for Red and -WINNINGVALUE to
     *  indicate a win for Blue. */
    private int staticEval(Board b, int winningValue) {
        int countRed = 0;
        int countBlue = 0;
        for (int i = 1; i <= b.size(); i++) {
            for (int j = 1; j <= b.size(); j++) {
                if (b.get(i, j).getSide().equals(RED)) {
                    countRed++;
                } else if (b.get(i, j).getSide().equals(BLUE)) {
                    countBlue++;
                }
            }
        }
        int heuristicValue = countRed - countBlue;
        if (countRed == b.size() * b.size()) {
            return winningValue;
        } else if (countBlue == b.size() * b.size()) {
            return -winningValue;
        }
        return heuristicValue;
    }

    /** A random-number generator used for move selection. */
    private Random _random;

    /** Used to convey moves discovered by minMax. */
    private int _foundMove;

    /** used to record the winning value for each move.*/
    private int winningEstimate;


}
