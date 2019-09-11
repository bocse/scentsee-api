package com.bocse.scentsee.web.api;

import com.aerospike.client.AerospikeClient;
import com.bocse.scentsee.users.User;
import com.bocse.scentsee.users.UserManagement;
import org.jsondoc.core.annotation.Api;
import org.jsondoc.core.annotation.ApiMethod;
import org.jsondoc.core.annotation.ApiQueryParam;
import org.jsondoc.core.pojo.ApiStage;
import org.jsondoc.core.pojo.ApiVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

@RestController
@RequestMapping("/rest/authentication")
@Api(name = "Authentication", description = "Methods for login (GET, POST) of registered users for accessing restricted features or lifting rate limiting.", group = "Authentication", visibility = ApiVisibility.PUBLIC, stage = ApiStage.RC)
public class AuthenticationRestController {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    AerospikeClient aerospikeClient;
    @Value("${aerospike.namespace}")
    String aerospikeNamespace;
    @Value("${collections.securityKey}")
    private String securityOverride = "MMKKDv4KhhwtMVzfT0G7W7hzEq87ID3noZbQVJx3";

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ApiMethod(description = "Performs authentication of an account, returning accessKey and secretKey in a JSON, together with other relevant information of the account. Subsequent requests need to be signed with accessKey and secretKey.")
    @ResponseBody
    Object loginPost(
            HttpServletRequest request,
            @ApiQueryParam(description = "Email associated with the account.") @RequestParam(value = "email", required = true) String email,
            @ApiQueryParam(description = "Password of the account") @RequestParam(value = "password", required = true) String password
    ) {
        return login(request, email, password);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    @ApiMethod(description = "Performs authentication of an account, returning accessKey and secretKey in a JSON, together with other relevant information of the account. Subsequent requests need to be signed with accessKey and secretKey.")
    @ResponseBody
    Object login(
            HttpServletRequest request,
            @ApiQueryParam(description = "Email associated with the account.") @RequestParam(value = "email", required = true) String email,
            @ApiQueryParam(description = "Password of the account") @RequestParam(value = "password", required = true) String password
    ) {
        UserManagement userManagement = new UserManagement(aerospikeClient, aerospikeNamespace);
        User user = userManagement.getUserByEmail(email);
        HashMap<String, Object> response = new HashMap<>();
        if (user != null && user.validatePassword(password)) {
            response.put("success", true);
            response.put("payload", user);
            response.put("accessKey", user.getAccessKey());
            response.put("secretKey", user.getSecretKey());
        } else {
            response.put("success", false);
            response.put("reason", "Unrecognized email-password combination.");
        }
        return response;
    }


    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/requestInvite", method = RequestMethod.GET)
    @ApiMethod(description = "Creates an invitation request.")
    @ResponseBody
    Object requestInvite(
            HttpServletRequest request,
            @ApiQueryParam(description = "Email associated with the account.") @RequestParam(value = "email", required = true) String email,
            @ApiQueryParam(description = "Full name associated with the account") @RequestParam(value = "fullName", required = true) String fullName,
            @ApiQueryParam(description = "Purpose declared by the user for requesting the invitation.") @RequestParam(value = "reason", required = false, defaultValue = "") String reason

    ) {
        HashMap<String, Object> response = new HashMap<>();
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        UserManagement userManagement = new UserManagement(aerospikeClient, aerospikeNamespace);
        userManagement.createInvitationRequest(email, fullName, reason, ipAddress);
        response.put("success", true);
        response.put("id", Math.abs(email.hashCode()));

        return response;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/requestInvite", method = RequestMethod.POST)
    @ApiMethod(description = "Creates an invitation request.")
    @ResponseBody
    Object requestInvitePost(
            HttpServletRequest request,
            @ApiQueryParam(description = "Email associated with the account.") @RequestParam(value = "email", required = true) String email,
            @ApiQueryParam(description = "Full name associated with the account") @RequestParam(value = "fullName", required = true) String fullName,
            @ApiQueryParam(description = "Purpose declared by the user for requesting the invitation.") @RequestParam(value = "reason", required = false, defaultValue = "") String reason

    ) {
        return requestInvite(request, email, fullName, reason);
    }


    @RequestMapping(value = "/showInvites", method = RequestMethod.GET)
    @ResponseBody
    Object requestInvite(
            HttpServletRequest request
    ) {
        if (!securityOverride.equals(request.getParameter("authorizationKey")))
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        HashMap<String, Object> response = new HashMap<>();
        UserManagement userManagement = new UserManagement(aerospikeClient, aerospikeNamespace);

        response.put("success", true);
        response.put("payload", userManagement.getAllInvites());

        return response;
    }
}