package com.example.botstock;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.graphics.Color;
import android.widget.*;
import java.text.NumberFormat;
import java.util.*;
import android.content.SharedPreferences;

public class MainActivity extends Activity {

    private TextView textView;
	private Handler handler;
	private Runnable runnable;

	private Handler playerMiningHandler;
	private Runnable playerMiningRunnable;

// Variabel mining global (bisa diubah kapan saja)
	private int minerStockPerMining = 10;
	private double minerMiningReward = 1000;
	private int minerMiningInterval = 5000; // interval mining untuk miner (NPC)
	private int playerStockPerMining = 	100000;
	private double playerMiningReward = 10;
	private int playerMiningInterval = 10000; // interval mining untuk player
	private boolean playerMiningActive = false;
	
	private static final int MINING_HALVING_CYCLE = 30; // setiap 30 putaran, misal
	private static final int MIN_MINER_STOCK_PER_MINING = 1;
	private static final double MIN_MINER_REWARD = 1.0;
	private static final int PLAYER_MINING_HALVING_CYCLE = 30; // misal, tiap 30 kali mining
	private static final int MIN_PLAYER_STOCK_PER_MINING = 1;
	private static final double MIN_PLAYER_REWARD = 1.0;
	private int playerMiningSession = 0; // hitung berapa kali player mining
	
	private double pricePerStock = 1;
	private double priceVolatility = 0.03;
	private NumberFormat rupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

// Supply
	private static final int MAX_SUPPLY = 1_000_000_000;
	private int totalStockAll = 0;
	private int totalStockAvailable = 0;
	private int deadStock = 0;

// Bot
	private Bot playerBot;
	private ArrayList<Bot> bots = new ArrayList<>();
	private ArrayList<Bot> miners = new ArrayList<>();
	private ArrayList<Bot> whales = new ArrayList<>();
	private ArrayList<Bot> institutions = new ArrayList<>();
	private ArrayList<Bot> richs = new ArrayList<>();
	private ArrayList<Bot> trillionaires = new ArrayList<>();
	private ArrayList<Bot> exitedBots = new ArrayList<>();
	private ArrayList<Bot> allBotsEver = new ArrayList<>(); // Untuk tracking richest ever

	private int round = 0;
	private double happiness = 100.0;

	private StringBuilder log = new StringBuilder();

// Constants
	private static final int FIRST_BOT_COUNT = 112;
	private static final int NEW_BOT_CHANCE = 100;
	private static final int BOT_EXIT_CHANCE = 10;
	private static final int MAX_BOT_PER_ROUND = 100;
	private static final int MAX_MINER = 0;
	private static final int MAX_WHALE = 30;
	private static final int MAX_INSTITUTION = 20;
	private static final int MAX_RICH = 5;
	private static final int MAX_TRILLIONAIRE = 10;
	private static final double PANIC_SELL_THRESHOLD = 0.9;
	private static final double MIN_PRICE = 100;

    // UI
    private EditText inputQty;
    private Button btnBuy, btnSell, btnRefresh, btnThrowStock, btnMine, btnStopMine;
    private TextView txtPlayer, txtMenu, txtHappiness, txtMiningMenu, txtDeadStock, txtPlayerTotal, txtPlayerMine, txtRichest;
    private long lastPlayerStockValueCompare = 0;

