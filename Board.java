package jump61;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Formatter;
import java.util.function.Consumer;
import static jump61.Side.*;
import static jump61.Square.square;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier---a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Haoqing Xuan
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** An N x N board in initial configuration. */
    Board(int N) {
        this();
        _history = new ArrayList<>();
        _size = N;
        _spot = 1;
        _board = new Square[N + 1][N + 1];
        for (int i = 1; i < N + 1; i++) {
            for (int j = 1; j < N + 1; j++) {
                _board[i][j] = Square.INITIAL;
            }
        }


    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        this(board0.size());
        this._notifier = NOP;
        _history = new ArrayList<>();
        copy(board0);
        _readonlyBoard = new ConstantBoard(this);

    }

    /** Returns a readonly version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo history and sets the number of moves to 0. */
    void clear(int N) {
        _size = N;
        Board newBoard = new Board(_size);
        copy(newBoard);
        newBoard.numMove = 0;
        newBoard._history = new ArrayList<>();
        announce();
    }

    /** Copy the contents of BOARD into me. */
    void copy(Board board) {
        internalCopy(board);
        numMove = 0;
        announce();
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    private void internalCopy(Board board) {
        assert size() == board.size();
        for (int i = 0; i < size() * size(); i++) {
            internalSet(i, board.get(i).getSpots(), board.get(i).getSide());
        }
        _readonlyBoard = new ConstantBoard(this);
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return _size;
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        return get(sqNum(r, c));
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */
    Square get(int n) {
        return _board[row(n)][col(n)];
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        int count = 0;
        for (int i = 1; i < size() + 1; i++) {
            for (int j = 1; j < size() + 1; j++) {
                count += get(i, j).getSpots();
            }
        }
        return count;
    }

    /** Returns the Side of the player who would be next to move.  If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
        to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return isLegal(player, sqNum(r, c));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     *  to square #N. */
    boolean isLegal(Side player, int n) {
        return player.playableSquare(get(n).getSide());
    }

    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        if (getWinner() == null) {
            return true;
        }
        return false;
    }

    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        for (int i = 1; i < size() + 1; i++) {
            for (int j = 1; j < size() + 1; j++) {
                if (!_board[i][j].getSide().equals(_board[1][1].getSide())
                        || _board[i][j].getSide().equals(WHITE)) {
                    return null;
                }
            }
        }
        return _board[1][1].getSide();
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        int numSide = 0;
        for (int i = 1; i < size() + 1; i++) {
            for (int j = 1; j < size() + 1; j++) {
                if (_board[i][j].getSide().equals(side)) {
                    numSide++;
                }
            }
        }
        return numSide;
    }

    /** Add a spot from PLAYER at row R, column C.  Assumes
     *  isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        addSpot(player, sqNum(r, c));
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        if (isLegal(player, n) && isLegal(player)) {
            markUndo();
            simpleAdd(player, n, 1);
            jump(n);
            numMove += 1;
        }
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        internalSet(r, c, num, player);
        announce();
        numMove++;
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        internalSet(sqNum(r, c), num, player);
    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {
        _board[row(n)][col(n)] = square(player, num);
    }

    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        if (_history.get(_history.size() - 1) != null) {
            this._board = _history.get(_history.size() - 1);
            _history.remove(_history.size() - 1);
        }
        numMove++;
    }

    /** Record the beginning of a move in the undo history. */
    private void markUndo() {
        Board currentBoard = new Board(this);
        _history.add(currentBoard._board);
    }

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();

    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    private void jump(int S) {

        if (getWinner() == null) {
            jumpHelperFirstLine(S);
            jumpHelperLastLine(S);
            jumpHelperMiddle(S);
            while (!_workQueue.isEmpty()) {
                jump(_workQueue.removeFirst());
            }
        }
    }
    /** helper function of jump.
     * @param S */
    private void jumpHelperFirstLine(int S) {
        int currentSpot = get(S).getSpots();
        Side currentSide = get(S).getSide();
        if (row(S) == 1) {
            if (col(S) == 1) {
                if (get(S).getSpots() > 2) {
                    set(row(S), col(S), currentSpot - 2, currentSide);
                    simpleAdd(currentSide, S + 1, 1);
                    simpleAdd(currentSide, S + size(), 1);
                    _workQueue.add(S + 1);
                    _workQueue.add(S + size());
                }
            }
            if (col(S) == size()) {
                if (get(S).getSpots() > 2) {
                    set(row(S), col(S), currentSpot - 2, currentSide);
                    simpleAdd(currentSide, S - 1, 1);
                    simpleAdd(currentSide, S + size(), 1);
                    _workQueue.add(S - 1);
                    _workQueue.add(S + size());
                }
            } else {
                if (get(S).getSpots() > 3) {
                    set(row(S), col(S), currentSpot - 3, currentSide);
                    simpleAdd(currentSide, S - 1, 1);
                    simpleAdd(currentSide, S + size(), 1);
                    simpleAdd(currentSide, S + 1, 1);
                    _workQueue.add(S - 1);
                    _workQueue.add(S + size());
                    _workQueue.add(S + 1);
                }
            }
        }
    }
    /** helper function of jump.
     * @param S */
    private void jumpHelperLastLine(int S) {
        int currentSpot = get(S).getSpots();
        Side currentSide = get(S).getSide();
        if (row(S) == size()) {
            if (col(S) == 1) {
                if (get(S).getSpots() > 2) {
                    set(row(S), col(S), currentSpot - 2, currentSide);
                    simpleAdd(currentSide, S + 1, 1);
                    simpleAdd(currentSide, S - size(), 1);
                    _workQueue.add(S + 1);
                    _workQueue.add(S - size());
                }
            }
            if (col(S) == size()) {
                if (get(S).getSpots() > 2) {
                    set(row(S), col(S), currentSpot - 2, currentSide);
                    simpleAdd(currentSide, S - 1, 1);
                    simpleAdd(currentSide, S - size(), 1);
                    _workQueue.add(S - 1);
                    _workQueue.add(S - size());
                }
            } else {
                if (get(S).getSpots() > 3) {
                    set(row(S), col(S), currentSpot - 3, currentSide);
                    simpleAdd(currentSide, S - 1, 1);
                    simpleAdd(currentSide, S - size(), 1);
                    simpleAdd(currentSide, S + 1, 1);
                    _workQueue.add(S - 1);
                    _workQueue.add(S - size());
                    _workQueue.add(S + 1);
                }
            }
        }
    }
    /** helper function of jump.
     * @param S */
    private void jumpHelperMiddle(int S) {
        int currentSpot = get(S).getSpots();
        Side currentSide = get(S).getSide();
        if (col(S) == 1 && row(S) > 1 && row(S) < size()) {
            if (get(S).getSpots() > 3) {
                set(row(S), col(S), currentSpot - 3, currentSide);
                simpleAdd(currentSide, S - size(), 1);
                simpleAdd(currentSide, S + size(), 1);
                simpleAdd(currentSide, S + 1, 1);
                _workQueue.add(S - size());
                _workQueue.add(S + size());
                _workQueue.add(S + 1);
            }
        }
        if (col(S) == size() && row(S) > 1 && row(S) < size()) {
            if (get(S).getSpots() > 3) {
                set(row(S), col(S), currentSpot - 3, currentSide);
                simpleAdd(currentSide, S - size(), 1);
                simpleAdd(currentSide, S + size(), 1);
                simpleAdd(currentSide, S - 1, 1);
                _workQueue.add(S - size());
                _workQueue.add(S + size());
                _workQueue.add(S - 1);
            }
        }
        if (get(S).getSpots() > 4) {
            set(row(S), col(S), currentSpot - 4, currentSide);
            simpleAdd(currentSide, S - size(), 1);
            simpleAdd(currentSide, S + size(), 1);
            simpleAdd(currentSide, S - 1, 1);
            simpleAdd(currentSide, S + 1, 1);
            _workQueue.add(S - size());
            _workQueue.add(S + size());
            _workQueue.add(S - 1);
            _workQueue.add(S + 1);
        }
    }


    /** Returns my dumped representation. */
    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===\n");
        for (int i = 1; i < size() + 1; i++) {
            out.format("   ");
            for (int j = 1; j < size() + 1; j++) {
                if (_board[i][j].getSide().equals(WHITE)) {
                    out.format(" %d%s", _board[i][j].getSpots(), "-");
                } else {
                    out.format(" %d%s", _board[i][j].getSpots(),
                            _board[i][j].getSide().toString().charAt(0));
                }

            }
            out.format("\n");
        }
        out.format("===");
        return out.toString();
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            Board B = (Board) obj;
            for (int i = 1; i < this.size() + 1; i++) {
                for (int j = 1; j < this.size() + 1; j++) {
                    if (this.get(i, j).getSpots()
                            != B.get(i, j).getSpots()
                            || this.get(i, j).getSide()
                            != B.get(i, j).getSide()) {
                        return false;
                    }
                }
            }

        }
        return true;
    }

    @Override
    public int hashCode() {
        return numPieces();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;
    /** Use numMove to track the # of moves. */
    private int numMove;
    /** Use _size to track the current side length of the board. */
    private int _size;
    /** Use board to store information of board. */
    private Square[][] _board;
    /** Use _spot to track the totoal spots on square. */
    private int _spot;
    /** track the move history. */
    private ArrayList<Square[][]> _history;


}
