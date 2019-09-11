package com.bocse.scentsee.web.api;

import com.bocse.perfume.affiliate.AffiliateCollection;
import com.bocse.perfume.data.Perfume;
import com.bocse.perfume.data.RecommendationAlgorithm;
import com.bocse.perfume.data.RecommendedPerfume;
import com.bocse.perfume.iterator.PerfumeIterator;
import com.bocse.perfume.recommender.PerfumeRecommender;
import com.bocse.perfume.signature.SignatureEvaluator;
import com.bocse.perfume.similarity.NoteSimilarity;
import com.bocse.perfume.similarity.SignatureSimilarity;
import com.bocse.perfume.statistical.DummyFrequencyAnalysis;
import com.bocse.perfume.statistical.InterfaceFrequencyAnalysis;
import com.bocse.scentsee.service.eventLogging.BusinessLoggingService;
import org.apache.commons.lang3.tuple.Pair;
import org.jsondoc.core.annotation.Api;
import org.jsondoc.core.annotation.ApiQueryParam;
import org.jsondoc.core.pojo.ApiStage;
import org.jsondoc.core.pojo.ApiVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Acts as a web controller for the root.
 */


@RestController
@RequestMapping(value = "/rest/network")     // maps this controller to serve requests to the root
@Api(name = "Perfume Navigation", description = "Methods for navigating in between similar perfumes based on their differences", group = "Navigation", visibility = ApiVisibility.PRIVATE, stage = ApiStage.PRE_ALPHA)
public class NetworkRestController {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    //protected Logger businessLogger = LoggerFactory.getLogger("com.bocse.scentsee.businessLogger");

    @Autowired
    BusinessLoggingService businessLoggingService;


    @Autowired
    PerfumeIterator perfumeIterator;

    @Autowired
    SignatureEvaluator signatureEvaluator;

    @Autowired
    InterfaceFrequencyAnalysis frequencyAnalysis;

    @Autowired
    AffiliateCollection affiliateCollection;

