package com.kk14222.gamex;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends Activity {
    private final Random random = new Random();
    private SharedPreferences prefs;
    private LinearLayout root;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable jumpTicker;

    private static final String[] PIG_SYMBOLS = {"🐷", "🥕", "🌽", "🍎", "🍇", "🍉", "⭐", "🔔", "🎁", "🍀", "🥬", "🍄"};
    private static final String[] SHEEP_SYMBOLS = {"🐑", "🌿", "🧶", "🌙", "☁", "🌼", "🍃", "💧", "🥛", "🪵", "🔆", "🍂"};
    private static final String[] FRUITS = {"🍒", "🍓", "🍊", "🍋", "🍎", "🍑", "🍍", "🍉"};

    private static final int PIG_GRID_SIZE = 7;
    private static final int PIG_BOARD_DP = 420;
    private static final int PIG_BASE_COUNT = 12;
    private static final int PIG_LEVEL_COUNT_STEP = 2;
    private static final int PIG_MAX_COUNT = 32;
    private static final int TILE_GAME_CELL_DP = 58;
    private static final int WATERMELON_CELL_DP = 62;
    private static final int GAME_2048_CELL_DP = 72;
    private static final long MIN_JUMP_TICK_MS = 110L;
    private static final long BASE_JUMP_TICK_MS = 280L;
    private static final long JUMP_LEVEL_SPEED_STEP_MS = 4L;
    private static final int TILE_BASE_GROUPS = 4;
    private static final int TILE_LEVEL_GROUP_CYCLE = 5;
    private static final int TILE_RANDOM_EXTRA_GROUPS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("game_x_progress", MODE_PRIVATE);
        showHome();
    }

    private void setRoot(String title) {
        if (jumpTicker != null) {
            handler.removeCallbacks(jumpTicker);
            jumpTicker = null;
        }
        ScrollView scrollView = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        root.setBackgroundColor(Color.rgb(255, 248, 239));
        scrollView.addView(root);
        TextView heading = label(title, 26, true);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading, fullWidth());
        setContentView(scrollView);
    }

    private void showHome() {
        setRoot("Game X 单机小游戏合集");
        root.addView(label("选择一个游戏开始。本地会自动保存关卡、分数和进度。", 16, false), fullWidth());
        addHomeButton("猪了个猪", "随机猪群冲刺解谜：点击小猪沿箭头冲出围栏，全部离场即可过关。", this::showPigRushGame);
        addHomeButton("羊了个羊", "羊主题三消，更多随机牌组，失败后可重开本关。", () -> showTileGame("sheep", "羊了个羊", SHEEP_SYMBOLS, 8));
        addHomeButton("跳一跳", "看准能量条落在绿色区域，连续跳台阶过关。", this::showJumpGame);
        addHomeButton("合成大西瓜", "点击空格落水果，相邻同水果会合成更大的水果。", this::showWatermelonGame);
        addHomeButton("合成 2048", "方向按钮移动数字，合成 2048 或更高分。", this::show2048Game);
    }

    private void addHomeButton(String title, String desc, Runnable action) {
        Button button = new Button(this);
        button.setText(title + "\n" + desc);
        button.setAllCaps(false);
        button.setTextSize(17);
        button.setPadding(dp(10), dp(12), dp(10), dp(12));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = fullWidth();
        params.setMargins(0, dp(10), 0, 0);
        root.addView(button, params);
    }

    private void addBackButton() {
        Button back = new Button(this);
        back.setText("返回游戏选择");
        back.setAllCaps(false);
        back.setOnClickListener(v -> showHome());
        root.addView(back, fullWidth());
    }

    private TextView label(String text, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(Color.rgb(69, 48, 36));
        view.setPadding(0, dp(6), 0, dp(6));
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return view;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int getProgress(String key, int fallback) {
        return prefs.getInt(key, fallback);
    }

    private void saveProgress(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    private void showPigRushGame() {
        int level = getProgress("pig_level", 1);
        PigRushState state = new PigRushState(level);
        renderPigRushGame(state);
    }

    private void renderPigRushGame(PigRushState state) {
        setRoot("猪了个猪  第 " + state.level + " 关");
        TextView status = label(pigRushStatus(state), 16, false);
        root.addView(label("点击任意小猪，它会沿身上的箭头方向向前冲；前方有猪会停在阻挡前，没有阻挡就冲出围栏。", 16, false), fullWidth());
        root.addView(status, fullWidth());

        PigRushBoardView board = new PigRushBoardView(state, status);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(PIG_BOARD_DP));
        boardParams.setMargins(0, dp(8), 0, dp(10));
        root.addView(board, boardParams);

        Button restart = new Button(this);
        restart.setText("重开本关（重新随机猪群）");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> renderPigRushGame(new PigRushState(state.level)));
        root.addView(restart, fullWidth());
        addBackButton();
    }

    private String pigRushStatus(PigRushState state) {
        return "剩余小猪：" + state.remaining + "/" + state.total + "。全部冲出围栏即可过关。";
    }

    private void handlePigRushTap(PigRushState state, PigRushBoardView board, TextView status, int row, int col) {
        Pig pig = state.pigAt(row, col);
        if (pig == null) return;

        int dr = PigRushState.DIR_ROWS[pig.direction];
        int dc = PigRushState.DIR_COLS[pig.direction];
        int nextRow = pig.row + dr;
        int nextCol = pig.col + dc;
        int steps = 0;
        while (state.isInside(nextRow, nextCol)) {
            if (state.pigAt(nextRow, nextCol) != null) break;
            steps++;
            nextRow += dr;
            nextCol += dc;
        }

        if (!state.isInside(nextRow, nextCol)) {
            pig.active = false;
            state.remaining--;
            Toast.makeText(this, "小猪冲出去了！", Toast.LENGTH_SHORT).show();
        } else if (steps > 0) {
            pig.row += dr * steps;
            pig.col += dc * steps;
            Toast.makeText(this, "前方被挡住，小猪停下了", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "前方紧贴着小猪，冲不动", Toast.LENGTH_SHORT).show();
        }

        status.setText(pigRushStatus(state));
        board.invalidate();
        if (state.remaining == 0) {
            int next = state.level + 1;
            saveProgress("pig_level", next);
            new AlertDialog.Builder(this)
                    .setTitle("过关啦")
                    .setMessage("所有小猪都冲出围栏了！下一关：" + next)
                    .setPositiveButton("下一关", (d, w) -> renderPigRushGame(new PigRushState(next)))
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        }
    }

    private void showTileGame(String key, String name, String[] symbols, int trayLimit) {
        int level = getProgress(key + "_level", 1);
        TileGameState state = new TileGameState(key, name, symbols, level, trayLimit);
        renderTileGame(state);
    }

    private void renderTileGame(TileGameState state) {
        setRoot(state.name + "  第 " + state.level + " 关");
        TextView status = label("消除所有牌即可过关。槽位上限：" + state.trayLimit + "。当前槽：空", 16, false);
        root.addView(status, fullWidth());

        LinearLayout toolRow = new LinearLayout(this);
        toolRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(toolRow, fullWidth());
        addToolButton(toolRow, "撤回", () -> askMathThen("使用撤回", () -> tileUndo(state, status)));
        addToolButton(toolRow, "洗牌", () -> askMathThen("使用洗牌", () -> tileShuffle(state)));
        addToolButton(toolRow, "移除", () -> askMathThen("使用移除", () -> tileRemove(state, status)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);
        grid.setUseDefaultMargins(true);
        root.addView(grid, fullWidth());
        state.buttons.clear();
        for (String symbol : state.deck) {
            Button tile = new Button(this);
            tile.setText(symbol);
            tile.setTextSize(24);
            tile.setAllCaps(false);
            tile.setOnClickListener(v -> tilePick(state, tile, status));
            state.buttons.add(tile);
            grid.addView(tile, new ViewGroupParams(dp(TILE_GAME_CELL_DP), dp(TILE_GAME_CELL_DP)));
        }

        Button restart = new Button(this);
        restart.setText("重开本关（重新随机牌面）");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> renderTileGame(new TileGameState(state.key, state.name, state.symbols, state.level, state.trayLimit)));
        root.addView(restart, fullWidth());
        addBackButton();
    }

    private void addToolButton(LinearLayout row, String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        row.addView(button, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
    }

    private void tilePick(TileGameState state, Button tile, TextView status) {
        String symbol = tile.getText().toString();
        tile.setEnabled(false);
        tile.setAlpha(0.35f);
        state.lastButton = tile;
        state.lastSymbol = symbol;
        state.tray.add(symbol);
        removeTriples(state.tray);
        updateTileStatus(state, status);
        if (allTilesCleared(state) && state.tray.isEmpty()) {
            int next = state.level + 1;
            saveProgress(state.key + "_level", next);
            new AlertDialog.Builder(this)
                    .setTitle("过关啦")
                    .setMessage("已保存进度，下一关：" + next)
                    .setPositiveButton("下一关", (d, w) -> renderTileGame(new TileGameState(state.key, state.name, state.symbols, next, state.trayLimit)))
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        } else if (state.tray.size() >= state.trayLimit) {
            new AlertDialog.Builder(this)
                    .setTitle("本关失败")
                    .setMessage("槽位满了，可以重开本关，牌面会重新随机。")
                    .setPositiveButton("重开", (d, w) -> renderTileGame(new TileGameState(state.key, state.name, state.symbols, state.level, state.trayLimit)))
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        }
    }

    private void tileUndo(TileGameState state, TextView status) {
        if (state.lastButton == null || state.lastButton.isEnabled() || state.lastSymbol == null) {
            Toast.makeText(this, "没有可撤回的牌", Toast.LENGTH_SHORT).show();
            return;
        }
        if (state.tray.remove(state.lastSymbol)) {
            state.lastButton.setEnabled(true);
            state.lastButton.setAlpha(1f);
            state.lastButton = null;
            state.lastSymbol = null;
            updateTileStatus(state, status);
        } else {
            Toast.makeText(this, "该牌已组成三消，不能撤回", Toast.LENGTH_SHORT).show();
        }
    }

    private void tileShuffle(TileGameState state) {
        List<Button> enabled = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        for (Button button : state.buttons) {
            if (button.isEnabled()) {
                enabled.add(button);
                texts.add(button.getText().toString());
            }
        }
        Collections.shuffle(texts, random);
        for (int i = 0; i < enabled.size(); i++) enabled.get(i).setText(texts.get(i));
        Toast.makeText(this, "剩余牌已洗牌", Toast.LENGTH_SHORT).show();
    }

    private void tileRemove(TileGameState state, TextView status) {
        if (!state.tray.isEmpty()) {
            state.tray.remove(0);
            updateTileStatus(state, status);
            Toast.makeText(this, "已移除槽内一张牌", Toast.LENGTH_SHORT).show();
            return;
        }
        for (Button button : state.buttons) {
            if (button.isEnabled()) {
                button.setEnabled(false);
                button.setAlpha(0.35f);
                Toast.makeText(this, "已移除牌面一张牌", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    private void updateTileStatus(TileGameState state, TextView status) {
        status.setText("消除所有牌即可过关。槽位上限：" + state.trayLimit + "。当前槽：" + (state.tray.isEmpty() ? "空" : state.tray.toString()));
    }

    private void removeTriples(List<String> tray) {
        boolean changed;
        do {
            changed = false;
            Map<String, Integer> counts = new HashMap<>();
            for (String item : tray) counts.put(item, counts.getOrDefault(item, 0) + 1);
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() >= 3) {
                    for (int i = 0; i < 3; i++) tray.remove(entry.getKey());
                    changed = true;
                    break;
                }
            }
        } while (changed);
    }

    private boolean allTilesCleared(TileGameState state) {
        for (Button button : state.buttons) if (button.isEnabled()) return false;
        return true;
    }

    private void askMathThen(String title, Runnable onSuccess) {
        boolean plus = random.nextBoolean();
        int a;
        int b;
        if (plus) {
            a = random.nextInt(101);
            b = random.nextInt(101 - a);
        } else {
            a = random.nextInt(101);
            b = random.nextInt(a + 1);
        }
        int answer = plus ? a + b : a - b;
        String question = a + (plus ? " + " : " - ") + b + " = ?";
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("答对 100 以内加减法才能使用道具：\n" + question)
                .setView(input)
                .setPositiveButton("提交", (dialog, which) -> {
                    try {
                        if (Integer.parseInt(input.getText().toString().trim()) == answer) {
                            onSuccess.run();
                        } else {
                            Toast.makeText(this, "答案不对，道具未生效", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException ex) {
                        Toast.makeText(this, "请输入数字", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showJumpGame() {
        int level = getProgress("jump_level", 1);
        setRoot("跳一跳  第 " + level + " 关");
        TextView hint = label("能量条会来回移动，点“跳！”时停在绿色区就成功。每关需要连续成功 5 次。", 16, false);
        TextView progress = label("本关进度：0/5", 18, true);
        TextView meter = label("▁▁▁▁▁▁▁▁▁▁", 34, true);
        meter.setGravity(Gravity.CENTER);
        root.addView(hint, fullWidth());
        root.addView(progress, fullWidth());
        root.addView(meter, fullWidth());
        JumpState state = new JumpState(level);
        Button jump = new Button(this);
        jump.setText("跳！");
        jump.setTextSize(22);
        jump.setAllCaps(false);
        jump.setOnClickListener(v -> {
            int targetStart = 3 + state.level % 3;
            boolean success = state.position >= targetStart && state.position <= targetStart + 2;
            if (success) state.successes++; else state.successes = 0;
            progress.setText("本关进度：" + state.successes + "/5" + (success ? "  成功" : "  失误，重新累计"));
            if (state.successes >= 5) {
                int next = state.level + 1;
                saveProgress("jump_level", next);
                new AlertDialog.Builder(this)
                        .setTitle("跳过本关")
                        .setMessage("下一关：" + next)
                        .setPositiveButton("下一关", (d, w) -> showJumpGame())
                        .setNegativeButton("返回", (d, w) -> showHome())
                        .show();
            }
        });
        root.addView(jump, fullWidth());
        addBackButton();
        jumpTicker = new Runnable() {
            @Override public void run() {
                state.position += state.direction;
                if (state.position <= 0 || state.position >= 9) state.direction *= -1;
                int targetStart = 3 + state.level % 3;
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < 10; i++) {
                    if (i == state.position) builder.append("▲");
                    else if (i >= targetStart && i <= targetStart + 2) builder.append("▰");
                    else builder.append("▱");
                }
                meter.setText(builder.toString());
                handler.postDelayed(this, Math.max(MIN_JUMP_TICK_MS, BASE_JUMP_TICK_MS - state.level * JUMP_LEVEL_SPEED_STEP_MS));
            }
        };
        handler.post(jumpTicker);
    }

    private void showWatermelonGame() {
        setRoot("合成大西瓜");
        int best = getProgress("watermelon_best", 0);
        WatermelonState state = new WatermelonState(best);
        TextView score = label("得分：0  最高：" + best, 18, true);
        root.addView(label("点击空格放入随机小水果；相邻同水果会自动合成。棋盘满时可重新开始。", 16, false), fullWidth());
        root.addView(score, fullWidth());
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);
        grid.setUseDefaultMargins(true);
        root.addView(grid, fullWidth());
        Button[][] cells = new Button[5][5];
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Button cell = new Button(this);
                cell.setText("＋");
                cell.setTextSize(22);
                int row = r;
                int col = c;
                cell.setOnClickListener(v -> watermelonTap(state, cells, row, col, score));
                cells[r][c] = cell;
                grid.addView(cell, new ViewGroupParams(dp(WATERMELON_CELL_DP), dp(WATERMELON_CELL_DP)));
            }
        }
        Button restart = new Button(this);
        restart.setText("重新开始");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> showWatermelonGame());
        root.addView(restart, fullWidth());
        addBackButton();
    }

    private void watermelonTap(WatermelonState state, Button[][] cells, int row, int col, TextView scoreLabel) {
        if (state.board[row][col] != 0) return;
        state.board[row][col] = random.nextInt(3) + 1;
        mergeAround(state, row, col);
        drawWatermelon(state, cells, scoreLabel);
        if (watermelonFull(state)) {
            saveProgress("watermelon_best", Math.max(getProgress("watermelon_best", 0), state.score));
            new AlertDialog.Builder(this)
                    .setTitle("棋盘已满")
                    .setMessage("本局得分：" + state.score)
                    .setPositiveButton("再来一局", (d, w) -> showWatermelonGame())
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        }
    }

    private void mergeAround(WatermelonState state, int row, int col) {
        boolean changed;
        do {
            changed = false;
            int value = state.board[row][col];
            int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
            for (int[] dir : dirs) {
                int nr = row + dir[0];
                int nc = col + dir[1];
                if (nr >= 0 && nr < 5 && nc >= 0 && nc < 5 && state.board[nr][nc] == value && value < FRUITS.length) {
                    state.board[nr][nc] = 0;
                    state.board[row][col] = value + 1;
                    state.score += value * 10;
                    changed = true;
                    break;
                }
            }
        } while (changed);
    }

    private void drawWatermelon(WatermelonState state, Button[][] cells, TextView scoreLabel) {
        int best = Math.max(getProgress("watermelon_best", 0), state.score);
        saveProgress("watermelon_best", best);
        scoreLabel.setText("得分：" + state.score + "  最高：" + best);
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                int value = state.board[r][c];
                cells[r][c].setText(value == 0 ? "＋" : FRUITS[value - 1]);
            }
        }
    }

    private boolean watermelonFull(WatermelonState state) {
        for (int[] row : state.board) for (int value : row) if (value == 0) return false;
        return true;
    }

    private void show2048Game() {
        setRoot("合成 2048");
        Game2048State state = new Game2048State(getProgress("2048_best", 0));
        add2048Tile(state);
        add2048Tile(state);
        TextView score = label("得分：0  最高：" + state.best, 18, true);
        root.addView(label("用方向按钮移动数字；相同数字碰撞会合成。", 16, false), fullWidth());
        root.addView(score, fullWidth());
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setUseDefaultMargins(true);
        root.addView(grid, fullWidth());
        Button[][] cells = new Button[4][4];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                Button cell = new Button(this);
                cell.setTextSize(20);
                cell.setEnabled(false);
                cells[r][c] = cell;
                grid.addView(cell, new ViewGroupParams(dp(GAME_2048_CELL_DP), dp(GAME_2048_CELL_DP)));
            }
        }
        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER);
        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER);
        root.addView(row1, fullWidth());
        root.addView(row2, fullWidth());
        addMoveButton(row1, "上", () -> move2048(state, 0, -1, cells, score));
        addMoveButton(row2, "左", () -> move2048(state, -1, 0, cells, score));
        addMoveButton(row2, "下", () -> move2048(state, 0, 1, cells, score));
        addMoveButton(row2, "右", () -> move2048(state, 1, 0, cells, score));
        Button restart = new Button(this);
        restart.setText("重新开始");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> show2048Game());
        root.addView(restart, fullWidth());
        addBackButton();
        draw2048(state, cells, score);
    }

    private void addMoveButton(LinearLayout row, String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(20);
        button.setOnClickListener(v -> action.run());
        row.addView(button, new LinearLayout.LayoutParams(dp(88), LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void add2048Tile(Game2048State state) {
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) if (state.board[r][c] == 0) empty.add(new int[]{r, c});
        if (empty.isEmpty()) return;
        int[] chosen = empty.get(random.nextInt(empty.size()));
        state.board[chosen[0]][chosen[1]] = random.nextInt(10) == 0 ? 4 : 2;
    }

    private void move2048(Game2048State state, int dx, int dy, Button[][] cells, TextView score) {
        boolean moved = false;
        boolean[][] merged = new boolean[4][4];
        int startR = dy > 0 ? 2 : 1;
        int endR = dy > 0 ? -1 : 4;
        int stepR = dy > 0 ? -1 : 1;
        int startC = dx > 0 ? 2 : 1;
        int endC = dx > 0 ? -1 : 4;
        int stepC = dx > 0 ? -1 : 1;
        if (dy != 0) {
            for (int r = startR; r != endR; r += stepR) for (int c = 0; c < 4; c++) moved |= slide2048(state, merged, r, c, dx, dy);
        } else {
            for (int c = startC; c != endC; c += stepC) for (int r = 0; r < 4; r++) moved |= slide2048(state, merged, r, c, dx, dy);
        }
        if (moved) add2048Tile(state);
        draw2048(state, cells, score);
        if (!canMove2048(state)) {
            new AlertDialog.Builder(this)
                    .setTitle("没有可移动格子")
                    .setMessage("最终得分：" + state.score)
                    .setPositiveButton("再来一局", (d, w) -> show2048Game())
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        }
    }

    private boolean slide2048(Game2048State state, boolean[][] merged, int r, int c, int dx, int dy) {
        if (state.board[r][c] == 0) return false;
        boolean moved = false;
        int cr = r;
        int cc = c;
        while (true) {
            int nr = cr + dy;
            int nc = cc + dx;
            if (nr < 0 || nr >= 4 || nc < 0 || nc >= 4) break;
            if (state.board[nr][nc] == 0) {
                state.board[nr][nc] = state.board[cr][cc];
                state.board[cr][cc] = 0;
                cr = nr;
                cc = nc;
                moved = true;
            } else if (state.board[nr][nc] == state.board[cr][cc] && !merged[nr][nc]) {
                state.board[nr][nc] *= 2;
                state.board[cr][cc] = 0;
                merged[nr][nc] = true;
                state.score += state.board[nr][nc];
                moved = true;
                break;
            } else break;
        }
        return moved;
    }

    private void draw2048(Game2048State state, Button[][] cells, TextView scoreLabel) {
        state.best = Math.max(state.best, state.score);
        saveProgress("2048_best", state.best);
        scoreLabel.setText("得分：" + state.score + "  最高：" + state.best);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int value = state.board[r][c];
                cells[r][c].setText(value == 0 ? "" : String.format(Locale.getDefault(), "%d", value));
                cells[r][c].setBackgroundColor(value == 0 ? Color.rgb(237, 229, 218) : Color.rgb(255, Math.max(140, 240 - value % 120), 120));
            }
        }
    }

    private boolean canMove2048(Game2048State state) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (state.board[r][c] == 0) return true;
                if (r < 3 && state.board[r][c] == state.board[r + 1][c]) return true;
                if (c < 3 && state.board[r][c] == state.board[r][c + 1]) return true;
            }
        }
        return false;
    }

    private static class ViewGroupParams extends GridLayout.LayoutParams {
        ViewGroupParams(int width, int height) {
            super();
            this.width = width;
            this.height = height;
            setMargins(4, 4, 4, 4);
        }
    }

    private class PigRushBoardView extends View {
        private final PigRushState state;
        private final TextView status;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF boardRect = new RectF();
        private float cellSize;

        PigRushBoardView(PigRushState state, TextView status) {
            super(MainActivity.this);
            this.state = state;
            this.status = status;
            setBackgroundColor(Color.rgb(255, 248, 239));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float side = Math.min(getWidth() - dp(24), getHeight() - dp(20));
            float left = (getWidth() - side) / 2f;
            float top = (getHeight() - side) / 2f;
            boardRect.set(left, top, left + side, top + side);
            cellSize = side / PIG_GRID_SIZE;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(120, 196, 145));
            canvas.drawRoundRect(boardRect, dp(24), dp(24), paint);
            paint.setColor(Color.rgb(146, 213, 161));
            canvas.drawRoundRect(new RectF(boardRect.left + dp(8), boardRect.top + dp(8), boardRect.right - dp(8), boardRect.bottom - dp(8)), dp(18), dp(18), paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.argb(82, 67, 123, 74));
            for (int i = 1; i < PIG_GRID_SIZE; i++) {
                float offset = boardRect.left + cellSize * i;
                canvas.drawLine(offset, boardRect.top + dp(10), offset, boardRect.bottom - dp(10), paint);
                float row = boardRect.top + cellSize * i;
                canvas.drawLine(boardRect.left + dp(10), row, boardRect.right - dp(10), row, paint);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(cellSize * 0.24f);
            paint.setColor(Color.rgb(250, 255, 236));
            canvas.drawText("出口", boardRect.centerX(), boardRect.top + cellSize * 0.45f, paint);
            canvas.drawText("出口", boardRect.centerX(), boardRect.bottom - cellSize * 0.25f, paint);
            canvas.drawText("出口", boardRect.left + cellSize * 0.55f, boardRect.centerY(), paint);
            canvas.drawText("出口", boardRect.right - cellSize * 0.55f, boardRect.centerY(), paint);

            for (Pig pig : state.pigs) {
                if (pig.active) drawPig(canvas, pig);
            }
        }

        private void drawPig(Canvas canvas, Pig pig) {
            float cx = boardRect.left + pig.col * cellSize + cellSize / 2f;
            float cy = boardRect.top + pig.row * cellSize + cellSize / 2f;
            float radius = cellSize * 0.32f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(55, 80, 62, 44));
            canvas.drawOval(new RectF(cx - radius * 0.95f, cy + radius * 0.56f, cx + radius * 0.95f, cy + radius * 1.0f), paint);

            paint.setColor(Color.rgb(255, 154, 181));
            canvas.drawCircle(cx - radius * 0.58f, cy - radius * 0.52f, radius * 0.34f, paint);
            canvas.drawCircle(cx + radius * 0.58f, cy - radius * 0.52f, radius * 0.34f, paint);
            paint.setColor(Color.rgb(255, 186, 203));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setColor(Color.rgb(255, 129, 161));
            canvas.drawOval(new RectF(cx - radius * 0.42f, cy - radius * 0.05f, cx + radius * 0.42f, cy + radius * 0.42f), paint);
            paint.setColor(Color.rgb(82, 48, 54));
            canvas.drawCircle(cx - radius * 0.32f, cy - radius * 0.2f, radius * 0.08f, paint);
            canvas.drawCircle(cx + radius * 0.32f, cy - radius * 0.2f, radius * 0.08f, paint);
            paint.setColor(Color.rgb(123, 65, 73));
            canvas.drawCircle(cx - radius * 0.16f, cy + radius * 0.18f, radius * 0.06f, paint);
            canvas.drawCircle(cx + radius * 0.16f, cy + radius * 0.18f, radius * 0.06f, paint);

            paint.setTextSize(radius * 0.95f);
            paint.setFakeBoldText(true);
            paint.setColor(Color.rgb(86, 83, 94));
            canvas.drawText(PigRushState.ARROWS[pig.direction], cx, cy + radius * 1.55f, paint);
            paint.setFakeBoldText(false);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP || !boardRect.contains(event.getX(), event.getY())) {
                return true;
            }
            int col = (int) ((event.getX() - boardRect.left) / cellSize);
            int row = (int) ((event.getY() - boardRect.top) / cellSize);
            handlePigRushTap(state, this, status, row, col);
            return true;
        }
    }

    private class PigRushState {
        static final int[] DIR_ROWS = {-1, 0, 1, 0};
        static final int[] DIR_COLS = {0, 1, 0, -1};
        static final String[] ARROWS = {"↑", "→", "↓", "←"};
        final int level;
        final int total;
        final List<Pig> pigs = new ArrayList<>();
        int remaining;

        PigRushState(int level) {
            this.level = level;
            total = Math.min(PIG_MAX_COUNT, PIG_BASE_COUNT + (level - 1) * PIG_LEVEL_COUNT_STEP);
            remaining = total;
            boolean[][] used = new boolean[PIG_GRID_SIZE][PIG_GRID_SIZE];
            for (int i = 0; i < total; i++) {
                int row;
                int col;
                do {
                    row = random.nextInt(PIG_GRID_SIZE);
                    col = random.nextInt(PIG_GRID_SIZE);
                } while (used[row][col]);
                used[row][col] = true;
                pigs.add(new Pig(row, col, random.nextInt(4)));
            }
        }

        boolean isInside(int row, int col) {
            return row >= 0 && row < PIG_GRID_SIZE && col >= 0 && col < PIG_GRID_SIZE;
        }

        Pig pigAt(int row, int col) {
            for (Pig pig : pigs) {
                if (pig.active && pig.row == row && pig.col == col) return pig;
            }
            return null;
        }
    }

    private static class Pig {
        int row;
        int col;
        final int direction;
        boolean active = true;

        Pig(int row, int col, int direction) {
            this.row = row;
            this.col = col;
            this.direction = direction;
        }
    }

    private class TileGameState {
        final String key;
        final String name;
        final String[] symbols;
        final int level;
        final int trayLimit;
        final List<String> deck = new ArrayList<>();
        final List<Button> buttons = new ArrayList<>();
        final List<String> tray = new ArrayList<>();
        Button lastButton;
        String lastSymbol;

        TileGameState(String key, String name, String[] symbols, int level, int trayLimit) {
            this.key = key;
            this.name = name;
            this.symbols = symbols;
            this.level = level;
            this.trayLimit = trayLimit;
            int groups = Math.min(symbols.length, TILE_BASE_GROUPS + level % TILE_LEVEL_GROUP_CYCLE + random.nextInt(TILE_RANDOM_EXTRA_GROUPS));
            List<String> pool = new ArrayList<>();
            Collections.addAll(pool, symbols);
            Collections.shuffle(pool, random);
            for (int i = 0; i < groups; i++) {
                for (int j = 0; j < 3; j++) deck.add(pool.get(i));
            }
            Collections.shuffle(deck, random);
        }
    }

    private static class JumpState {
        final int level;
        int position;
        int direction = 1;
        int successes;
        JumpState(int level) { this.level = level; }
    }

    private static class WatermelonState {
        final int[][] board = new int[5][5];
        final int best;
        int score;
        WatermelonState(int best) { this.best = best; }
    }

    private static class Game2048State {
        final int[][] board = new int[4][4];
        int score;
        int best;
        Game2048State(int best) { this.best = best; }
    }
}
