package com.jackpotsaver.bot.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackpotsaver.bot.domain.AdMediaType;
import com.jackpotsaver.bot.service.AdminConversationService;
import com.jackpotsaver.bot.service.AdminService;
import com.jackpotsaver.bot.service.MediaContent;
import com.jackpotsaver.bot.service.MessageCatalog;
import com.jackpotsaver.bot.service.UpdateOrchestrator;
import com.jackpotsaver.bot.service.UserService;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class TelegramUpdateHandlerMediaTest {
    private final TelegramUpdateHandler handler = new TelegramUpdateHandler(
            mock(UserService.class),
            mock(MessageCatalog.class),
            mock(TelegramApiClient.class),
            mock(UpdateOrchestrator.class),
            mock(AdminService.class),
            mock(AdminConversationService.class));
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void extractsLargestPhotoAndCaption() throws Exception {
        MediaContent content = content("""
                {
                  "caption": "Advertising caption",
                  "photo": [
                    {"file_id": "small"},
                    {"file_id": "large"}
                  ]
                }
                """);

        assertThat(content).isEqualTo(
                new MediaContent(AdMediaType.PHOTO, "large", "Advertising caption"));
    }

    @Test
    void extractsVideoAndCaption() throws Exception {
        MediaContent content = content("""
                {
                  "caption": "Video caption",
                  "video": {"file_id": "video-file"}
                }
                """);

        assertThat(content).isEqualTo(
                new MediaContent(AdMediaType.VIDEO, "video-file", "Video caption"));
    }

    private MediaContent content(String json) throws Exception {
        Method method = TelegramUpdateHandler.class.getDeclaredMethod(
                "mediaContent", com.fasterxml.jackson.databind.JsonNode.class);
        method.setAccessible(true);
        return (MediaContent) method.invoke(handler, mapper.readTree(json));
    }
}
