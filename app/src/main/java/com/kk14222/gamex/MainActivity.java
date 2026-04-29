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
    private Runnable tankTicker;

    private static final String[] PIG_SYMBOLS = {"🐷", "🥕", "🌽", "🍎", "🍇", "🍉", "⭐", "🔔", "🎁", "🍀", "🥬", "🍄"};
    private static final String[] SHEEP_SYMBOLS = {"🐑", "🌿", "🧶", "🌙", "☁", "🌼", "🍃", "💧", "🥛", "🪵", "🔆", "🍂"};
    private static final String[] FRUITS = {"🍒", "🍓", "🍊", "🍋", "🍎", "🍑", "🍍", "🍉"};

    private static final int PIG_GRID_ROWS = 13;
    private static final int PIG_GRID_COLS = 9;
    private static final int PIG_BOARD_DP = 520;
    private static final int PIG_BASE_COUNT = 32;
    private static final int PIG_LEVEL_COUNT_STEP = 6;
    private static final int PIG_MAX_COUNT = 90;
    private static final int PIG_DIRECTION_COUNT = 8;
    // Direction indexes are N, NE, E, SE, S, SW, W, NW; rows, cols, and angles must stay aligned.
    private static final int[] PIG_DIR_ROWS = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static final int[] PIG_DIR_COLS = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final float[] PIG_DIR_ANGLES = {0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f};
    private static final int PIG_TOOL_REMOVE = 3;
    private static final int PIG_TOOL_SHUFFLE_DIR = 3;
    private static final int PIG_TOOL_FLIP = 2;
    private static final int PIG_TOOL_SHUFFLE_POS = 2;
    private static final int PIG_BG_GRASS = Color.rgb(186, 232, 142);
    private static final int PIG_BG_PEN = Color.rgb(170, 222, 130);
    private static final int PIG_BG_PEN_DARK = Color.rgb(141, 199, 102);
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

    // ============== 消消乐 (Match-3) ==============
    private static final int MATCH3_SIZE = 8;
    private static final int MATCH3_BOARD_DP = 360;
    private static final String[] MATCH3_SYMBOLS = {"🍓", "🍇", "🍊", "🍋", "🫐", "🍒"};
    private static final int MATCH3_BASE_TARGET = 500;
    private static final int MATCH3_LEVEL_TARGET_STEP = 250;
    private static final int MATCH3_BASE_MOVES = 25;

    // ============== 坦克大战 (Tank) ==============
    private static final int TANK_GRID = 13;
    private static final int TANK_BOARD_DP = 320;
    private static final int TANK_EMPTY = 0;
    private static final int TANK_BRICK = 1;
    private static final int TANK_STEEL = 2;
    private static final int TANK_GRASS = 3;
    private static final int TANK_BASE = 4;
    private static final int TANK_DIR_UP = 0;
    private static final int TANK_DIR_RIGHT = 1;
    private static final int TANK_DIR_DOWN = 2;
    private static final int TANK_DIR_LEFT = 3;
    private static final int[] TANK_DR = {-1, 0, 1, 0};
    private static final int[] TANK_DC = {0, 1, 0, -1};
    private static final long TANK_TICK_MS = 130L;
    private static final int TANK_FIXED_LEVELS = 3;

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
        if (tankTicker != null) {
            handler.removeCallbacks(tankTicker);
            tankTicker = null;
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
        addHomeButton("消消乐", "8×8 糖果棋盘：交换相邻糖果形成三连消除，达成关卡目标分通关。", this::showMatch3Game);
        addHomeButton("坦克大战", "经典 13×13 网格坦克：用方向键和开火键击毁全部敌人，守住老鹰基地。", this::showTankGame);
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
        root.addView(label("点击任意小猪，它会沿自己脸朝向的方向冲刺；遇到同伴会停下，没人挡路就直接冲出围栏。", 14, false), fullWidth());
        root.addView(status, fullWidth());

        PigRushBoardView board = new PigRushBoardView(state, status);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(PIG_BOARD_DP));
        boardParams.setMargins(0, dp(8), 0, dp(8));
        root.addView(board, boardParams);

        // Bottom power-up bar: 消除 / 洗牌(方向) / 翻转 / 洗牌(位置)
        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setPadding(0, dp(2), 0, dp(2));
        Button btnRemove = pigToolButton("消除", "随机带走 1 只", Color.rgb(255, 165, 79), state.removeLeft);
        btnRemove.setOnClickListener(v -> usePigTool(state, board, status, "remove"));
        Button btnShuffleDir = pigToolButton("洗牌", "随机所有方向", Color.rgb(167, 117, 207), state.shuffleDirLeft);
        btnShuffleDir.setOnClickListener(v -> usePigTool(state, board, status, "shuffleDir"));
        Button btnFlip = pigToolButton("翻转", "全体掉头", Color.rgb(96, 175, 224), state.flipLeft);
        btnFlip.setOnClickListener(v -> usePigTool(state, board, status, "flip"));
        Button btnShufflePos = pigToolButton("洗牌", "重新分布位置", Color.rgb(167, 117, 207), state.shufflePosLeft);
        btnShufflePos.setOnClickListener(v -> usePigTool(state, board, status, "shufflePos"));
        tools.addView(btnRemove, equalToolParams());
        tools.addView(btnShuffleDir, equalToolParams());
        tools.addView(btnFlip, equalToolParams());
        tools.addView(btnShufflePos, equalToolParams());
        state.toolButtons = new Button[]{btnRemove, btnShuffleDir, btnFlip, btnShufflePos};
        root.addView(tools, fullWidth());

        Button restart = new Button(this);
        restart.setText("重开本关（重新随机猪群）");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> renderPigRushGame(new PigRushState(state.level)));
        root.addView(restart, fullWidth());
        addBackButton();
    }

    private Button pigToolButton(String title, String hint, int color, int count) {
        Button b = new Button(this);
        b.setText(title + "\n" + hint + " ×" + count);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(color);
        b.setPadding(dp(4), dp(8), dp(4), dp(8));
        // Manual press feedback: setBackgroundColor wipes the default selector, so dim alpha on press.
        b.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (v.isEnabled()) v.setAlpha(0.7f);
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.setAlpha(v.isEnabled() ? 1f : 0.45f);
            }
            return false;
        });
        return b;
    }

    private LinearLayout.LayoutParams equalToolParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(4), dp(4), dp(4), dp(4));
        return p;
    }

    private void refreshPigToolButtons(PigRushState state) {
        if (state.toolButtons == null) return;
        int[] counts = {state.removeLeft, state.shuffleDirLeft, state.flipLeft, state.shufflePosLeft};
        String[] titles = {"消除", "洗牌", "翻转", "洗牌"};
        String[] hints = {"随机带走 1 只", "随机所有方向", "全体掉头", "重新分布位置"};
        for (int i = 0; i < state.toolButtons.length; i++) {
            Button b = state.toolButtons[i];
            if (b == null) continue;
            b.setText(titles[i] + "\n" + hints[i] + " ×" + counts[i]);
            b.setEnabled(counts[i] > 0 && state.remaining > 0);
            b.setAlpha(counts[i] > 0 && state.remaining > 0 ? 1f : 0.45f);
        }
    }

    private void usePigTool(PigRushState state, PigRushBoardView board, TextView status, String which) {
        if (state.remaining == 0) return;
        boolean used = false;
        switch (which) {
            case "remove":
                if (state.removeLeft > 0) {
                    used = state.removeRandomPig();
                    if (used) state.removeLeft--;
                }
                break;
            case "shuffleDir":
                if (state.shuffleDirLeft > 0) {
                    state.shuffleDirections(random);
                    state.shuffleDirLeft--;
                    used = true;
                }
                break;
            case "flip":
                if (state.flipLeft > 0) {
                    state.flipAllDirections();
                    state.flipLeft--;
                    used = true;
                }
                break;
            case "shufflePos":
                if (state.shufflePosLeft > 0) {
                    state.shufflePositions(random);
                    state.shufflePosLeft--;
                    used = true;
                }
                break;
        }
        if (!used) return;
        status.setText(pigRushStatus(state));
        refreshPigToolButtons(state);
        board.invalidate();
        if (state.remaining == 0) showPigWinDialog(state);
    }

    private String pigRushStatus(PigRushState state) {
        int cleared = state.total - state.remaining;
        int progress = state.total == 0 ? 100 : Math.round(cleared * 100f / state.total);
        return "进度 " + progress + "%　·　剩余 " + state.remaining + "/" + state.total
                + (state.remaining > 0 && !state.hasAnyMove() ? "　·　暂时无解，试试技能" : "");
    }

    private void showPigWinDialog(PigRushState state) {
        int next = state.level + 1;
        saveProgress("pig_level", next);
        new AlertDialog.Builder(this)
                .setTitle("过关啦")
                .setMessage("所有小猪都冲出围栏了！下一关：" + next)
                .setPositiveButton("下一关", (d, w) -> renderPigRushGame(new PigRushState(next)))
                .setNegativeButton("返回", (d, w) -> showHome())
                .show();
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
        // Dash through empty placeable cells; out-of-grid OR out-of-mask both mean "exit".
        while (state.isPlaceable(scanRow, scanCol)) {
            if (state.pigAt(scanRow, scanCol) != null) break;
            targetRow = scanRow;
            targetCol = scanCol;
            scanRow += dr;
            scanCol += dc;
        }

        boolean escaped = !state.isPlaceable(scanRow, scanCol);
        if (escaped) {
            state.board[pig.row][pig.col] = null;
            pig.active = false;
            state.remaining--;
        } else if (targetRow != pig.row || targetCol != pig.col) {
            state.board[pig.row][pig.col] = null;
            pig.row = targetRow;
            pig.col = targetCol;
            state.board[pig.row][pig.col] = pig;
        } else {
            // Hard-blocked, still update for haptic feel via status hint.
        }

        status.setText(pigRushStatus(state));
        refreshPigToolButtons(state);
        board.invalidate();
        if (state.remaining == 0) showPigWinDialog(state);
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
        root.addView(label("在棋盘上滑动方向移动数字；相同数字碰撞会合成，出现 2048 即达成目标，可继续挑战更高分。", 16, false), fullWidth());

        // Score cards: current / best, side by side.
        LinearLayout scoreRow = new LinearLayout(this);
        scoreRow.setOrientation(LinearLayout.HORIZONTAL);
        scoreRow.setGravity(Gravity.CENTER);
        TextView scoreCard = make2048ScoreCard("得分", "0");
        TextView bestCard = make2048ScoreCard("最高", String.valueOf(state.best));
        TextView gainHint = new TextView(this);
        gainHint.setTextSize(18);
        gainHint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        gainHint.setTextColor(Color.rgb(245, 124, 0));
        gainHint.setPadding(dp(8), 0, dp(8), 0);
        gainHint.setAlpha(0f);
        scoreRow.addView(scoreCard, score2048CardParams());
        scoreRow.addView(bestCard, score2048CardParams());
        LinearLayout.LayoutParams gainParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gainParams.gravity = Gravity.CENTER_VERTICAL;
        scoreRow.addView(gainHint, gainParams);
        root.addView(scoreRow, fullWidth());

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
                float adx = Math.abs(dx);
                float ady = Math.abs(dy);
                float maxAxis = Math.max(adx, ady);
                // Larger threshold and clear-direction requirement avoid ambiguous diagonal swipes.
                if (maxAxis >= dp(32) && maxAxis >= Math.min(adx, ady) * 1.3f) {
                    if (adx > ady) move2048(state, dx > 0 ? 1 : -1, 0, cells, scoreCard, bestCard, gainHint);
                    else move2048(state, 0, dy > 0 ? 1 : -1, cells, scoreCard, bestCard, gainHint);
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
                cell.setStateListAnimator(null);
                cell.setAllCaps(false);
                cells[r][c] = cell;
                grid.addView(cell, new ViewGroupParams(dp(GAME_2048_CELL_DP), dp(GAME_2048_CELL_DP)));
            }
        }
        Button restart = new Button(this);
        restart.setText("重新开始");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("重新开始？")
                .setMessage("当前局得分会清零，最高分会保留。确定重开？")
                .setPositiveButton("重开", (d, w) -> show2048Game())
                .setNegativeButton("取消", null)
                .show());
        root.addView(restart, fullWidth());
        addBackButton();
        draw2048(state, cells, scoreCard, bestCard);
    }

    private TextView make2048ScoreCard(String title, String value) {
        TextView tv = new TextView(this);
        tv.setText(title + "\n" + value);
        tv.setTextSize(16);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(Color.rgb(187, 173, 160));
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));
        return tv;
    }

    private LinearLayout.LayoutParams score2048CardParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(6), dp(6), dp(6), dp(6));
        return p;
    }

    private void add2048Tile(Game2048State state) {
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) if (state.board[r][c] == 0) empty.add(new int[]{r, c});
        if (empty.isEmpty()) return;
        int[] chosen = empty.get(random.nextInt(empty.size()));
        state.board[chosen[0]][chosen[1]] = random.nextInt(10) == 0 ? 4 : 2;
    }

    private void move2048(Game2048State state, int dx, int dy, Button[][] cells, TextView scoreCard, TextView bestCard, TextView gainHint) {
        boolean moved = false;
        boolean[][] merged = new boolean[4][4];
        int gainedBefore = state.score;
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
        if (!moved) return;
        add2048Tile(state);
        draw2048(state, cells, scoreCard, bestCard);
        // Pop merged tiles for visual feedback.
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (merged[r][c]) pop2048Cell(cells[r][c]);
            }
        }
        int gained = state.score - gainedBefore;
        if (gained > 0) showScoreGain(gainHint, gained);
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

    private void pop2048Cell(final Button cell) {
        cell.setScaleX(1.18f);
        cell.setScaleY(1.18f);
        handler.postDelayed(() -> {
            cell.setScaleX(1f);
            cell.setScaleY(1f);
        }, 110L);
    }

    private void showScoreGain(final TextView gainHint, int gained) {
        gainHint.setText("+" + gained);
        gainHint.setAlpha(1f);
        handler.postDelayed(() -> gainHint.setAlpha(0f), 700L);
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

    private void draw2048(Game2048State state, Button[][] cells, TextView scoreCard, TextView bestCard) {
        state.best = Math.max(state.best, state.score);
        saveProgress("2048_best", state.best);
        scoreCard.setText("得分\n" + state.score);
        bestCard.setText("最高\n" + state.best);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int value = state.board[r][c];
                Button cell = cells[r][c];
                cell.setText(value == 0 ? "" : String.format(Locale.getDefault(), "%d", value));
                cell.setBackgroundColor(get2048TileColor(value));
                cell.setTextColor(get2048TextColor(value));
                // Bigger numbers shrink to fit the cell.
                if (value >= 1024) cell.setTextSize(16);
                else if (value >= 128) cell.setTextSize(18);
                else cell.setTextSize(22);
            }
        }
    }

    private int get2048TileColor(int value) {
        switch (value) {
            case 0: return Color.rgb(205, 193, 180);
            case 2: return Color.rgb(238, 228, 218);
            case 4: return Color.rgb(237, 224, 200);
            case 8: return Color.rgb(242, 177, 121);
            case 16: return Color.rgb(245, 149, 99);
            case 32: return Color.rgb(246, 124, 95);
            case 64: return Color.rgb(246, 94, 59);
            case 128: return Color.rgb(237, 207, 114);
            case 256: return Color.rgb(237, 204, 97);
            case 512: return Color.rgb(237, 200, 80);
            case 1024: return Color.rgb(237, 197, 63);
            case 2048: return Color.rgb(237, 194, 46);
            default: return Color.rgb(60, 58, 50);
        }
    }

    private int get2048TextColor(int value) {
        if (value == 0) return Color.rgb(205, 193, 180);
        if (value <= 4) return Color.rgb(119, 110, 101);
        return Color.WHITE;
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
        private float cellW;
        private float cellH;
        private float originX;
        private float originY;

        PigRushBoardView(PigRushState state, TextView status) {
            super(MainActivity.this);
            this.state = state;
            this.status = status;
            setBackgroundColor(PIG_BG_GRASS);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // Reserve a uniform inset, then compute cell size so the rows×cols grid (with
            // hex-stagger offset of half a cell) fits within the available area.
            float availW = getWidth() - dp(20);
            float availH = getHeight() - dp(20);
            // Effective horizontal extent is (cols + 0.5) cells because of staggering.
            float fitByW = availW / (PIG_GRID_COLS + 0.5f);
            // Vertical packing: rows partially overlap when staggered (typical hex factor ~0.86).
            float fitByH = availH / (1f + (PIG_GRID_ROWS - 1) * 0.86f);
            cellW = Math.min(fitByW, fitByH);
            cellH = cellW * 0.86f;
            float boardW = (PIG_GRID_COLS + 0.5f) * cellW;
            float boardH = cellW + (PIG_GRID_ROWS - 1) * cellH;
            originX = (getWidth() - boardW) / 2f + cellW * 0.5f;
            originY = (getHeight() - boardH) / 2f + cellW * 0.5f;
            boardRect.set(originX - cellW * 0.5f, originY - cellW * 0.5f,
                    originX - cellW * 0.5f + boardW, originY - cellW * 0.5f + boardH);

            drawPigRushBackground(canvas);

            for (Pig pig : state.pigs) {
                if (pig.active) drawPig(canvas, pig);
            }
        }

        private float pigCenterX(int row, int col) {
            float stagger = (row & 1) == 1 ? cellW * 0.5f : 0f;
            return originX + col * cellW + stagger;
        }

        private float pigCenterY(int row) {
            return originY + row * cellH;
        }

        private void drawPigRushBackground(Canvas canvas) {
            // Solid grass background already painted by setBackgroundColor; add subtle pen oval.
            paint.setStyle(Paint.Style.FILL);
            float padX = cellW * 0.4f;
            float padY = cellH * 0.4f;
            RectF pen = new RectF(boardRect.left + padX, boardRect.top + padY,
                    boardRect.right - padX, boardRect.bottom - padY);
            paint.setColor(PIG_BG_PEN);
            canvas.drawOval(pen, paint);

            // Faint dashed boundary suggesting the open-air "fence".
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.argb(70, 90, 140, 70));
            paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(6), dp(6)}, 0));
            canvas.drawOval(pen, paint);
            paint.setPathEffect(null);
            paint.setStyle(Paint.Style.FILL);

            // Decorative grass tufts scattered along the rim.
            paint.setColor(PIG_BG_PEN_DARK);
            Random deco = new Random(state.level * 9176L + 31);
            for (int i = 0; i < 18; i++) {
                float angle = (float) (deco.nextDouble() * Math.PI * 2);
                float rx = pen.width() * 0.5f * (0.55f + deco.nextFloat() * 0.55f);
                float ry = pen.height() * 0.5f * (0.55f + deco.nextFloat() * 0.55f);
                float gx = pen.centerX() + (float) Math.cos(angle) * rx;
                float gy = pen.centerY() + (float) Math.sin(angle) * ry;
                drawGrassTuft(canvas, gx, gy, cellW * 0.2f);
            }
            // Butterflies: a few small white/yellow accents like in the reference image.
            for (int i = 0; i < 4; i++) {
                float bx = boardRect.left + dp(20) + deco.nextFloat() * (boardRect.width() - dp(40));
                float by = boardRect.top + dp(20) + deco.nextFloat() * (boardRect.height() - dp(40));
                drawButterfly(canvas, bx, by, cellW * 0.18f, deco.nextBoolean());
            }
        }

        private void drawGrassTuft(Canvas canvas, float cx, float cy, float size) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(dp(1), size * 0.18f));
            paint.setColor(Color.argb(160, 96, 156, 78));
            canvas.drawLine(cx - size * 0.4f, cy + size * 0.3f, cx - size * 0.2f, cy - size * 0.5f, paint);
            canvas.drawLine(cx, cy + size * 0.3f, cx, cy - size * 0.6f, paint);
            canvas.drawLine(cx + size * 0.4f, cy + size * 0.3f, cx + size * 0.2f, cy - size * 0.5f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawButterfly(Canvas canvas, float cx, float cy, float size, boolean yellow) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(yellow ? Color.argb(220, 255, 244, 170) : Color.argb(220, 255, 255, 255));
            canvas.drawOval(new RectF(cx - size, cy - size * 0.7f, cx - size * 0.05f, cy + size * 0.2f), paint);
            canvas.drawOval(new RectF(cx + size * 0.05f, cy - size * 0.7f, cx + size, cy + size * 0.2f), paint);
            canvas.drawOval(new RectF(cx - size * 0.85f, cy + size * 0.05f, cx - size * 0.1f, cy + size * 0.65f), paint);
            canvas.drawOval(new RectF(cx + size * 0.1f, cy + size * 0.05f, cx + size * 0.85f, cy + size * 0.65f), paint);
            paint.setColor(Color.argb(220, 90, 70, 60));
            canvas.drawOval(new RectF(cx - size * 0.08f, cy - size * 0.5f, cx + size * 0.08f, cy + size * 0.6f), paint);
        }

        private void drawPig(Canvas canvas, Pig pig) {
            float cx = pigCenterX(pig.row, pig.col);
            float cy = pigCenterY(pig.row);
            float radius = Math.min(cellW, cellH) * 0.55f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(60, 80, 62, 44));
            canvas.drawOval(new RectF(cx - radius * 0.95f, cy + radius * 0.58f,
                    cx + radius * 0.95f, cy + radius * 1.05f), paint);

            canvas.save();
            canvas.translate(cx, cy);
            canvas.rotate(PIG_DIR_ANGLES[pig.direction]);

            // Ears
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

            // Body
            paint.setColor(Color.rgb(255, 186, 203));
            canvas.drawOval(new RectF(-radius * 0.88f, -radius * 0.55f, radius * 0.88f, radius * 0.75f), paint);
            paint.setColor(Color.rgb(255, 202, 215));
            canvas.drawCircle(0, -radius * 0.33f, radius * 0.66f, paint);

            // Snout / nose
            paint.setColor(Color.rgb(255, 128, 162));
            Path nose = new Path();
            nose.moveTo(0, -radius * 0.98f);
            nose.cubicTo(-radius * 0.44f, -radius * 0.82f, -radius * 0.44f, -radius * 0.45f, 0, -radius * 0.38f);
            nose.cubicTo(radius * 0.44f, -radius * 0.45f, radius * 0.44f, -radius * 0.82f, 0, -radius * 0.98f);
            nose.close();
            canvas.drawPath(nose, paint);

            // Eyes
            paint.setColor(Color.rgb(82, 48, 54));
            canvas.drawCircle(-radius * 0.24f, -radius * 0.36f, radius * 0.08f, paint);
            canvas.drawCircle(radius * 0.24f, -radius * 0.36f, radius * 0.08f, paint);

            // Nostrils
            paint.setColor(Color.rgb(255, 129, 161));
            canvas.drawOval(new RectF(-radius * 0.24f, -radius * 0.82f, radius * 0.24f, -radius * 0.58f), paint);
            paint.setColor(Color.rgb(123, 65, 73));
            canvas.drawCircle(-radius * 0.08f, -radius * 0.7f, radius * 0.035f, paint);
            canvas.drawCircle(radius * 0.08f, -radius * 0.7f, radius * 0.035f, paint);

            // Curly tail at the back
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
            // Find the nearest active pig to the touch point, within one cell radius.
            float tx = event.getX();
            float ty = event.getY();
            float bestDistSq = Float.MAX_VALUE;
            Pig best = null;
            float threshold = Math.min(cellW, cellH) * 0.6f;
            float thresholdSq = threshold * threshold;
            for (Pig pig : state.pigs) {
                if (!pig.active) continue;
                float dx = pigCenterX(pig.row, pig.col) - tx;
                float dy = pigCenterY(pig.row) - ty;
                float d2 = dx * dx + dy * dy;
                if (d2 < bestDistSq && d2 <= thresholdSq) {
                    bestDistSq = d2;
                    best = pig;
                }
            }
            if (best != null) {
                handlePigRushTap(state, this, status, best.row, best.col);
            }
            return true;
        }
    }

    private class PigRushState {
        final int level;
        int total;
        final List<Pig> pigs = new ArrayList<>();
        final Pig[][] board = new Pig[PIG_GRID_ROWS][PIG_GRID_COLS];
        final boolean[][] placeable = new boolean[PIG_GRID_ROWS][PIG_GRID_COLS];
        int remaining;
        int removeLeft = PIG_TOOL_REMOVE;
        int shuffleDirLeft = PIG_TOOL_SHUFFLE_DIR;
        int flipLeft = PIG_TOOL_FLIP;
        int shufflePosLeft = PIG_TOOL_SHUFFLE_POS;
        Button[] toolButtons;

        PigRushState(int level) {
            this.level = level;
            // Build oval pen mask (placeable cells inside ellipse).
            float cy = (PIG_GRID_ROWS - 1) / 2f;
            float cx = (PIG_GRID_COLS - 1) / 2f;
            float ry = (PIG_GRID_ROWS - 1) / 2f + 0.05f;
            float rx = (PIG_GRID_COLS - 1) / 2f + 0.05f;
            int placeableCells = 0;
            List<int[]> positions = new ArrayList<>();
            for (int row = 0; row < PIG_GRID_ROWS; row++) {
                for (int col = 0; col < PIG_GRID_COLS; col++) {
                    float ny = (row - cy) / ry;
                    float nx = (col - cx) / rx;
                    if (nx * nx + ny * ny <= 1f) {
                        placeable[row][col] = true;
                        placeableCells++;
                        positions.add(new int[]{row, col});
                    }
                }
            }
            int desired = PIG_BASE_COUNT + (level - 1) * PIG_LEVEL_COUNT_STEP;
            total = Math.min(Math.min(PIG_MAX_COUNT, placeableCells), desired);
            remaining = total;
            Collections.shuffle(positions, random);
            for (int i = 0; i < total; i++) {
                int[] position = positions.get(i);
                Pig pig = new Pig(position[0], position[1], random.nextInt(PIG_DIRECTION_COUNT));
                pigs.add(pig);
                board[pig.row][pig.col] = pig;
            }
        }

        boolean isInside(int row, int col) {
            return row >= 0 && row < PIG_GRID_ROWS && col >= 0 && col < PIG_GRID_COLS;
        }

        boolean isPlaceable(int row, int col) {
            return isInside(row, col) && placeable[row][col];
        }

        Pig pigAt(int row, int col) {
            if (!isInside(row, col)) return null;
            return board[row][col];
        }

        boolean hasAnyMove() {
            // Returns true if at least one pig can dash to any new cell or escape.
            for (Pig pig : pigs) {
                if (!pig.active) continue;
                int dr = PIG_DIR_ROWS[pig.direction];
                int dc = PIG_DIR_COLS[pig.direction];
                int nr = pig.row + dr;
                int nc = pig.col + dc;
                if (!isPlaceable(nr, nc)) return true; // can escape
                if (board[nr][nc] == null) return true; // can move
            }
            return false;
        }

        boolean removeRandomPig() {
            List<Pig> active = new ArrayList<>();
            for (Pig p : pigs) if (p.active) active.add(p);
            if (active.isEmpty()) return false;
            Pig victim = active.get(random.nextInt(active.size()));
            board[victim.row][victim.col] = null;
            victim.active = false;
            remaining--;
            return true;
        }

        void shuffleDirections(Random rng) {
            for (Pig p : pigs) {
                if (p.active) p.direction = rng.nextInt(PIG_DIRECTION_COUNT);
            }
        }

        void flipAllDirections() {
            for (Pig p : pigs) {
                if (p.active) p.direction = (p.direction + 4) % PIG_DIRECTION_COUNT;
            }
        }

        void shufflePositions(Random rng) {
            List<Pig> active = new ArrayList<>();
            for (Pig p : pigs) if (p.active) active.add(p);
            List<int[]> slots = new ArrayList<>();
            for (int r = 0; r < PIG_GRID_ROWS; r++) {
                for (int c = 0; c < PIG_GRID_COLS; c++) {
                    if (placeable[r][c]) slots.add(new int[]{r, c});
                    board[r][c] = null;
                }
            }
            Collections.shuffle(slots, rng);
            for (int i = 0; i < active.size(); i++) {
                Pig p = active.get(i);
                int[] slot = slots.get(i);
                p.row = slot[0];
                p.col = slot[1];
                board[p.row][p.col] = p;
            }
        }
    }

    private static class Pig {
        int row;
        int col;
        int direction;
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
            // Warn when one slot away from filling up; tray full = lose.
            boolean warn = state.tray.size() >= SHEEP_TRAY_LIMIT - 1;
            int slotColor = warn ? Color.rgb(248, 198, 198) : Color.rgb(238, 224, 203);
            for (int i = 0; i < SHEEP_TRAY_LIMIT; i++) {
                RectF rect = new RectF(left + i * slot, top, left + (i + 1) * slot - dp(4), top + slot);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(slotColor);
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
        private long lastReleaseAt;

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
                // Lockout brief window after release so rapid taps don't reset charge mid-frame.
                if (System.currentTimeMillis() - lastReleaseAt < 250L) return true;
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
                lastReleaseAt = System.currentTimeMillis();
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
        private long lastDropAt;

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
            // Cooldown 500ms to prevent rapid-fire double drops while gravity collapses.
            long now = System.currentTimeMillis();
            if (now - lastDropAt < 500L) return true;
            lastDropAt = now;
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

    // ============================================================================
    // 消消乐 (Match-3)
    // ============================================================================

    private void showMatch3Game() {
        int level = getProgress("match3_level", 1);
        Match3State state = new Match3State(level, getProgress("match3_best", 0));
        renderMatch3Game(state);
    }

    private void renderMatch3Game(Match3State state) {
        setRoot("消消乐  第 " + state.level + " 关");
        root.addView(label("点击糖果选中再点相邻糖果交换，或在糖果上滑动方向交换；形成 ≥3 连即可消除并连锁。",
                14, false), fullWidth());
        TextView status = label(match3Status(state), 16, true);
        root.addView(status, fullWidth());
        Match3BoardView board = new Match3BoardView(state, status);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(MATCH3_BOARD_DP));
        boardParams.setMargins(0, dp(8), 0, dp(8));
        root.addView(board, boardParams);
        Button restart = new Button(this);
        restart.setText("重开本关（重新随机棋盘）");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> renderMatch3Game(new Match3State(state.level, state.best)));
        root.addView(restart, fullWidth());
        addBackButton();
    }

    private String match3Status(Match3State state) {
        return "得分：" + state.score + " / 目标 " + state.target
                + "    剩余步数：" + state.movesLeft
                + "    最高：" + state.best;
    }

    private void onMatch3Move(Match3State state, Match3BoardView board, TextView status) {
        state.best = Math.max(state.best, state.score);
        saveProgress("match3_best", state.best);
        status.setText(match3Status(state));
        board.invalidate();
        if (state.score >= state.target) {
            int next = state.level + 1;
            saveProgress("match3_level", next);
            new AlertDialog.Builder(this)
                    .setTitle("过关啦")
                    .setMessage("达成目标 " + state.target + " 分！下一关：" + next)
                    .setPositiveButton("下一关", (d, w) -> renderMatch3Game(new Match3State(next, state.best)))
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        } else if (state.movesLeft <= 0) {
            new AlertDialog.Builder(this)
                    .setTitle("步数用完")
                    .setMessage("本关得分：" + state.score + " / " + state.target)
                    .setPositiveButton("重开本关", (d, w) -> renderMatch3Game(new Match3State(state.level, state.best)))
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        }
    }

    private boolean trySwapMatch3(Match3State state, int r1, int c1, int r2, int c2) {
        if (Math.abs(r1 - r2) + Math.abs(c1 - c2) != 1) return false;
        int tmp = state.board[r1][c1];
        state.board[r1][c1] = state.board[r2][c2];
        state.board[r2][c2] = tmp;
        if (findMatch3(state) == 0) {
            // No match formed; revert.
            tmp = state.board[r1][c1];
            state.board[r1][c1] = state.board[r2][c2];
            state.board[r2][c2] = tmp;
            return false;
        }
        // Cascade resolve until stable.
        resolveMatch3Cascade(state);
        state.movesLeft--;
        return true;
    }

    private void resolveMatch3Cascade(Match3State state) {
        while (true) {
            int gained = findAndClearMatch3(state);
            if (gained == 0) break;
            state.score += gained;
            collapseMatch3(state);
        }
    }

    /** Returns count of cells that would match (without clearing); 0 if none. */
    private int findMatch3(Match3State state) {
        boolean[][] mark = computeMatch3Marks(state);
        int n = 0;
        for (int r = 0; r < MATCH3_SIZE; r++) for (int c = 0; c < MATCH3_SIZE; c++) if (mark[r][c]) n++;
        return n;
    }

    private int findAndClearMatch3(Match3State state) {
        boolean[][] mark = computeMatch3Marks(state);
        // Score: 10 per cell + bonus for runs of 4 (+20) and 5+ (+50 each).
        int score = 0;
        int cleared = 0;
        // Count contiguous run lengths to grant bonuses.
        for (int r = 0; r < MATCH3_SIZE; r++) {
            int run = 1;
            for (int c = 1; c <= MATCH3_SIZE; c++) {
                boolean inRun = c < MATCH3_SIZE && mark[r][c] && state.board[r][c] == state.board[r][c - 1];
                if (inRun) run++;
                else {
                    if (mark[r][c - 1] && run >= 4) score += (run >= 5) ? 50 : 20;
                    run = 1;
                }
            }
        }
        for (int c = 0; c < MATCH3_SIZE; c++) {
            int run = 1;
            for (int r = 1; r <= MATCH3_SIZE; r++) {
                boolean inRun = r < MATCH3_SIZE && mark[r][c] && state.board[r][c] == state.board[r - 1][c];
                if (inRun) run++;
                else {
                    if (mark[r - 1][c] && run >= 4) score += (run >= 5) ? 50 : 20;
                    run = 1;
                }
            }
        }
        for (int r = 0; r < MATCH3_SIZE; r++) {
            for (int c = 0; c < MATCH3_SIZE; c++) {
                if (mark[r][c]) {
                    state.board[r][c] = -1; // marker for empty
                    cleared++;
                }
            }
        }
        score += cleared * 10;
        return score;
    }

    private boolean[][] computeMatch3Marks(Match3State state) {
        boolean[][] mark = new boolean[MATCH3_SIZE][MATCH3_SIZE];
        // Horizontal runs of length >= 3.
        for (int r = 0; r < MATCH3_SIZE; r++) {
            int runStart = 0;
            for (int c = 1; c <= MATCH3_SIZE; c++) {
                if (c == MATCH3_SIZE || state.board[r][c] != state.board[r][runStart] || state.board[r][c] < 0) {
                    int len = c - runStart;
                    if (len >= 3 && state.board[r][runStart] >= 0) {
                        for (int k = runStart; k < c; k++) mark[r][k] = true;
                    }
                    runStart = c;
                }
            }
        }
        // Vertical runs of length >= 3.
        for (int c = 0; c < MATCH3_SIZE; c++) {
            int runStart = 0;
            for (int r = 1; r <= MATCH3_SIZE; r++) {
                if (r == MATCH3_SIZE || state.board[r][c] != state.board[runStart][c] || state.board[r][c] < 0) {
                    int len = r - runStart;
                    if (len >= 3 && state.board[runStart][c] >= 0) {
                        for (int k = runStart; k < r; k++) mark[k][c] = true;
                    }
                    runStart = r;
                }
            }
        }
        return mark;
    }

    private void collapseMatch3(Match3State state) {
        for (int c = 0; c < MATCH3_SIZE; c++) {
            int writeRow = MATCH3_SIZE - 1;
            for (int r = MATCH3_SIZE - 1; r >= 0; r--) {
                if (state.board[r][c] >= 0) {
                    int v = state.board[r][c];
                    state.board[r][c] = -1;
                    state.board[writeRow--][c] = v;
                }
            }
            // Refill remaining top cells with random symbols.
            for (int r = writeRow; r >= 0; r--) {
                state.board[r][c] = random.nextInt(MATCH3_SYMBOLS.length);
            }
        }
    }

    private class Match3State {
        final int level;
        final int target;
        final int[][] board = new int[MATCH3_SIZE][MATCH3_SIZE];
        int score;
        int movesLeft;
        int best;

        Match3State(int level, int best) {
            this.level = level;
            this.target = MATCH3_BASE_TARGET + (level - 1) * MATCH3_LEVEL_TARGET_STEP;
            this.movesLeft = MATCH3_BASE_MOVES;
            this.best = best;
            // Generate a board with no initial matches; bound the retries to avoid any pathological loop.
            int attempts = 0;
            do {
                for (int r = 0; r < MATCH3_SIZE; r++) {
                    for (int c = 0; c < MATCH3_SIZE; c++) {
                        board[r][c] = random.nextInt(MATCH3_SYMBOLS.length);
                    }
                }
                attempts++;
            } while (findMatch3(this) > 0 && attempts < 50);
            if (findMatch3(this) > 0) {
                // Fallback: clear any pre-existing matches without scoring before play starts.
                resolveMatch3Cascade(this);
                score = 0;
            }
        }
    }

    private class Match3BoardView extends View {
        private final Match3State state;
        private final TextView status;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF boardRect = new RectF();
        private float cell;
        private int selR = -1;
        private int selC = -1;
        private float downX, downY;
        private int downR = -1, downC = -1;

        Match3BoardView(Match3State state, TextView status) {
            super(MainActivity.this);
            this.state = state;
            this.status = status;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float side = Math.min(getWidth() - dp(16), getHeight() - dp(16));
            float left = (getWidth() - side) / 2f;
            float top = (getHeight() - side) / 2f;
            boardRect.set(left, top, left + side, top + side);
            cell = side / MATCH3_SIZE;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 244, 222));
            canvas.drawRoundRect(boardRect, dp(14), dp(14), paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(cell * 0.62f);
            for (int r = 0; r < MATCH3_SIZE; r++) {
                for (int c = 0; c < MATCH3_SIZE; c++) {
                    float cx = boardRect.left + c * cell + cell / 2f;
                    float cy = boardRect.top + r * cell + cell / 2f;
                    paint.setStyle(Paint.Style.FILL);
                    boolean selected = r == selR && c == selC;
                    paint.setColor(selected ? Color.rgb(255, 220, 130) : Color.rgb(255, 252, 240));
                    RectF tile = new RectF(cx - cell * 0.45f, cy - cell * 0.45f,
                            cx + cell * 0.45f, cy + cell * 0.45f);
                    canvas.drawRoundRect(tile, dp(8), dp(8), paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(1f, dp(2) * 0.75f));
                    paint.setColor(selected ? Color.rgb(232, 154, 30) : Color.rgb(225, 200, 160));
                    canvas.drawRoundRect(tile, dp(8), dp(8), paint);
                    paint.setStyle(Paint.Style.FILL);
                    int v = state.board[r][c];
                    if (v >= 0) {
                        paint.setColor(Color.rgb(69, 48, 36));
                        canvas.drawText(MATCH3_SYMBOLS[v], cx, cy + cell * 0.22f, paint);
                    }
                }
            }
        }

        private int rowAt(float y) {
            if (cell <= 0f) return -1;
            int r = (int) ((y - boardRect.top) / cell);
            return (r < 0 || r >= MATCH3_SIZE) ? -1 : r;
        }

        private int colAt(float x) {
            if (cell <= 0f) return -1;
            int c = (int) ((x - boardRect.left) / cell);
            return (c < 0 || c >= MATCH3_SIZE) ? -1 : c;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (state.movesLeft <= 0 || state.score >= state.target) return true;
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                downX = event.getX();
                downY = event.getY();
                downR = rowAt(downY);
                downC = colAt(downX);
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                int upR = rowAt(event.getY());
                int upC = colAt(event.getX());
                if (downR < 0 || downC < 0) return true;
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.max(Math.abs(dx), Math.abs(dy)) >= cell * 0.4f) {
                    // Swipe gesture: swap with neighbor in dominant direction.
                    int dr = 0, dc = 0;
                    if (Math.abs(dx) > Math.abs(dy)) dc = dx > 0 ? 1 : -1;
                    else dr = dy > 0 ? 1 : -1;
                    int tr = downR + dr;
                    int tc = downC + dc;
                    if (tr >= 0 && tr < MATCH3_SIZE && tc >= 0 && tc < MATCH3_SIZE) {
                        attemptSwap(downR, downC, tr, tc);
                    }
                    selR = -1; selC = -1;
                    invalidate();
                    return true;
                }
                // Tap: select / swap with already-selected adjacent cell.
                if (upR == downR && upC == downC && upR >= 0) {
                    if (selR < 0) {
                        selR = upR;
                        selC = upC;
                    } else if (selR == upR && selC == upC) {
                        selR = -1; selC = -1;
                    } else if (Math.abs(selR - upR) + Math.abs(selC - upC) == 1) {
                        attemptSwap(selR, selC, upR, upC);
                        selR = -1; selC = -1;
                    } else {
                        selR = upR;
                        selC = upC;
                    }
                    invalidate();
                }
                return true;
            }
            return true;
        }

        private void attemptSwap(int r1, int c1, int r2, int c2) {
            boolean ok = trySwapMatch3(state, r1, c1, r2, c2);
            if (ok) {
                onMatch3Move(state, this, status);
            } else {
                Toast.makeText(MainActivity.this, "无法形成三连，已撤回", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ============================================================================
    // 坦克大战 (Tank Battle)
    // ============================================================================

    private void showTankGame() {
        int level = getProgress("tank_level", 1);
        TankState state = new TankState(level, getProgress("tank_best", 0));
        renderTankGame(state);
    }

    private void renderTankGame(TankState state) {
        setRoot("坦克大战  第 " + state.level + " 关");
        root.addView(label("方向键控制坦克朝向并移动，开火按钮发射子弹；击毁全部敌人过关，老鹰被击中则失败。",
                14, false), fullWidth());
        TextView status = label(tankStatus(state), 16, true);
        root.addView(status, fullWidth());
        TankBoardView board = new TankBoardView(state, status);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(TANK_BOARD_DP));
        boardParams.setMargins(0, dp(8), 0, dp(8));
        root.addView(board, boardParams);

        // Control panel: D-pad on left, fire button on right.
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(controls, fullWidth());

        GridLayout dpad = new GridLayout(this);
        dpad.setColumnCount(3);
        dpad.setRowCount(3);
        addTankDPadCell(dpad, null, 0, state, board);
        addTankDPadCell(dpad, "上", TANK_DIR_UP, state, board);
        addTankDPadCell(dpad, null, 0, state, board);
        addTankDPadCell(dpad, "左", TANK_DIR_LEFT, state, board);
        addTankDPadCell(dpad, null, 0, state, board);
        addTankDPadCell(dpad, "右", TANK_DIR_RIGHT, state, board);
        addTankDPadCell(dpad, null, 0, state, board);
        addTankDPadCell(dpad, "下", TANK_DIR_DOWN, state, board);
        addTankDPadCell(dpad, null, 0, state, board);
        controls.addView(dpad);

        Button fire = new Button(this);
        fire.setText("开火");
        fire.setAllCaps(false);
        fire.setTextSize(20);
        fire.setTextColor(Color.WHITE);
        fire.setBackgroundColor(Color.rgb(220, 70, 70));
        fire.setOnClickListener(v -> tankPlayerFire(state, board));
        LinearLayout.LayoutParams fireParams = new LinearLayout.LayoutParams(0,
                dp(96), 1f);
        fireParams.setMargins(dp(16), dp(8), dp(8), dp(8));
        controls.addView(fire, fireParams);

        Button restart = new Button(this);
        restart.setText("重开本关");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> {
            stopTankLoop(state);
            renderTankGame(new TankState(state.level, state.best));
        });
        root.addView(restart, fullWidth());
        addBackButton();

        startTankLoop(state, board, status);
    }

    private void addTankDPadCell(GridLayout dpad, String label, int dir,
                                  TankState state, TankBoardView board) {
        if (label == null) {
            // Spacer.
            View spacer = new View(this);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = dp(64);
            p.height = dp(48);
            dpad.addView(spacer, p);
            return;
        }
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(16);
        // Hold-to-move: button keeps moving while pressed.
        b.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                state.playerHeldDir = dir;
                state.player.dir = dir;
                board.invalidate();
                return false; // allow click effects too
            }
            if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (state.playerHeldDir == dir) state.playerHeldDir = -1;
                return false;
            }
            return false;
        });
        // Single tap also rotates without moving (handled via DOWN above).
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = dp(64);
        p.height = dp(48);
        p.setMargins(dp(2), dp(2), dp(2), dp(2));
        dpad.addView(b, p);
    }

    private String tankStatus(TankState state) {
        return "敌方剩余：" + state.enemiesRemaining
                + "    生命：" + state.playerLives
                + "    得分：" + state.score
                + "    最高：" + state.best;
    }

    private void startTankLoop(TankState state, TankBoardView board, TextView status) {
        stopTankLoop(state);
        state.tickRunnable = new Runnable() {
            @Override public void run() {
                if (state.gameOver) return;
                tankTick(state, board, status);
                if (!state.gameOver) handler.postDelayed(this, TANK_TICK_MS);
            }
        };
        tankTicker = state.tickRunnable;
        handler.postDelayed(state.tickRunnable, TANK_TICK_MS);
    }

    private void stopTankLoop(TankState state) {
        if (state.tickRunnable != null) {
            handler.removeCallbacks(state.tickRunnable);
            if (tankTicker == state.tickRunnable) tankTicker = null;
            state.tickRunnable = null;
        }
    }

    private void tankTick(TankState state, TankBoardView board, TextView status) {
        // 1. Move player if direction held.
        if (state.playerHeldDir >= 0 && state.player.alive) {
            state.player.dir = state.playerHeldDir;
            tankTryMove(state, state.player);
        }
        // 2. Move enemies + AI fire.
        for (Tank t : state.enemies) {
            if (!t.alive) continue;
            t.aiCooldown--;
            if (t.aiCooldown <= 0 || !tankCanStep(state, t, t.dir)) {
                // Pick a new direction; bias toward facing the base / player when adjacent line-of-sight.
                int preferred = tankPreferredDir(state, t);
                t.dir = preferred >= 0 ? preferred : random.nextInt(4);
                t.aiCooldown = 6 + random.nextInt(8);
            }
            tankTryMove(state, t);
            // Random fire chance.
            if (random.nextInt(14) == 0) tankFire(state, t);
        }
        // 3. Spawn enemies if pool has more and below cap.
        if (state.enemiesQueued > 0 && countAlive(state.enemies) < state.maxAliveEnemies) {
            tankSpawnEnemy(state);
        }
        // 4. Move bullets twice per tick (faster than tanks).
        for (int step = 0; step < 2; step++) tankAdvanceBullets(state);
        board.invalidate();
        status.setText(tankStatus(state));
        // 5. End conditions.
        if (state.baseDestroyed || !state.player.alive) {
            tankEndLevel(state, false);
        } else if (state.enemiesRemaining == 0) {
            tankEndLevel(state, true);
        }
    }

    private int tankPreferredDir(TankState state, Tank t) {
        // If aligned with the base or player on the same row/col, face them.
        if (t.col == state.baseCol && t.row < state.baseRow) return TANK_DIR_DOWN;
        if (t.row == state.baseRow && t.col < state.baseCol) return TANK_DIR_RIGHT;
        if (t.row == state.baseRow && t.col > state.baseCol) return TANK_DIR_LEFT;
        if (state.player.alive) {
            if (t.col == state.player.col && t.row < state.player.row) return TANK_DIR_DOWN;
            if (t.col == state.player.col && t.row > state.player.row) return TANK_DIR_UP;
            if (t.row == state.player.row && t.col < state.player.col) return TANK_DIR_RIGHT;
            if (t.row == state.player.row && t.col > state.player.col) return TANK_DIR_LEFT;
        }
        return -1;
    }

    private boolean tankCanStep(TankState state, Tank t, int dir) {
        int nr = t.row + TANK_DR[dir];
        int nc = t.col + TANK_DC[dir];
        if (nr < 0 || nr >= TANK_GRID || nc < 0 || nc >= TANK_GRID) return false;
        int cellv = state.map[nr][nc];
        if (cellv == TANK_BRICK || cellv == TANK_STEEL || cellv == TANK_BASE) return false;
        // Don't step onto another tank.
        for (Tank other : state.allTanks()) {
            if (other != t && other.alive && other.row == nr && other.col == nc) return false;
        }
        return true;
    }

    private void tankTryMove(TankState state, Tank t) {
        if (tankCanStep(state, t, t.dir)) {
            t.row += TANK_DR[t.dir];
            t.col += TANK_DC[t.dir];
        }
    }

    private void tankPlayerFire(TankState state, TankBoardView board) {
        if (state.gameOver || !state.player.alive) return;
        // Limit player to 2 active bullets.
        int active = 0;
        for (Bullet b : state.bullets) if (b.alive && b.fromPlayer) active++;
        if (active >= 2) return;
        tankFire(state, state.player);
        board.invalidate();
    }

    private void tankFire(TankState state, Tank t) {
        Bullet b = new Bullet();
        b.row = t.row + TANK_DR[t.dir];
        b.col = t.col + TANK_DC[t.dir];
        b.dir = t.dir;
        b.fromPlayer = (t == state.player);
        b.alive = true;
        // If immediately off-grid, drop.
        if (b.row < 0 || b.row >= TANK_GRID || b.col < 0 || b.col >= TANK_GRID) return;
        state.bullets.add(b);
    }

    private void tankAdvanceBullets(TankState state) {
        for (Bullet b : state.bullets) {
            if (!b.alive) continue;
            int nr = b.row + TANK_DR[b.dir];
            int nc = b.col + TANK_DC[b.dir];
            if (nr < 0 || nr >= TANK_GRID || nc < 0 || nc >= TANK_GRID) {
                b.alive = false;
                continue;
            }
            b.row = nr;
            b.col = nc;
            // Hit wall?
            int cellv = state.map[nr][nc];
            if (cellv == TANK_BRICK) {
                state.map[nr][nc] = TANK_EMPTY;
                b.alive = false;
                continue;
            }
            if (cellv == TANK_STEEL) {
                b.alive = false;
                continue;
            }
            if (cellv == TANK_BASE) {
                state.baseDestroyed = true;
                b.alive = false;
                continue;
            }
            // Hit tank?
            for (Tank t : state.allTanks()) {
                if (!t.alive) continue;
                if (t.row == nr && t.col == nc) {
                    if (t == state.player) {
                        if (b.fromPlayer) continue; // own bullet won't hurt self
                        state.player.alive = false;
                        state.playerLives--;
                        if (state.playerLives > 0) {
                            // Respawn at start.
                            state.player.row = state.playerStartRow;
                            state.player.col = state.playerStartCol;
                            state.player.dir = TANK_DIR_UP;
                            state.player.alive = true;
                        }
                    } else {
                        if (!b.fromPlayer) continue; // enemies don't shoot each other
                        t.alive = false;
                        state.enemiesRemaining--;
                        state.score += 100;
                    }
                    b.alive = false;
                    break;
                }
            }
        }
        // Cull dead bullets occasionally.
        for (int i = state.bullets.size() - 1; i >= 0; i--) {
            if (!state.bullets.get(i).alive) state.bullets.remove(i);
        }
    }

    private int countAlive(List<Tank> tanks) {
        int n = 0;
        for (Tank t : tanks) if (t.alive) n++;
        return n;
    }

    private void tankSpawnEnemy(TankState state) {
        // Spawn at one of three top positions (corners and center) if cell empty.
        int[] cols = {0, TANK_GRID / 2, TANK_GRID - 1};
        for (int c : cols) {
            if (state.map[0][c] != TANK_EMPTY) continue;
            boolean blocked = false;
            for (Tank t : state.allTanks()) if (t.alive && t.row == 0 && t.col == c) { blocked = true; break; }
            if (blocked) continue;
            Tank enemy = new Tank(0, c, TANK_DIR_DOWN);
            enemy.aiCooldown = 5;
            state.enemies.add(enemy);
            state.enemiesQueued--;
            return;
        }
    }

    private void tankEndLevel(TankState state, boolean win) {
        if (state.gameOver) return;
        state.gameOver = true;
        stopTankLoop(state);
        state.best = Math.max(state.best, state.score);
        saveProgress("tank_best", state.best);
        if (win) {
            int next = state.level + 1;
            saveProgress("tank_level", next);
            new AlertDialog.Builder(this)
                    .setTitle("过关啦")
                    .setMessage("击毁全部敌人，本关得分：" + state.score + "，下一关：" + next)
                    .setPositiveButton("下一关", (d, w) -> renderTankGame(new TankState(next, state.best)))
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("本关失败")
                    .setMessage(state.baseDestroyed ? "老鹰基地被击毁。" : "全部生命用尽。")
                    .setPositiveButton("重开本关", (d, w) -> renderTankGame(new TankState(state.level, state.best)))
                    .setNegativeButton("返回", (d, w) -> showHome())
                    .show();
        }
    }

    private class TankState {
        final int level;
        int best;
        int score;
        int playerLives = 3;
        int enemiesQueued;
        int enemiesRemaining;
        int maxAliveEnemies = 4;
        int baseRow;
        int baseCol;
        int playerStartRow;
        int playerStartCol;
        boolean baseDestroyed;
        boolean gameOver;
        int playerHeldDir = -1;
        Runnable tickRunnable;
        final int[][] map = new int[TANK_GRID][TANK_GRID];
        final Tank player;
        final List<Tank> enemies = new ArrayList<>();
        final List<Bullet> bullets = new ArrayList<>();

        TankState(int level, int best) {
            this.level = level;
            this.best = best;
            // Map: fixed for first 3 levels, random afterward.
            buildTankMap(level);
            int totalEnemies = Math.min(12, 5 + (level - 1));
            this.enemiesQueued = totalEnemies;
            this.enemiesRemaining = totalEnemies;
            // Player spawns near the base.
            playerStartRow = TANK_GRID - 1;
            playerStartCol = Math.max(0, baseCol - 2);
            // Make sure spawn is empty.
            if (map[playerStartRow][playerStartCol] != TANK_EMPTY) {
                map[playerStartRow][playerStartCol] = TANK_EMPTY;
            }
            this.player = new Tank(playerStartRow, playerStartCol, TANK_DIR_UP);
        }

        List<Tank> allTanks() {
            List<Tank> all = new ArrayList<>();
            if (player.alive) all.add(player);
            all.addAll(enemies);
            return all;
        }

        private void buildTankMap(int level) {
            // Base at bottom-center.
            baseRow = TANK_GRID - 1;
            baseCol = TANK_GRID / 2;
            map[baseRow][baseCol] = TANK_BASE;
            // Surround base with bricks.
            int[][] guard = {{baseRow - 1, baseCol - 1}, {baseRow - 1, baseCol}, {baseRow - 1, baseCol + 1},
                    {baseRow, baseCol - 1}, {baseRow, baseCol + 1}};
            for (int[] g : guard) if (inBounds(g[0], g[1])) map[g[0]][g[1]] = TANK_BRICK;

            if (level <= TANK_FIXED_LEVELS) {
                buildFixedTankMap(level);
            } else {
                buildRandomTankMap();
            }
        }

        private boolean inBounds(int r, int c) {
            return r >= 0 && r < TANK_GRID && c >= 0 && c < TANK_GRID;
        }

        private void buildFixedTankMap(int level) {
            // Three small handcrafted layouts emphasizing brick walls & corridors.
            if (level == 1) {
                // Cross of bricks in the center.
                for (int r = 4; r <= 8; r++) map[r][6] = TANK_BRICK;
                for (int c = 4; c <= 8; c++) map[6][c] = TANK_BRICK;
                // Steel pillars at corners.
                map[2][2] = TANK_STEEL;
                map[2][TANK_GRID - 3] = TANK_STEEL;
                map[TANK_GRID - 3][2] = TANK_STEEL;
                map[TANK_GRID - 3][TANK_GRID - 3] = TANK_STEEL;
            } else if (level == 2) {
                // Two horizontal brick walls leaving a center corridor.
                for (int c = 1; c < TANK_GRID - 1; c++) {
                    if (c != 6) {
                        map[3][c] = TANK_BRICK;
                        map[9][c] = TANK_BRICK;
                    }
                }
                map[6][3] = TANK_STEEL;
                map[6][TANK_GRID - 4] = TANK_STEEL;
            } else { // level 3
                // Chambered layout with grass cover near the base.
                for (int r = 2; r <= 4; r++) {
                    map[r][3] = TANK_BRICK;
                    map[r][TANK_GRID - 4] = TANK_BRICK;
                }
                for (int c = 3; c <= TANK_GRID - 4; c++) map[5][c] = TANK_BRICK;
                map[5][6] = TANK_EMPTY;
                for (int r = TANK_GRID - 5; r < TANK_GRID - 2; r++) {
                    for (int c = 1; c <= 3; c++) map[r][c] = TANK_GRASS;
                    for (int c = TANK_GRID - 4; c <= TANK_GRID - 2; c++) map[r][c] = TANK_GRASS;
                }
            }
        }

        private void buildRandomTankMap() {
            // Sprinkle bricks (~22%), steel (~3%), grass (~6%) into empty cells.
            for (int r = 0; r < TANK_GRID; r++) {
                for (int c = 0; c < TANK_GRID; c++) {
                    if (map[r][c] != TANK_EMPTY) continue;
                    // Reserve top spawn lanes (row 0) as empty.
                    if (r == 0) continue;
                    int rv = random.nextInt(100);
                    if (rv < 22) map[r][c] = TANK_BRICK;
                    else if (rv < 25) map[r][c] = TANK_STEEL;
                    else if (rv < 31) map[r][c] = TANK_GRASS;
                }
            }
        }
    }

    private static class Tank {
        int row, col, dir;
        boolean alive = true;
        int aiCooldown;
        Tank(int row, int col, int dir) { this.row = row; this.col = col; this.dir = dir; }
    }

    private static class Bullet {
        int row, col, dir;
        boolean fromPlayer;
        boolean alive;
    }

    private class TankBoardView extends View {
        private final TankState state;
        private final TextView status;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF boardRect = new RectF();
        private float cell;

        TankBoardView(TankState state, TextView status) {
            super(MainActivity.this);
            this.state = state;
            this.status = status;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float side = Math.min(getWidth() - dp(16), getHeight() - dp(16));
            float left = (getWidth() - side) / 2f;
            float top = (getHeight() - side) / 2f;
            boardRect.set(left, top, left + side, top + side);
            cell = side / TANK_GRID;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(40, 40, 40));
            canvas.drawRect(boardRect, paint);
            // Cells.
            for (int r = 0; r < TANK_GRID; r++) {
                for (int c = 0; c < TANK_GRID; c++) {
                    int v = state.map[r][c];
                    if (v == TANK_EMPTY) continue;
                    float x = boardRect.left + c * cell;
                    float y = boardRect.top + r * cell;
                    RectF rect = new RectF(x, y, x + cell, y + cell);
                    if (v == TANK_BRICK) paint.setColor(Color.rgb(189, 99, 56));
                    else if (v == TANK_STEEL) paint.setColor(Color.rgb(190, 190, 200));
                    else if (v == TANK_GRASS) paint.setColor(Color.rgb(80, 170, 70));
                    else if (v == TANK_BASE) paint.setColor(Color.rgb(220, 200, 70));
                    canvas.drawRect(rect, paint);
                    if (v == TANK_BASE) {
                        paint.setColor(Color.rgb(120, 70, 30));
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setTextSize(cell * 0.7f);
                        canvas.drawText("🦅", rect.centerX(), rect.centerY() + cell * 0.25f, paint);
                    }
                }
            }
            // Tanks.
            for (Tank t : state.enemies) drawTank(canvas, t, Color.rgb(220, 90, 90));
            if (state.player.alive) drawTank(canvas, state.player, Color.rgb(110, 200, 240));
            // Bullets.
            paint.setColor(Color.rgb(255, 230, 100));
            for (Bullet b : state.bullets) {
                if (!b.alive) continue;
                float bx = boardRect.left + b.col * cell + cell / 2f;
                float by = boardRect.top + b.row * cell + cell / 2f;
                canvas.drawCircle(bx, by, cell * 0.18f, paint);
            }
        }

        private void drawTank(Canvas canvas, Tank t, int color) {
            float x = boardRect.left + t.col * cell;
            float y = boardRect.top + t.row * cell;
            RectF body = new RectF(x + cell * 0.1f, y + cell * 0.1f,
                    x + cell * 0.9f, y + cell * 0.9f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawRoundRect(body, dp(3), dp(3), paint);
            // Barrel.
            paint.setColor(Color.rgb(50, 50, 50));
            float cx = body.centerX();
            float cy = body.centerY();
            RectF barrel;
            switch (t.dir) {
                case TANK_DIR_UP:
                    barrel = new RectF(cx - cell * 0.08f, y, cx + cell * 0.08f, cy);
                    break;
                case TANK_DIR_DOWN:
                    barrel = new RectF(cx - cell * 0.08f, cy, cx + cell * 0.08f, y + cell);
                    break;
                case TANK_DIR_LEFT:
                    barrel = new RectF(x, cy - cell * 0.08f, cx, cy + cell * 0.08f);
                    break;
                default: // RIGHT
                    barrel = new RectF(cx, cy - cell * 0.08f, x + cell, cy + cell * 0.08f);
            }
            canvas.drawRect(barrel, paint);
        }
    }
}
