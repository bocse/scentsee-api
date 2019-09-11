package com.bocse.scentsee;

import com.aerospike.client.AerospikeClient;
import com.bocse.perfume.affiliate.AffiliateCollection;
import com.bocse.perfume.data.Perfume;
import com.bocse.perfume.iterator.PerfumeIterator;
import com.bocse.perfume.iterator.QuestionnaireIterator;
import com.bocse.perfume.raw.RawMaterialCollection;
import com.bocse.perfume.signature.SignatureEvaluator;
import com.bocse.perfume.statistical.CollocationAnalysis;
import com.bocse.perfume.statistical.FrequencyAnalysis;
import com.bocse.perfume.statistical.InterfaceFrequencyAnalysis;
import org.jsondoc.spring.boot.starter.EnableJSONDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;

//import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;

/**
 * Spring Boot application.
 */

//@EnableMetrics
@EnableAutoConfiguration    // spring boot auto configuration
@EnableScheduling
@EnableAsync
@EnableJSONDoc
@ComponentScan(basePackages = {"com.bocse.scentsee"})   // scans the source code for annotations
public class SynesicaWebApp {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${aerospike.address}")
    String aerospikeAddress;
    @Value("${aerospike.port}")
    Integer aerospikePort;
    @Value("${presentation.cdn.host}")
    String cdnHost;
    //
    @Value("${presentation.brands.excludedLogo}")
    String brandsExcludedLogoString;


    @Value("${presentation.brands.excludedCompletely}")
    String brandsExcludedCompletelyString;

    @Value("${presentation.fallbackImage}")
    String fallbackImage;
    @Value("${datasource.notesFile}")
    String notesFilePath;
    @Value("${datasource.perfumeFile}")
    String perfumeFilePath;
    @Value("${datasource.perfumeFileUpdated}")
    String perfumeUpdatedFilePath;
    @Value("${affiliates.orderedList}")
    String orderedAffiliates;
    @Value("${datasource.questionnaireFile}")
    String questionnairePath;
    @Autowired
    SignatureEvaluator signatureEvaluator;
    @Autowired
    PerfumeIterator perfumeIterator;
    @Autowired
    InterfaceFrequencyAnalysis frequencyAnalysis;
    @Value("${datasource.rawMaterialsFiles:null}")
    private String[] rawMaterialsFiles;
    @Value("${datasource.rawMaterialsNames:null}")
    private String[] rawMaterialsNames;

    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(SynesicaWebApp.class);
        app.run(args);
    }

    @Bean
    public SignatureEvaluator loadAllSignatures() throws IOException {
        SignatureEvaluator signatureEvaluator = new SignatureEvaluator();
        signatureEvaluator.iterateAndKeep(new File(notesFilePath));
        signatureEvaluator.swap();
        return signatureEvaluator;
    }

    @Bean
    public PerfumeIterator initializePerfumeIterator() throws IOException {
        File originalFilePath = new File(perfumeFilePath);
        File updateFilePath = new File(perfumeUpdatedFilePath);
        PerfumeIterator perfumeIterator;
        perfumeIterator = new PerfumeIterator();
        List<String> brandsExcludedCompletely = new ArrayList<>();
        List<String> brandsExcludedLogo = new ArrayList<>();
        if (!brandsExcludedCompletelyString.trim().isEmpty())

            brandsExcludedCompletely = Arrays.asList(brandsExcludedCompletelyString.split(","));
        if (!brandsExcludedLogoString.trim().isEmpty())
            brandsExcludedLogo = Arrays.asList(brandsExcludedLogoString.split(","));

        perfumeIterator.addBrandsCompletelyExcluded(brandsExcludedCompletely);
        perfumeIterator.addBrandsWithExcludedLogo(brandsExcludedLogo);
        if (updateFilePath.exists()) {
            logger.info("Reloading from an updated file: " + perfumeUpdatedFilePath);
            perfumeIterator.iterateAndKeep(updateFilePath);

        } else {
            logger.info("Reloading from an original file: " + perfumeFilePath);
            perfumeIterator.iterateAndKeep(originalFilePath);
        }
        perfumeIterator.swap();
        for (Perfume perfume : perfumeIterator.getPerfumeList()) {
            signatureEvaluator.embedPerfumeSignature(perfume);
        }
        if (true) {
            for (Perfume perfume : perfumeIterator.getPerfumeList()) {
                if (perfume.getPictureURL().isEmpty() || perfume.getPictureURL().equals("http://static.scentsee.com/scentsee.png") || perfume.getPictureURL().equals(fallbackImage)) {
                    perfume.setPictureURL(fallbackImage);
                } else {
                    perfume.setPictureURL(cdnHost + perfume.getId() + ".jpg");
                }
            }
        }
        return perfumeIterator;
    }

    @Bean
    public QuestionnaireIterator initializeQuestionnaire() throws IOException {
        QuestionnaireIterator questionnaireIterator = new QuestionnaireIterator();
        questionnaireIterator.readProductFromJsonAndKeep(new File(questionnairePath));
        return questionnaireIterator;
    }

    @Bean
    public InterfaceFrequencyAnalysis initializeFrequencyAnalysis() {
        if (perfumeIterator == null) {
            logger.error("Iterator not available, cannot perform frequency analysis!");
            throw new IllegalStateException("Frequency analysis failted: no data");
        }
        InterfaceFrequencyAnalysis frequencyAnalysis = new FrequencyAnalysis(perfumeIterator.getPerfumeList());
        frequencyAnalysis.process();
        return frequencyAnalysis;
    }

    @Bean
    public CollocationAnalysis initializeCollocationAnalysis() {
        if (perfumeIterator == null) {
            logger.error("Iterator not available, cannot perform collocation analysis!");
            throw new IllegalStateException("Collocation analysis failted: no data");
        }
        CollocationAnalysis collocationAnalysis = new CollocationAnalysis(perfumeIterator.getPerfumeList());
        collocationAnalysis.process();
        return collocationAnalysis;
    }

    @Bean
    public RawMaterialCollection initializeRawMaterialsCollection() throws IOException {
        RawMaterialCollection rawMaterialCollection = new RawMaterialCollection(signatureEvaluator);

        for (int i = 0; i < rawMaterialsFiles.length; i++) {
            rawMaterialCollection.loadFromTextFile(rawMaterialsNames[i], rawMaterialsFiles[i]);
        }
        return rawMaterialCollection;
    }

    @Bean
    public AffiliateCollection initilizeAffiliateCollection() {
        String[] affiliateArray = orderedAffiliates.split(",");
        AffiliateCollection affiliateCollection = new AffiliateCollection();
        affiliateCollection.setAffiliates(Arrays.asList(affiliateArray));
        return affiliateCollection;
    }

    @Bean
    public AerospikeClient initializeAerospike() {
        try {
            AerospikeClient client = new AerospikeClient(aerospikeAddress, aerospikePort);

            return client;
        } catch (Exception ex) {
            logger.error("Connecting to Aerospike FAILED ", ex);
            return null;
        }
    }

}
