package com.devhub.ocr.QA.A0.QAA0_0101.mod;

import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QAA0_0101BotServiceTest {

    @Mock
    DatabasePlugin db;

    @InjectMocks
    QAA0_0101BotService svc;

    @Captor
    ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Test
    void createBot_should_call_db_execute_with_expected_params() {
        when(db.execute(any(), any())).thenReturn(1);

        int res = svc.createBot("bot123", "secret", null, "https://example.com/cb", "desc");

        assertThat(res).isEqualTo(1);
        verify(db, times(1)).execute(startsWith("INSERT INTO qaa0_bot_configs"), mapCaptor.capture());
        Map<String,Object> params = mapCaptor.getValue();
        assertThat(params.get("bot_id")).isEqualTo("bot123");
        assertThat(params.get("token")).isEqualTo("secret");
        assertThat(params.get("callback_url")).isEqualTo("https://example.com/cb");
    }

    @Test
    void persistSendHistory_should_extract_file_id_from_response() {
        when(db.execute(any(), any())).thenReturn(1);

        String response = "{\"ok\":true, \"result\":{\"file_id\":\"ABC123\"}}";
        int r = svc.persistSendHistory("bot123", "chat1", "http://file.url/file.pdf", "caption", response, 200);

        assertThat(r).isEqualTo(1);
        verify(db, times(1)).execute(startsWith("INSERT INTO qaa0_bot_send_history"), mapCaptor.capture());
        Map<String,Object> params = mapCaptor.getValue();
        assertThat(params.get("file_id")).isEqualTo("ABC123");
    }

}
