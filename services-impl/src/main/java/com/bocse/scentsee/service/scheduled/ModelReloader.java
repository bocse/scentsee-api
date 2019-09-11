package com.bocse.scentsee.service.scheduled;

import com.bocse.perfume.affiliate.AffiliateAoroRo;
import com.bocse.perfume.affiliate.AffiliateInterface;
import com.bocse.perfume.affiliate.AffiliateParfumExpress;
import com.bocse.perfume.affiliate.AffiliateStrawberryNet;
import com.bocse.perfume.data.AffiliatePerfume;
import com.bocse.perfume.data.Perfume;
import com.bocse.perfume.iterator.PerfumeIterator;
import com.bocse.perfume.signature.SignatureEvaluator;
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by bocse on 22.12.2015.
 */
@Component
public class ModelReloader {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${presentation.cdn.host}")
    String cdnHost;
    @Value("${presentation.fallbackImage}")
    String fallbackImage;
    @Value("${datasource.notesFile}")
    String notesFilePath;
    @Value("${maintenance.startHour}")
    Integer maintenanceStartHour;
    @Value("${maintenance.endHour}")
    Integer maintenanceEndHour;
    @Value("${datasource.perfumeFile}")
    String perfumeFilePath;
    @Value("${affiliate.strawberry.url}")
    String affiliateStrawberryUrl;
    @Value("${affiliate.parfumexpress.url}")
    String affiliateParfumExpressUrl;
    @Value("${affiliate.aoro.url}")
    String affiliateAoroUrl;
    @Value("${affiliate.aoro.username}")
    String affiliateAoroUsername;
    @Value("${affiliate.aoro.password}")
    String affiliateAoroPassword;
    @Value("${datasource.reloadPeriodically}")
    Boolean reloadPeriodically;

    @Value("${datasource.perfumeFileUpdated}")
    String perfumeUpdatedFilePath;

    @Autowired
    PerfumeIterator perfumeIterator;
    private Long lastReload = 0L;
    private Integer hoursBetweenReloads = 5;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private SignatureEvaluator signatureEvaluator;

    //private AffiliateInterface affiliateInterface=null;

