package com.nicapz.gym.Functions;

import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class MoodService implements Function<MoodService.Request, MoodService.Response> {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final UserContext userContext;

    @Autowired
    public MoodService(SimpMessagingTemplate simpMessagingTemplate, UserContext userContext) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.userContext = userContext;
    }


    @Override
    public Response apply(Request request) {

        System.out.println(request);

        String consequence = switch (request.userMood) {
            case 1 -> "This is really bad. Be extremely sensitive. Suggest numbers for helplines.";
            case 2 -> "This is bad. Be encouraging.";
            case 3 -> "The user seems to be fine, but not bubbling. Dont make too many jokes, just be professional.";
            case 4 -> "The user is feeling pretty good today. Try to match their vibes but keep it professional.";
            case 5 -> "The user seems ecstatic, suggest they play the clicker game they can now see.";
            default -> "";
        };
        Response response = new Response(new MoodInfo(request.userMood, consequence));

        System.out.println(response);

        String userDestination = "/topic/moodUpdates/" + userContext.getUserId();
        simpMessagingTemplate.convertAndSend("/topic/moodUpdates/" + userContext.getUserId(), response.moodInfo.userMoodNumerical);
        System.out.println("Destination: " + userDestination + " mood: " + response.moodInfo.userMoodNumerical);

        simpMessagingTemplate.convertAndSend("/topic/game/", "game should start");

        return response;
    }

    public record Request(int userMood) {
    }

    public record Response(MoodInfo moodInfo) {
    }

    public record MoodInfo(int userMoodNumerical, String consequence) {
    }
}
