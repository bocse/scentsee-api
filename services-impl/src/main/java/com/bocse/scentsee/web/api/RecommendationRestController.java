package com.bocse.scentsee.web.api;

import com.bocse.perfume.affiliate.AffiliateCollection;
import com.bocse.perfume.data.*;
import com.bocse.perfume.iterator.PerfumeIterator;
import com.bocse.perfume.iterator.QuestionnaireIterator;
import com.bocse.perfume.recommender.PerfumeRecommender;
import com.bocse.perfume.signature.SignatureEvaluator;
import com.bocse.perfume.similarity.NoteSimilarity;
import com.bocse.perfume.similarity.SignatureSimilarity;
import com.bocse.perfume.statistical.DummyFrequencyAnalysis;
import com.bocse.perfume.statistical.InterfaceFrequencyAnalysis;
import com.bocse.scentsee.service.eventLogging.BusinessLoggingService;
import com.bocse.scentsee.service.eventLogging.QuestionnaireEvent;
import com.bocse.scentsee.service.eventLogging.SearchEvent;
import com.bocse.scentsee.utils.RecommendedPerfumeComparators;
import org.jsondoc.core.annotation.Api;
import org.jsondoc.core.annotation.ApiMethod;
import org.jsondoc.core.annotation.ApiQueryParam;
import org.jsondoc.core.pojo.ApiStage;
import org.jsondoc.core.pojo.ApiVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
//@RequestMapping("/{userId}/bookmarks")
@RequestMapping("/rest/recommendation")
@Api(name = "Perfume Recommendation", description = "Methods for obtaining perfume recommendation either based on search (by other preferred perfumes) or on quiz (answers of the user to specific questions)", group = "Recommendation", visibility = ApiVisibility.PUBLIC, stage = ApiStage.RC)
public class RecommendationRestController {

