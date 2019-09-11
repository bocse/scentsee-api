package com.bocse.scentsee.web;

import com.bocse.perfume.iterator.PerfumeIterator;
import com.bocse.perfume.iterator.QuestionnaireIterator;
import com.bocse.scentsee.service.eventLogging.BusinessLoggingService;
import com.bocse.scentsee.service.eventLogging.QuestionnaireLikeEvent;
import com.bocse.scentsee.service.eventLogging.SearchLikeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;

@RestController
//@RequestMapping("/{userId}/bookmarks")
@RequestMapping("/rest/interaction")
public class InteractionRestController {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    BusinessLoggingService businessLoggingService;

    @Autowired
    QuestionnaireIterator questionnaireIterator;

    @Autowired
    PerfumeIterator perfumeIterator;

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/likeQuestionnaire", method = RequestMethod.GET)
    Object likeQuestionnaire(
            HttpServletRequest request,
            @RequestParam(value = "perfumeIndex", required = true) Long perfumeIndex,
            @RequestParam(value = "perfumeId", required = true) Long perfumeId,
            @RequestParam Map<String, String> answers
    ) {

        QuestionnaireLikeEvent event = new QuestionnaireLikeEvent();
        event.setIp(request.getRemoteAddr());
        String xforward = request.getHeader("X-Forwarded-For");
        if (xforward != null && !xforward.isEmpty())
            event.setIp(xforward);
        event.setUserAgent(request.getHeader("user-agent"));
        event.setAnswers(answers);
        event.setRecommendedPerfumeId(perfumeId);
        event.setRecommendedPerfumeIndex(perfumeIndex);
        businessLoggingService.logEvent(event);

        return new String[]{"OK"};

    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/likeSearch", method = RequestMethod.GET)
    Object likeSearch(
            HttpServletRequest request,
            @RequestParam(value = "perfumeIndex", required = true) Long perfumeIndex,
            @RequestParam(value = "originalPerfumeIds[]", required = true) Long[] originalPerfumeIds,
            @RequestParam(value = "perfumeId", required = true) Long perfumeId

    ) {

        SearchLikeEvent event = new SearchLikeEvent();
        event.setIp(request.getRemoteAddr());
        String xforward = request.getHeader("X-Forwarded-For");
        if (xforward != null && !xforward.isEmpty())
            event.setIp(xforward);
        event.setUserAgent(request.getHeader("user-agent"));

        event.setOriginalPerfumeIds(Arrays.asList(originalPerfumeIds));
        event.setRecommendedPerfumeId(perfumeId);
        event.setRecommendedPerfumeIndex(perfumeIndex);
        businessLoggingService.logEvent(event);

        return new String[]{"OK"};

    }

}