    @RequestMapping(value = "/graphByFavoriteFragranceId", method = RequestMethod.GET)
    @ResponseBody
    Object retrieveGraphByFavoriteFragrance(
            HttpServletRequest request,
            @RequestParam("ids[]") Long[] ids,
            @RequestParam(value = "mustBeInStock", required = false, defaultValue = "true") Boolean mustBeInStock,
            @RequestParam(value = "power", required = false, defaultValue = "2.5") Double power,
            @RequestParam(value = "priceDiscriminationFactor", required = false, defaultValue = "2.051") Double priceDiscriminationFactor,
            @RequestParam(value = "signatureWeight", required = false, defaultValue = "0.22") Double signatureWeight,
            @RequestParam(value = "noteWeight", required = false, defaultValue = "0.6931") Double noteWeight,
            @RequestParam(value = "topWeight", required = false, defaultValue = "0.142") Double topWeight,
            @RequestParam(value = "heartWeight", required = false, defaultValue = "0.4139") Double heartWeight,
            @RequestParam(value = "baseWeight", required = false, defaultValue = "0.8701") Double baseWeight,
            @RequestParam(value = "mixedWeight", required = false, defaultValue = "0.0155") Double mixedWeight,
            @RequestParam(value = "mixedExponentialPenalty", required = false, defaultValue = "1.87") Double mixedExponentialPenalty,
            @RequestParam(value = "computeFrequencyAnalysis", required = false, defaultValue = "true") Boolean computeFrequencyAnalysis,
            @ApiQueryParam(description = "Strict list of affiliate stocks considered for the recommendation. Leave empty to use the default.") @RequestParam(value = "acceptedAffiliates", required = false) List<String> acceptedAffiliates,
            @RequestParam(value = "maxResults", required = false, defaultValue = "8") Integer maxResults) {
        AffiliateCollection localAffiliateCollection;
        if (acceptedAffiliates != null && !acceptedAffiliates.isEmpty()) {
            localAffiliateCollection = new AffiliateCollection();
            localAffiliateCollection.setAffiliates(acceptedAffiliates);
        } else {
            localAffiliateCollection = affiliateCollection;
        }

        final RecommendationAlgorithm algorithm = RecommendationAlgorithm.favoriteSimilarity;
        List<Perfume> chosenPerfumes = new ArrayList<>();
        for (Long id : ids) {
            Perfume chosenPerfume = perfumeIterator.getPerfumeMap().get(id);
            if (chosenPerfume != null) {
                chosenPerfumes.add(chosenPerfume);
            } else {
                logger.error("Cannot map perfume with ID " + id);
            }
        }
        if (chosenPerfumes.size() == 0)
            throw new IllegalStateException("No valid perfumes selected.");

        List<Long> chosenIds = new ArrayList<>();
        for (Perfume perfume : chosenPerfumes) {
            chosenIds.add(perfume.getId());
        }
        List<RecommendedPerfume> croppedChosen = new ArrayList<>();
        for (Perfume perfume : chosenPerfumes) {
            RecommendedPerfume recommendedPerfume = new RecommendedPerfume(perfume, affiliateCollection);
            signatureEvaluator.embedDominantClasses(recommendedPerfume, perfume);
            croppedChosen.add(recommendedPerfume);

        }


        NoteSimilarity noteSimilarity;
        SignatureSimilarity signatureSimilarity;
        if (computeFrequencyAnalysis) {
            noteSimilarity = new NoteSimilarity(frequencyAnalysis);
            signatureSimilarity = new SignatureSimilarity(frequencyAnalysis);
        } else {
            noteSimilarity = new NoteSimilarity(new DummyFrequencyAnalysis());
            signatureSimilarity = new SignatureSimilarity(new DummyFrequencyAnalysis());
        }
        PerfumeRecommender perfumeRecommender = new PerfumeRecommender(perfumeIterator.getPerfumeList(), noteSimilarity, signatureSimilarity);
//RandomRecommender perfumeRecommender=new RandomRecommender(perfumes);
        perfumeRecommender.setPower(power);
        perfumeRecommender.setSignatureWeight(signatureWeight);
        perfumeRecommender.setNoteWeight(noteWeight);
        perfumeRecommender.setPriceDiscriminationFactor(priceDiscriminationFactor);


        SortedMap<Double, Perfume> mostSimilar;

        perfumeRecommender.getNoteSimilarity().setTopWeight(topWeight);
        perfumeRecommender.getNoteSimilarity().setHeartWeight(heartWeight);
        perfumeRecommender.getNoteSimilarity().setBaseWeight(baseWeight);
        perfumeRecommender.getNoteSimilarity().setMixedWeight(mixedWeight);
        perfumeRecommender.getNoteSimilarity().setMixedExponentialPenalty(mixedExponentialPenalty);

        perfumeRecommender.getSignatureSimilarity().setTopWeight(topWeight);
        perfumeRecommender.getSignatureSimilarity().setHeartWeight(heartWeight);
        perfumeRecommender.getSignatureSimilarity().setBaseWeight(baseWeight);
        perfumeRecommender.getSignatureSimilarity().setMixedWeight(mixedWeight);
        perfumeRecommender.getSignatureSimilarity().setMixedExponentialPenalty(mixedExponentialPenalty);
        mostSimilar = perfumeRecommender.recommendByPerfumeSimilarity(chosenPerfumes, chosenPerfumes.get(0).getGender(), mustBeInStock, maxResults, localAffiliateCollection);

        List<Perfume> allPerfumes = new ArrayList<>();

        List<Map<String, Object>> nodes = new ArrayList<>();

        for (Perfume perfume : chosenPerfumes) {
            allPerfumes.add(perfume);
            RecommendedPerfume recommendedPerfume = new RecommendedPerfume(perfume, affiliateCollection);
            signatureEvaluator.embedDominantClasses(recommendedPerfume, perfume);
            Map<String, Object> node = perfumeToNode(recommendedPerfume, true);
            node.put("borderWidth", 3);
            node.put("borderWidthSelected", 4);
            nodes.add(node);
        }

        for (Map.Entry<Double, Perfume> entry : mostSimilar.entrySet()) {
            allPerfumes.add(entry.getValue());
            RecommendedPerfume recommendedPerfume = new RecommendedPerfume(entry.getValue(), affiliateCollection);


            //recommendedPerfume.setMatchRate(entry.getKey());
            //recommendedPerfume.getMatchRates().put(algorithm, entry.getKey());
            signatureEvaluator.embedDominantClasses(recommendedPerfume, entry.getValue());
            Map<String, Object> node = perfumeToNode(recommendedPerfume, false);
            nodes.add(node);
        }


        List<Map<String, Object>> edges = new ArrayList<>();
        List<Double> similairtyList = new ArrayList<>();
        Map<Pair<Perfume, Perfume>, Double> preCalculation = new HashMap<>();
        for (Perfume perfume1 : allPerfumes)
            for (Perfume perfume2 : allPerfumes)
                if (perfume1.getId() < perfume2.getId()) {
                    Double signatureMatch = perfumeRecommender.getSignatureSimilarity().getBlendedSignatureSimilarity(perfume1, perfume2);
                    Double noteMatch = perfumeRecommender.getNoteSimilarity().getBlendedNoteSimilarity(perfume1, perfume2);
                    //Arithmetic mean
                    Double overallMatch = (signatureWeight * signatureMatch + noteWeight * noteMatch) / (noteWeight + signatureWeight);
                    similairtyList.add(overallMatch);
                    preCalculation.put(Pair.of(perfume1, perfume2), overallMatch);
                }
        Collections.sort(similairtyList);
        Double maxSimilarity = similairtyList.get(similairtyList.size() - 1);
        Double minSimilarity = similairtyList.get((int) (0.3 * (similairtyList.size() - 1)));
        for (Map.Entry<Pair<Perfume, Perfume>, Double> entry : preCalculation.entrySet()) {
            if (entry.getValue() >= minSimilarity) {
                Map<String, Object> edge = perfumePairToEdge(entry.getKey().getLeft(), entry.getKey().getRight(), entry.getValue(), minSimilarity, maxSimilarity);
                edges.add(edge);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("edges", edges);
        response.put("nodes", nodes);
        //response.put("recommendations", croppedMostSimilar);
        //response.put("basedOn", croppedChosen);
        return response;


    }

    private Map<String, Object> perfumeToNode(RecommendedPerfume perfume, Boolean original) {
        Map<String, Object> map = new HashMap<>();
        //{"id": 1,  "shape": 'circularImage',
        // image: "http://www.anrdoezrs.net/click-7958800-12225557?url=http%3A%2F%2Fwww.aoro.ro%2Fjean-p-gaultier%2Fle-male-eau-de-toilette-pentru-barbati%2F",
        // label:"Gaultier"},
        if (original) {
            //xmap.put("fixed", true);
            map.put("mass", 5.0);
            map.put("size", 35);
        } else {
            map.put("mass", 2.0);
        }
        map.put("id", perfume.getId());
        map.put("shape", "circularImage");
        map.put("font", "14px arial navy");
        map.put("brokenImage", "/images/logos/logo-00.png");
        map.put("image", perfume.getPictureURL());
        map.put("label", perfume.getBrand() + " " + perfume.getName());

        map.put("title", perfume.getDominantClasses());
        //map.put("value", 1.0);
        return map;
    }

    private Map<String, Object> perfumePairToEdge(Perfume perfume1, Perfume perfume2, Double matchRate, Double minSimilarity, Double maxSimilarity) {
        final Double maxWidth = 3.0;
        Map<String, Object> map = new HashMap<>();
        map.put("from", perfume1.getId());
        map.put("to", perfume2.getId());
        if (maxSimilarity != minSimilarity) {
            Double visibility = (matchRate - minSimilarity) / (maxSimilarity - minSimilarity);
            map.put("width", visibility * maxWidth);
            map.put("opacity", visibility);
        }

        return map;
    }
}