    @Autowired
    final AffiliateCollection affiliateCollection = null;
    protected Logger logger = LoggerFactory.getLogger(getClass());
    //protected Logger businessLogger = LoggerFactory.getLogger("com.bocse.scentsee.businessLogger");
    @Autowired
    BusinessLoggingService businessLoggingService;
    @Autowired
    QuestionnaireIterator questionnaireIterator;
    @Autowired
    PerfumeIterator perfumeIterator;
    @Autowired
    SignatureEvaluator signatureEvaluator;
    @Autowired
    InterfaceFrequencyAnalysis frequencyAnalysis;
    private RecommendedPerfumeComparators defaultComparator = RecommendedPerfumeComparators.MATCH_POPULARITY_DESCENDING;

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/byQuestionnaireAnswersId", method = RequestMethod.GET)
    @ApiMethod(description = "Retrieves recommendation for a specific gender based on the user's answers to the questionnaire (quiz), the gender and the preferred price.")
    @ResponseBody
    Object retrieveRecommendationByQuestionnaire(
            HttpServletRequest request,
            @ApiQueryParam(description = "Defines how important selected price is in the recommendation. Values from 0.5 to 2.5 are recommended.") @RequestParam(value = "priceDiscriminationFactor", required = false, defaultValue = "1.5") Double priceDiscriminationFactor,
            @ApiQueryParam(description = "The gender of the user for whom the recommendation is requested (FEMALE, MALE, UNI)") @RequestParam(value = "gender", defaultValue = "FEMALE") Gender gender,
            @ApiQueryParam(description = "If set to true, only perfumes from vendors accepted by ScentSee will be recommended. In order to set this parameter to false, you need to have a registered account with ScentSee and append the accessKey and secretKey as GET parameters.") @RequestParam(value = "mustBeInStock", required = false, defaultValue = "true") Boolean mustBeInStock,
            @ApiQueryParam(description = "When true, this parameter instructs the recommendation algorithm to consider how common notes and note types are when comparing two perfumes.") @RequestParam(value = "computeFrequencyAnalysis", required = false, defaultValue = "true") Boolean computeFrequencyAnalysis,
            @ApiQueryParam(description = "The desired price for the perfume.") @RequestParam(value = "price", required = false, defaultValue = "150") Double price,
            @ApiQueryParam(description = "Number of recommendations returned. For more than 8 recommendations, you need to have a registered account with ScentSee and append the accessKey and secretKey as GET parameters.") @RequestParam(value = "maxResults", required = false, defaultValue = "8") Integer maxResults,
            @ApiQueryParam(description = "Strict list of affiliate stocks considered for the recommendation. Leave empty to use the default.") @RequestParam(value = "acceptedAffiliates", required = false) List<String> acceptedAffiliates,
            @ApiQueryParam(description = "When true, widens the search to perfumes which are rare.") @RequestParam(value = "includeRarePerfumes", required = false, defaultValue = "true") Boolean includeRarePerfumes,
            @ApiQueryParam(name = "questionMap", description = "Include parameters in the form questionId=answerId. For a full list of questions and answers, consult the Constants/Questionnaire section.") @RequestParam Map<String, String> answers

    ) {
        AffiliateCollection localAffiliateCollection;
        if (acceptedAffiliates != null && !acceptedAffiliates.isEmpty()) {
            localAffiliateCollection = new AffiliateCollection();
            localAffiliateCollection.setAffiliates(acceptedAffiliates);
        } else {
            localAffiliateCollection = affiliateCollection;
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

        perfumeRecommender.setPriceDiscriminationFactor(priceDiscriminationFactor);
        perfumeRecommender.setIncludeSubstandard(includeRarePerfumes);
        answers.remove("priceDiscriminationFactor");
        SortedMap<Double, Perfume> mostSuited = perfumeRecommender.recommendByQuestionnaireSimilarity(questionnaireIterator.getQuestionnaireMapping(), answers, price, gender, mustBeInStock, maxResults, localAffiliateCollection);
        Map<String, Object> response = new HashMap<>();
        List<RecommendedPerfume> croppedMostSimilar = new ArrayList<>();
        List<Long> croppedMostSimilarIds = new ArrayList<>();
        for (Map.Entry<Double, Perfume> entry : mostSuited.entrySet()) {
            RecommendedPerfume recommendedPerfume = new RecommendedPerfume(entry.getValue(), localAffiliateCollection);
            recommendedPerfume.setMatchRate(entry.getKey());
            //recommendedPerfume.getMatchRates().put("questionnaire", entry.getKey());
            signatureEvaluator.embedDominantClasses(recommendedPerfume, entry.getValue());
            croppedMostSimilar.add(recommendedPerfume);
            croppedMostSimilarIds.add(recommendedPerfume.getId());
        }
        croppedMostSimilar.sort(defaultComparator.getComparator());
        QuestionnaireEvent event = new QuestionnaireEvent();
        event.setIp(request.getRemoteAddr());
        String xforward = request.getHeader("X-Forwarded-For");
        if (xforward != null && !xforward.isEmpty())
            event.setIp(xforward);
        event.setUserAgent(request.getHeader("user-agent"));
        event.setAnswers(answers);
        event.setRecommendedPerfumeId(croppedMostSimilarIds);
        businessLoggingService.logEvent(event);

        response.put("recommendations", croppedMostSimilar);
        response.put("basedOn", answers);
        return response;

    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/byFavoriteFragranceId", method = RequestMethod.GET)
    @ApiMethod(description = "Recommends perfumes based on other perfumes the user like or is interested in, also taking into account the price and the gender. The parameters allow you to tweak the importance of the actual notes vs. the importance of note classes, as well as the importance of the top, heart and base notes of the perfume.")
    @ResponseBody
    Object retrieveRecommendationByFavoriteFragrance(
            HttpServletRequest request,
            @ApiQueryParam(description = "The comma-separated list of ids for perfumes the user already likes and gives as input") @RequestParam("ids[]") Long[] ids,
            @ApiQueryParam(description = "Restricts the answer to a specific gender. Both MALE and FEMALE will include UNI (unisex) recommendations.") @RequestParam(value = "gender", required = false) Gender gender,
            @ApiQueryParam(description = "If set to true, only perfumes from vendors accepted by ScentSee will be recommended. In order to set this parameter to false, you need to have a registered account with ScentSee and append the accessKey and secretKey as GET parameters.") @RequestParam(value = "mustBeInStock", required = false, defaultValue = "true") Boolean mustBeInStock,
            @ApiQueryParam(description = "This parameter defines how fast the differences between perfumes are considered. Low value (as low as 0.5) mean that the differences grow slowly, with high values (up to 2.5) mean that differences grow sharply") @RequestParam(value = "power", required = false, defaultValue = "2.5") Double power,
            @ApiQueryParam(description = "Defines how important it is for the recommended perfumes to be in the same price range as the input perfumes") @RequestParam(value = "priceDiscriminationFactor", required = false, defaultValue = "2.051") Double priceDiscriminationFactor,
            @ApiQueryParam(description = "How important is for compared perfumes to have the same note types (CITRIC, FLORAL, LEATHER and so on). Should be between 0.0 and 1.0 If not sure, omit this parameter.") @RequestParam(value = "signatureWeight", required = false, defaultValue = "0.22") Double signatureWeight,
            @ApiQueryParam(description = "How important it is for perfumes to have the exact same notes (lavender, bergamote, iris, ambre). Should be between 0.0 and 1.0 If not sure, omit this parameter.") @RequestParam(value = "noteWeight", required = false, defaultValue = "0.6931") Double noteWeight,
            @ApiQueryParam(description = "How important the top notes are. Should be between 0.0 and 1.0 If not sure, omit this parameter.") @RequestParam(value = "topWeight", required = false, defaultValue = "0.142") Double topWeight,
            @ApiQueryParam(description = "How important heart notes are. Should be between 0.0 and 1.0 If not sure, omit this parameter.") @RequestParam(value = "heartWeight", required = false, defaultValue = "0.4139") Double heartWeight,
            @ApiQueryParam(description = "How important base notes are. Should be between 0.0 and 1.0  If not sure, omit this parameter.") @RequestParam(value = "baseWeight", required = false, defaultValue = "0.8701") Double baseWeight,
            @ApiQueryParam(description = "How important the set of notes is regardless of segment (top, heart, base). Should be between 0.0 and 1.0 If not sure, omit this parameter.") @RequestParam(value = "mixedWeight", required = false, defaultValue = "0.0155") Double mixedWeight,
            @ApiQueryParam(description = "Penalty for having the same notes, in different segments. Should be >1.0 If not sure, omit this parameter") @RequestParam(value = "mixedExponentialPenalty", required = false, defaultValue = "1.87") Double mixedExponentialPenalty,
            @ApiQueryParam(description = "When true, this parameter instructs the recommendation algorithm to consider how common notes and note types are when comparing two perfumes.") @RequestParam(value = "computeFrequencyAnalysis", required = false, defaultValue = "true") Boolean computeFrequencyAnalysis,
            @ApiQueryParam(description = "When true, widens the search to perfumes which are rare.") @RequestParam(value = "includeRarePerfumes", required = false, defaultValue = "true") Boolean includeRarePerfumes,
            @ApiQueryParam(description = "Strict list of affiliate stocks considered for the recommendation. Leave empty to use the default.") @RequestParam(value = "acceptedAffiliates", required = false) List<String> acceptedAffiliates,
            @ApiQueryParam(description = "Number of recommendations returned. For more than 8 recommendations, you need to have a registered account with ScentSee and append the accessKey and secretKey as GET parameters.") @RequestParam(value = "maxResults", required = false, defaultValue = "8") Integer maxResults) {

        AffiliateCollection localAffiliateCollection;
        if (acceptedAffiliates != null && !acceptedAffiliates.isEmpty()) {
            localAffiliateCollection = new AffiliateCollection();
            localAffiliateCollection.setAffiliates(acceptedAffiliates);
        } else {
            localAffiliateCollection = affiliateCollection;
        }
        final RecommendationAlgorithm algorithm = RecommendationAlgorithm.favoriteSimilarity;
        Map<String, Object> response = new HashMap<>();
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
            RecommendedPerfume recommendedPerfume = new RecommendedPerfume(perfume, localAffiliateCollection);
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
        perfumeRecommender.setIncludeSubstandard(includeRarePerfumes);

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
        mostSimilar = perfumeRecommender.recommendByPerfumeSimilarity(chosenPerfumes, gender, mustBeInStock, maxResults, localAffiliateCollection);

        List<Long> croppedMostSimilarIds = new ArrayList<>();
        List<RecommendedPerfume> croppedMostSimilar = new ArrayList<>();
        for (Map.Entry<Double, Perfume> entry : mostSimilar.entrySet()) {
            RecommendedPerfume recommendedPerfume = new RecommendedPerfume(entry.getValue(), affiliateCollection);
            recommendedPerfume.setMatchRate(entry.getKey());
            recommendedPerfume.getMatchRates().put(algorithm, entry.getKey());
            signatureEvaluator.embedDominantClasses(recommendedPerfume, entry.getValue());
            croppedMostSimilar.add(recommendedPerfume);
            croppedMostSimilarIds.add(recommendedPerfume.getId());
        }
        croppedMostSimilar.sort(defaultComparator.getComparator());
        SearchEvent event = new SearchEvent();
        event.setIp(request.getRemoteAddr());
        String xforward = request.getHeader("X-Forwarded-For");
        if (xforward != null && !xforward.isEmpty())
            event.setIp(xforward);
        event.setUserAgent(request.getHeader("user-agent"));
        event.setPerfumeIds(chosenIds);
        event.setRecommendedIds(croppedMostSimilarIds);
        businessLoggingService.logEvent(event);

        response.put("recommendations", croppedMostSimilar);
        response.put("basedOn", croppedChosen);
        return response;

    }


    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/byAdvancedSearch", method = RequestMethod.GET)
    @ApiMethod(description = "Advances search based on other perfumes of interest, price and the particular preference (affinity or aversity) for note types (CITRIC, FLORAL, AQUATIC)" +
            "The parameters allow you to tweak the importance of the actual notes vs. the importance of note classes, as well as the importance of the top, heart and base notes of the perfume." +
            "This method is only available for registered users (requires accessKey and secretKey)")
    @ResponseBody
    Object retrieveRecommendationByAdvancedSearch(
            HttpServletRequest request,
            @RequestParam(value = "gender", required = false) Gender gender,
            @RequestParam(value = "affinityIds[]", required = false) Long[] affinityIds,
            @RequestParam(value = "adversityIds[]", required = false) Long[] adversityIds,
            //@RequestParam(value = "algorithm", required = false, defaultValue = "favoriteSimilarity") RecommendationAlgorithm algorithm,
            @RequestParam(value = "power", required = false, defaultValue = "2.5") Double power,
            @RequestParam(value = "priceDiscriminationFactor", required = false, defaultValue = "2.051") Double priceDiscriminationFactor,
            @RequestParam(value = "priceTarget", required = false) Double priceTarget,
            @RequestParam(value = "priceMin", required = false) Double priceMin,
            @RequestParam(value = "priceMax", required = false) Double priceMax,
            @RequestParam(value = "fullNameFragments[]", required = false) List<String> fullNameFragments,
            @RequestParam(value = "brandFragments[]", required = false) List<String> brandFragments,
            @RequestParam(value = "nameFragments[]", required = false) List<String> nameFragments,
            @ApiQueryParam(description = "Strict list of affiliate stocks considered for the recommendation. Leave empty to use the default.") @RequestParam(value = "acceptedAffiliates", required = false) List<String> acceptedAffiliates,
            @RequestParam(value = "mustBeInStock", required = false, defaultValue = "true") Boolean mustBeInStock,
            @RequestParam(value = "signatureWeight", required = false, defaultValue = "0.22") Double signatureWeight,
            @RequestParam(value = "noteWeight", required = false, defaultValue = "0.6931") Double noteWeight,
            @RequestParam(value = "topWeight", required = false, defaultValue = "0.142") Double topWeight,
            @RequestParam(value = "heartWeight", required = false, defaultValue = "0.4139") Double heartWeight,
            @RequestParam(value = "baseWeight", required = false, defaultValue = "0.8701") Double baseWeight,
            @RequestParam(value = "mixedWeight", required = false, defaultValue = "0.0155") Double mixedWeight,
            @RequestParam(value = "mixedExponentialPenalty", required = false, defaultValue = "1.87") Double mixedExponentialPenalty,
            @RequestParam(value = "computeFrequencyAnalysis", required = false, defaultValue = "true") Boolean computeFrequencyAnalysis,
            @ApiQueryParam(description = "When true, widens the search to perfumes which are rare.") @RequestParam(value = "includeRarePerfumes", required = false, defaultValue = "true") Boolean includeRarePerfumes,
            @RequestParam(value = "maxResults", required = false, defaultValue = "8") Integer maxResults,
            @RequestParam(value = "accessKey", required = true) String accessKey,
            @RequestParam(value = "secretKey", required = true) String secretKey,
            @RequestParam Map<String, String> composedSignatureString)

    {
        AffiliateCollection localAffiliateCollection;
        if (acceptedAffiliates != null && !acceptedAffiliates.isEmpty()) {
            localAffiliateCollection = new AffiliateCollection();
            localAffiliateCollection.setAffiliates(acceptedAffiliates);
        } else {
            localAffiliateCollection = affiliateCollection;
        }

        final RecommendationAlgorithm algorithm = RecommendationAlgorithm.favoriteSimilarity;
        if (acceptedAffiliates != null)
            for (int i = 0; i < acceptedAffiliates.size(); i++) {
                acceptedAffiliates.set(i, acceptedAffiliates.get(i).trim().toLowerCase());
            }
        Map<String, Object> response = new HashMap<>();
        List<Perfume> affinityPerfumes = new ArrayList<>();
        if (affinityIds != null)
            for (Long id : affinityIds) {
                Perfume chosenPerfume = perfumeIterator.getPerfumeMap().get(id);
                if (chosenPerfume != null) {
                    affinityPerfumes.add(chosenPerfume);
                } else {
                    logger.error("Cannot map perfume with ID " + id);
                }
            }

        List<Perfume> adversityPerfumes = new ArrayList<>();
        if (adversityIds != null)
            for (Long id : adversityIds) {
                Perfume chosenPerfume = perfumeIterator.getPerfumeMap().get(id);
                if (chosenPerfume != null) {
                    adversityPerfumes.add(chosenPerfume);
                } else {
                    logger.error("Cannot map perfume with ID " + id);
                }
            }
        Set<String> noteTypeSet = new HashSet<>();
        List<NoteType> noteTypeList = Arrays.asList(NoteType.values());
        for (NoteType noteType : noteTypeList) {
            noteTypeSet.add(noteType.toString());
        }

        Map<NoteType, Double> signatureDelta = new HashMap<>();
        for (Map.Entry<String, String> potentialNoteType : composedSignatureString.entrySet()) {
            if (noteTypeSet.contains(potentialNoteType.getKey())) {
                signatureDelta.put(NoteType.valueOf(potentialNoteType.getKey()), Double.valueOf(potentialNoteType.getValue()));
            }
        }

        List<Long> chosenAffinityIds = new ArrayList<>();
        for (Perfume perfume : affinityPerfumes) {
            chosenAffinityIds.add(perfume.getId());
        }

        List<Long> chosenAdversityIds = new ArrayList<>();
        for (Perfume perfume : affinityPerfumes) {
            chosenAdversityIds.add(perfume.getId());
        }

        List<RecommendedPerfume> croppedChosen = new ArrayList<>();
        for (Perfume perfume : affinityPerfumes) {
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
        perfumeRecommender.setIncludeSubstandard(includeRarePerfumes);

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


        mostSimilar = perfumeRecommender.recommendByAdvancesSearch(
                gender, priceMin, priceMax, priceTarget, signatureDelta, affinityPerfumes, adversityPerfumes, fullNameFragments, brandFragments, nameFragments, mustBeInStock, localAffiliateCollection, maxResults);


        List<Long> croppedMostSimilarIds = new ArrayList<>();
        List<RecommendedPerfume> croppedMostSimilar = new ArrayList<>();
        for (Map.Entry<Double, Perfume> entry : mostSimilar.entrySet()) {
            RecommendedPerfume recommendedPerfume = new RecommendedPerfume(entry.getValue(), localAffiliateCollection);
            recommendedPerfume.setMatchRate(entry.getKey());
            recommendedPerfume.getMatchRates().put(algorithm, entry.getKey());
            signatureEvaluator.embedDominantClasses(recommendedPerfume, entry.getValue());
            croppedMostSimilar.add(recommendedPerfume);
            croppedMostSimilarIds.add(recommendedPerfume.getId());
        }
        croppedMostSimilar.sort(defaultComparator.getComparator());
        SearchEvent event = new SearchEvent();
        event.setIp(request.getRemoteAddr());
        String xforward = request.getHeader("X-Forwarded-For");
        if (xforward != null && !xforward.isEmpty())
            event.setIp(xforward);
        event.setUserAgent(request.getHeader("user-agent"));
        event.setPerfumeIds(chosenAffinityIds);
        event.setRecommendedIds(croppedMostSimilarIds);
        businessLoggingService.logEvent(event);

        response.put("recommendations", croppedMostSimilar);
        response.put("basedOn", croppedChosen);
        return response;


    }


}