package sample;

import com.google.gson.Gson;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;

public class Controller {

    public ChoiceBox<String> keuze, currencychoice;
    public Button startButton, stopButton, quitButton;
    public TableView coinTable;
    public TableColumn symbol, value, low24h, high24h, ath, daysago, ptoath, pfromath;
    public volatile boolean run;

    public enum TijdInterval {
        HM("30 sec", 30),
        MIN1("1 minuut", 60),
        MIN5("5 minuten", 300),
        MIN10("10 minuten", 600),
        MIN30("30 minuten", 1800),
        MIN60("1 uur", 3600);

        public final String content;
        public final int time;

        TijdInterval(String content, int time) {
            this.time = time;
            this.content = content;
        }
    }

    public enum Currency {
        USD("USD"),
        EUR("EURO");

        public final String curr;

        Currency(String curr) {
            this.curr = curr;
        }
    }

    public enum CoinLocation {
        BTC("BTC", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&ids=bitcoin"),
        BCHN("BCHN", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin-cash", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&ids=bitcoin-cash"),
        BCHA("BCHA", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin-cash-abc-2", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&ids=bitcoin-cash-abc-2"),
        BSV("BSV", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin-cash-sv", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&ids=bitcoin-cash-sv"),
        BTG("BTG", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin-gold", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&ids=bitcoin-gold"),
        BCD("BCD", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin-diamond", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&ids=bitcoin-diamond"),
        BCA("BCA", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin-atom", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&ids=bitcoin-atom"),
        ETHER("Ether", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=ethereum", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&ids=ethereum"),
        XMR("XMR", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=monero", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&ids=monero");

        public final String coin;
        public final String httpStringUSD;
        public final String httpStringEUR;

        CoinLocation(String coin, String httpStringUSD, String httpStringEUR) {
            this.coin = coin;
            this.httpStringUSD = httpStringUSD;
            this.httpStringEUR = httpStringEUR;
        }
    }

    public static class CoinGeckoMarketData {
        String id;
        String symbol;
        String name;
        String image;
        BigDecimal current_price;
        BigDecimal market_cap;
        int market_cap_rank;
        BigDecimal fully_diluted_valuation;
        BigDecimal total_volume;
        BigDecimal high_24h;
        BigDecimal low_24h;
        BigDecimal price_change_24h;
        BigDecimal price_change_percentage_24h;
        BigDecimal market_cap_change_24h;
        BigDecimal market_cap_change_percentage_24h;
        BigDecimal circulating_supply;
        BigDecimal total_supply;
        BigDecimal max_supply;
        BigDecimal ath;
        BigDecimal ath_change_percentage;
        String ath_date;
        BigDecimal atl;
        BigDecimal atl_change_percentage;
        String atl_date;
        Roi roi;
        String last_updated;

        static DecimalFormat df = new DecimalFormat("#,###.00");

        public String getSymbol() {
            return symbol.toUpperCase();
        }

        public String getValue() {
            return df.format(current_price);
        }

        public String getLow24h() {
            return df.format(low_24h);
        }

        public String getHigh24h() {
            return df.format(high_24h);
        }

        public String getAth() {
            return df.format(ath);
        }

        public long getDaysago() {
            Instant today = Instant.now();
            Instant athdate = Instant.parse(ath_date);  // ath_date is using ZULU time, so using java.time.Instant
            return Duration.between(athdate, today).toDays();
        }

        public String getPtoath() {
            BigDecimal calc1 = ath.subtract(current_price);
            BigDecimal calc2 = calc1.divide(current_price, 5, BigDecimal.ROUND_CEILING);
            BigDecimal calc3 = calc2.multiply(new BigDecimal(100.0));
            return df.format(calc3)+" %";
        }

        public String getPfromath() {
            BigDecimal calc1 = ath.subtract(current_price);
            BigDecimal calc2 = calc1.divide(ath, 5, BigDecimal.ROUND_CEILING);
            BigDecimal calc3 = new BigDecimal(1.0).subtract(calc2);
            BigDecimal calc4 = calc3.multiply(new BigDecimal(100.0));
            return df.format(calc4)+" %";
        }
    }

    public static class Roi {
        BigDecimal times;
        String currency;
        BigDecimal percentage;
    }

    public void ClickQuit() {
        System.exit(0);
    }

    public void ClickStart() {
        run = true;
        startButton.setDisable(true);
        quitButton.setDisable(true);
        stopButton.setDisable(false);
        Runnable runner = new UpdateCoinData();
        new Thread(runner).start();
    }

    public void ClickStop() {
        run = false;
    }

    public void initialize() {
        keuze.getItems().addAll(TijdInterval.HM.content, TijdInterval.MIN1.content, TijdInterval.MIN5.content, TijdInterval.MIN10.content, TijdInterval.MIN30.content, TijdInterval.MIN60.content);
        keuze.setValue(TijdInterval.MIN1.content);
        currencychoice.getItems().addAll(Currency.USD.curr, Currency.EUR.curr);
        currencychoice.setValue(Currency.EUR.curr);
        stopButton.setDisable(true);
        symbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        value.setCellValueFactory(new PropertyValueFactory<>("value"));
        low24h.setCellValueFactory(new PropertyValueFactory<>("low24h"));
        high24h.setCellValueFactory(new PropertyValueFactory<>("high24h"));
        ath.setCellValueFactory(new PropertyValueFactory<>("ath"));
        daysago.setCellValueFactory(new PropertyValueFactory<>("daysago"));
        ptoath.setCellValueFactory(new PropertyValueFactory<>("ptoath"));
        pfromath.setCellValueFactory(new PropertyValueFactory<>("pfromath"));
    }

    private class UpdateCoinData implements Runnable {

        public void run() {
            while (run) {
                long startTime = System.currentTimeMillis() / 1000;
                coinTable.getItems().clear();
                for (CoinLocation c : CoinLocation.values()) {
                    CoinGeckoMarketData cgmd = GetCoingeckoData(c);
                    coinTable.getItems().add(cgmd);
                }
                long currentTime = System.currentTimeMillis() / 1000;
                int interval = getInterval();
                while (run && ((currentTime - startTime) < interval)) {
                    try {
                        Thread.sleep(5000);
                        currentTime = System.currentTimeMillis() / 1000;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            stopButton.setDisable(true);
            startButton.setDisable(false);
            quitButton.setDisable(false);
        }
    }

    public CoinGeckoMarketData GetCoingeckoData(CoinLocation coin) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        URL url;
        try {
            if (currencychoice.getValue().equals(Currency.USD.curr)) {
                url = new URL(coin.httpStringUSD);
            } else {
                url = new URL(coin.httpStringEUR);
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
            Gson gson = new Gson();  // take care of "malformed JSON" error
            CoinGeckoMarketData[] cgData = gson.fromJson(buffer.toString().trim(), CoinGeckoMarketData[].class);
            return cgData[0];
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public int getInterval() {
        String intv = keuze.getValue();
        TijdInterval tInt = TijdInterval.HM;
        for (TijdInterval tT : TijdInterval.values()) {
            if (tT.content.equals(intv)) {
                tInt = tT;
            }
        }
        int result = switch (tInt) {
            case HM -> TijdInterval.HM.time;
            case MIN1 -> TijdInterval.MIN1.time;
            case MIN5 -> TijdInterval.MIN5.time;
            case MIN10 -> TijdInterval.MIN10.time;
            case MIN30 -> TijdInterval.MIN30.time;
            case MIN60 -> TijdInterval.MIN60.time;
        };
        return result;
    }
}
