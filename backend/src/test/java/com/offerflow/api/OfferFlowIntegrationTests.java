package com.offerflow.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerflow.api.application.ApplicationStatus;
import com.offerflow.api.application.JobApplication;
import com.offerflow.api.application.JobApplicationRepository;
import com.offerflow.api.interview.Interview;
import com.offerflow.api.interview.InterviewRepository;
import com.offerflow.api.interview.InterviewResult;
import com.offerflow.api.user.User;
import com.offerflow.api.user.UserRepository;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:offerflow-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class OfferFlowIntegrationTests {
    private static final String PASSWORD = "Password123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private JobApplicationRepository applications;
    @Autowired private InterviewRepository interviews;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearDatabase() {
        interviews.deleteAll();
        applications.deleteAll();
        users.deleteAll();
    }

    @Test
    void applicationStartsAndHealthEndpointResponds() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.service", is("offerflow-api")));
    }

    @Test
    void userCanRegisterAndDuplicateUsernameIsRejected() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "username", "student01",
                "email", "student01@example.com",
                "password", PASSWORD));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("student01")));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationRejectsEmailAlreadyUsedByAnotherUsername() throws Exception {
        String firstRequest = objectMapper.writeValueAsString(Map.of(
                "username", "email-owner",
                "email", "shared@example.com",
                "password", PASSWORD));
        String secondRequest = objectMapper.writeValueAsString(Map.of(
                "username", "email-other",
                "email", "shared@example.com",
                "password", PASSWORD));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(firstRequest))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(secondRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Username or email is already in use")));
        org.junit.jupiter.api.Assertions.assertEquals(1, users.count());
    }

    @Test
    void protectedEndpointsRejectAnonymousAndInvalidCredentials() throws Exception {
        createUser("auth-user");

        mvc.perform(get("/api/applications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/applications").with(auth("missing-user")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/applications").with(request -> {
                    request.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                            "auth-user:WrongPassword".getBytes(StandardCharsets.UTF_8)));
                    return request;
                }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanReadOwnProfileWithoutPasswordHash() throws Exception {
        createUser("profile-user");

        mvc.perform(get("/api/auth/me").with(auth("profile-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("profile-user")))
                .andExpect(jsonPath("$.email", is("profile-user@example.com")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void userCanCreateReadUpdateAndDeleteApplication() throws Exception {
        createUser("owner");
        long id = createApplication("owner");

        mvc.perform(get("/api/applications/{id}", id).with(auth("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company", is("Example Co")));

        mvc.perform(put("/api/applications/{id}", id)
                        .with(auth("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson("Updated Co", "Developer", "APPLIED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company", is("Updated Co")));

        mvc.perform(delete("/api/applications/{id}", id).with(auth("owner")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/applications/{id}", id).with(auth("owner")))
                .andExpect(status().isNotFound());
    }

    @Test
    void negativeSalaryIsRejected() throws Exception {
        createUser("salary-user");

        mvc.perform(post("/api/applications")
                        .with(auth("salary-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company":"Example Co","position":"Developer","status":"SAVED","salaryMin":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.details.salaryMin").exists());
    }

    @Test
    void minimumSalaryGreaterThanMaximumIsRejected() throws Exception {
        createUser("salary-range-user");

        mvc.perform(post("/api/applications").with(auth("salary-range-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company":"Example Co","position":"Developer","status":"APPLIED","salaryMin":120,"salaryMax":100}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Minimum salary cannot be greater than maximum salary")));
        org.junit.jupiter.api.Assertions.assertEquals(0, applications.count());
    }

    @Test
    void malformedApplicationPayloadsReturnBadRequest() throws Exception {
        createUser("malformed-user");

        mvc.perform(post("/api/applications").with(auth("malformed-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\":\"Example\",\"position\":\"Developer\",\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/applications").with(auth("malformed-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\":\"Example\",\"position\":\"Developer\","))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/applications").with(auth("malformed-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company":"Example","position":"Developer","status":"APPLIED","appliedAt":"not-a-date"}
                                """))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(0, applications.count());
    }

    @Test
    void requiredAndLengthValidationRejectInvalidApplicationFields() throws Exception {
        createUser("validation-user");

        mvc.perform(post("/api/applications").with(auth("validation-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company":"   ","position":"Developer"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.company").exists())
                .andExpect(jsonPath("$.details.status").exists());

        String oversizedNote = "x".repeat(2001);
        mvc.perform(post("/api/applications").with(auth("validation-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company":"Example","position":"Developer","status":"APPLIED","note":"%s"}
                                """.formatted(oversizedNote)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.note").exists());
        org.junit.jupiter.api.Assertions.assertEquals(0, applications.count());
    }

    @Test
    void requiredAndLengthValidationRejectInvalidInterviewFields() throws Exception {
        User owner = createUser("interview-validation-user");
        JobApplication application = saveApplication(owner, "Example", "Developer", ApplicationStatus.INTERVIEWING);

        mvc.perform(post("/api/applications/{applicationId}/interviews", application.getId())
                        .with(auth("interview-validation-user")).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.roundNumber").exists())
                .andExpect(jsonPath("$.details.result").exists());

        String oversizedReflection = "x".repeat(10001);
        mvc.perform(post("/api/applications/{applicationId}/interviews", application.getId())
                        .with(auth("interview-validation-user")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundNumber":1,"result":"PENDING","reflection":"%s"}
                                """.formatted(oversizedReflection)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.reflection").exists());
        org.junit.jupiter.api.Assertions.assertEquals(0, interviews.count());
    }

    @Test
    void userCannotReadUpdateOrDeleteAnotherUsersApplication() throws Exception {
        createUser("owner");
        createUser("other");
        long id = createApplication("owner");
        var otherUser = auth("other");

        mvc.perform(get("/api/applications/{id}", id).with(otherUser)).andExpect(status().isNotFound());
        mvc.perform(put("/api/applications/{id}", id).with(otherUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson("Stolen", "Developer", "OFFER")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/applications/{id}", id).with(otherUser)).andExpect(status().isNotFound());

        mvc.perform(get("/api/applications/{id}", id).with(auth("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company", is("Example Co")));
    }

    @Test
    void missingApplicationResourcesReturnNotFoundForReadUpdateAndDelete() throws Exception {
        createUser("missing-application-user");
        long missingId = 987654321L;

        mvc.perform(get("/api/applications/{id}", missingId).with(auth("missing-application-user")))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/applications/{id}", missingId).with(auth("missing-application-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson("Example", "Developer", "APPLIED")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/applications/{id}", missingId).with(auth("missing-application-user")))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingInterviewResourcesReturnNotFoundForReadUpdateAndDelete() throws Exception {
        User owner = createUser("missing-interview-user");
        JobApplication application = saveApplication(owner, "Example", "Developer", ApplicationStatus.INTERVIEWING);
        long missingId = 987654321L;

        mvc.perform(put("/api/applications/{applicationId}/interviews/{id}", application.getId(), missingId)
                        .with(auth("missing-interview-user")).contentType(MediaType.APPLICATION_JSON)
                        .content(interviewJson(1, "NOT FOUND")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/applications/{applicationId}/interviews/{id}", application.getId(), missingId)
                        .with(auth("missing-interview-user")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/applications/{applicationId}/interviews", missingId)
                        .with(auth("missing-interview-user")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/applications/{applicationId}/interviews", application.getId())
                        .with(auth("missing-interview-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.empty()));
    }

    @Test
    void applicationListIsIsolatedAndSupportsSearchStatusAndPagination() throws Exception {
        User owner = createUser("list-owner");
        User other = createUser("list-other");
        saveApplication(owner, "Acme Systems", "Engineer", ApplicationStatus.APPLIED);
        saveApplication(owner, "Acme Labs", "Analyst", ApplicationStatus.OFFER);
        saveApplication(owner, "Other Corp", "Engineer", ApplicationStatus.SAVED);
        saveApplication(other, "Acme Systems", "Engineer", ApplicationStatus.APPLIED);

        mvc.perform(get("/api/applications").with(auth("list-owner"))
                        .param("page", "0").param("size", "2").param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[0].company", is("Acme Systems")))
                .andExpect(jsonPath("$.content[1].company", is("Acme Labs")));

        mvc.perform(get("/api/applications").with(auth("list-owner"))
                        .param("page", "1").param("size", "2").param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.number", is(1)))
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].company", is("Other Corp")));

        mvc.perform(get("/api/applications").with(auth("list-owner"))
                        .param("keyword", "acme").param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].company", is("Acme Systems")))
                .andExpect(jsonPath("$.content[1].company", is("Acme Labs")));

        mvc.perform(get("/api/applications").with(auth("list-owner"))
                        .param("status", "OFFER").param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].company", is("Acme Labs")))
                .andExpect(jsonPath("$.content[0].status", is("OFFER")));

        mvc.perform(get("/api/applications").with(auth("list-owner"))
                        .param("keyword", "ACME").param("status", "OFFER").param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].company", is("Acme Labs")));
    }

    @Test
    void ownerCanManageInterviewButAnotherUserCannotAccessIt() throws Exception {
        User owner = createUser("owner");
        createUser("other");
        JobApplication application = applications.save(new JobApplication(owner.getId(), "Example Co", "Developer", "Foshan", "Referral", ApplicationStatus.INTERVIEWING, LocalDate.now(), null, null, ""));
        long interviewId = createInterview("owner", application.getId());
        var ownerAuth = auth("owner");
        var otherAuth = auth("other");

        mvc.perform(get("/api/applications/{applicationId}/interviews", application.getId()).with(ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roundNumber", is(1)));

        mvc.perform(put("/api/applications/{applicationId}/interviews/{id}", application.getId(), interviewId)
                        .with(ownerAuth).contentType(MediaType.APPLICATION_JSON)
                        .content(interviewJson(1, "UPDATED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", is("UPDATED")));

        mvc.perform(get("/api/applications/{applicationId}/interviews", application.getId()).with(otherAuth))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/applications/{applicationId}/interviews", application.getId()).with(otherAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(interviewJson(2, "UNAUTHORIZED")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/applications/{applicationId}/interviews/{id}", application.getId(), interviewId).with(otherAuth))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/applications/{applicationId}/interviews/{id}", application.getId(), interviewId)
                        .with(otherAuth).contentType(MediaType.APPLICATION_JSON)
                        .content(interviewJson(1, "TAMPERED")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/applications/{applicationId}/interviews", application.getId()).with(ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questions", is("UPDATED")));

        mvc.perform(delete("/api/applications/{applicationId}/interviews/{id}", application.getId(), interviewId).with(ownerAuth))
                .andExpect(status().isNoContent());
    }

    @Test
    void duplicateInterviewRoundIsRejected() throws Exception {
        User owner = createUser("round-owner");
        JobApplication application = applications.save(new JobApplication(owner.getId(), "Example Co", "Developer", "Foshan", "Referral", ApplicationStatus.INTERVIEWING, LocalDate.now(), null, null, ""));
        createInterview("round-owner", application.getId());

        mvc.perform(post("/api/applications/{applicationId}/interviews", application.getId())
                        .with(auth("round-owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(interviewJson(1, "DUPLICATE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Interview round already exists")));
    }

    @Test
    void dashboardReportsOwnStatusCountsAndOfferConversionRate() throws Exception {
        User owner = createUser("dashboard-owner");
        User other = createUser("dashboard-other");
        saveApplication(owner, "Saved Co", "Engineer", ApplicationStatus.SAVED);
        saveApplication(owner, "Applied Co", "Engineer", ApplicationStatus.APPLIED);
        saveApplication(owner, "Test Co", "Engineer", ApplicationStatus.WRITTEN_TEST);
        saveApplication(owner, "Interview Co", "Engineer", ApplicationStatus.INTERVIEWING);
        saveApplication(owner, "Offer Co", "Engineer", ApplicationStatus.OFFER);
        saveApplication(owner, "Rejected Co", "Engineer", ApplicationStatus.REJECTED);
        saveApplication(other, "Other Offer", "Engineer", ApplicationStatus.OFFER);

        mvc.perform(get("/api/dashboard").with(auth("dashboard-owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(6)))
                .andExpect(jsonPath("$.byStatus.SAVED", is(1)))
                .andExpect(jsonPath("$.byStatus.APPLIED", is(1)))
                .andExpect(jsonPath("$.byStatus.WRITTEN_TEST", is(1)))
                .andExpect(jsonPath("$.byStatus.INTERVIEWING", is(1)))
                .andExpect(jsonPath("$.byStatus.OFFER", is(1)))
                .andExpect(jsonPath("$.byStatus.REJECTED", is(1)))
                .andExpect(jsonPath("$.offerConversionRate", is(20.0)));
    }

    @Test
    void dashboardReturnsZeroCountsAndConversionRateWhenUserHasNoApplications() throws Exception {
        createUser("empty-dashboard");
        User other = createUser("populated-dashboard");
        saveApplication(other, "Other Offer", "Engineer", ApplicationStatus.OFFER);

        mvc.perform(get("/api/dashboard").with(auth("empty-dashboard")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(0)))
                .andExpect(jsonPath("$.byStatus.SAVED", is(0)))
                .andExpect(jsonPath("$.byStatus.OFFER", is(0)))
                .andExpect(jsonPath("$.offerConversionRate", is(0.0)));
    }

    private User createUser(String username) {
        return users.save(new User(username, username + "@example.com", passwordEncoder.encode(PASSWORD)));
    }

    private JobApplication saveApplication(User user, String company, String position, ApplicationStatus status) {
        return applications.save(new JobApplication(user.getId(), company, position, "Foshan", "Referral", status,
                LocalDate.now(), null, null, "test"));
    }

    private long createApplication(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/applications")
                        .with(auth(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson("Example Co", "Developer", "SAVED")))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createInterview(String username, long applicationId) throws Exception {
        MvcResult result = mvc.perform(post("/api/applications/{applicationId}/interviews", applicationId)
                        .with(auth(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(interviewJson(1, "QUESTION")))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String applicationJson(String company, String position, String status) {
        return """
                {"company":"%s","position":"%s","city":"Foshan","source":"Referral","status":"%s","appliedAt":"2026-10-18","note":"test"}
                """.formatted(company, position, status);
    }

    private RequestPostProcessor auth(String username) {
        String value = Base64.getEncoder().encodeToString((username + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
        return request -> { request.addHeader("Authorization", "Basic " + value); return request; };
    }

    private String interviewJson(int round, String questions) {
        return """
                {"roundNumber":%d,"result":"PENDING","questions":"%s","reflection":"review"}
                """.formatted(round, questions);
    }
}
