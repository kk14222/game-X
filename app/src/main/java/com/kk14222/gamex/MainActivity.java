package com.kk14222.gamex;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
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
        addHomeButton("猪了个猪", "随机三消闯关，带撤回、洗牌、移除道具。道具需答 100 以内加减法。", () -> showTileGame("pig", "猪了个猪", PIG_SYMBOLS, 7));
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
        return Math.max(fallback, prefs.getInt(key, fallback));
    }

    private void saveProgress(String key, int value) {
        prefs.edit().putInt(key, value).apply();
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
            grid.addView(tile, new ViewGroupParams(dp(58), dp(58)));
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
        int a = random.nextInt(100);
        int b = random.nextInt(100);
        boolean plus = random.nextBoolean();
        if (!plus && b > a) {
            int swap = a;
            a = b;
            b = swap;
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
                handler.postDelayed(this, Math.max(110, 280 - state.level * 4L));
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
                grid.addView(cell, new ViewGroupParams(dp(62), dp(62)));
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
                grid.addView(cell, new ViewGroupParams(dp(72), dp(72)));
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
            int groups = Math.min(symbols.length, 4 + level % 5 + random.nextInt(3));
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
