package com.bocse.scentsee.web.api;

import com.bocse.perfume.composer.PerfumeComposer;
import com.bocse.perfume.data.ComposedPerfume;
import com.bocse.perfume.data.Perfume;
import com.bocse.perfume.iterator.PerfumeIterator;
import com.bocse.perfume.iterator.QuestionnaireIterator;
import com.bocse.perfume.raw.RawMaterialCollection;
import com.bocse.perfume.signature.SignatureEvaluator;
import com.bocse.perfume.statistical.CollocationAnalysis;
import com.bocse.perfume.statistical.FrequencyAnalysis;
import org.apache.commons.lang3.StringUtils;
import org.jsondoc.core.annotation.Api;
import org.jsondoc.core.annotation.ApiMethod;
import org.jsondoc.core.annotation.ApiPathParam;
import org.jsondoc.core.pojo.ApiStage;
import org.jsondoc.core.pojo.ApiVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/rest/composition")
@Api(name = "Perfume Composition", description = "Methods for determining the composing notes of new perfumes", group = "Composition", visibility = ApiVisibility.PUBLIC, stage = ApiStage.PRE_ALPHA)
public class CompositionRestController {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    QuestionnaireIterator questionnaireIterator;

    @Autowired
    PerfumeIterator perfumeIterator;

    @Autowired
    SignatureEvaluator signatureEvaluator;

    @Autowired
    RawMaterialCollection rawMaterialCollection;

    @Autowired
    CollocationAnalysis collocationAnalysis;

    @RequestMapping(value = "/suggestions", method = RequestMethod.POST)
    @CrossOrigin(origins = "*")
    @ApiMethod(description = "Suggest notes based on questionnaire")
    @ResponseBody
    ComposedPerfume suggestions(
            @ApiPathParam(description = "This value, between 0 and 1, defines how much randomness you allow in the composition. Small values may be biased toward more conservative compositions, while high values will encourage exploration.") @RequestParam(value = "shuffleFactor", defaultValue = "0.0") Double shuffleFactor,
            @ApiPathParam(description = "Match notes to how they are most commonly used in the top, heart or base of the perfume.") @RequestParam(value = "regroupPerSegment", defaultValue = "true") Boolean regroupPerSegment,
            @ApiPathParam(description = "Vendor of raw materials (oils, essence) for which the stock of notes should be considered. Vendors have to be previous agreed by ScentSee.") @RequestParam(value = "vendor", required = false) String vendor,
            @RequestParam(value = "accessKey", required = true) String accessKey,
            @RequestParam(value = "secretKey", required = true) String secretKey,
            @RequestBody ComposedPerfume composedPerfume) {

        List<Perfume> perfumeList = perfumeIterator.getPerfumeList();
        List<Perfume> filteredPerfumeList = new ArrayList<>();
        for (Perfume candidatePerfume : perfumeList) {
            if (candidatePerfume.getGender().equals(composedPerfume.getGender())
                    && candidatePerfume.getInProduction()
                    && !candidatePerfume.isSubstandard()) {
                filteredPerfumeList.add(candidatePerfume);
            }
        }
        FrequencyAnalysis frequencyAnalysis = new FrequencyAnalysis(filteredPerfumeList);
        frequencyAnalysis.process();
        Map<String, Object> response = new HashMap<>();
        //String vendorName="Eden Botanicals";
        PerfumeComposer perfumeComposer = new PerfumeComposer(filteredPerfumeList, questionnaireIterator.getQuestionnaireMapping(), signatureEvaluator, rawMaterialCollection, frequencyAnalysis, collocationAnalysis, vendor);
        ComposedPerfume newComposedPerfume = perfumeComposer.getSuggestions(composedPerfume, shuffleFactor, regroupPerSegment);
        return composedPerfume;
    }

    @RequestMapping(value = "/searchNotes", method = RequestMethod.GET)
    @CrossOrigin(origins = "*")
    @ApiMethod(description = "Search for notes by a query string")
    @ResponseBody
    SortedSet<String> searchNotes(
            @ApiPathParam(description = "") @RequestParam(value = "query") String query,
            @RequestParam(value = "accessKey", required = true) String accessKey,
            @RequestParam(value = "secretKey", required = true) String secretKey) {
        if (query.length() < 3)
            throw new IllegalStateException("Query string is too short");
        SortedSet<String> results = new TreeSet<>();
        for (String note : signatureEvaluator.getNoteTypeMap().keySet()) {
            if (results.size() > 20)
                break;
            if (note.contains(query))
                results.add(note);
        }
        if (results.size() < 10 && query.length() > 3) {
            for (String note : signatureEvaluator.getNoteTypeMap().keySet()) {
                if (results.size() > 20)
                    break;
                if (StringUtils.getLevenshteinDistance(note, query) <= 2)
                    results.add(note);
            }
        }
        return results;
    }

}