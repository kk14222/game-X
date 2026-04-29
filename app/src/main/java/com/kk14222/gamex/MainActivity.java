package com.kk14222.gamex;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    private static final int PIG_BASE_COUNT = 18;
    private static final int PIG_LEVEL_COUNT_STEP = 3;
    private static final int PIG_MAX_COUNT = 42;
    private static final int PIG_DIRECTION_COUNT = 8;
    // Direction indexes are N, NE, E, SE, S, SW, W, NW; rows, cols, and angles must stay aligned.
    private static final int[] PIG_DIR_ROWS = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static final int[] PIG_DIR_COLS = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final float[] PIG_DIR_ANGLES = {0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f};
    private static final float PIG_BACKGROUND_SUN_X = 0.9f;
    private static final float PIG_BACKGROUND_SUN_Y = 0.75f;
    private static final float PIG_BACKGROUND_SUN_RADIUS = 0.34f;
    private static final int PIG_GRASS_STRIPE_OVERFLOW = 2;
    private static final int SHEEP_TRAY_LIMIT = 7;
    private static final int SHEEP_BOARD_DP = 420;
    private static final int SHEEP_LAYERS = 3;
    private static final int WATERMELON_BOARD_DP = 520;
    private static final int WATERMELON_ROWS = 10;
    private static final int WATERMELON_COLS = 7;
    private static final String[] WATERMELON_SYMBOLS = {"🍒", "🍓", "🍇", "🍊", "🍎", "🍐", "🍑", "🍍", "🍈", "🍉"};
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
        addHomeButton("猪了个猪", "随机猪群冲刺解谜：点击朝向不同的小猪冲出围栏，全部离场即可过关。", this::showPigRushGame);
        addHomeButton("羊了个羊", "多层叠牌三消：只能点击未被覆盖的牌，七槽满即失败。", this::showSheepGame);
        addHomeButton("跳一跳", "按住屏幕蓄力，松开跳向下一个方块，中心落点加分。", this::showJumpGame);
        addHomeButton("合成大西瓜", "从顶部选择位置投放水果，相同水果碰撞合成更大水果。", this::showWatermelonGame);
        addHomeButton("合成 2048", "滑动棋盘移动数字，合成 2048 后可继续挑战。", this::show2048Game);
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
        root.addView(label("点击任意小猪，它会沿自己脸朝向的方向冲刺（可斜向）；前方有猪会停在阻挡前，没有阻挡就冲出围栏。", 16, false), fullWidth());
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

        int dr = PIG_DIR_ROWS[pig.direction];
        int dc = PIG_DIR_COLS[pig.direction];
        int targetRow = pig.row;
        int targetCol = pig.col;
        int scanRow = pig.row + dr;
        int scanCol = pig.col + dc;
        while (state.isInside(scanRow, scanCol)) {
            if (state.pigAt(scanRow, scanCol) != null) break;
            targetRow = scanRow;
            targetCol = scanCol;
            scanRow += dr;
            scanCol += dc;
        }

        if (!state.isInside(scanRow, scanCol)) {
            state.board[pig.row][pig.col] = null;
            pig.active = false;
            state.remaining--;
            Toast.makeText(this, "小猪冲出去了！", Toast.LENGTH_SHORT).show();
        } else if (targetRow != pig.row || targetCol != pig.col) {
            state.board[pig.row][pig.col] = null;
            pig.row = targetRow;
            pig.col = targetCol;
            state.board[pig.row][pig.col] = pig;
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

    private void showSheepGame() {
        int level = getProgress("sheep_level", 1);
        SheepGameState state = new SheepGameState(level);
        renderSheepGame(state);
    }

    private void renderSheepGame(SheepGameState state) {
        setRoot("羊了个羊  第 " + state.level + " 关");
        TextView status = label(sheepStatus(state), 16, false);
        root.addView(label("只能点击没有被上层牌覆盖的牌；牌进入下方 7 个槽位，凑齐 3 张同款会自动消除。", 16, false), fullWidth());
        root.addView(status, fullWidth());
        SheepBoardView board = new SheepBoardView(state, status);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(SHEEP_BOARD_DP));
        boardParams.setMargins(0, dp(8), 0, dp(10));
        root.addView(board, boardParams);
        Button restart = new Button(this);
        restart.setText("重开本关（重新随机叠牌）");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> renderSheepGame(new SheepGameState(state.level)));
        root.addView(restart, fullWidth());
        addBackButton();
    }

    private String sheepStatus(SheepGameState state) {
        return "剩余牌：" + state.remaining + "  槽位：" + (state.tray.isEmpty() ? "空" : state.tray.toString()) + " / " + SHEEP_TRAY_LIMIT;
    }

    private void handleSheepTap(SheepGameState state, SheepBoardView board, TextView status, SheepTile tile) {
        if (tile == null || !tile.active) return;
        if (state.isCovered(tile)) {
            Toast.makeText(this, "这张牌被上层压住了", Toast.LENGTH_SHORT).show();
            return;
        }
        tile.active = false;
        state.remaining--;
        state.tray.add(tile.symbol);
        removeTriples(state.tray);
        status.setText(sheepStatus(state));
        board.invalidate();
        if (state.remaining == 0 && state.tray.isEmpty()) {
            int next = state.level + 1;
            saveProgress("sheep_level", next);
            new AlertDialog.Builder(this)
                    .setTitle("过关啦")
                    .setMessage("叠牌全部消除，下一关：" + next)
                    .setPositiveButton("下一关", (d, w) -> renderSheepGame(new SheepGameState(next)))
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        } else if (state.tray.size() >= SHEEP_TRAY_LIMIT) {
            new AlertDialog.Builder(this)
                    .setTitle("本关失败")
                    .setMessage("7 个槽位已经放满，可以重开本关。")
                    .setPositiveButton("重开", (d, w) -> renderSheepGame(new SheepGameState(state.level)))
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
        setRoot("跳一跳");
        JumpState state = new JumpState(getProgress("jump_best", 0));
        TextView score = label(jumpStatus(state), 18, true);
        root.addView(label("按住下方画面蓄力，松开后小人会向下一个方块跳跃；落在中心可获得额外分数。", 16, false), fullWidth());
        root.addView(score, fullWidth());
        JumpGameView board = new JumpGameView(state, score);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(420));
        boardParams.setMargins(0, dp(8), 0, dp(10));
        root.addView(board, boardParams);
        Button restart = new Button(this);
        restart.setText("重新开始");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> showJumpGame());
        root.addView(restart, fullWidth());
        addBackButton();
    }

    private String jumpStatus(JumpState state) {
        String suffix = state.message.isEmpty() ? "" : "  " + state.message;
        return "得分：" + state.score + "  最高：" + state.best + suffix;
    }

    private void finishJump(JumpState state, JumpGameView board, TextView scoreLabel, long chargeMs) {
        if (state.gameOver) return;
        state.charge = 0f;
        float jumpDistance = Math.min(1.45f, chargeMs / 720f) * state.targetDistance;
        float diff = Math.abs(jumpDistance - state.targetDistance);
        float safeRange = 0.18f + state.targetSize * 0.35f;
        if (diff <= safeRange) {
            boolean perfect = diff <= 0.08f;
            state.score += perfect ? 2 : 1;
            state.best = Math.max(state.best, state.score);
            saveProgress("jump_best", state.best);
            state.message = perfect ? "Perfect +2" : "+1";
            state.targetDistance = 0.65f + random.nextFloat() * 0.75f;
            state.targetSize = 0.34f + random.nextFloat() * 0.2f;
        } else {
            state.gameOver = true;
            state.message = jumpDistance < state.targetDistance ? "跳短了" : "跳远了";
            new AlertDialog.Builder(this)
                    .setTitle("游戏结束")
                    .setMessage("本局得分：" + state.score)
                    .setPositiveButton("再来一局", (d, w) -> showJumpGame())
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        }
        scoreLabel.setText(jumpStatus(state));
        board.invalidate();
    }

    private void showWatermelonGame() {
        setRoot("合成大西瓜");
        WatermelonState state = new WatermelonState(getProgress("watermelon_best", 0));
        TextView score = label(watermelonStatus(state), 18, true);
        root.addView(label("点击容器上方选择投放位置，水果会从顶部落下；相邻同水果会合成更大的水果，超过警戒线则结束。", 16, false), fullWidth());
        root.addView(score, fullWidth());
        WatermelonBoardView board = new WatermelonBoardView(state, score);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(WATERMELON_BOARD_DP));
        boardParams.setMargins(0, dp(8), 0, dp(10));
        root.addView(board, boardParams);
        Button restart = new Button(this);
        restart.setText("重新开始");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> showWatermelonGame());
        root.addView(restart, fullWidth());
        addBackButton();
    }

    private String watermelonStatus(WatermelonState state) {
        return "得分：" + state.score + "  最高：" + state.best + "  下一个：" + WATERMELON_SYMBOLS[state.nextFruit];
    }

    private void dropWatermelon(WatermelonState state, WatermelonBoardView board, TextView scoreLabel, int col) {
        if (state.gameOver || col < 0 || col >= WATERMELON_COLS) return;
        int row = WATERMELON_ROWS - 1;
        while (row >= 0 && state.board[row][col] != 0) row--;
        if (row < 0) {
            endWatermelon(state, scoreLabel);
            return;
        }
        state.board[row][col] = state.nextFruit + 1;
        mergeWatermelon(state, row, col);
        state.nextFruit = random.nextInt(5);
        state.best = Math.max(state.best, state.score);
        saveProgress("watermelon_best", state.best);
        scoreLabel.setText(watermelonStatus(state));
        board.invalidate();
        if (watermelonOverLine(state)) endWatermelon(state, scoreLabel);
    }

    private void mergeWatermelon(WatermelonState state, int row, int col) {
        boolean changed;
        do {
            changed = false;
            int value = state.board[row][col];
            if (value <= 0 || value >= WATERMELON_SYMBOLS.length) return;
            int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
            for (int[] dir : dirs) {
                int nr = row + dir[0];
                int nc = col + dir[1];
                if (nr >= 0 && nr < WATERMELON_ROWS && nc >= 0 && nc < WATERMELON_COLS && state.board[nr][nc] == value) {
                    state.board[nr][nc] = 0;
                    state.board[row][col] = value + 1;
                    state.score += value * 10;
                    collapseWatermelon(state);
                    row = lowestInColumn(state, col, value + 1);
                    changed = true;
                    break;
                }
            }
        } while (changed);
    }

    private void collapseWatermelon(WatermelonState state) {
        for (int col = 0; col < WATERMELON_COLS; col++) {
            int write = WATERMELON_ROWS - 1;
            for (int row = WATERMELON_ROWS - 1; row >= 0; row--) {
                if (state.board[row][col] != 0) {
                    int value = state.board[row][col];
                    state.board[row][col] = 0;
                    state.board[write--][col] = value;
                }
            }
        }
    }

    private int lowestInColumn(WatermelonState state, int col, int value) {
        for (int row = WATERMELON_ROWS - 1; row >= 0; row--) {
            if (state.board[row][col] == value) return row;
        }
        return WATERMELON_ROWS - 1;
    }

    private boolean watermelonOverLine(WatermelonState state) {
        for (int col = 0; col < WATERMELON_COLS; col++) if (state.board[1][col] != 0) return true;
        return false;
    }

    private void endWatermelon(WatermelonState state, TextView scoreLabel) {
        if (state.gameOver) return;
        state.gameOver = true;
        state.best = Math.max(state.best, state.score);
        saveProgress("watermelon_best", state.best);
        scoreLabel.setText(watermelonStatus(state));
        new AlertDialog.Builder(this)
                .setTitle("游戏结束")
                .setMessage("水果超过顶部警戒线，本局得分：" + state.score)
                .setPositiveButton("再来一局", (d, w) -> showWatermelonGame())
                .setNegativeButton("返回", (d, w) -> showHome())
                .show();
    }

    private void show2048Game() {
        setRoot("合成 2048");
        Game2048State state = new Game2048State(getProgress("2048_best", 0));
        add2048Tile(state);
        add2048Tile(state);
        TextView score = label("得分：0  最高：" + state.best, 18, true);
        root.addView(label("在棋盘上滑动或使用方向按钮移动数字；相同数字碰撞会合成，出现 2048 即达成目标。", 16, false), fullWidth());
        root.addView(score, fullWidth());
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setUseDefaultMargins(true);
        Button[][] cells = new Button[4][4];
        final float[] down = new float[2];
        grid.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                down[0] = event.getX();
                down[1] = event.getY();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float dx = event.getX() - down[0];
                float dy = event.getY() - down[1];
                if (Math.max(Math.abs(dx), Math.abs(dy)) > dp(24)) {
                    if (Math.abs(dx) > Math.abs(dy)) move2048(state, dx > 0 ? 1 : -1, 0, cells, score);
                    else move2048(state, 0, dy > 0 ? 1 : -1, cells, score);
                }
                return true;
            }
            return true;
        });
        root.addView(grid, fullWidth());
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
        if (!state.won && has2048(state)) {
            state.won = true;
            new AlertDialog.Builder(this)
                    .setTitle("合成 2048！")
                    .setMessage("已达成原版目标，可以继续挑战更高分。")
                    .setPositiveButton("继续", null)
                    .setNegativeButton("重新开始", (d, w) -> show2048Game())
                    .show();
        }
        if (!canMove2048(state)) {
            new AlertDialog.Builder(this)
                    .setTitle("没有可移动格子")
                    .setMessage("最终得分：" + state.score)
                    .setPositiveButton("再来一局", (d, w) -> show2048Game())
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        }
    }

    private boolean has2048(Game2048State state) {
        for (int[] row : state.board) for (int value : row) if (value >= 2048) return true;
        return false;
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

            drawPigRushBackground(canvas);

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

        private void drawPigRushBackground(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(169, 224, 255));
            canvas.drawRoundRect(boardRect, dp(24), dp(24), paint);
            paint.setColor(Color.rgb(255, 246, 178));
            canvas.drawCircle(boardRect.right - cellSize * PIG_BACKGROUND_SUN_X, boardRect.top + cellSize * PIG_BACKGROUND_SUN_Y, cellSize * PIG_BACKGROUND_SUN_RADIUS, paint);
            paint.setColor(Color.argb(215, 255, 255, 255));
            drawCloud(canvas, 0.35f, 0.45f, 1.75f, 0.88f);
            drawCloud(canvas, 3.75f, 0.72f, 5.35f, 1.16f);

            Path hills = new Path();
            hills.moveTo(boardRect.left + dp(8), boardRect.top + cellSize * 1.75f);
            hills.cubicTo(boardRect.left + cellSize * 1.2f, boardRect.top + cellSize * 0.95f, boardRect.left + cellSize * 2.1f, boardRect.top + cellSize * 2.45f, boardRect.left + cellSize * 3.4f, boardRect.top + cellSize * 1.45f);
            hills.cubicTo(boardRect.left + cellSize * 4.45f, boardRect.top + cellSize * 0.78f, boardRect.left + cellSize * 5.25f, boardRect.top + cellSize * 2.25f, boardRect.right - dp(8), boardRect.top + cellSize * 1.4f);
            hills.lineTo(boardRect.right - dp(8), boardRect.bottom - dp(8));
            hills.lineTo(boardRect.left + dp(8), boardRect.bottom - dp(8));
            hills.close();
            paint.setColor(Color.rgb(107, 190, 120));
            canvas.drawPath(hills, paint);

            paint.setColor(Color.rgb(140, 210, 132));
            canvas.drawRoundRect(new RectF(boardRect.left + dp(8), boardRect.top + cellSize * 1.2f, boardRect.right - dp(8), boardRect.bottom - dp(8)), dp(18), dp(18), paint);
            paint.setColor(Color.argb(85, 255, 240, 170));
            for (int i = -PIG_GRASS_STRIPE_OVERFLOW; i < PIG_GRID_SIZE + PIG_GRASS_STRIPE_OVERFLOW; i += 2) {
                float x = boardRect.left + i * cellSize;
                canvas.drawOval(new RectF(x, boardRect.top + cellSize * 1.35f, x + cellSize * 2.4f, boardRect.bottom - cellSize * 0.15f), paint);
            }

            paint.setColor(Color.rgb(156, 108, 66));
            paint.setStrokeWidth(dp(5));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(new RectF(boardRect.left + dp(10), boardRect.top + dp(10), boardRect.right - dp(10), boardRect.bottom - dp(10)), dp(17), dp(17), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawCloud(Canvas canvas, float leftCell, float topCell, float rightCell, float bottomCell) {
            canvas.drawOval(new RectF(boardRect.left + cellSize * leftCell, boardRect.top + cellSize * topCell, boardRect.left + cellSize * rightCell, boardRect.top + cellSize * bottomCell), paint);
        }

        private void drawPig(Canvas canvas, Pig pig) {
            float cx = boardRect.left + pig.col * cellSize + cellSize / 2f;
            float cy = boardRect.top + pig.row * cellSize + cellSize / 2f;
            float radius = cellSize * 0.35f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(55, 80, 62, 44));
            canvas.drawOval(new RectF(cx - radius * 0.95f, cy + radius * 0.56f, cx + radius * 0.95f, cy + radius * 1.0f), paint);

            canvas.save();
            canvas.translate(cx, cy);
            canvas.rotate(PIG_DIR_ANGLES[pig.direction]);

            paint.setColor(Color.rgb(252, 135, 169));
            Path leftEar = new Path();
            leftEar.moveTo(-radius * 0.38f, -radius * 0.58f);
            leftEar.lineTo(-radius * 0.82f, -radius * 0.9f);
            leftEar.lineTo(-radius * 0.64f, -radius * 0.32f);
            leftEar.close();
            canvas.drawPath(leftEar, paint);
            Path rightEar = new Path();
            rightEar.moveTo(radius * 0.38f, -radius * 0.58f);
            rightEar.lineTo(radius * 0.82f, -radius * 0.9f);
            rightEar.lineTo(radius * 0.64f, -radius * 0.32f);
            rightEar.close();
            canvas.drawPath(rightEar, paint);

            paint.setColor(Color.rgb(255, 186, 203));
            canvas.drawOval(new RectF(-radius * 0.88f, -radius * 0.55f, radius * 0.88f, radius * 0.75f), paint);
            paint.setColor(Color.rgb(255, 202, 215));
            canvas.drawCircle(0, -radius * 0.33f, radius * 0.66f, paint);

            paint.setColor(Color.rgb(255, 128, 162));
            Path nose = new Path();
            nose.moveTo(0, -radius * 0.98f);
            nose.cubicTo(-radius * 0.44f, -radius * 0.82f, -radius * 0.44f, -radius * 0.45f, 0, -radius * 0.38f);
            nose.cubicTo(radius * 0.44f, -radius * 0.45f, radius * 0.44f, -radius * 0.82f, 0, -radius * 0.98f);
            nose.close();
            canvas.drawPath(nose, paint);

            paint.setColor(Color.rgb(82, 48, 54));
            canvas.drawCircle(-radius * 0.24f, -radius * 0.36f, radius * 0.08f, paint);
            canvas.drawCircle(radius * 0.24f, -radius * 0.36f, radius * 0.08f, paint);

            paint.setColor(Color.rgb(255, 129, 161));
            canvas.drawOval(new RectF(-radius * 0.24f, -radius * 0.82f, radius * 0.24f, -radius * 0.58f), paint);
            paint.setColor(Color.rgb(123, 65, 73));
            canvas.drawCircle(-radius * 0.08f, -radius * 0.7f, radius * 0.035f, paint);
            canvas.drawCircle(radius * 0.08f, -radius * 0.7f, radius * 0.035f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(radius * 0.1f);
            paint.setColor(Color.rgb(230, 107, 145));
            canvas.drawArc(new RectF(radius * 0.43f, radius * 0.25f, radius * 0.92f, radius * 0.74f), 160, 260, false, paint);
            paint.setStyle(Paint.Style.FILL);

            canvas.restore();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }
            if (!boardRect.contains(event.getX(), event.getY())) {
                return true;
            }
            int col = (int) ((event.getX() - boardRect.left) / cellSize);
            int row = (int) ((event.getY() - boardRect.top) / cellSize);
            handlePigRushTap(state, this, status, row, col);
            return true;
        }
    }

    private class PigRushState {
        final int level;
        final int total;
        final List<Pig> pigs = new ArrayList<>();
        final Pig[][] board = new Pig[PIG_GRID_SIZE][PIG_GRID_SIZE];
        int remaining;

        PigRushState(int level) {
            this.level = level;
            total = Math.min(PIG_MAX_COUNT, PIG_BASE_COUNT + (level - 1) * PIG_LEVEL_COUNT_STEP);
            remaining = total;
            List<int[]> positions = new ArrayList<>();
            for (int row = 0; row < PIG_GRID_SIZE; row++) {
                for (int col = 0; col < PIG_GRID_SIZE; col++) {
                    positions.add(new int[]{row, col});
                }
            }
            Collections.shuffle(positions, random);
            for (int i = 0; i < total; i++) {
                int[] position = positions.get(i);
                Pig pig = new Pig(position[0], position[1], random.nextInt(PIG_DIRECTION_COUNT));
                pigs.add(pig);
                board[pig.row][pig.col] = pig;
            }
        }

        boolean isInside(int row, int col) {
            return row >= 0 && row < PIG_GRID_SIZE && col >= 0 && col < PIG_GRID_SIZE;
        }

        Pig pigAt(int row, int col) {
            if (!isInside(row, col)) return null;
            return board[row][col];
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

    private class SheepBoardView extends View {
        private final SheepGameState state;
        private final TextView status;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF boardRect = new RectF();
        private float tileSize;

        SheepBoardView(SheepGameState state, TextView status) {
            super(MainActivity.this);
            this.state = state;
            this.status = status;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float side = Math.min(getWidth() - dp(20), getHeight() - dp(84));
            float left = (getWidth() - side) / 2f;
            float top = dp(8);
            boardRect.set(left, top, left + side, top + side);
            tileSize = side / 5.2f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(246, 235, 210));
            canvas.drawRoundRect(boardRect, dp(18), dp(18), paint);
            for (SheepTile tile : state.tiles) {
                if (tile.active) drawSheepTile(canvas, tile);
            }
            drawSheepTray(canvas, boardRect.bottom + dp(16));
        }

        private void drawSheepTile(Canvas canvas, SheepTile tile) {
            float x = boardRect.left + tile.col * tileSize * 0.72f + tile.layer * tileSize * 0.19f + dp(12);
            float y = boardRect.top + tile.row * tileSize * 0.62f + tile.layer * tileSize * 0.17f + dp(12);
            tile.bounds.set(x, y, x + tileSize, y + tileSize);
            boolean covered = state.isCovered(tile);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(covered ? Color.rgb(206, 194, 177) : Color.rgb(255, 252, 236));
            canvas.drawRoundRect(tile.bounds, dp(10), dp(10), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(covered ? Color.rgb(151, 139, 122) : Color.rgb(226, 162, 74));
            canvas.drawRoundRect(tile.bounds, dp(10), dp(10), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(tileSize * 0.46f);
            paint.setColor(covered ? Color.rgb(110, 102, 92) : Color.rgb(69, 48, 36));
            canvas.drawText(tile.symbol, tile.bounds.centerX(), tile.bounds.centerY() + tileSize * 0.17f, paint);
        }

        private void drawSheepTray(Canvas canvas, float top) {
            float slot = Math.min((getWidth() - dp(24)) / (float) SHEEP_TRAY_LIMIT, dp(48));
            float left = (getWidth() - slot * SHEEP_TRAY_LIMIT) / 2f;
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(slot * 0.48f);
            for (int i = 0; i < SHEEP_TRAY_LIMIT; i++) {
                RectF rect = new RectF(left + i * slot, top, left + (i + 1) * slot - dp(4), top + slot);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(238, 224, 203));
                canvas.drawRoundRect(rect, dp(8), dp(8), paint);
                if (i < state.tray.size()) {
                    paint.setColor(Color.rgb(69, 48, 36));
                    canvas.drawText(state.tray.get(i), rect.centerX(), rect.centerY() + slot * 0.18f, paint);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) return true;
            for (int i = state.tiles.size() - 1; i >= 0; i--) {
                SheepTile tile = state.tiles.get(i);
                if (tile.active && tile.bounds.contains(event.getX(), event.getY())) {
                    handleSheepTap(state, this, status, tile);
                    return true;
                }
            }
            return true;
        }
    }

    private class JumpGameView extends View {
        private final JumpState state;
        private final TextView scoreLabel;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private long chargeStartedAt;

        JumpGameView(JumpState state, TextView scoreLabel) {
            super(MainActivity.this);
            this.state = state;
            this.scoreLabel = scoreLabel;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(224, 239, 255));
            canvas.drawRoundRect(new RectF(dp(8), dp(8), w - dp(8), h - dp(8)), dp(24), dp(24), paint);
            float baseY = h * 0.72f;
            float startX = w * 0.28f;
            float targetX = startX + w * 0.42f * state.targetDistance;
            float startHalf = w * 0.09f;
            float targetHalf = w * 0.11f * state.targetSize;
            drawPlatform(canvas, startX, baseY, startHalf, Color.rgb(92, 119, 180));
            drawPlatform(canvas, targetX, baseY - dp(52), targetHalf, Color.rgb(238, 149, 91));
            paint.setColor(Color.rgb(72, 63, 72));
            float squat = state.charging ? dp(10) + state.charge * dp(18) : 0f;
            canvas.drawCircle(startX, baseY - dp(46) + squat, dp(18), paint);
            paint.setColor(Color.rgb(255, 230, 96));
            canvas.drawCircle(targetX, baseY - dp(52), dp(5), paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(16));
            paint.setColor(Color.rgb(69, 48, 36));
            canvas.drawText(state.gameOver ? "点击“重新开始”继续" : "按住蓄力，松开跳跃", w / 2f, dp(40), paint);
            if (state.charging) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(255, 193, 96));
                canvas.drawRoundRect(new RectF(w * 0.22f, h - dp(48), w * 0.22f + w * 0.56f * state.charge, h - dp(28)), dp(10), dp(10), paint);
            }
        }

        private void drawPlatform(Canvas canvas, float cx, float cy, float half, int color) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(50, 20, 20, 20));
            canvas.drawOval(new RectF(cx - half, cy + dp(20), cx + half, cy + dp(40)), paint);
            paint.setColor(color);
            canvas.drawRoundRect(new RectF(cx - half, cy - dp(24), cx + half, cy + dp(24)), dp(12), dp(12), paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (state.gameOver) return true;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                state.charging = true;
                state.charge = 0f;
                chargeStartedAt = System.currentTimeMillis();
                jumpTicker = new Runnable() {
                    @Override public void run() {
                        state.charge = Math.min(1f, (System.currentTimeMillis() - chargeStartedAt) / 720f);
                        invalidate();
                        if (state.charging) handler.postDelayed(this, 30L);
                    }
                };
                handler.post(jumpTicker);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP && state.charging) {
                state.charging = false;
                if (jumpTicker != null) handler.removeCallbacks(jumpTicker);
                finishJump(state, this, scoreLabel, System.currentTimeMillis() - chargeStartedAt);
                return true;
            }
            return true;
        }
    }

    private class WatermelonBoardView extends View {
        private final WatermelonState state;
        private final TextView scoreLabel;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF boardRect = new RectF();
        private float cellW;
        private float cellH;

        WatermelonBoardView(WatermelonState state, TextView scoreLabel) {
            super(MainActivity.this);
            this.state = state;
            this.scoreLabel = scoreLabel;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = Math.min(getWidth() - dp(28), dp(360));
            float height = getHeight() - dp(24);
            float left = (getWidth() - width) / 2f;
            boardRect.set(left, dp(10), left + width, dp(10) + height);
            cellW = boardRect.width() / WATERMELON_COLS;
            cellH = boardRect.height() / WATERMELON_ROWS;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 244, 218));
            canvas.drawRoundRect(boardRect, dp(16), dp(16), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(Color.rgb(128, 92, 58));
            canvas.drawRoundRect(boardRect, dp(16), dp(16), paint);
            paint.setColor(Color.rgb(238, 80, 80));
            paint.setStrokeWidth(dp(2));
            canvas.drawLine(boardRect.left, boardRect.top + cellH * 1.5f, boardRect.right, boardRect.top + cellH * 1.5f, paint);
            for (int row = 0; row < WATERMELON_ROWS; row++) {
                for (int col = 0; col < WATERMELON_COLS; col++) {
                    int value = state.board[row][col];
                    if (value > 0) drawFruit(canvas, row, col, value);
                }
            }
        }

        private void drawFruit(Canvas canvas, int row, int col, int value) {
            float cx = boardRect.left + col * cellW + cellW / 2f;
            float cy = boardRect.top + row * cellH + cellH / 2f;
            float radius = Math.min(cellW, cellH) * (0.25f + Math.min(value, 8) * 0.025f);
            int[] colors = {Color.rgb(226, 51, 71), Color.rgb(238, 87, 118), Color.rgb(129, 90, 185), Color.rgb(249, 149, 56), Color.rgb(225, 66, 59), Color.rgb(238, 203, 90), Color.rgb(244, 132, 110), Color.rgb(242, 180, 72), Color.rgb(119, 190, 92), Color.rgb(84, 170, 88)};
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[(value - 1) % colors.length]);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(radius * 0.82f);
            paint.setColor(Color.WHITE);
            canvas.drawText(WATERMELON_SYMBOLS[value - 1], cx, cy + radius * 0.28f, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) return true;
            if (!boardRect.contains(event.getX(), event.getY())) return true;
            int col = (int) ((event.getX() - boardRect.left) / cellW);
            dropWatermelon(state, this, scoreLabel, col);
            return true;
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

    private class SheepGameState {
        final int level;
        final List<SheepTile> tiles = new ArrayList<>();
        final List<String> tray = new ArrayList<>();
        int remaining;

        SheepGameState(int level) {
            this.level = level;
            int groups = Math.min(SHEEP_SYMBOLS.length, 7 + level % 4);
            List<String> symbols = new ArrayList<>();
            List<String> pool = new ArrayList<>();
            Collections.addAll(pool, SHEEP_SYMBOLS);
            Collections.shuffle(pool, random);
            for (int i = 0; i < groups; i++) for (int j = 0; j < 3; j++) symbols.add(pool.get(i));
            Collections.shuffle(symbols, random);
            int index = 0;
            for (int layer = 0; layer < SHEEP_LAYERS; layer++) {
                int layerCount = symbols.size() / SHEEP_LAYERS + (layer < symbols.size() % SHEEP_LAYERS ? 1 : 0);
                for (int i = 0; i < layerCount && index < symbols.size(); i++) {
                    float row = random.nextInt(5) + random.nextFloat() * 0.35f;
                    float col = random.nextInt(5) + random.nextFloat() * 0.35f;
                    tiles.add(new SheepTile(row, col, layer, symbols.get(index++)));
                }
            }
            remaining = tiles.size();
        }

        boolean isCovered(SheepTile tile) {
            for (SheepTile other : tiles) {
                if (!other.active || other.layer <= tile.layer) continue;
                if (Math.abs(other.row - tile.row) < 0.82f && Math.abs(other.col - tile.col) < 0.82f) return true;
            }
            return false;
        }
    }

    private static class SheepTile {
        final float row;
        final float col;
        final int layer;
        final String symbol;
        final RectF bounds = new RectF();
        boolean active = true;

        SheepTile(float row, float col, int layer, String symbol) {
            this.row = row;
            this.col = col;
            this.layer = layer;
            this.symbol = symbol;
        }
    }

    private static class JumpState {
        int score;
        int best;
        float targetDistance = 1f;
        float targetSize = 0.45f;
        float charge;
        boolean charging;
        boolean gameOver;
        String message = "";
        JumpState(int best) { this.best = best; }
    }

    private static class WatermelonState {
        final int[][] board = new int[WATERMELON_ROWS][WATERMELON_COLS];
        int best;
        int score;
        int nextFruit;
        boolean gameOver;
        WatermelonState(int best) {
            this.best = best;
            this.nextFruit = new Random().nextInt(5);
        }
    }

    private static class Game2048State {
        final int[][] board = new int[4][4];
        int score;
        int best;
        boolean won;
        Game2048State(int best) { this.best = best; }
    }
}