    // SharedPreferences for saving/loading
    private static final String PREF_NAME = "botstock_save";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollViewAll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 12, 12, 12);

        txtPlayer = new TextView(this);
        txtPlayer.setTextSize(18f);
        txtPlayer.setPadding(10, 16, 10, 8);
        txtPlayer.setBackgroundColor(0xFFDFEAF7);
        root.addView(txtPlayer);

        txtPlayerTotal = new TextView(this);
        txtPlayerTotal.setTextSize(16f);
        txtPlayerTotal.setPadding(10, 0, 10, 4);
        txtPlayerTotal.setGravity(Gravity.START);
        txtPlayerTotal.setBackgroundColor(0xFFE6F8DF);
        root.addView(txtPlayerTotal);

        LinearLayout playerMineMenu = new LinearLayout(this);
        playerMineMenu.setOrientation(LinearLayout.HORIZONTAL);
        playerMineMenu.setPadding(10, 0, 10, 0);

        txtPlayerMine = new TextView(this);
        txtPlayerMine.setPadding(5, 0, 20, 0);
        txtPlayerMine.setTextSize(10f);
        playerMineMenu.addView(txtPlayerMine);

        btnMine = new Button(this);
        btnMine.setText("Aktifkan Mining Player");
        playerMineMenu.addView(btnMine);

        btnStopMine = new Button(this);
        btnStopMine.setText("Hentikan Mining Player");
        playerMineMenu.addView(btnStopMine);
        btnStopMine.setEnabled(false);

        root.addView(playerMineMenu);

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.HORIZONTAL);
        menu.setPadding(10, 0, 10, 0);

        inputQty = new EditText(this);
        inputQty.setHint("Qty");
        inputQty.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputQty.setEms(5);
        menu.addView(inputQty);

        btnBuy = new Button(this);
        btnBuy.setText("Beli");
        menu.addView(btnBuy);

        btnSell = new Button(this);
        btnSell.setText("Jual");
        menu.addView(btnSell);

        btnThrowStock = new Button(this);
        btnThrowStock.setText("Buang");
        menu.addView(btnThrowStock);

        btnRefresh = new Button(this);
        btnRefresh.setText("Refresh");
        menu.addView(btnRefresh);

        root.addView(menu);

        txtMenu = new TextView(this);
        txtMenu.setPadding(15, 5, 15, 5);
        txtMenu.setBackgroundColor(0xFFE7F0FF);
        root.addView(txtMenu);

        txtHappiness = new TextView(this);
        txtHappiness.setPadding(15, 5, 15, 5);
        root.addView(txtHappiness);

        txtMiningMenu = new TextView(this);
        txtMiningMenu.setPadding(15, 5, 15, 5);
        txtMiningMenu.setTextSize(16f);
        root.addView(txtMiningMenu);

        txtDeadStock = new TextView(this);
        txtDeadStock.setPadding(15, 5, 15, 5);
        root.addView(txtDeadStock);

        txtRichest = new TextView(this);
        txtRichest.setPadding(15, 10, 15, 10);
        txtRichest.setTextSize(17f);
        txtRichest.setGravity(Gravity.CENTER_HORIZONTAL);
        txtRichest.setBackgroundColor(0xFFEAEAEA);
        root.addView(txtRichest);

        textView = new TextView(this);
        textView.setPadding(15, 10, 15, 10);
        textView.setTextSize(14f);
        textView.setGravity(Gravity.START);
        textView.setVerticalScrollBarEnabled(true);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(textView);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        scrollViewAll.addView(root);
        setContentView(scrollViewAll);

        // SharedPrefs
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
		
		String event = triggerEvent();
        if (event != null) {
            txtMenu.setText(event);
        }
    }

        if (!loadGame()) {
			playerBot = new Bot("PLAYER", 5_000_000_000L, 0, BotType.PLAYER);
			allBotsEver.add(playerBot);

			Random random = new Random();

			for (int i = 0; i < FIRST_BOT_COUNT; i++) {
				Bot b = new Bot("BotTrader" + (i + 1), randomSaldo(), 0, BotType.TRADER);
				bots.add(b);
				allBotsEver.add(b);
			}

			// Ganti miningPerRound dengan DEFAULT_MINER_COUNT atau sesuai kebutuhan
			int initialMinerCount = 5; // Atur sesuai kebutuhan awal, misal 10 miner NPC
			for (int i = 0; i < initialMinerCount; i++) {
				Bot b = new Bot("MinerNPC" + (i + 1), 0, 0, BotType.MINER);
				miners.add(b);
				allBotsEver.add(b);
			}

			for (int i = 0; i < MAX_WHALE; i++) {
				Bot b = new Bot("Whale" + (i + 1), 500_000_000 + random.nextInt(350_000_000), 0, BotType.WHALE);
				whales.add(b);
				allBotsEver.add(b);
			}
			for (int i = 0; i < MAX_INSTITUTION; i++) {
				Bot b = new Bot("Institution" + (i + 1), 1_000_000_000 + random.nextInt(1_000_000_000), 0, BotType.INSTITUTION);
				institutions.add(b);
				allBotsEver.add(b);
			}
			for (int i = 0; i < MAX_RICH; i++) {
				Bot b = new Bot("RichGuy" + (i + 1), 50_000_000 + random.nextInt(500_000_000), 0, BotType.RICH);
				richs.add(b);
				allBotsEver.add(b);
			}
			for (int i = 0; i < MAX_TRILLIONAIRE; i++) {
				long saldoAwal = 500_000_000_000L + (long) (random.nextDouble() * 250_000_000_000L);
				Bot b = new Bot("Trillionaire" + (trillionaires.size() + 1), saldoAwal, 0, BotType.TRILLIONAIRE);
				trillionaires.add(b);
				allBotsEver.add(b);
			}
		}

        btnBuy.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					handlePlayerAction(PlayerAction.BUY);
				}
			});
        btnSell.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					handlePlayerAction(PlayerAction.SELL);
				}
			});
        btnThrowStock.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					handlePlayerAction(PlayerAction.THROW);
				}
			});
        btnRefresh.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					updateTextView();
				}
			});

        btnMine.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startPlayerMining();
				}
			});

        btnStopMine.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					stopPlayerMining();
				}
			});

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                simulateMarket();
                updateTextView();
                saveGame();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(runnable);

        playerMiningHandler = new Handler();

        updateTextView();
    }

    private double randomSaldo() {
        return 100_000_000 + new Random().nextInt(1_000_000_000);
    }

    private String triggerEvent() {
        int roll = new Random().nextInt(1000);
		if (roll < 40) { pricePerStock = Math.max(MIN_PRICE, pricePerStock * 0.7); happiness -= 100 + roll%8; return "PERANG! Harga crypto jatuh!"; }
		else if (roll < 80) { pricePerStock = Math.max(MIN_PRICE, pricePerStock * 100.0); happiness -= 10 + roll%6; return "Bencana besar! Harga crypto turun!"; }
		else if (roll < 120) { pricePerStock *= 1.4 + new Random().nextDouble() * 0.3; happiness += 7 + roll%10; return "FOMO! Harga naik!"; }
		else if (roll < 160) { pricePerStock = Math.max(MIN_PRICE, pricePerStock * 0.5); happiness -= 15 + roll%7; return "FUD! Harga crypto anjlok!"; }
		else if (roll < 200) { pricePerStock *= 1.5 + new Random().nextDouble(); happiness += 15 + roll%10; return "Whale PUMP besar!"; }
		else if (roll < 240) { pricePerStock = Math.max(MIN_PRICE, pricePerStock * 0.6); happiness -= 17 + roll%12; return "Whale DUMP!"; }
		else if (roll < 280) { pricePerStock *= 1.25 + new Random().nextDouble()*0.25; happiness += 10 + roll%6; return "Institusi besar masuk!"; }
		else if (roll < 320) { pricePerStock = Math.max(MIN_PRICE, pricePerStock * 0.82); happiness -= 8 + roll%6; return "Regulasi buruk!"; }
		else if (roll < 360) { pricePerStock *= 1.8 + new Random().nextDouble()*0.6; happiness += 12 + roll%8; return "Trilioner memborong!"; }
		else if (roll < 400) { pricePerStock = Math.max(MIN_PRICE, pricePerStock * 0.4); happiness -= 18 + roll%12; return "Trilioner DUMP!"; }
		return null;
    }

	private void simulateMarket() {
		round++;
		log.append(String.format(Locale.US, "Putaran %d:\n", round));
		String event = triggerEvent();
		if (event != null) log.append("!! ").append(event).append("\n");

		// Sistem Halving untuk Miner/NPC
		if (round > 0 && round % MINING_HALVING_CYCLE == 0) {
			int prevStock = minerStockPerMining;
			double prevReward = minerMiningReward;

			minerStockPerMining = Math.max(minerStockPerMining / 2, MIN_MINER_STOCK_PER_MINING);
			minerMiningReward = Math.max(minerMiningReward / 2, MIN_MINER_REWARD);

			log.append(">> Mining reward HALVING! Sekarang per mining: ")
				.append(minerStockPerMining)
				.append(" koin. Reward: ")
				.append(rupiah.format(minerMiningReward))
				.append("\n");
		}

		if (happiness < 0) happiness = 0; 
		if (happiness > 100) happiness = 100;

		int minedTotal = 0;
		for (int i = 0; i < miners.size(); i++) {
			Bot miner = miners.get(i);
			if (totalStockAll < MAX_SUPPLY) {
				int sisa = MAX_SUPPLY - totalStockAll;
				int mined = Math.min(minerStockPerMining, sisa);
				miner.stock += mined;
				totalStockAll += mined;
				minedTotal += mined;
				miner.saldo += minerMiningReward;
				log.append(Html.fromHtml(
							   minerNameColor(miner.name) + " mining +" + mined + " (saldo +" + rupiah.format(minerMiningReward) + ")<br>"
						   ));
			}
		}
		// (Bagian kode lain tetap)
        if (totalStockAll >= MAX_SUPPLY) log.append(">> Mining telah mencapai batas maksimum supply!\n");

        ArrayList<ArrayList<Bot>> allBotGroups = new ArrayList<ArrayList<Bot>>();
        allBotGroups.add(bots); allBotGroups.add(miners); allBotGroups.add(whales); allBotGroups.add(institutions); allBotGroups.add(richs); allBotGroups.add(trillionaires);
        for (int g = 0; g < allBotGroups.size(); g++) {
            ArrayList<Bot> group = allBotGroups.get(g);
            ArrayList<Bot> keluar = new ArrayList<Bot>();
            for (int j = 0; j < group.size(); j++) {
                Bot bot = group.get(j);
                if (bot.type == BotType.PLAYER) continue;
                if (new Random().nextInt(100) < BOT_EXIT_CHANCE) keluar.add(bot);
            }
            for (int j = 0; j < keluar.size(); j++) {
                Bot bot = keluar.get(j);
                group.remove(bot);
                exitedBots.add(bot);
                log.append(">> ").append(bot.name).append(" keluar dari market sementara.\n");
            }
        }
        int botMasuk = 0;
        for (int i = 0; i < MAX_BOT_PER_ROUND; i++) {
            if(new Random().nextInt(100) < NEW_BOT_CHANCE && !exitedBots.isEmpty()) {
                Bot comeback = exitedBots.remove(0);
                getGroupByType(comeback.type).add(comeback);
                if (!allBotsEver.contains(comeback)) allBotsEver.add(comeback);
                log.append(">> ").append(comeback.name).append(" kembali ke market!\n");
                botMasuk++;
            } else if (new Random().nextInt(100) < NEW_BOT_CHANCE) {
				BotType[] possibleTypes = {BotType.TRADER, BotType.MINER, BotType.WHALE, BotType.INSTITUTION, BotType.RICH, BotType.TRILLIONAIRE};
				BotType t = possibleTypes[new Random().nextInt(possibleTypes.length)];
				double saldo = t == BotType.MINER ? 0 : randomSaldo();
				String name = (t == BotType.MINER ? "MinerNPCBaru" : "BotTraderBaru") + (getGroupByType(t).size() + 1);
				Bot bot = new Bot(name, saldo, 0, t);
				getGroupByType(t).add(bot);
				allBotsEver.add(bot);
				botMasuk++;
			}
        }
        if(botMasuk > 0) log.append(" >> ").append(botMasuk).append(" bot masuk pasar!\n");

        ArrayList<Bot> allBots = new ArrayList<Bot>();
        allBots.addAll(bots); allBots.addAll(miners); allBots.addAll(whales); allBots.addAll(institutions); allBots.addAll(richs); allBots.addAll(trillionaires); allBots.add(playerBot);

        for (int k = 0; k < allBots.size(); k++) {
			Bot bot = allBots.get(k);
			if (bot.type == BotType.PLAYER) continue;
			boolean ikut = false;
			switch(bot.type) {
				case WHALE: case INSTITUTION: case TRILLIONAIRE:
					if (new Random().nextInt(100) < (int)(75 + happiness/5)) ikut = true; break;
				case RICH:
					if (new Random().nextInt(100) < (int)(60 + happiness/7)) ikut = true; break;
				case MINER:
					if (new Random().nextInt(100) < (int)(55 + happiness / 10)) ikut = true; break;
				case TRADER: default:
					if (new Random().nextInt(100) < (int)(45 + happiness/10)) ikut = true; break;
			}

			if (!ikut) { 
				log.append(" - ").append(botNameColor(bot)).append(" pasif.\n"); 
				continue; 
			}

			boolean aksiBesar = (bot.type == BotType.WHALE || bot.type == BotType.INSTITUTION || bot.type == BotType.TRILLIONAIRE) 
				&& new Random().nextInt(100) < 80 + happiness/4;

			boolean willSell = false, panic = false;
			if (bot.buyCount > 0 && pricePerStock < bot.avgBuyPrice * PANIC_SELL_THRESHOLD && bot.stock > 0 && new Random().nextInt(100) < 65) {
				willSell = true; panic = true;
			} else if (bot.stock > 0 && new Random().nextInt(100) < 50 - (happiness/5)) {
				willSell = true;
			}

			boolean willThrow = bot.stock > 0 && new Random().nextInt(1000) < 10;
			if (willThrow) {
				int qty = 1 + new Random().nextInt(bot.stock);
				bot.stock -= qty; deadStock += qty;
				log.append(" - ").append(botNameColor(bot)).append(" membuang ").append(qty).append(" stock ke stock mati.\n");
				continue;
			}

			if (willSell && bot.stock > 0) {
				int qty = panic ? bot.stock : (aksiBesar ? (int)Math.max(1, bot.stock * (0.7 + new Random().nextDouble() * 0.3)) : 1 + new Random().nextInt(bot.stock));
				qty = Math.min(qty, bot.stock);
				double hasil = qty * pricePerStock;
				bot.stock -= qty;
				bot.saldo += hasil;
				bot.sellCount += qty;
				totalStockAvailable += qty;
				double turun = pricePerStock * (qty / (double)(totalStockAvailable + 1)) * 
					(panic ? 0.3 : (aksiBesar ? 0.15 : 0.05)) * 
					(1.1 - (happiness/100.0)) + acakVolatil();
				pricePerStock = Math.max(MIN_PRICE, pricePerStock - turun);
				if(bot.type == BotType.WHALE || bot.type == BotType.INSTITUTION || bot.type == BotType.TRILLIONAIRE) 
					happiness -= qty * 0.0015;
				log.append(" - ").append(botNameColor(bot))
					.append(panic ? " PANIC SELL " : (aksiBesar ? " JUAL BESAR " : " MENJUAL "))
					.append(qty).append(" stock @").append(rupiah(pricePerStock + turun))
					.append(" → ").append(rupiah(pricePerStock))
					.append(" (+").append(rupiah(hasil)).append(" saldo)\n");
			}
			else if (bot.saldo > pricePerStock && totalStockAvailable > 0) {
				double maxBuy = Math.min(bot.saldo / pricePerStock, totalStockAvailable);
				if (maxBuy < 1) { 
					log.append(" - ").append(botNameColor(bot)).append(" saldo tidak cukup atau stock kosong.\n"); 
					continue; 
				}
				int qty = aksiBesar ? (int)Math.max(1, maxBuy * (0.5 + new Random().nextDouble() * 0.5)) : 1 + new Random().nextInt((int)maxBuy);
				qty = Math.min(qty, totalStockAvailable);
				double biaya = qty * pricePerStock;
				bot.stock += qty;
				bot.saldo -= biaya;
				bot.buyCount += qty;
				bot.totalBuyPrice += pricePerStock * qty;
				bot.avgBuyPrice = bot.totalBuyPrice / bot.buyCount;
				totalStockAvailable -= qty;

				double naik = pricePerStock * (qty / (double)(totalStockAvailable + 1)) * 
					(aksiBesar ? 0.15 : 0.07) * 
					(0.85 + (happiness/120.0)) + acakVolatil();
				pricePerStock += naik;

				if(bot.type == BotType.WHALE || bot.type == BotType.INSTITUTION || bot.type == BotType.TRILLIONAIRE) 
					happiness += qty * 0.001;
				log.append(" - ").append(botNameColor(bot))
					.append(aksiBesar ? " BELI BESAR " : " MEMBELI ").append(qty)
					.append(" stock @").append(rupiah(pricePerStock - naik))
					.append(" → ").append(rupiah(pricePerStock))
					.append(" (-").append(rupiah(biaya)).append(" saldo)\n");
			} else {
				log.append(" - ").append(botNameColor(bot)).append(" pasif/tidak bisa transaksi.\n");
			}
		}

// Simpan harga sebelum volatilitas harian
		double oldPrice = pricePerStock;

// Volatilitas harian (lebih kecil)
		pricePerStock += acakVolatil() * 0.5;

// Spike harga kalau supply tipis (lebih lemah)
		if (totalStockAvailable < 250 && totalStockAvailable > 0) {
			double spike = pricePerStock * 0.03 * (250 - totalStockAvailable) / 250;
			pricePerStock += spike;
			log.append(" >> Harga spike karena supply sangat tipis!\n");
		}

// Batasi kenaikan harga per iterasi (maksimal 25%)
		double maxKenaikan = oldPrice * 0.25;
		if (pricePerStock - oldPrice > maxKenaikan) {
			pricePerStock = oldPrice + maxKenaikan;
			log.append(" >> Kenaikan harga dibatasi agar tetap wajar.\n");
		}

// Normalisasi happiness
		if(happiness > 100) happiness = 100;
		if(happiness < 0) happiness = 0;

// Batasan log agar tidak terlalu panjang
		String[] lines = log.toString().split("\n");
		if (lines.length > 25) {
			log = new StringBuilder();
			for (int i = lines.length - 25; i < lines.length; i++) {
				log.append(lines[i]).append("\n");
			}
		}
	}
    private ArrayList<Bot> getGroupByType(BotType type) {
        switch(type) {
            case MINER: return miners;
            case WHALE: return whales;
            case INSTITUTION: return institutions;
            case RICH: return richs;
            case TRILLIONAIRE: return trillionaires;
            default: return bots;
        }
    }

    private double acakVolatil() {
        return (new Random().nextDouble() - 0.5) * pricePerStock * priceVolatility;
    }

    private String rupiah(double val) {
        return rupiah.format(val);
    }

    private enum PlayerAction { BUY, SELL, THROW }

	private void handlePlayerAction(PlayerAction action) {
		String sQty = inputQty.getText().toString().trim();
		int qty;

		if (sQty.isEmpty()) {
			txtMenu.setText("Masukkan jumlah terlebih dahulu!");
			return;
		}

		try {
			qty = Integer.parseInt(sQty);
		} catch (NumberFormatException e) {
			txtMenu.setText("Qty tidak valid!");
			return;
		}

		if (qty < 1) {
			txtMenu.setText("Qty minimal 1!");
			return;
		}

		switch (action) {
			case BUY: {
					if (qty > totalStockAvailable) {
						txtMenu.setText("Stok tersedia hanya " + totalStockAvailable + "!");
						return;
					}

					double biaya = qty * pricePerStock;
					if (playerBot.saldo < biaya) {
						txtMenu.setText("Saldo tidak cukup!");
						return;
					}

					playerBot.stock += qty;
					playerBot.saldo -= biaya;
					playerBot.buyCount += qty;
					playerBot.totalBuyPrice += pricePerStock * qty;
					playerBot.avgBuyPrice = playerBot.totalBuyPrice / playerBot.buyCount;
					totalStockAvailable -= qty;

					double naik = pricePerStock * (qty / (double)(totalStockAvailable + 1)) * 0.13 * (0.85 + (happiness / 120.0)) + acakVolatil();
					pricePerStock += naik;
					happiness += qty * 0.0005;

					txtMenu.setText("Beli " + qty + " stock berhasil!");
					log.append("PLAYER membeli ").append(qty)
						.append(" stock @").append(rupiah(pricePerStock - naik))
						.append(" → ").append(rupiah(pricePerStock))
						.append(" (total: ").append(rupiah(biaya)).append(")\n");
					break;
				}

			case SELL: {
					if (qty > playerBot.stock) {
						txtMenu.setText("Stock anda hanya " + playerBot.stock + "!");
						return;
					}

					double hasil = qty * pricePerStock;
					playerBot.stock -= qty;
					playerBot.saldo += hasil;
					playerBot.sellCount += qty;
					totalStockAvailable += qty;

					boolean panic = (playerBot.buyCount > 0 && pricePerStock < playerBot.avgBuyPrice * PANIC_SELL_THRESHOLD && new Random().nextInt(100) < 70);
					double turun = pricePerStock * (qty / (double)(totalStockAvailable + 1)) *
						(panic ? 0.15 : 0.09) * (1.1 - (happiness / 100.0)) + acakVolatil();
					pricePerStock = Math.max(MIN_PRICE, pricePerStock - turun);

					if (panic) happiness -= qty * 0.002;

					txtMenu.setText((panic ? "PANIC SELL! " : "") + "Jual " + qty + " stock berhasil!");
					log.append("PLAYER ").append(panic ? "PANIC SELL " : "menjual ")
						.append(qty).append(" stock @").append(rupiah(pricePerStock + turun))
						.append(" → ").append(rupiah(pricePerStock))
						.append(" (total: ").append(rupiah(hasil)).append(")\n");
					break;
				}

			case THROW: {
					if (qty > playerBot.stock) {
						txtMenu.setText("Stock anda hanya " + playerBot.stock + "!");
						return;
					}

					playerBot.stock -= qty;
					deadStock += qty;

					txtMenu.setText("Buang " + qty + " stock ke stock mati berhasil!");
					log.append("PLAYER membuang ").append(qty)
						.append(" stock ke stock mati (tidak bisa dijual lagi).\n");
					break;
				}
		}

		// ✅ Trigger event acak setelah aksi pemain
		if (new Random().nextInt(100) < 15) { // 15% kemungkinan muncul event
			String event = game.triggerEvent();
			if (event != null) {
				log.append("\n⚠ EVENT: ").append(event).append("\n");
				txtMenu.setText(txtMenu.getText() + "\n⚠ " + event);
			}
		}

		updateTextView();
		saveGame();
	}
	

    // Player mining action as a background process (per 10 detik)
    // Tambahkan variabel global untuk tracking halving player

	private void startPlayerMining() {
		if (playerMiningActive) {
			txtMenu.setText("Mining player sudah aktif!");
			return;
		}
		if (totalStockAll >= MAX_SUPPLY) {
			txtMenu.setText("Maksimum supply sudah tercapai, tidak bisa mining lagi.");
			return;
		}
		playerMiningActive = true;
		btnMine.setEnabled(false);
		btnStopMine.setEnabled(true);
		txtMenu.setText("Mining player diaktifkan!");
		playerMiningRunnable = new Runnable() {
			@Override
			public void run() {
				if (!playerMiningActive) return;
				if (totalStockAll >= MAX_SUPPLY) {
					txtMenu.setText("Maksimum supply sudah tercapai, mining player dihentikan.");
					stopPlayerMining();
					return;
				}

				// Halving check
				playerMiningSession++;
				if (playerMiningSession > 0 && playerMiningSession % PLAYER_MINING_HALVING_CYCLE == 0) {
					int prevStock = playerStockPerMining;
					double prevReward = playerMiningReward;

					playerStockPerMining = Math.max(playerStockPerMining / 2, MIN_PLAYER_STOCK_PER_MINING);
					playerMiningReward = Math.max(playerMiningReward / 2, MIN_PLAYER_REWARD);

					log.append(Html.fromHtml(ColorString(
												 ">> PLAYER Mining reward HALVING! Sekarang per mining: " +
												 playerStockPerMining + " koin. Reward: " + rupiah(playerMiningReward) + "<br>", "#FFAA33")));
				}

				int sisa = MAX_SUPPLY - totalStockAll;
				int mined = Math.min(playerStockPerMining, sisa);
				playerBot.stock += mined;
				totalStockAll += mined;
				playerBot.saldo += playerMiningReward;
				log.append(Html.fromHtml(ColorString(
											 "PLAYER mining +" + mined + " (saldo +" + rupiah(playerMiningReward) + ")<br>", "#33AA44")));
				updateTextView();
				saveGame();
				playerMiningHandler.postDelayed(this, playerMiningInterval);
			}
		};
		playerMiningHandler.post(playerMiningRunnable);
	}

    private void stopPlayerMining() {
        if (!playerMiningActive) {
            txtMenu.setText("Mining player sudah non-aktif.");
            return;
        }
        playerMiningActive = false;
        btnMine.setEnabled(true);
        btnStopMine.setEnabled(false);
        txtMenu.setText("Mining player dihentikan.");
    }

    private void updateTextView() {
        // Info player
        String playerInfo = "PLAYER\nSaldo: " + rupiah(playerBot.saldo)
			+ " | Stock: " + playerBot.stock
			+ " | Avg Buy: " + rupiah(playerBot.avgBuyPrice)
			+ " | Total Beli: " + playerBot.buyCount
			+ " | Total Jual: " + playerBot.sellCount;
        txtPlayer.setText(playerInfo);

        // Nilai total stock player, update setiap kali updateTextView (per 5 detik)
        long playerStockValue = (long)(playerBot.stock * pricePerStock);
        String totalText = "Nilai total stock: " + rupiah(playerStockValue);
        if (lastPlayerStockValueCompare == 0) lastPlayerStockValueCompare = playerStockValue;
        if (playerStockValue > lastPlayerStockValueCompare) {
            txtPlayerTotal.setTextColor(Color.parseColor("#228B22"));
            txtPlayerTotal.setBackgroundColor(0xFFDFF8E6);
        } else if (playerStockValue < lastPlayerStockValueCompare) {
            txtPlayerTotal.setTextColor(Color.RED);
            txtPlayerTotal.setBackgroundColor(0xFFFFE6E6);
        } else {
            txtPlayerTotal.setTextColor(Color.DKGRAY);
            txtPlayerTotal.setBackgroundColor(0xFFE6F8DF);
        }
        txtPlayerTotal.setText(totalText);
        lastPlayerStockValueCompare = playerStockValue;

        if (playerMiningActive) {
			txtPlayerMine.setTextColor(Color.parseColor("#357A38"));
			txtPlayerMine.setText(
				"Mining Player: " + playerStockPerMining + " / " +
				(playerMiningInterval / 1000) + " detik | Reward: " +
				rupiah.format(playerMiningReward) + " (AKTIF)"
			);
		} else {
			txtPlayerMine.setTextColor(Color.DKGRAY);
			txtPlayerMine.setText(
				"Mining Player: " + playerStockPerMining + " / " +
				(playerMiningInterval / 1000) + " detik | Reward: " +
				rupiah.format(playerMiningReward) + " (non-aktif)"
			);
		}

		txtHappiness.setText("Kebahagiaan Market: " + String.format(Locale.US, "%.1f", happiness) + " %");
		txtMiningMenu.setText("MENU TAMBANG\n"
							  + "Miner: " + miners.size()
							  + " | Mining Per " + (minerMiningInterval / 1000) + " detik: " + minerStockPerMining
							  + " | Sudah ditambang: " + totalStockAll + " / " + MAX_SUPPLY
							  + " | Stock Mati: " + deadStock
							  );
		txtDeadStock.setText("Stock Mati: " + deadStock + " (Tidak bisa dijual/beli, tidak mempengaruhi harga)");

        StringBuilder botInfo = new StringBuilder();
        int trader = bots.size(), miner = miners.size(), whale = whales.size(), institution = institutions.size(), rich = richs.size(), trillionaire = trillionaires.size();
        double totalSaldo = 0;
        int totalStock = 0;
        double totalBuy = 0, totalSell = 0;
        ArrayList<Bot> allBots = new ArrayList<Bot>();
        allBots.addAll(bots); allBots.addAll(miners); allBots.addAll(whales); allBots.addAll(institutions); allBots.addAll(richs); allBots.addAll(trillionaires); allBots.add(playerBot);
        for (int i = 0; i < allBots.size(); i++) {
            Bot bot = allBots.get(i);
            totalSaldo += bot.saldo;
            totalStock += bot.stock;
            totalBuy += bot.buyCount;
            totalSell += bot.sellCount;
        }
        for (int i = Math.max(0, allBots.size()-8); i < allBots.size(); i++) {
            Bot bot = allBots.get(i);
            botInfo.append(bot.name)
				.append(" | Saldo: ").append(rupiah(bot.saldo))
				.append(" | Stok: ").append(bot.stock)
				.append(" | Avg Buy: ").append(rupiah(bot.avgBuyPrice)).append("\n");
        }
        textView.setText(Html.fromHtml(
							 "Simulasi Crypto Bot Market<br>" +
							 "Harga/stock  : " + rupiah(pricePerStock) + "<br>" +
							 "Supply pasar : " + totalStockAvailable + " (stock di market)<br>" +
							 "Sudah ditambang : " + totalStockAll + " / " + MAX_SUPPLY + "<br>" +
							 "Trader: " + trader + " | Miner: " + miner + " | Whale: " + whale +
							 " | Institution: " + institution + " | Rich: " + rich + " | Trillionaire: " + trillionaire +
							 " | Bot total: " + (allBots.size()-1) + "<br>" +
							 "Total saldo bot : " + rupiah(totalSaldo) + "<br>" +
							 "Total stock bot : " + totalStock + "<br>" +
							 "Total buy       : " + (int)totalBuy + " | Total sell: " + (int)totalSell + "<br><br>" +
							 "8 bot terakhir (termasuk player):<br>" + botInfo.toString().replace("\n","<br>") +
							 "<br>Log:<br>" + log.toString().replace("\n","<br>")
						 ));

        // Daftar entitas terkaya
        Bot richest = null;
        long richestValue = 0;
        for (Bot b : allBotsEver) {
            long val = (long)(b.stock * pricePerStock + b.saldo);
            if (val > richestValue) {
                richest = b;
                richestValue = val;
            }
        }
        if (richest != null) {
            String color = "#B8860B";
            String richestString = "🏆 ENTITAS TERKAYA: ";
            if (richest.type == BotType.PLAYER) {
                color = "#00A99D";
                richestString += "PLAYER";
            } else if (richest.type == BotType.MINER) {
                color = "#4682B4";
                richestString += richest.name;
            } else if (richest.type == BotType.WHALE) {
                color = "#8B0000";
                richestString += richest.name;
            } else if (richest.type == BotType.RICH) {
                color = "#4B8B3B";
                richestString += richest.name;
            } else if (richest.type == BotType.INSTITUTION) {
                color = "#353636";
                richestString += richest.name;
            } else if (richest.type == BotType.TRILLIONAIRE) {
                color = "#800080";
                richestString += richest.name;
            } else {
                richestString += richest.name;
            }
            richestString += "<br>Stock: " + richest.stock + " | Nilai: " + rupiah(richestValue);
            txtRichest.setText(Html.fromHtml("<b><font color='"+color+"'>" + richestString + "</font></b>"));
            txtRichest.setBackgroundColor(0xFFD8E6F7);
        }
    }

    private String minerNameColor(String name) {
        return "<font color='#357A38'>" + name + "</font>";
    }
    private String botNameColor(Bot bot) {
        switch (bot.type) {
            case MINER: return "<font color='#357A38'>" + bot.name + "</font>";
            case PLAYER: return "<font color='#00A99D'>" + bot.name + "</font>";
            case WHALE: return "<font color='#8B0000'>" + bot.name + "</font>";
            case INSTITUTION: return "<font color='#353636'>" + bot.name + "</font>";
            case RICH: return "<font color='#4B8B3B'>" + bot.name + "</font>";
            case TRILLIONAIRE: return "<font color='#800080'>" + bot.name + "</font>";
            default: return bot.name;
        }
    }
    private String ColorString(String s, String color) {
        return "<font color='" + color + "'>" + s + "</font>";
    }

    // == SAVE & LOAD GAME ==
    private void saveGame() {
		try {
			SharedPreferences.Editor e = prefs.edit();
			e.putString("player", serializeBot(playerBot));
			e.putString("bots", serializeBots(bots));
			e.putString("miners", serializeBots(miners));
			e.putString("whales", serializeBots(whales));
			e.putString("institutions", serializeBots(institutions));
			e.putString("richs", serializeBots(richs));
			e.putString("trillionaires", serializeBots(trillionaires));
			e.putString("exitedBots", serializeBots(exitedBots));
			e.putString("allBotsEver", serializeBots(allBotsEver));
			e.putInt("totalStockAll", totalStockAll);
			e.putInt("totalStockAvailable", totalStockAvailable);
			e.putInt("deadStock", deadStock);
			e.putLong("pricePerStock", Double.doubleToRawLongBits(pricePerStock));
			e.putLong("priceVolatility", Double.doubleToRawLongBits(priceVolatility));
			e.putInt("round", round);
			e.putFloat("happiness", (float)happiness);
			e.putString("log", log.toString());

			// SISTEM BARU: mining variabel terpisah
			e.putInt("minerStockPerMining", minerStockPerMining);
			e.putFloat("minerMiningReward", (float)minerMiningReward);
			e.putInt("minerMiningInterval", minerMiningInterval);

			e.putInt("playerStockPerMining", playerStockPerMining);
			e.putFloat("playerMiningReward", (float)playerMiningReward);
			e.putInt("playerMiningInterval", playerMiningInterval);

			e.apply();
		} catch (Exception ex) {}
	}

	private boolean loadGame() {
		try {
			if (!prefs.contains("player")) return false;
			playerBot = deserializeBot(prefs.getString("player", ""));
			bots = deserializeBots(prefs.getString("bots", ""));
			miners = deserializeBots(prefs.getString("miners", ""));
			whales = deserializeBots(prefs.getString("whales", ""));
			institutions = deserializeBots(prefs.getString("institutions", ""));
			richs = deserializeBots(prefs.getString("richs", ""));
			trillionaires = deserializeBots(prefs.getString("trillionaires", ""));
			exitedBots = deserializeBots(prefs.getString("exitedBots", ""));
			allBotsEver = deserializeBots(prefs.getString("allBotsEver", ""));
			totalStockAll = prefs.getInt("totalStockAll", 0);
			totalStockAvailable = prefs.getInt("totalStockAvailable", 0);
			deadStock = prefs.getInt("deadStock", 0);
			pricePerStock = Double.longBitsToDouble(prefs.getLong("pricePerStock", Double.doubleToRawLongBits(1000d)));
			priceVolatility = Double.longBitsToDouble(prefs.getLong("priceVolatility", Double.doubleToRawLongBits(0.03d)));
			round = prefs.getInt("round", 0);
			happiness = prefs.getFloat("happiness", 100f);
			log = new StringBuilder(prefs.getString("log", ""));

			// SISTEM BARU: mining variabel terpisah
			minerStockPerMining = prefs.getInt("minerStockPerMining", 1000);
			minerMiningReward = prefs.getFloat("minerMiningReward", 1000f);
			minerMiningInterval = prefs.getInt("minerMiningInterval", 5000);

			playerStockPerMining = prefs.getInt("playerStockPerMining", 500);
			playerMiningReward = prefs.getFloat("playerMiningReward", 500f);
			playerMiningInterval = prefs.getInt("playerMiningInterval", 10000);

			return true;
		} catch (Exception ex) {
			return false;
		}
	}
    // == SERIALIZATION HELPERS ==
    private String serializeBot(Bot b) {
        return b.name + ";" + b.saldo + ";" + b.stock + ";" + b.type + ";" + b.buyCount + ";" + b.sellCount + ";" + b.totalBuyPrice + ";" + b.avgBuyPrice;
    }
    private Bot deserializeBot(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] arr = s.split(";");
        Bot b = new Bot(arr[0], Double.parseDouble(arr[1]), Integer.parseInt(arr[2]), BotType.valueOf(arr[3]));
        b.buyCount = Integer.parseInt(arr[4]);
        b.sellCount = Integer.parseInt(arr[5]);
        b.totalBuyPrice = Double.parseDouble(arr[6]);
        b.avgBuyPrice = Double.parseDouble(arr[7]);
        return b;
    }
    private String serializeBots(ArrayList<Bot> bs) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<bs.size(); i++) {
            if (i>0) sb.append("|");
            sb.append(serializeBot(bs.get(i)));
        }
        return sb.toString();
    }
    private ArrayList<Bot> deserializeBots(String s) {
        ArrayList<Bot> res = new ArrayList<>();
        if (s == null || s.isEmpty()) return res;
        for (String part : s.split("\\|")) {
            Bot b = deserializeBot(part);
            if (b != null) res.add(b);
        }
        return res;
    }

    private enum BotType { PLAYER, TRADER, MINER, WHALE, INSTITUTION, RICH, TRILLIONAIRE }
    private static class Bot {
        String name;
        double saldo;
        int stock;
        BotType type;
        int buyCount = 0, sellCount = 0;
        double totalBuyPrice = 0, avgBuyPrice = 0;
        Bot(String name, double saldo, int stock, BotType type) {
            this.name = name; this.saldo = saldo; this.stock = stock; this.type = type;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        stopPlayerMining();
        saveGame();
    }
}
