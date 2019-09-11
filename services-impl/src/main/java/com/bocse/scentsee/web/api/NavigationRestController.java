package com.bocse.scentsee.web.api;

import com.bocse.perfume.data.NoteType;
import com.bocse.perfume.data.Perfume;
import com.bocse.perfume.iterator.PerfumeIterator;
import com.bocse.perfume.signature.SignatureEvaluator;
import org.jsondoc.core.annotation.Api;
import org.jsondoc.core.annotation.ApiMethod;
import org.jsondoc.core.annotation.ApiPathParam;
import org.jsondoc.core.pojo.ApiStage;
import org.jsondoc.core.pojo.ApiVisibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: Not implemented
@RestController
@RequestMapping("/rest/navigation")
@Api(name = "Perfume Navigation", description = "Methods for navigating in between similar perfumes based on their differences", group = "Navigation", visibility = ApiVisibility.PRIVATE, stage = ApiStage.PRE_ALPHA)
public class NavigationRestController {

    @Autowired
    PerfumeIterator perfumeIterator;

    @Autowired
    SignatureEvaluator signatureEvaluator;

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/compare")
    @ApiMethod(description = "Shows a map of the difference between the first and the second perfume")
    @ResponseBody
    public Map<String, Object> comparePerfumes(
            @ApiPathParam(description = "The ID of the first perfume") @RequestParam("id1") Long id1,
            @ApiPathParam(description = "The ID of the second perfume") @RequestParam(value = "id2") Long id2
    ) {
        Map<String, Object> response = new HashMap<>();

        Perfume perfume1 = perfumeIterator.getPerfumeMap().get(id1);
        if (perfume1 == null)
            throw new IllegalStateException("Unknown perfume id " + id1);
        Perfume perfume2 = perfumeIterator.getPerfumeMap().get(id2);
        if (perfume2 == null)
            throw new IllegalStateException("Unknown perfume id " + id2);
        Map<NoteType, Double> difference = signatureEvaluator.signatureDifference(perfume1.getMixedSignature(), perfume2.getMixedSignature());
        Map<NoteType, Double> sortedDifference = signatureEvaluator.orderSignature(difference);
        List<Map.Entry<NoteType, Double>> differenceList = new ArrayList<>(sortedDifference.entrySet());
        List<NoteType> firstIsMore = new ArrayList<>();
        List<NoteType> secondIsMore = new ArrayList<>();
        List<NoteType> bothAreJustAsMuch = new ArrayList<>();
        for (int i = 0; i < differenceList.size(); i++) {
            if (differenceList.get(i).getValue() > 0.0)
                firstIsMore.add(differenceList.get(i).getKey());
            else {
                if (differenceList.get(i).getValue() == 0.0)
                    bothAreJustAsMuch.add(differenceList.get(i).getKey());
            }

        }
        for (int i = differenceList.size() - 1; i >= 0; i--) {
            if (differenceList.get(i).getValue() < 0.0)
                secondIsMore.add(differenceList.get(i).getKey());
        }

        response.put("firstIsMore", firstIsMore);
        response.put("secondIsMore", secondIsMore);
        response.put("bothAreJustAsMuch", bothAreJustAsMuch);
        response.put("difference", sortedDifference);
        return response;
    }

}