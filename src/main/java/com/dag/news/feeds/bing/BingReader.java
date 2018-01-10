package com.dag.news.feeds.bing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.dag.news.bo.TempNew;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BingReader implements Iterator<TempNew> {
    static private Logger logger = LoggerFactory.getLogger(BingReader.class);
    private String clientID = null;

    private ObjectMapper mapper;
    int offset = -1, pointer = 0, number = 0;
    List<TempNew> result = null;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private SimpleDateFormat sdfs = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private int bingReaderMax = 100;
    private String acctKey;
    private String langauge;

    public BingReader(String acctKey, String language, int bingReaderMax) {
        this.bingReaderMax = bingReaderMax;
        this.acctKey = acctKey;
        this.langauge = language;
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdfs.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdt.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper = new ObjectMapper();
    }

    @Override
    public boolean hasNext() {
        if (pointer < number) return true;
        check();
        return pointer < number;
    }

    @Override
    public TempNew next() {
        return result.get(pointer++);
    }

    private void check() {
        if (offset == -1) {
            result = read(langauge, 0);
            offset = bingReaderMax;
        } else if (number < bingReaderMax) {
            // last page has less results as expected, it means last page
            result = null;
        } else {
            result = read(langauge, offset);
            offset += bingReaderMax;
        }
        pointer = 0;
        number = result != null ? result.size() : 0;
    }

    static final String HOST = "https://api.cognitive.microsoft.com";
    static final String URL = "/bing/v7.0/news/search";

    private List<TempNew> read(String language, int skip) {
        List<TempNew> result = new ArrayList<>(bingReaderMax);
        InputStream in = null;
        int M = 0;
        try {
//freshness=Day
            String urlTxt;
            if (skip <= 0)
                urlTxt = HOST + URL + "?q=&mkt=" + language + "&safeSearch=Off&count=" + bingReaderMax;
            else
                urlTxt = HOST + URL + "?q=&mkt=" + language + "&offset=" + skip;

            URL url = new URL(urlTxt);

            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Ocp-Apim-Subscription-Key", acctKey);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 Edge/16.16299");
            if (clientID != null) {
                connection.setRequestProperty("X-MSEdge-ClientID", clientID);
            }

            in = connection.getInputStream();
            if (skip == 0 && clientID == null) {
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
                    result.add(new TempNew(item.getName().trim(), item.getDescription() != null ? item.getDescription().trim() : "", item.getUrl(), dateItem, new ArrayList<String>()));
                } catch (Exception ex) {
                    logger.error("error processing item " + item.toString(), ex);
                }
            }
        } catch (Exception ex) {
            // feed.setDisabled ( true );
            logger.error("error processing bing ", ex);
            return null;
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {

            }
        }

        return result;
    }

    private static <T> Stream<T> iteratorToFiniteStream(final Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }

/*    static <T> Stream<T> iteratorToInfiniteStream(final Iterator<T> iterator) {
        return Stream.generate(iterator::next);
    }
  */

    static public Stream<TempNew> find(String acctKey, String language, int bingReaderMax) {
        return iteratorToFiniteStream(new BingReader(acctKey, language, bingReaderMax));
    }

    public static void main(String[] ar) {
        BingReader.find("----key----", "es-es", 100).forEach((a) -> System.out.println(a.getLink()));
    }
}
