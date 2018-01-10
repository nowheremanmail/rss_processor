package com.dag.source;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.dag.news.bo.TempNew;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dag.news.feeds.bing.NewsArticle;
import com.dag.news.feeds.bing.BingNewsAnswer;

public class BingSource extends RichSourceFunction<TempNew> {
    static private Logger logger = LoggerFactory.getLogger(BingSource.class);

    private int bingReaderMax = 100;
    private ObjectMapper mapper;
    private String acctKey;
    private String langauge;
    private volatile boolean isRunning = true;
    private long waitTime;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private SimpleDateFormat sdfs = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public BingSource(String acctKey, String language, int bingReaderMax, int waitTime) {
        this.bingReaderMax = bingReaderMax;
        this.acctKey = acctKey;
        this.langauge = language;
        this.waitTime = waitTime * 1000L * 60;
    }


    interface ProcessItem {
        boolean processItem(TempNew n);
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdfs.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdt.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper = new ObjectMapper();
    }

    private String clientID;

    @Override
    public void run(SourceContext<TempNew> ctx) throws Exception {
        logger.info("start bing " + langauge);
        while (isRunning) {
            int skip = 0;
            int i = 0;

            LocalDateTime current = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);

            Map<String, String> currentLinks = new HashMap<String, String>(bingReaderMax);

            logger.info("check bing " + langauge);
            // logger.info(feed.getUrl() + " " + skip);
            while ((i = read(langauge, skip, item -> {
                if (currentLinks.putIfAbsent(item.getLink(), "Y") == null) {
                    ctx.collect(item);
                }
                return true;
            })) > 0 && skip < bingReaderMax) {
                skip += bingReaderMax;
                logger.info("bing continue " + langauge + " " + skip);
            }

            try {

                Thread.sleep(waitTime);
            } catch (InterruptedException ex) {
                isRunning = false;
            }
            currentLinks.clear();
        }
        logger.info("end bing " + langauge);
    }

    private int read(String language, int skip, ProcessItem action) {
        InputStream in = null;
        int M = 0;
        try {
//freshness=Day

            String urlTxt;
            if (skip <= 0)
                urlTxt = "https://api.cognitive.microsoft.com/bing/v7.0/news/search?q=&safeSearch=Off&mkt="
                        + language + "&safeSearch=Off&count=" + bingReaderMax;
            else
                urlTxt = "https://api.cognitive.microsoft.com/bing/v7.0/news/search?q=&safeSearch=Off&mkt="
                        + language + "&safeSearch=Off&count=" + bingReaderMax + "&offset="
                        + skip;

            URL url = new URL(urlTxt);

            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Ocp-Apim-Subscription-Key", acctKey);
            if (clientID != null) {
                connection.setRequestProperty("X-MSEdge-ClientID", clientID);
            }

            in = connection.getInputStream();
            if (skip == 0) {
                clientID = connection.getHeaderField("X-MSEdge-ClientID");
            }

            BingNewsAnswer json = mapper.readValue(in, BingNewsAnswer.class);

            for (NewsArticle item : json.getValue()) {
                try {
                    Date dateItem;
                    if (item.getDatePublished().length() > 10) {
                        dateItem = sdf.parse(item.getDatePublished());
                    } else {
                        dateItem = sdfs.parse(item.getDatePublished());
                    }
                    M++;
                    if (!action.processItem(new TempNew(item.getName().trim(), item.getDescription() != null ? item.getDescription().trim() : "", item.getUrl(), dateItem, new ArrayList<String>()))) {
                        return 0;
                    }
                } catch (Exception ex) {
                    logger.error("error processing item " + item.toString(), ex);
                }
            }
        } catch (Exception ex) {
            // feed.setDisabled ( true );
            logger.error("error processing bing ", ex);
            return -1;
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {

            }
        }

        return M;
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

}
