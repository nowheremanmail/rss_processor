package com.dag.source;

import com.dag.DataService;
import com.dag.bo.Feed;
import com.dag.news.bo.TempNew;
import com.dag.news.feeds.bing.BingReader;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.checkpoint.ListCheckpointed;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RSSSources extends RichSourceFunction<TempNew> implements ListCheckpointed<Tuple3<String, LocalDateTime, LocalDateTime>> {

    static private Logger logger = LoggerFactory.getLogger(RSSSources.class);
    static TrustStrategy trustStrategy = new TrustStrategy() {

        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (logger.isDebugEnabled()) {
                for (X509Certificate cert : chain) {
                    logger.debug("certification " + cert);
                }
            }
            return true;
        }

    };
    private int ttl;
    private int timeout = 20000;
    private volatile boolean isRunning = true;
    private String lang;
    private DataService dataService;
    private String pathDB;
    private String bingKey;
    private Map<String, Tuple3<String, LocalDateTime, LocalDateTime>> currentInfo = new HashMap<>();

    public RSSSources(String lang, int minutes, String dataPath, String bingKey) {
        this.lang = lang;
        ttl = minutes;
        pathDB = dataPath;
        this.bingKey = bingKey;
    }

    static String extractGoogleLink(String url) {
        if (url.startsWith("http://news.google") || url.startsWith("https://news.google")) {

            Pattern pattern = Pattern.compile("(&?url=)([^&]+)");
            Matcher matcher = pattern.matcher(url);
            // check all occurance
            if (matcher.find() && matcher.groupCount() == 2) {
                url = matcher.group(2);
            }
        }
        return url;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        dataService = new DataService(pathDB); //parameters.getString("pathDB", "tcp://localhost:9092/c:/work/scala/news/dbs//news"));
        //ttl = parameters.getInteger("ttl", 30);
    }

    @Override
    public void run(SourceContext<TempNew> ctx) throws Exception {
        final Object lock = ctx.getCheckpointLock();

        List<Feed> urlsToProcess = new ArrayList<>();

        long wait = 1000L;
        int position = 0, size = 1;
        int times2 = 60 / (int) ttl;
        if (times2 <= 0) times2 = 1;
        int count = 0;

        while (isRunning) {
            if (position == 0) {
                scala.collection.immutable.List<Feed> urls = dataService.getFeeds(lang); //.fil;
                size = urls.size();
                if (size == 0) return;

                IntStream.range(0, size).forEach(a -> {
                    urlsToProcess.add(urls.apply(a));
                });

                wait = ttl * 60L * 1000L / size;
            }

            String url = urlsToProcess.get(position).url();
            if (url.startsWith("bing-")) {
                count++;
                if (count % times2 == 0) {
                    for (Iterator<TempNew> it = BingReader.iterator(dataService, bingKey, lang, 100); it.hasNext(); ) {
                        ctx.collect(it.next());
                    }
                }
            } else {
                //String lang = urlsToProcess.get(position).language();
                boolean needToAdd = false;

                Tuple3<String, LocalDateTime, LocalDateTime> currentTuple = currentInfo.get(url);
                if (currentTuple == null) {
                    currentTuple = new Tuple3<>(url, null, null);
                    needToAdd = true;
                }

                LocalDateTime lastStoredDate = currentTuple.getField(1);
                LocalDateTime lastFoundDate = currentTuple.getField(2);

                SyndFeed feed = null;
                try {

                    logger.info("[" + url + "] reading ");
                    feed = readFeed(url);

                    if (feed != null && feed.getEntries() != null) {

                        List<String> categoriesFeed = new ArrayList<String>();
                        for (SyndCategory cat : feed.getCategories()) {
                            String tax = cat.getTaxonomyUri();
                            if (tax == null)
                                tax = "";
                            else
                                tax = ":" + tax;
                            categoriesFeed.add(cat.getName() + tax);
                        }

                        Date _feedPublishDate = feed.getPublishedDate();
                        LocalDateTime feedPublishedDate = _feedPublishDate != null ? LocalDateTime.ofInstant(_feedPublishDate.toInstant(), ZoneId.of("UTC")) : null;

                        if (lastStoredDate == null || feedPublishedDate.isAfter(lastStoredDate)) {
                            LocalDateTime newestPubDate = null;
                            for (SyndEntry entry : feed.getEntries()) {
                                LocalDateTime entryPublishedDate = null;
                                Date d = entry.getPublishedDate();
                                if (d == null) {
                                    d = entry.getUpdatedDate();
                                }

                                if (d == null) {
                                    logger.info("[" + url + "] ignoring new because has no date");
                                    continue;
                                }

                                entryPublishedDate = LocalDateTime.ofInstant(d.toInstant(), ZoneId.of("UTC"));

                                if (entry.getTitle() != null && entry.getLink() != null &&
                                        (lastFoundDate == null || lastFoundDate.isBefore(entryPublishedDate))) {

                                    List<String> categoriesNew = new ArrayList<String>(categoriesFeed);
                                    for (SyndCategory cat : entry.getCategories()) {
                                        String tax = cat.getTaxonomyUri();
                                        if (tax == null)
                                            tax = "";
                                        else
                                            tax = ":" + tax;
                                        if (!categoriesNew.contains(cat.getName() + tax)) {
                                            categoriesNew.add(cat.getName() + tax);
                                        }
                                    }

                                    ctx.collect(new TempNew(entry.getTitle().trim(), entry.getDescription() != null ? entry.getDescription().getValue().trim() : "", extractGoogleLink(entry.getLink()), d, categoriesNew));
                                }

                                if (newestPubDate == null) {
                                    newestPubDate = entryPublishedDate;
                                } else if (newestPubDate.isBefore(entryPublishedDate)) {
                                    newestPubDate = entryPublishedDate;
                                }
                            }

                            synchronized (lock) {
                                currentTuple.setField(feedPublishedDate, 1);
                                currentTuple.setField(newestPubDate, 2);

                                if (needToAdd)
                                    currentInfo.put(url, currentTuple);
                            }
                        } else {
                            logger.info("[" + url + "] ignoring feed, data not updated " + lastStoredDate + " " + feedPublishedDate);
                        }

                    } else {
                        logger.info("[" + url + "] ignoring feed, no items");
                    }

                    dataService.updateInfo(urlsToProcess.get(position), "");
                } catch (Exception ex) {
                    logger.error("[" + url + "] fails ", ex);
                    dataService.updateInfo(urlsToProcess.get(position), "Exception: " + ex.getMessage());
                }
            }

            position = (position + 1) % size;
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ex) {
                isRunning = false;
            }

        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

    private SyndFeed readFeed(String url) throws Exception {
        SyndFeed feed = null;
        CloseableHttpClient httpclient = null;

        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());

            httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

            HttpGet httpget = new HttpGet(url);

            httpget.setHeader("Accept", "text/html, application/xhtml+xml, image/jxr, */*");
            httpget.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36 Edge/15.15063");

            httpget.setHeader("Accept-Encoding", "gzip, deflate");
            httpget.setHeader("Accept-Language", "en-GB, en; q=0.8, es-ES; q=0.6, es; q=0.4, ca; q=0.2");

            httpget.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectionRequestTimeout(timeout)
                    .setConnectTimeout(timeout).setCircularRedirectsAllowed(true).setRedirectsEnabled(true).build());

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (httpget != null) {
                            httpget.abort();
                        }
                    } catch (Exception ex) {

                    }
                }
            };
            Timer time = new Timer(true);
            time.schedule(task, timeout);

            logger.debug("Executing request " + httpget.getRequestLine());

            CloseableHttpResponse response = httpclient.execute(httpget);
            time.cancel();
            Header _contentType = null;
            try {
                _contentType = response.getFirstHeader("Content-Type");

                if (logger.isDebugEnabled())
                    logger.debug("get " + response.getStatusLine() + " " + _contentType);

                if (response.getStatusLine().getStatusCode() == 200) {
                    SyndFeedInput input = new SyndFeedInput();
                    feed = input.build(new XmlReader(response.getEntity().getContent()));
                } else {
                    throw new RuntimeException("network error [" + response.getStatusLine().getStatusCode() + "]");
                }
            } finally {
                response.close();
            }
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {

            }
        }

        return feed;
    }

    @Override
    public List<Tuple3<String, LocalDateTime, LocalDateTime>> snapshotState(long checkpointId, long timestamp) throws Exception {
        List<Tuple3<String, LocalDateTime, LocalDateTime>> list = currentInfo.values().stream().filter(a -> a.f1 != null || a.f2 != null).collect(Collectors.toList());
        logger.info("detected " + list.size() + " to be stored");
        return list;
    }

    @Override
    public void restoreState(List<Tuple3<String, LocalDateTime, LocalDateTime>> state) throws Exception {
        currentInfo = state.stream().collect(Collectors.toMap(a -> a.f0, a -> a));
        logger.info("detected " + currentInfo.size() + " recovered urls");
    }
}
