package com.example.hackathoncodaro2026;

import com.example.hackathoncodaro2026.dto.RegistrationRequest;
import com.example.hackathoncodaro2026.dto.ReservationRequest;
import com.example.hackathoncodaro2026.model.Reservation;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.PaymentMethod;
import com.example.hackathoncodaro2026.repository.SportResourceRepository;
import com.example.hackathoncodaro2026.repository.UserRepository;
import com.example.hackathoncodaro2026.service.ReservationService;
import com.example.hackathoncodaro2026.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationEditWebTests {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SportResourceRepository sportResourceRepository;

    @Test
    void ownerCanGetAndPostPendingEdit() throws Exception {
        User owner = player("edit_web_owner", "edit.web.owner@example.com");
        SportResource court = exclusiveCourt();
        LocalDate date = LocalDate.now(WARSAW).plusDays(54);
        LocalTime start = court.getOpeningTime();
        Reservation created = reservationService.create(owner, request(court, date, start, 1));
        mockMvc.perform(get("/reservations/" + created.getId() + "/edit").with(user("edit_web_owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Only pending reservations can be changed")))
                .andExpect(content().string(containsString("Save changes")))
                .andExpect(content().string(containsString("Current amount")));
        mockMvc.perform(editPost(created, date, start.plusHours(1), 1)
                        .with(user("edit_web_owner").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"));
    }

    @Test
    void otherUserCannotOpenOrPostEdit() throws Exception {
        User owner = player("edit_web_keep", "edit.web.keep@example.com");
        player("edit_web_intruder", "edit.web.intruder@example.com");
        SportResource court = exclusiveCourt();
        LocalDate date = LocalDate.now(WARSAW).plusDays(55);
        Reservation created = reservationService.create(owner, request(court, date, court.getOpeningTime(), 1));
        mockMvc.perform(get("/reservations/" + created.getId() + "/edit").with(user("edit_web_intruder").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"));
        mockMvc.perform(editPost(created, date, court.getOpeningTime().plusHours(2), 1)
                        .with(user("edit_web_intruder").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"));
    }

    @Test
    void confirmedAndPastHaveNoEditButtonAndDirectGetIsRejected() throws Exception {
        User owner = player("edit_web_history", "edit.web.history@example.com");
        User manager = userRepository.findByUsernameIgnoreCase("manager").orElseThrow();
        SportResource court = exclusiveCourt();
        LocalDate date = LocalDate.now(WARSAW).plusDays(56);
        Reservation pending = reservationService.create(owner, request(court, date, court.getOpeningTime(), 1));
        Reservation confirmed = reservationService.create(owner, request(court, date, court.getOpeningTime().plusHours(2), 1));
        reservationService.confirm(manager, confirmed.getId());
        mockMvc.perform(get("/reservations").with(user("edit_web_history").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Only pending reservations can be changed")))
                .andExpect(content().string(containsString("/reservations/" + pending.getId() + "/edit")))
                .andExpect(content().string(not(containsString("/reservations/" + confirmed.getId() + "/edit"))));
        mockMvc.perform(get("/reservations/" + confirmed.getId() + "/edit").with(user("edit_web_history").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"));
        mockMvc.perform(editPost(confirmed, date, court.getOpeningTime(), 2)
                        .with(user("edit_web_history").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"));
    }

    @Test
    void notificationsPageIsOwnerOnlyAndQuoteStillWorks() throws Exception {
        User owner = player("edit_web_notice", "edit.web.notice@example.com");
        player("edit_web_notice_other", "edit.web.notice.other@example.com");
        SportResource court = exclusiveCourt();
        LocalDate date = LocalDate.now(WARSAW).plusDays(57);
        Reservation created = reservationService.create(owner, request(court, date, court.getOpeningTime(), 1));
        reservationService.update(owner, created.getId(), request(court, date, court.getOpeningTime().plusHours(1), 1));
        mockMvc.perform(get("/notifications").with(user("edit_web_notice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reservation updated")))
                .andExpect(content().string(containsString("Notices")));
        mockMvc.perform(get("/notifications").with(user("edit_web_notice_other").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No notices yet")));
        mockMvc.perform(get("/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/reservations/" + created.getId() + "/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/resources/" + court.getId() + "/quote")
                        .param("date", date.toString())
                        .param("start", "10:00")
                        .param("durationHours", "1")
                        .with(user("edit_web_notice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("amount")));
        mockMvc.perform(post("/notifications/read-all")
                        .with(user("edit_web_notice").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));
    }

    @Test
    void invalidEditRedisplaysForm() throws Exception {
        User owner = player("edit_web_invalid", "edit.web.invalid@example.com");
        SportResource court = exclusiveCourt();
        LocalDate date = LocalDate.now(WARSAW).plusDays(58);
        Reservation created = reservationService.create(owner, request(court, date, court.getOpeningTime(), 1));
        mockMvc.perform(editPost(created, date, LocalTime.of(21, 0), 4)
                        .with(user("edit_web_invalid").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Save changes")))
                .andExpect(content().string(containsString("opening hours")));
    }

    private User player(String username, String email) {
        return userRepository.findByUsernameIgnoreCase(username).orElseGet(() -> {
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername(username);
            request.setEmail(email);
            request.setFullName("Player " + username);
            request.setPhone("+48 555 010 040");
            request.setPassword("Password1");
            request.setConfirmPassword("Password1");
            return userService.register(request);
        });
    }

    private SportResource exclusiveCourt() {
        return sportResourceRepository.findAll().stream()
                .filter(resource -> resource.getCapacity() == 1 && resource.isEnabled())
                .findFirst()
                .orElseThrow();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder editPost(
            Reservation reservation,
            LocalDate date,
            LocalTime startTime,
            int durationHours
    ) {
        var builder = post("/reservations/" + reservation.getId() + "/edit")
                .param("resourceId", String.valueOf(reservation.getResource().getId()))
                .param("date", date.toString())
                .param("startTime", startTime.toString())
                .param("durationHours", String.valueOf(durationHours))
                .param("paymentMethod", reservation.getPaymentMethod().name())
                .param("kind", reservation.getKind().name())
                .param("partySize", String.valueOf(reservation.getPartySize()));
        if (reservation.getSkillLevel() != null) {
            builder.param("skillLevel", reservation.getSkillLevel());
        }
        if (reservation.getCoach() != null) {
            builder.param("coachId", String.valueOf(reservation.getCoach().getId()));
        }
        return builder;
    }

    private ReservationRequest request(SportResource resource, LocalDate date, LocalTime startTime, int durationHours) {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(resource.getId());
        request.setDate(date);
        request.setStartTime(startTime);
        request.setDurationHours(durationHours);
        request.setPaymentMethod(PaymentMethod.CASH);
        if (resource.requiresPartySize()) {
            request.setPartySize(resource.getMinPartySize());
        }
        return request;
    }
}
