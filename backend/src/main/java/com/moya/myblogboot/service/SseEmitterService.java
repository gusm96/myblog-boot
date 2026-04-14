package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.event.PostChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitterService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private static final long EMITTER_TIMEOUT = 30 * 60 * 1000L; // 30분

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
            log.warn("SSE 초기 연결 이벤트 전송 실패", e);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostChange(PostChangeEvent event) {
        broadcast("POST_CHANGED", event.getChangeType());
    }

    @Scheduled(fixedRate = 30_000)
    public void heartbeat() {
        broadcast("HEARTBEAT", "ping");
    }

    private void broadcast(String eventName, String data) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
