package com.pafolder.graduation.controller;

import com.pafolder.graduation.model.Vote;
import com.pafolder.graduation.util.ControllerUtil;
import com.pafolder.graduation.util.DateTimeUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static com.pafolder.graduation.TestData.*;
import static com.pafolder.graduation.controller.VoteController.NO_MENU_RESTAURANT_FOUND;
import static com.pafolder.graduation.controller.VoteController.REST_URL;
import static com.pafolder.graduation.util.DateTimeUtil.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VoteControllerTest extends AbstractControllerTest {
    @Test
    void createVote() throws Exception {
        DateTimeUtil.setCurrentTimeForTests(CURRENT_TIME_BEFORE_VOTING_TIME_LIMIT);
        mockMvc.perform(MockMvcRequestBuilders.post(REST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic(admin.getEmail(), admin.getPassword()))
                        .param("restaurantId", Integer.toString(RESTAURANT_ID_FOR_FIRST_VOTE)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    void updateVote() throws Exception {
        DateTimeUtil.setCurrentTimeForTests(CURRENT_TIME_BEFORE_VOTING_TIME_LIMIT);
        mockMvc.perform(MockMvcRequestBuilders.put(REST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic(user.getEmail(), user.getPassword()))
                        .param("restaurantId", Integer.toString(RESTAURANT_ID_FOR_SECOND_VOTE)))
                .andDo(print())
                .andExpect(status().isNoContent());
        Optional<Vote> updatedVote = voteRepository.findByDateAndUser(LocalDate.now(), user);
        Assertions.assertTrue(updatedVote.map(value -> value.getMenu()
                .getRestaurant().getId().equals(RESTAURANT_ID_FOR_SECOND_VOTE)).orElse(false));
    }

    @Test
    void sendVoteUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(
                                REST_URL + "/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("restaurantId", "0"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendVoteTooLate() throws Exception {
        DateTimeUtil.setCurrentTimeForTests(CURRENT_TIME_AFTER_VOTING_TIME_LIMIT);
        Assertions.assertTrue(mockMvc.perform(MockMvcRequestBuilders.post(REST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic(user.getEmail(), user.getPassword()))
                        .param("restaurantId", Integer.toString(RESTAURANT_ID_FOR_FIRST_VOTE)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .matches(".*" + ControllerUtil.TOO_LATE_TO_VOTE + ".*"));
    }

    @Test
    void sendVoteForNonexistentRestaurant() throws Exception {
        setCurrentTimeForTests(CURRENT_TIME_BEFORE_VOTING_TIME_LIMIT);
        Assertions.assertTrue(mockMvc.perform(MockMvcRequestBuilders.post(REST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic(user.getEmail(), user.getPassword()))
                        .param("restaurantId", NONEXISTENT_ID_STRING))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .matches(".*" + NO_MENU_RESTAURANT_FOUND + ".*"));
    }

    @Test
    void deleteVote() throws Exception {
        setCurrentTimeForTests(CURRENT_TIME_BEFORE_VOTING_TIME_LIMIT);
        mockMvc.perform(MockMvcRequestBuilders.delete(REST_URL).contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic(user.getEmail(), user.getPassword())))
                .andDo(print())
                .andExpect(status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get(REST_URL).contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic(user.getEmail(), user.getPassword()))
                )
                .andDo(print())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deleteVoteTooLate() throws Exception {
        LocalTime currentTime = getCurrentTime();
        setCurrentTimeForTests(CURRENT_TIME_AFTER_VOTING_TIME_LIMIT);
        Assertions.assertTrue(mockMvc.perform(MockMvcRequestBuilders.delete(REST_URL).contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic(user.getEmail(), user.getPassword())))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .matches(".*" + ControllerUtil.TOO_LATE_TO_VOTE + ".*"));
        setCurrentTimeForTests(currentTime);
    }
}