    @Scheduled(initialDelay = 10000, fixedDelay = 2 * 3600 * 1000)
    public void reloadPerfumes() throws IOException, InterruptedException {

        logger.info("Alive for: " + (System.currentTimeMillis() - applicationContext.getStartupDate()) / 1000);
        if (reloadPeriodically) {

            if (new DateTime().getHourOfDay() >= maintenanceStartHour && new DateTime().getHourOfDay() <= maintenanceEndHour) {
                if (System.currentTimeMillis() - lastReload > hoursBetweenReloads * 3600 * 1000) {
                    File notesFile = new File(notesFilePath);
                    Boolean shouldReload = signatureEvaluator.shouldReload(notesFile);
                    if (shouldReload) {
                        logger.info("Reloading notes");
                        signatureEvaluator.iterateAndKeep(notesFile);
                        signatureEvaluator.swap();
                    } else {

                        logger.info("Not realoding notes");

                    }


                    File perfumeFile;
                    File originalPerfumeFile = new File(perfumeFilePath);
                    File updatedPerfumeFile = new File(perfumeUpdatedFilePath);

                    if (updatedPerfumeFile.exists())
                        perfumeFile = updatedPerfumeFile;
                    else
                        perfumeFile = originalPerfumeFile;

                    shouldReload = shouldReload || perfumeIterator.shouldReload(perfumeFile);
                    if (shouldReload) {
                        logger.info("Reloading perfumes.");
                        perfumeIterator.iterateAndKeep(perfumeFile);
                        for (Perfume perfume : perfumeIterator.getBackgroundPerfumeList()) {
                            signatureEvaluator.embedPerfumeSignature(perfume);
                        }

                        for (Perfume perfume : perfumeIterator.getBackgroundPerfumeList()) {
                            if (perfume.getPictureURL().isEmpty()) {
                                perfume.setPictureURL(fallbackImage);
                            } else {
                                perfume.setPictureURL(cdnHost + perfume.getId() + ".jpg");
                            }
                        }

                    } else {
                        logger.info("Not reloading, nothing changed.");
                        perfumeIterator.mirror();
                    }

                    final StopWatch stopWatch = new StopWatch();
//                    final AtomicLong matchedWithAoroCount = new AtomicLong(0);
//                    final AtomicLong matchedWithStrawberryCount = new AtomicLong(0);
                    final AtomicLong matchedWithAffiliate = new AtomicLong(0);
                    final Map<String, AtomicLong> affiliateMatchCount = new HashMap<>();
                    final List<AffiliateInterface> affiliateInterfaces = new ArrayList<>();

                    int index = 0;
                    stopWatch.start();

                    AffiliateAoroRo affiliateAoroRo = new AffiliateAoroRo();
                    affiliateAoroRo.readProductsFromURL(affiliateAoroUrl, affiliateAoroUsername, affiliateAoroPassword);
                    affiliateInterfaces.add(affiliateAoroRo);
                    affiliateMatchCount.put(affiliateAoroRo.getAffiliateName(), new AtomicLong(0));

                    AffiliateParfumExpress affiliateParfumExpress = new AffiliateParfumExpress();
                    affiliateParfumExpress.readProductsFromURL(affiliateParfumExpressUrl);
                    affiliateInterfaces.add(affiliateParfumExpress);
                    affiliateMatchCount.put(affiliateParfumExpress.getAffiliateName(), new AtomicLong(0));

                    AffiliateStrawberryNet affiliateStrawberryNet = new AffiliateStrawberryNet();
                    affiliateStrawberryNet.readProductsFromURL(affiliateStrawberryUrl);
                    affiliateInterfaces.add(affiliateStrawberryNet);
                    affiliateMatchCount.put(affiliateStrawberryNet.getAffiliateName(), new AtomicLong(0));

                    for (Perfume perfume : perfumeIterator.getBackgroundPerfumeList()) {
                        boolean hasMatch = false;
                        for (AffiliateInterface affiliateInterface : affiliateInterfaces) {
                            List<AffiliatePerfume> affiliatePerfumes = affiliateInterface.lookup(perfume);

                            if (affiliatePerfumes.size() > 0) {
                                hasMatch = true;
                                affiliateMatchCount.get(affiliateInterface.getAffiliateName()).addAndGet(1);
                                perfume.getAffiliateProducts().put(affiliateInterface.getAffiliateName(), affiliatePerfumes);
                            } else {
                                perfume.getAffiliateProducts().remove(affiliateInterface.getAffiliateName());
                            }

                        }
                        if (hasMatch) {
                            matchedWithAffiliate.addAndGet(1);
                        }
                        index++;
                        if (index % 1000 == 0) {
                            logger.info("Done " + (100.0 * index / perfumeIterator.getPerfumeList().size()) + " %");
                            logger.info("Affiliate   matches: " + matchedWithAffiliate.get() + "(" + (100.0 * matchedWithAffiliate.get() / perfumeIterator.getPerfumeList().size()) + " %)");
                            logger.info("Affiliate match breakdown: " + affiliateMatchCount.toString());
                        }
                    }
                    if (shouldReload) {
                        //signatureEvaluator.swap();
                        perfumeIterator.swap();
                    }
                    perfumeIterator.serialize(updatedPerfumeFile);
                    stopWatch.stop();
                    lastReload = System.currentTimeMillis();
                    logger.info("Elapsed seconds: " + stopWatch.getTime() / 1000);
                    logger.info("Affiliate   matches: " + matchedWithAffiliate.get() + "(" + (100.0 * matchedWithAffiliate.get() / perfumeIterator.getPerfumeList().size()) + " %)");
                    logger.info("Affiliate match breakdown: " + affiliateMatchCount.toString());
                    logger.info("FINISHED reloading affiliates.");
                } else {
                    logger.info("Only " + (System.currentTimeMillis() - lastReload) / 1000 + " s since last reload, out of minimum " + hoursBetweenReloads * 3600);
                }
            } else {
                logger.info("Not reloading outside maintenance window " + maintenanceStartHour + " - " + maintenanceEndHour);
            }
        }
    }


}
